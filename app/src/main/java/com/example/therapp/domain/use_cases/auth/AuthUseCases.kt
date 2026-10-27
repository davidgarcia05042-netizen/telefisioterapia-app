package com.example.therapp.domain.use_cases.auth


/**
 * @author Santiago Varela Daza
 * @email svarela03@uan.edu.co
 * @github https://github.com/sanvarela03
 * @since 8/31/2025
 * @version 1.0
 */
data class AuthUseCases (
    val signIn: SignIn,
    val signOut: SignOut,
    val isSignedIn: IsSignedIn,
    val changePassword: ChangePassword,
)