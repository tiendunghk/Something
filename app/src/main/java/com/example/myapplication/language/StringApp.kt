package com.example.myapplication.language

import kotlinx.serialization.Serializable

@Serializable
data class StringApp(
    val profileName: String = "Default Profile Name",
)
