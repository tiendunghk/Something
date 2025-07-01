package com.example.kspcore

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import wu.seal.jsontokotlin.library.JsonToKotlinBuilder
import wu.seal.jsontokotlin.model.ConfigManager
import wu.seal.jsontokotlin.model.DefaultValueStrategy
import wu.seal.jsontokotlin.model.PropertyTypeStrategy
import wu.seal.jsontokotlin.model.TargetJsonConverter
import wu.seal.jsontokotlin.test.TestConfig.isNestedClassModel
import java.net.HttpURLConnection
import java.net.URL

class TestProcessor(
    private val codeGenerator: CodeGenerator,
    private val kspLogger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        kspLogger.info("TestProcessor is processing symbols...")
        try {
            generateClassFile()
        } catch (e: Exception) {
            kspLogger.error("Exception during processing: ${e.message}")
        }
        return emptyList()
    }

    private fun fetchTodoAsync(): String? {
        return try {
            val url = URL("https://jsonplaceholder.typicode.com/todos/1")
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Kotlin-App")
                doInput = true
            }

            val responseCode = connection.responseCode
            println("Response Code: $responseCode")

            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    response
                }

                else -> {
                    val errorResponse =
                        connection.errorStream?.bufferedReader()?.use { it.readText() }
                    println("Error Response: $errorResponse")
                    connection.disconnect()
                    null
                }
            }
        } catch (e: Exception) {
            println("Network error: ${e.message}")
            e.printStackTrace()
            null
        }
    }


    private fun generateClassFile() {
        try {
            val actualOutput = JsonToKotlinBuilder()
                .setPackageName("com.example.myapplication.language")

            ConfigManager.apply {
                // Correct property names for JsonToKotlin library
                targetJsonConverterLib = TargetJsonConverter.MoshiCodeGen
                propertyTypeStrategy = PropertyTypeStrategy.NotNullable
                defaultValueStrategy = DefaultValueStrategy.AvoidNull
                isNestedClassModel = false
                isCommentOff = true
                isOrderByAlphabetical = true
            }

            val json1 = fetchTodoAsync()?.trimIndent() ?: ""

            // Use Moshi to parse JSON into a Map
            val moshi = Moshi.Builder().build()
            val type =
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any>>(type)
            val propertyDefaults = adapter.fromJson(json1) ?: emptyMap()

            var output = actualOutput.build(json1, "AppLanguage")

            kspLogger.info("Generated class output: $output")

            // Replace property declarations with default values
            for ((prop, value) in propertyDefaults) {
                val defaultValue = when (value) {
                    is String -> "\"${value.replace("\"", "\\\"").replace("\n", "\\n")}\""
                    is Number -> value.toString().toIntOrNull() ?: "0"
                    is Boolean -> value.toString()
                    is Double -> "${value}0" // Ensure it's treated as Double
                    else -> "\"$value\""
                }

                // More robust regex pattern that handles different property types
                val patterns = listOf(
                    Regex("""val\s+$prop:\s+([A-Za-z0-9_<>?]+)(\s*=\s*[^,\n]+)?"""),
                    Regex("""val\s+$prop:\s+([A-Za-z0-9_<>?]+)""")
                )

                for (pattern in patterns) {
                    if (pattern.containsMatchIn(output)) {
                        output = output.replace(pattern, "val $prop: $1 = $defaultValue")
                        kspLogger.info("Replaced property '$prop' with default value: $defaultValue")
                        break
                    }
                }
            }


            output = "/**\n" +
                    " * Automatically generated file. DO NOT MODIFY\n" +
                    " */ \n\n" + output
            // Create the file using KSP CodeGenerator
            kotlin.runCatching {
                val file = codeGenerator.createNewFile(
                    dependencies = Dependencies(false), // Consider adding actual dependencies if needed
                    packageName = "com.example.myapplication.language",
                    fileName = "AppLanguage",
                    extensionName = "kt" // Explicitly specify the extension
                )

                file.write(output.toByteArray())
                file.close() //
            }

            // Log success for debugging
            kspLogger.info("Successfully generated AppLanguage.kt")

        } catch (e: Exception) {
            kspLogger.error("Error generating class file: ${e.message}")
        }
    }
}