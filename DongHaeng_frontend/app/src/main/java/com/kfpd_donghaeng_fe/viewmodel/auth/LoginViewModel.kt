package com.kfpd_donghaeng_fe.viewmodel.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kfpd_donghaeng_fe.domain.entity.auth.LoginAccountUiState
import com.kfpd_donghaeng_fe.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val checkCanLoginUseCase: LoginUseCase
): ViewModel()
{
    // 내부용 (수정 가능)
    private val _uiState = MutableStateFlow(LoginAccountUiState(currentPage = 0))
    // 외부용 (읽기 전용)
    val uiState = _uiState.asStateFlow()

    private val _loginEvent = MutableSharedFlow<String>()
    val loginEvent: SharedFlow<String> = _loginEvent.asSharedFlow()

    fun login() {
        viewModelScope.launch {
            val current = _uiState.value.currentPage
            if (current<=1) {
                _uiState.update { currentState ->
                    currentState.copy(currentPage = currentState.currentPage + 1)
                }
            }
        }
    }

    fun MovetoMain() {
        viewModelScope.launch {
            try {
                val loginResult = checkCanLoginUseCase("requester@test.com", "test1234")

                if (loginResult.success) {
                    // 2️⃣ 성공 시 UserType 추출 (null이면 기본값 "NEEDY")
                    // API 응답의 userType이 "HELPER"인지 "helper"인지 확인 필요 (대소문자 주의)
                    val userType = loginResult.userData.userType ?: "NEEDY"

                    Log.e("Login", "로그인 성공! 타입: $userType")
                    _loginEvent.emit(userType) // 유저 타입 방출
                } else {
                    Log.e("Login", "로그인 실패!")
                }
            } catch (e: Exception) {
                Log.e("LOGIN_ERROR", "로그인 과정 중 예외 발생: ${e.message}", e)
            }
        }
    }
    fun MovetoMakeAccount(){
        val current = _uiState.value.currentPage
        _uiState.update { currentState ->
            currentState.copy(currentPage = currentState.currentPage + 2)
        }
    }
}















