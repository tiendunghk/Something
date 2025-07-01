package com.example.myapplication.language

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import androidx.core.content.edit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * Created by FPL on 16/06/2025.
 */
var stringAppVi = StringApp(
    profileName = "vi trung lê tieengs Vieetj",
)
var stringAppEn = StringApp(
    profileName = "lee trung vi english",
)

class LanguageViewModelFactory(private val context: Context) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LanguageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LanguageViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LanguageViewModel(val context: Context) : ViewModel() {
    val currentLanguage: MutableStateFlow<String> = MutableStateFlow("vi")
    var appLanguage: MutableStateFlow<StringApp> =
        MutableStateFlow(if (currentLanguage.value == "vi") stringAppVi else stringAppEn)

    private val sharedPreferences = context.getSharedPreferences("lang", Context.MODE_PRIVATE)

    init {
        val lang = sharedPreferences?.getString("lang", "vi")
        currentLanguage.update { lang ?: "vi" }
    }

    fun fetchLanguage() {
        viewModelScope.launch {
            delay(2.seconds)
//            val lang = LanguageService.getLanguage()
//            lang.forEach { (key, value) ->
//                if (key == "vi") {
//                    stringAppVi = value
//                } else if (key == "en") {
//                    stringAppEn = value
//                }
//                if (key == currentLanguage.value) {
//                    appLanguage.update { value }
//                }
//                saveLanguageToFile("$key.json", value)
//            }
            readStringAppFromJsonFile(context, "vi.json").let {
                stringAppVi = it
                appLanguage.update { stringAppVi }
            }
        }
    }

    private fun saveLanguageToFile(fileName: String, stringApp: StringApp) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { output ->
            output.write(Json.encodeToString(stringApp).toByteArray())
        }
    }

    fun readStringAppFromJsonFile(context: Context, fileName: String): StringApp {
        val jsonString = context.openFileInput(fileName).bufferedReader().use { it.readText() }
        return Json.decodeFromString(jsonString)
    }

    fun changeLanguage() {
        currentLanguage.update {
            if (it == "vi") {
                appLanguage.update { stringAppEn }
                sharedPreferences.edit { putString("lang", "en") }
                "en"
            } else {
                appLanguage.update { stringAppVi }
                sharedPreferences.edit { putString("lang", "vi") }
                "vi"
            }
        }
    }
}