import java.net.URI
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val urlAutenticacao = providers.gradleProperty("unisync.authUrl")
val urlAcademica = providers.gradleProperty("unisync.apiUrl")
val arquivoAssinatura = providers.environmentVariable("UNISYNC_KEYSTORE_FILE")
val senhaArmazem = providers.environmentVariable("UNISYNC_KEYSTORE_PASSWORD")
val aliasAssinatura = providers.environmentVariable("UNISYNC_KEY_ALIAS")
val senhaChave = providers.environmentVariable("UNISYNC_KEY_PASSWORD")
fun String.literalBuildConfig() = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "br.com.unisync.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.unisync.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "AUTH_BASE_URL", urlAutenticacao.getOrElse("http://10.0.2.2:5041").literalBuildConfig())
            buildConfigField("String", "ACADEMIC_BASE_URL", urlAcademica.getOrElse("http://10.0.2.2:8000").literalBuildConfig())
        }
        release {
            buildConfigField("String", "AUTH_BASE_URL", urlAutenticacao.getOrElse("").literalBuildConfig())
            buildConfigField("String", "ACADEMIC_BASE_URL", urlAcademica.getOrElse("").literalBuildConfig())
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    signingConfigs {
        create("producao") {
            storeFile = arquivoAssinatura.orNull?.let { file(it) }
            storePassword = senhaArmazem.orNull
            keyAlias = aliasAssinatura.orNull
            keyPassword = senhaChave.orNull
        }
    }
    buildTypes.getByName("release").signingConfig = signingConfigs.getByName("producao")
}

val validarAmbienteRelease = tasks.register("validarAmbienteRelease") {
    val endereco = urlAutenticacao.getOrElse("")
    val academico = urlAcademica.getOrElse("")
    val caminhoChave = arquivoAssinatura.getOrElse("")
    val senhaStore = senhaArmazem.getOrElse("")
    val aliasChave = aliasAssinatura.getOrElse("")
    val senhaPrivada = senhaChave.getOrElse("")
    inputs.property("authUrl", endereco)
    inputs.property("apiUrl", academico)
    inputs.property("storeFile", caminhoChave)
    inputs.property("signingConfigured", senhaStore.isNotBlank() && aliasChave.isNotBlank() && senhaPrivada.isNotBlank())
    doLast {
        val uri = runCatching { URI(endereco) }.getOrNull()
        check(uri?.scheme == "https" && !uri.host.isNullOrBlank()) {
            "Informe a API HTTPS para release com -Punisync.authUrl=https://seu-servidor"
        }
        val api = runCatching { URI(academico) }.getOrNull()
        check(api?.scheme == "https" && !api.host.isNullOrBlank()) {
            "Informe a API acadêmica HTTPS com -Punisync.apiUrl=https://seu-servidor"
        }
        check(caminhoChave.isNotBlank() && File(caminhoChave).isFile &&
            senhaStore.isNotBlank() && aliasChave.isNotBlank() && senhaPrivada.isNotBlank()) {
            "Configure UNISYNC_KEYSTORE_FILE, UNISYNC_KEYSTORE_PASSWORD, UNISYNC_KEY_ALIAS e UNISYNC_KEY_PASSWORD para assinar o release."
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validarAmbienteRelease) }

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation("org.json:json:20240303")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }
