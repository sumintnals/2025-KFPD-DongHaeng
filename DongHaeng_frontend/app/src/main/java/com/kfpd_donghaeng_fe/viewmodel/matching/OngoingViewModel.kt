package com.kfpd_donghaeng_fe.viewmodel.matching

import android.location.Location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kfpd_donghaeng_fe.data.local.TokenLocalDataSource
import com.kfpd_donghaeng_fe.data.location.LocationTracker
import com.kfpd_donghaeng_fe.domain.entity.auth.UserType
import com.kfpd_donghaeng_fe.domain.entity.matching.OngoingEntity
import com.kfpd_donghaeng_fe.domain.entity.matching.OngoingRequestEntity
// 💡 누락된 Flow 관련 Import 추가
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.kfpd_donghaeng_fe.domain.entity.RoutePoint
import com.kfpd_donghaeng_fe.domain.entity.WalkingRoute
import com.kfpd_donghaeng_fe.data.remote.socket.SocketManager
import com.kfpd_donghaeng_fe.data.repository.MatchRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.kfpd_donghaeng_fe.domain.entity.LocationType
import com.kfpd_donghaeng_fe.domain.entity.RouteLocation
import kotlinx.coroutines.flow.StateFlow

// -----------------------------------------------------------
// 1. 일회성 내비게이션 이벤트를 위한 Sealed Class 정의
// -----------------------------------------------------------
//sealed class OngoingUiEvent {
//    object NavigateToReview : OngoingUiEvent()
//    // object ShowErrorMessage : OngoingUiEvent()
//}

// -----------------------------------------------------------
// 2. OngoingViewModel 클래스를 단일 정의
// -----------------------------------------------------------


@HiltViewModel
class OngoingViewModel @Inject constructor(
    private val socketManager: SocketManager,
    private val tokenDataSource: TokenLocalDataSource,
    private val matchRepository: MatchRepositoryImpl,
    private val locationTracker: LocationTracker
    // private val locationClient: FusedLocationProviderClient (위치 수집용, Hilt 주입 필요)
): ViewModel(){

    // A. UI 상태 (State) 관리 (화면 렌더링을 위한 데이터)
    private val _uiState = MutableStateFlow(OngoingEntity())
    val uiState = _uiState.asStateFlow()

    private val _uiState2 = MutableStateFlow(OngoingRequestEntity())
    val uiState2 = _uiState2.asStateFlow()

    // B. 일회성 이벤트 (Event) 관리 (화면 전환, Snackbar 표시 등)
    private val _eventFlow = MutableSharedFlow<OngoingUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()



    // --- UI State 변경 함수 ---

    fun nextPage() {
        if (_uiState.value.OngoingPage < 2) {
            _uiState.update { it.copy(OngoingPage = it.OngoingPage + 1) }
        }
    }

    fun previousPage() {
        if (_uiState.value.OngoingPage > 0) {
            _uiState.update { it.copy(OngoingPage = it.OngoingPage - 1) }
        }
    }



    // --- One-shot Event 발행 함수 ---

    /**
     * 리뷰 화면으로 이동 요청 이벤트를 발행합니다.
     * 이 함수는 Route/Screen에서 구독됩니다.
     */
//    fun NavigateToReview() {
//        viewModelScope.launch {
//            _eventFlow.emit(OngoingUiEvent.NavigateToReview)
//        }
//    }

    fun NavigateToReview(timeMin: Int, earnedPoints: Int) {
        viewModelScope.launch {
            // 거리 변환 (기존 유지)
            val distanceStr = if (_totalDistanceMeters < 1000) {
                "${_totalDistanceMeters.toInt()}m"
            } else {
                String.format("%.1fkm", _totalDistanceMeters / 1000)
            }

            val timeStr = "${timeMin}분"

            _eventFlow.emit(
                OngoingUiEvent.NavigateToReview(
                    matchId = currentMatchId,
                    partnerId = partnerId,
                    totalTime = timeStr,
                    distance = distanceStr,
                )
            )
        }
    }

    // 💡 참고: 기존 NavigateToReview 함수는 이벤트 발행 로직과 중복되므로 제거하거나 이름을 변경해야 합니다.
    // fun NavigateToReview(){
    //     viewModelScope.launch {
    //         _navigationEvent.emit("review") // _navigationEvent 미정의 오류 발생 지점
    //     }
    // }


    // 📍 지도 데이터

    // 누적 이동 거리 (미터 단위)
    private var _totalDistanceMeters = 0.0

    // 이전 위치 저장용
    private var lastLocation: Location? = null

    private val _mapMarkers = MutableStateFlow<List<RouteLocation>>(emptyList())
    val mapMarkers = _mapMarkers.asStateFlow()

    private val _routePath = MutableStateFlow<WalkingRoute?>(null)
    val routePath = _routePath.asStateFlow()

    // 🏁 도착 및 종료 상태
    private val _remainingDistance = MutableStateFlow("계산 중...")
    val remainingDistance = _remainingDistance.asStateFlow()

    private val _isArrived = MutableStateFlow(false)
    val isArrived = _isArrived.asStateFlow()

    // 📝 리뷰 작성을 위한 데이터 저장
    private var currentMatchId: Long = -1
    private var partnerId: Long = -1
    private var myUserType: UserType = UserType.NEEDY

    // 도착지 좌표
    private var destLat: Double = 0.0
    private var destLng: Double = 0.0



    init {
        viewModelScope.launch {
            val typeString = tokenDataSource.getUserType()
            myUserType = if (typeString == "helper") UserType.HELPER else UserType.NEEDY
            _uiState.update { currentState ->
                currentState.copy(userType = myUserType)
            }
        }
    }



    private fun connectSocket() {
        viewModelScope.launch {
            val token = tokenDataSource.getToken()
            if (token != null) {
                socketManager.connect(token)
            } else {
                // 토큰 없음 에러 처리
            }
        }
    }

    private fun checkArrival(currentLat: Double, currentLng: Double) {
        // 도착지 정보가 아직 로드되지 않았으면 계산 중단
        if (destLat == 0.0 || destLng == 0.0) return

        val results = FloatArray(1)
        // 현재 위치와 목적지 사이의 거리(미터) 계산
        Location.distanceBetween(currentLat, currentLng, destLat, destLng, results)
        val distanceInMeters = results[0]

        // 남은 거리 UI 업데이트
        _remainingDistance.value = if (distanceInMeters < 1000) {
            "${distanceInMeters.toInt()}m 남음"
        } else {
            String.format("%.1fkm 남음", distanceInMeters / 1000)
        }

        // 50m 이내로 접근하면 도착으로 간주
        if (distanceInMeters < 50) {
            _isArrived.value = true
            // 필요 시 여기서 Toast 메시지나 알림 트리거 이벤트를 보낼 수 있습니다.
        }
    }

    fun startLocationTracking() {
        viewModelScope.launch {
            locationTracker.getLocationFlow() // Flow<Location>
                .collect { location ->
                    // 위치가 들어올 때마다 내 위치 업데이트 로직 실행
                    updateMyLocation(location.latitude, location.longitude)
                }
        }
    }

    // 1. 매칭 정보 로드 & 소켓 연결
    fun loadMatchData(matchId: Long) {
        currentMatchId = matchId
        viewModelScope.launch {
            // API 호출
            matchRepository.getMatchDetail(matchId).onSuccess { data ->
                // 1) 파트너 ID 설정
                partnerId = if (myUserType == UserType.NEEDY) {
                    data.helper?.id ?: -1
                } else {
                    data.requester.id
                }

                // 2) 도착지 설정
                destLat = data.request.destinationLatitude
                destLng = data.request.destinationLongitude

                // 3) 경로 그리기 (DTO -> Domain 변환)
                val routePoints = data.request.route?.points?.map {
                    RoutePoint(it.lng, it.lat) // API DTO에 따라 순서 확인 필요
                } ?: emptyList()

                if (routePoints.isNotEmpty()) {
                    _routePath.value = WalkingRoute(
                        points = routePoints,
                        totalDistance = data.request.route?.totalDistanceMeters ?: 0,
                        totalTime = 0
                    )
                }

                // 4) 소켓 연결
                connectSocket() // 토큰 내부 처리
                socketManager.joinMatch(matchId)

                // 상대방 위치 구독 시작
                launch {
                    socketManager.observePartnerLocation().collect { (lat, lng) ->
                        val partnerType = if (myUserType == UserType.NEEDY) LocationType.COMPANION else LocationType.REQUESTER
                        updateMarker(partnerType, lat, lng)
                    }
                }
            }
        }
    }

    // 2. 내 위치 업데이트 (GPS)
    fun updateMyLocation(lat: Double, lng: Double) {
        // 1. 거리 누적 계산
        val currentLocation = Location("dummy").apply {
            latitude = lat
            longitude = lng
        }

        if (lastLocation != null) {
            // 이전 위치가 있으면 거리 계산해서 더하기
            _totalDistanceMeters += lastLocation!!.distanceTo(currentLocation)
        }
        lastLocation = currentLocation // 현재 위치를 '이전 위치'로 저장

        // 2. 내 마커 업데이트 & 도착 판별 (기존 로직)
        val myMarkerType = if (myUserType == UserType.NEEDY) LocationType.REQUESTER else LocationType.COMPANION
        updateMarker(myMarkerType, lat, lng)

        // 서버로 전송 (MatchId가 있을 때만)
        if (currentMatchId != -1L) {
            socketManager.sendLocation(currentMatchId, lat, lng)
        }

        // 도착 판별
        checkArrival(lat, lng)
    }

    private fun updateMarker(type: LocationType, lat: Double, lng: Double) {
        _mapMarkers.update { list ->
            val newList = list.toMutableList()
            newList.removeIf { it.type == type }
            newList.add(RouteLocation(id = type.name, type = type, placeName = "", address = "", latitude = lat, longitude = lng))
            newList
        }
    }

    private fun calculateDistance(lat: Double, lng: Double) {
        if (destLat == 0.0) return
        val results = FloatArray(1)
        Location.distanceBetween(lat, lng, destLat, destLng, results)
        val distMeters = results[0].toInt()

        _remainingDistance.value = if (distMeters < 1000) "${distMeters}m 남음" else String.format("%.1fkm 남음", distMeters / 1000f)

        // 50m 이내 접근 시 UI 처리 가능
    }
}

sealed class OngoingUiEvent {
    object NavigateAfterQrScan: OngoingUiEvent()
    data class ShowSnackbar(val message: String) : OngoingUiEvent()
    data class NavigateToReview(
        val matchId: Long,
        val partnerId: Long,
        val totalTime: String,
        val distance: String,
    ) : OngoingUiEvent()
}