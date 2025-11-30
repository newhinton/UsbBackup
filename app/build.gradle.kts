plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlinx-serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "de.felixnuesse.usbbackup"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.felixnuesse.usbbackup2"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    applicationVariants.all {
        //outputs.all { output ->
           // outputFileName = new File("backupusb-release-v"+versionName+".apk")
        //}
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.appintro)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.room.runtime)

    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.biometric)

    implementation(project(":crypto"))
}

tasks.register<Copy>("deployDesktop") {
    val desktopVersion = project(":desktop").version

    dependsOn(":desktop:jar")
    println("${rootDir}/desktop/build/libs/aes-tool-$desktopVersion.jar")
    from("${rootDir}/desktop/build/libs/aes-tool-$desktopVersion.jar")
    into("${rootDir}/app/src/main/assets/")
    include("*.jar", "*.md")

    from("${rootDir}/desktop/README.md")
    into("${rootDir}/app/src/main/assets/")
}


tasks.preBuild {
    dependsOn("deployDesktop")
}