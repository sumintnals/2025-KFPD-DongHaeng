package com.kfpd_donghaeng_fe.ui.matching.ongoing

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.kfpd_donghaeng_fe.GlobalApplication
import com.kfpd_donghaeng_fe.domain.entity.RouteLocation
import com.kfpd_donghaeng_fe.domain.entity.WalkingRoute
import com.kfpd_donghaeng_fe.domain.entity.auth.UserType
import com.kfpd_donghaeng_fe.domain.entity.matching.OngoingEntity
import com.kfpd_donghaeng_fe.domain.entity.matching.OngoingRequestEntity
import com.kfpd_donghaeng_fe.domain.entity.matching.QREntity
import com.kfpd_donghaeng_fe.domain.entity.matching.QRScanEndEntity
import com.kfpd_donghaeng_fe.domain.entity.matching.QRScanResultEntity
import com.kfpd_donghaeng_fe.domain.entity.matching.QRScandEntity
import com.kfpd_donghaeng_fe.domain.entity.matching.QRScreenUiState
import com.kfpd_donghaeng_fe.domain.entity.matching.QRTypes
import com.kfpd_donghaeng_fe.domain.service.AppSettingsNavigator
import com.kfpd_donghaeng_fe.domain.service.PermissionChecker
import com.kfpd_donghaeng_fe.ui.common.KakaoMapView
import com.kfpd_donghaeng_fe.ui.common.permission.rememberLocationPermissionRequester
import com.kfpd_donghaeng_fe.util.AppScreens
import com.kfpd_donghaeng_fe.viewmodel.matching.OngoingUiEvent
import com.kfpd_donghaeng_fe.viewmodel.matching.OngoingViewModel
import com.kfpd_donghaeng_fe.viewmodel.matching.QRViewModel

// =========================================================================================
// 1. Map Composable
// =========================================================================================

@Composable
fun Background_Map(
    markers: List<RouteLocation>, // 👈 추가: 마커 리스트 받기
    route: WalkingRoute?          // 👈 추가: 경로 정보 받기
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // 지도 로딩 전 흰색 배경
    ) {
        KakaoMapView(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
            // 중심 좌표는 내 위치(REQUESTER or COMPANION)가 있으면 거기로, 없으면 서울 시청 등 기본값
            locationX = markers.firstOrNull()?.longitude ?: 126.9780,
            locationY = markers.firstOrNull()?.latitude ?: 37.5665,

            // 💡 ViewModel에서 받은 데이터 연결
            route = route,
            markers = markers,

            enabled = GlobalApplication.isMapLoaded
        )
    }
}

// =========================================================================================
// 2. Screen (UI 렌더링 전용)
// 💡 오류 수정: ViewModel 인자를 제거하고, 필요한 이벤트 핸들러만 받습니다.
// =========================================================================================

var user: Int = 2// 테스트용 1 = 요청자 2 = 동행자


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngoingScreen(
    uiState: OngoingEntity,
    uiState2: OngoingRequestEntity,
    uiStateqr: QRScreenUiState,
    onScanRequest: (QRScandEntity, QRTypes, Long) -> Unit,
    resultUiState: QRScanResultEntity, // 여기에 스캔 시간
    locateUiState : QRScandEntity, // 스캔 시작 장소
    requestScan: (matchId: Long, qrType: QRTypes) -> Unit,
    nextPage:()->Unit,
    NavigateToReview: () -> Unit // 리뷰 화면 이동 함수를 인자로 받음
    ,
    mapMarkers: List<RouteLocation>,
    routePath: WalkingRoute?,
) {


    // Box 안의 컴포넌트들은 순서대로 쌓입니다 (1 -> 2 -> 3 -> 4)
    Box(modifier = Modifier.fillMaxSize()) {

        Background_Map(
            markers = mapMarkers,
            route = routePath
        )
        // 동행자(user=2)일 경우 QR 코드 시트로 시작
        if (uiState.userType == UserType.NEEDY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                TopSheet(uiState,uiState2)
            }
            val page = uiState.OngoingPage
            // 배경 오버레이
            when(page){
               0,2-> {Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
                   // QR 코드 시트
                   Box(
                       modifier = Modifier
                           .fillMaxSize(),
                       contentAlignment = Alignment.Center
                   ) {
                       QRSheet(uiStateqr,uiState,onScanRequest)
                   }

               }
                else->BottomSheet(

                    uiState = uiState, // BottomSheet이 필요한 경우 상태 전달
                    resultUiState = resultUiState,
                    locateUiState = locateUiState,
                    requestScan=requestScan,
                    nextPage = nextPage,
                    NavigateToReview = NavigateToReview
                )

            }

        }

        // 요청자(user=1)일 경우 하단 시트
        if (uiState.userType == UserType.HELPER) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                TopSheet(uiState,uiState2)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {

                BottomSheet(
                    uiState = uiState, // BottomSheet이 필요한 경우 상태 전달
                    resultUiState = resultUiState,
                    locateUiState = locateUiState,
                    requestScan = requestScan,
                    nextPage = nextPage,
                    NavigateToReview = NavigateToReview
                )
            }

        }
    }
}

// =========================================================================================
// 3. Route (ViewModel/Hilt 연결)
// =========================================================================================

@Composable
fun OngoingRoute(
    viewModel: OngoingViewModel = hiltViewModel(),
    viewModel2: QRViewModel = hiltViewModel(),
    matchId: Long,
    appSettingsNavigator: AppSettingsNavigator,
    permissionChecker: PermissionChecker,
    navController: NavHostController,
) {
    val scannerState by viewModel2.scannerState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val uiState2 by viewModel.uiState2.collectAsState()

    // 💡 수정: Non-null QRScreenUiState 구독
    val qrScreenUiState by viewModel2.uiState.collectAsState()

    val qrEntity = qrScreenUiState.qrEntity

    val locateUiState by viewModel2.locateUiState.collectAsState()
    val resultUiState by viewModel2.resultUiState.collectAsState()

    // 💡 Non-null 상태에서 qrScanned 플래그 추출
    val isScanned = qrScreenUiState.qrEntity.qrScanned
    val ongoingPage = uiState.OngoingPage
    var currentQrType by remember { mutableStateOf(QRTypes.NONE) }
    var currentMatchId by remember { mutableStateOf(0L) }
    val context = LocalContext.current

    // 1. QRViewModel 이벤트 구독
    LaunchedEffect(key1 = Unit) {
        viewModel2.eventFlow.collect { event ->
            when (event) {
                // QRViewModel에서 발행한 페이지 이동 요청 이벤트 처리
                is OngoingUiEvent.NavigateAfterQrScan -> {
                    // 🎯 [핵심] nextPage() 실행
                    viewModel.nextPage()
                    Log.d("QR_NAV", "QR Scan 성공 이벤트 수신 -> OngoingViewModel.nextPage() 실행")
                }
                else -> { }
            }
        }
    }

    // ✅ [수정] uiState3.qrScanned -> qrEntity.qrScanned 로 변경
    LaunchedEffect(qrEntity.qrScanned) {
        if (qrEntity.qrScanned) {
            // 1. 페이지 넘기기
            viewModel.nextPage()

            // ✅ [수정] uiState3.qrType -> qrEntity.qrType 로 변경
            if (qrEntity.qrType == QRTypes.END && resultUiState is QRScanEndEntity) {
                val result = resultUiState as QRScanEndEntity

                viewModel.NavigateToReview(
                    timeMin = result.actualDurationMinutes,
                    earnedPoints = result.earnedPoints
                )
            }
        }
    }
    LaunchedEffect(matchId, ongoingPage) {
        if (ongoingPage == 0) { // Start QR 페이지
            viewModel2.loadStartQRInfo(matchId, QRTypes.START)
        } else if (ongoingPage == 2) { // End QR 페이지
            viewModel2.loadEndQRInfo(matchId, QRTypes.END)
        }
    }






    /*
    지도
     */
    val requester = rememberLocationPermissionRequester(permissionChecker, appSettingsNavigator)
    val permissionState = requester.state.value
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val routePath by viewModel.routePath.collectAsState()


    LaunchedEffect(Unit) {
        if (!permissionState.isGranted) {
            requester.request()
        }
    }

    // 권한 상태를 감시하다가 '승인됨(true)'이면 위치 추적 시작
    LaunchedEffect(permissionState.isGranted) {
        if (permissionState.isGranted) {
            viewModel.startLocationTracking()
        }
    }

    // 데이터 로드는 권한과 상관없이 진행
    LaunchedEffect(matchId) {
        viewModel.loadMatchData(matchId)
    }

    // 리뷰
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is OngoingUiEvent.NavigateToReview -> {
                    val route = "${AppScreens.REVIEW_BASE}/${event.matchId}/${event.partnerId}" +
                            "?time=${event.totalTime}&dist=${event.distance}"
                    navController.navigate(route) {
                        popUpTo(AppScreens.HOME_BASE) { inclusive = false }
                    }
                }
                else -> {}
            }
        }
    }

    if (permissionState.isGranted) {
        // ✅ 권한이 있으면 정상 화면 표시
        if (scannerState.isScannerActive) {

            // 🚨 실제 QrScannerScreen을 여기에 호출합니다.
            // QrScannerScreen은 카메라 미리보기를 띄우고 QR 문자열을 인식한 후 콜백을 호출해야 합니다.
            QrScannerScreen(
                // ⚠️ 실제 위치 정보는 여기서 GPS/Location Manager를 통해 가져와야 합니다.
                onQrCodeScanned = { scannedCode ->
                    // 임시 위치 정보 (실제 구현 시 수정 필요)
                    val currentLatitude = 37.5665
                    val currentLongitude = 126.9780

                    viewModel2.handleScannedCode(scannedCode, currentLatitude, currentLongitude)
                },
                onStopScanning = viewModel2::closeScanner
            )
            // 스캐너 화면이 켜지면 더 이상 아래의 OngoingScreen을 렌더링하지 않습니다.
            return
        }
        OngoingScreen(
            uiState = uiState,
            uiState2 = uiState2,
            uiStateqr = qrScreenUiState,
            resultUiState = resultUiState,
            locateUiState = locateUiState,
            mapMarkers = mapMarkers,
            routePath = routePath,
            onScanRequest = viewModel2::scanQR,
            requestScan = viewModel2::requestQrScan,
            nextPage = viewModel::nextPage,

            // 람다식({ })으로 변경하여 인자(0, 0) 전달
            NavigateToReview = {
                viewModel.NavigateToReview(0, 0)
            }
        )
    } else {
        // 🚫 권한이 없으면 안내 문구 표시 (간단하게 처리)
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("위치 권한이 있어야 동행을 시작할 수 있습니다.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { requester.request() }) {
                    Text("권한 허용하기")
                }
            }
        }
    }

//    OngoingScreen(
//        uiState = uiState,
//        uiState2 = uiState2,
//        uiState3=uiState3,
//        resultUiState = resultUiState,
//        locateUiState=locateUiState,
//        onScanRequest= viewModel2::scanQR,
//        nextPage=viewModel::nextPage,
//        NavigateToReview = viewModel::NavigateToReview,
//        mapMarkers = mapMarkers,
//        routePath = routePath,
//    )
}



