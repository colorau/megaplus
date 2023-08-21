package com.lotteriasmais.megaplus.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lotteriasmais.megaplus.repositories.DataStoreRepository
import com.lotteriasmais.megaplus.repositories.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val loginRepository: LoginRepository
): ViewModel() {

    private val _fragmentEvents = MutableStateFlow<FragmentEvents>(FragmentEvents.Processing)
    val fragmentEvent: StateFlow<FragmentEvents> get() = _fragmentEvents

    init{
        checkSplashShow()
    }

    fun checkUser(email: String, password: String)  = viewModelScope.launch{
        when{
            email.isEmpty() -> _fragmentEvents.value = FragmentEvents.ShowEmailEmpty
            password.isEmpty() -> _fragmentEvents.value = FragmentEvents.ShowPasswordEmpty
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> _fragmentEvents.value = FragmentEvents.ShowEmailNotMatches
            password.length < 6 -> _fragmentEvents.value = FragmentEvents.ShowPasswordLess6Digits
            else -> {
                val userAuth = withContext(Dispatchers.IO){
                    loginRepository.login(email, password)
                }
                if(userAuth != null && userAuth.isEmailVerified){
                    checkDestination()
                }
            }
        }
    }

    private suspend fun checkDestination(){
        if(dataStoreRepository.readData.first().isFirstLaunch){
            _fragmentEvents.value = FragmentEvents.NavigateToOnBoardingScreen
        }
        else{
            _fragmentEvents.value = FragmentEvents.NavigateToWellcomeScreen
        }
    }

    fun checkForgotPassword(emailText: String) = viewModelScope.launch {
        val success = withContext(Dispatchers.IO) {
            loginRepository.sendUserLinkEmail(emailText)
        }
        if (success.isNotEmpty()) {
            _fragmentEvents.value = FragmentEvents.FailureToSendLinkToEmail
        } else {
            _fragmentEvents.value = FragmentEvents.ShowSendLinkToEmail
        }
    }

    fun firebaseAuthWithGoogle(idToken: String)= viewModelScope.launch {
        val userAuth = withContext(Dispatchers.IO){
            loginRepository.firebaseAuthWithGoogle(idToken)
        }
        if(userAuth != null){
            checkDestination()
        }
    }

    fun sendEmailVerificationMessage() = viewModelScope.launch(Dispatchers.IO) {
        loginRepository.sendEmailVerification()
    }

    fun checkSplashShow() = viewModelScope.launch {
        if(!dataStoreRepository.readData.first().isSplashShow) {
            _fragmentEvents.value = FragmentEvents.NavigateToSplashScreen
        }
    }

    sealed class FragmentEvents{
        object NavigateToWellcomeScreen: FragmentEvents()
        object NavigateToOnBoardingScreen: FragmentEvents()
        object NavigateToSplashScreen: FragmentEvents()
        object Processing: FragmentEvents()
        object ShowEmailEmpty: FragmentEvents()
        object ShowPasswordEmpty: FragmentEvents()
        object ShowEmailNotMatches: FragmentEvents()
        object ShowPasswordLess6Digits: FragmentEvents()
        object ShowSendLinkToEmail: FragmentEvents()
        object FailureToSendLinkToEmail: FragmentEvents()
    }
}