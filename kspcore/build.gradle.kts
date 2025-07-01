plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    // https://mvnrepository.com/artifact/com.squareup.retrofit2/converter-moshi
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    // https://mvnrepository.com/artifact/com.squareup.moshi/moshi
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.1.21-2.0.1")
    implementation("com.sealwu.jsontokotlin:library:3.7.4")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation ("com.squareup.moshi:moshi-kotlin:1.15.2")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
}

//buildscript{
//    dependencies {
//        implementation("com.squareup.retrofit2:retrofit:2.11.0")
//        // https://mvnrepository.com/artifact/com.squareup.retrofit2/converter-moshi
//        implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
//        // https://mvnrepository.com/artifact/com.squareup.moshi/moshi
//        implementation("com.squareup.moshi:moshi:1.15.2")
//    }
//}