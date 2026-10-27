package com.example.therapp.data.auth.remote.payload.req

import com.google.gson.annotations.SerializedName

/** POST /auth/users/change_password/: la actual y la nueva (el backend valida la nueva). */
data class ChangePasswordReq(
    val password: String,
    @SerializedName("new_password")
    val newPassword: String,
)
