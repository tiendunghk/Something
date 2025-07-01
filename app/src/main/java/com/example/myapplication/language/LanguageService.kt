package com.example.myapplication.language


/**
 * Created by FPL on 16/06/2025.
 */

object LanguageService {
    fun getLanguage(): Map<String, StringApp> {
        val vi = StringApp(
            profileName = "vi tieng viet ${System.currentTimeMillis()}"
        )
        val en = StringApp(
            profileName = "vi tieng anh ${System.currentTimeMillis()}"
        )
        return mapOf<String, StringApp> (
            "vi" to vi,
            "en" to en
        )
    }
}
