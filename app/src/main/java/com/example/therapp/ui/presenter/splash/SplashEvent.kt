package com.example.therapp.ui.presenter.splash

sealed class SplashEvent {
    object CheckAuthentication : SplashEvent()
}