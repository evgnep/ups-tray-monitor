plugins {
    kotlin("jvm") version "2.4.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    // builds a trimmed JRE + native launcher via jlink/jpackage
    id("org.beryx.runtime") version "1.13.1"
}

group = "com.example"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

javafx {
    version = "21.0.2"
    // javafx.swing is needed by FXTrayIcon - it wraps java.awt.SystemTray.
    modules = listOf("javafx.controls", "javafx.swing")
}

dependencies {
    // Modbus master (RTU/ASCII/TCP), used to poll the UPS. See PROJECT_BRIEF.md.
    implementation("com.ghgande:j2mod:3.4.0")
    // Tray icon backed by JavaFX. See PROJECT_BRIEF.md.
    implementation("com.dustinredmond.fxtrayicon:FXTrayIcon:4.2.3")
    testImplementation(kotlin("test"))
}

application {
    // top-level main() in src/main/kotlin/App.kt
    mainClass.set("AppKt")
}

tasks.test {
    useJUnitPlatform()
}

// Packaging: self-contained app-image with a trimmed JRE, no installer.
// Build: `.\gradlew.bat jpackageImage` -> build\jpackage\ups-tray-monitor\
// Copy that folder to the target machine and run `ups-tray-monitor.exe`
// (native GUI launcher, no console window). To update, replace jars in `app\`.
runtime {
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages", "--compress", "zip-6"))

    // JDK modules needed by non-modular deps (j2mod / jSerialComm, FXTrayIcon, kotlin).
    // Run `.\gradlew.bat suggestModules` to review this list.
    modules.set(
        listOf(
            "java.base",
            "java.desktop",
            "java.logging",
            "java.management",
            "java.naming",
            "jdk.unsupported",
        )
    )

    imageDir.set(layout.buildDirectory.dir("ups-tray-monitor"))
    imageZip.set(layout.buildDirectory.file("ups-tray-monitor-${project.version}.zip"))

    jpackage {
        imageName = "ups-tray-monitor"
        // app-image only, no setup.exe; launcher has no --win-console -> runs windowless
        skipInstaller = true
    }
}

// Standalone test: poll UPS registers over Modbus ASCII on COM4.
// Run with: .\gradlew.bat modbusPollTest
tasks.register<JavaExec>("modbusPollTest") {
    group = "verification"
    description = "Poll UPS holding registers over Modbus ASCII (COM4) to check the connection"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("ModbusPollTestKt")
}
