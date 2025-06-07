plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    // don't forget to update the version in the source code
    version="1.0.0"
    java.sourceSets["main"].java {
        srcDir("..app/src/main/java/")
    }
}


kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}


dependencies {
    implementation(libs.clikt)
    implementation(libs.kotlinx.datetime)
    implementation(project(":crypto"))
}


tasks.jar {
    dependsOn(":crypto:jar")
    manifest.attributes["Main-Class"] = "de.felixnuesse.desktop.MainKt"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveBaseName.set("aes-tool")
    archiveVersion = project.version.toString()
}

tasks.register<JavaExec>("execute") {
    group = "run"
    mainClass.set("de.felixnuesse.desktop.MainKt")
    classpath = sourceSets["main"].runtimeClasspath
}