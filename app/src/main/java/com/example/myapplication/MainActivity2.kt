package com.example.myapplication

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.myapplication.language.AppLanguage
import com.example.myapplication.language.LanguageViewModel
import com.example.myapplication.language.LanguageViewModelFactory
import com.example.myapplication.language.StringApp
import java.util.Locale
import kotlin.properties.Delegates

val LocalLanguage = staticCompositionLocalOf<StringApp> {
    StringApp()
}


//@TestAnnotation
//data class TestAbc(val a : String)

class MainActivity2 : ComponentActivity() {
    val languageViewModel: LanguageViewModel by viewModels<LanguageViewModel>(
        factoryProducer = { LanguageViewModelFactory(this@MainActivity2.applicationContext) }
    )

    var testDataClass : AppLanguage by Delegates.notNull<AppLanguage>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LaunchedEffect(Unit) {
                languageViewModel.fetchLanguage()
            }

            val appString = languageViewModel.appLanguage.collectAsState()

            CompositionLocalProvider(LocalLanguage provides appString.value) {
                val context = LocalContext.current
                Scaffold { padding ->
                    Column(Modifier.padding(padding)) {
                        Text(
                            text = LocalLanguage.current.profileName,
                        )
                        Text(
                            text = "current language: ${Locale.getDefault().language}",
                        )
                        Button(onClick = {
                            languageViewModel.changeLanguage()
                        }) {
                            Text("switch language")
                        }
                        Button(
                            onClick = {
                                context.openFileOutput("fileName.txt", Context.MODE_PRIVATE).use { output ->
                                    output.write("my content is my content".toByteArray())
                                }
                            }
                        ) {
                            Text("Save file")
                        }
                    }

                }
            }
        }
    }
}

fun changeLanguage(language: String, context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            LocaleList.forLanguageTags(language)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }
}