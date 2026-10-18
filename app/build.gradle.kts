import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Firebase/Crashlytics só são aplicados quando o google-services.json existe,
// assim o projeto continua compilando antes da configuração do Firebase.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// Segredos vêm do local.properties (fora do controle de versão).
val segredos = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}
fun segredo(nome: String): String = segredos.getProperty(nome, "").trim()

android {
    namespace = "com.luistech.socorre"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.luistech.socorre"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.luistech.socorre.ExecutorTestesHilt"

        // API Socorro (pública, sem chave). Pode ser trocada no local.properties.
        val urlApi = segredo("API_URL_EVENTOS").ifEmpty { "https://ideaberta.com.br/socorro/" }
        buildConfigField("String", "API_URL_EVENTOS", "\"$urlApi\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    annotationProcessor(libs.androidx.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.work.runtime)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.play.location)
    implementation(libs.maps.utils) // só PolyUtil/LatLng (codificar a rota); não usa chave
    implementation(libs.osmdroid)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.core)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestAnnotationProcessor(libs.hilt.compiler)
}

// Hilt sem o plugin Gradle não convive bem com a compilação Java incremental: as classes Hilt_*
// geradas somem entre builds e o projeto quebra com "cannot find symbol Hilt_...".
// Compilar sempre por completo evita isso (o projeto é pequeno; custa poucos segundos).
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = false
}
