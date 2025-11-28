package com.syj.geotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syj.geotask.domain.usecase.LoginUseCase
import com.syj.geotask.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainEvent {
    data object NavigateToHome : MainEvent()
    data object NavigateToLogin : MainEvent()
    data object NavigateToRegister : MainEvent()
    data object ShowInvalidCredentialsToast : MainEvent()
    data object ShowUserAlreadyExistsToast : MainEvent()
}

data class MainUiState(
    val currentDestination: String = "login",
    val isLoading: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<MainEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val success = loginUseCase(username, password)
            if (success) {
                _eventFlow.emit(MainEvent.NavigateToHome)
                _uiState.value = _uiState.value.copy(currentDestination = "home", isLoading = false)
            } else {
                _eventFlow.emit(MainEvent.ShowInvalidCredentialsToast)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun register(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val success = registerUseCase(username, password)
            if (success) {
                _eventFlow.emit(MainEvent.NavigateToHome)
                _uiState.value = _uiState.value.copy(currentDestination = "home", isLoading = false)
            } else {
                _eventFlow.emit(MainEvent.ShowUserAlreadyExistsToast)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun navigateToLogin() {
        _uiState.value = _uiState.value.copy(currentDestination = "login")
    }

    fun navigateToRegister() {
        _uiState.value = _uiState.value.copy(currentDestination = "register")
    }
}
