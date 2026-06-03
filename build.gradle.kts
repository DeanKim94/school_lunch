import org.gradle.api.tasks.Sync

plugins {
    application
    java
}

group = "com.school.lunch"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("com.formdev:flatlaf:3.2.5")
    implementation("com.github.lgooddatepicker:LGoodDatePicker:11.2.1")
}

application {
    mainClass = "com.school.lunch.Main"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.jar {
    archiveBaseName = "school-lunch"
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
}

val runtimeDir = layout.buildDirectory.dir("runtime")
val packageInputDir = layout.buildDirectory.dir("jpackage/input")
val packageOutputDir = layout.buildDirectory.dir("jpackage/output")
val generatedIconDir = layout.buildDirectory.dir("generated/icons")
val currentOs = System.getProperty("os.name").lowercase()
val isMac = currentOs.contains("mac")
val isWindows = currentOs.contains("win")

val macIconPng = layout.projectDirectory.file("src/main/resources/icon.png")
val windowsIcon = layout.projectDirectory.file("src/main/resources/icon.ico")
val macIconsetDir = generatedIconDir.map { it.dir("school-lunch.iconset") }
val macIcnsFile = generatedIconDir.map { it.file("school-lunch.icns") }

val copyRuntimeLibs by tasks.registering(Sync::class) {
    dependsOn(tasks.jar)
    into(runtimeDir)
    from(tasks.jar)
    from(configurations.runtimeClasspath)
}

val preparePackageInput by tasks.registering(Sync::class) {
    dependsOn(copyRuntimeLibs)
    into(packageInputDir)
    from(runtimeDir)
}

val generateMacIcon by tasks.registering {
    group = "build setup"
    description = "Generates a macOS .icns file from src/main/resources/icon.png."
    onlyIf { isMac }

    inputs.file(macIconPng)
    outputs.file(macIcnsFile)

    doLast {
        val iconset = macIconsetDir.get().asFile
        val outputFile = macIcnsFile.get().asFile
        val sourceFile = macIconPng.asFile

        fun runCommand(vararg args: String) {
            val process = ProcessBuilder(*args)
                .directory(layout.projectDirectory.asFile)
                .inheritIO()
                .start()

            val exitCode = process.waitFor()
            check(exitCode == 0) { "Command failed (${args.joinToString(" ")}), exit code: $exitCode" }
        }

        delete(iconset)
        iconset.mkdirs()
        outputFile.parentFile.mkdirs()
        val variants = listOf(
            16 to "icon_16x16.png",
            32 to "icon_16x16@2x.png",
            32 to "icon_32x32.png",
            64 to "icon_32x32@2x.png",
            128 to "icon_128x128.png",
            256 to "icon_128x128@2x.png",
            256 to "icon_256x256.png",
            512 to "icon_256x256@2x.png",
            512 to "icon_512x512.png",
            1024 to "icon_512x512@2x.png",
        )

        variants.forEach { (size, fileName) ->
            runCommand(
                "sips",
                "-s", "format", "png",
                "-z", size.toString(), size.toString(),
                sourceFile.absolutePath,
                "--out", iconset.resolve(fileName).absolutePath,
            )
        }

        runCommand(
            "iconutil",
            "-c", "icns",
            iconset.absolutePath,
            "-o", outputFile.absolutePath,
        )
    }
}

fun nativePackageTask(
    name: String,
    packageType: String,
    enabledOnCurrentOs: Boolean,
    iconPath: String? = null,
) = tasks.register<Exec>(name) {
    group = "distribution"
    description = "Builds a native ${packageType.uppercase()} package with jpackage."
    dependsOn(preparePackageInput)
    if (iconPath == null && enabledOnCurrentOs && packageType == "dmg") {
        dependsOn(generateMacIcon)
    }
    onlyIf { enabledOnCurrentOs }

    doFirst {
        val output = packageOutputDir.get().asFile
        output.mkdirs()
        val args = mutableListOf(
            "jpackage",
            "--type", packageType,
            "--name", "SchoolLunch",
            "--app-version", project.version.toString(),
            "--vendor", "School Lunch",
            "--input", packageInputDir.get().asFile.absolutePath,
            "--dest", output.absolutePath,
            "--main-jar", tasks.jar.get().archiveFileName.get(),
            "--main-class", application.mainClass.get(),
        )

        val resolvedIconPath = when {
            iconPath != null -> layout.projectDirectory.file(iconPath).asFile.absolutePath
            packageType == "dmg" && isMac -> macIcnsFile.get().asFile.absolutePath
            else -> null
        }

        resolvedIconPath?.let { args.addAll(listOf("--icon", it)) }
        commandLine(args)
    }
}

val packageDmg = nativePackageTask(
    name = "packageDmg",
    packageType = "dmg",
    enabledOnCurrentOs = isMac,
)

val packageExe = nativePackageTask(
    name = "packageExe",
    packageType = "exe",
    enabledOnCurrentOs = isWindows,
    iconPath = windowsIcon.asFile.relativeTo(layout.projectDirectory.asFile).path,
)

tasks.register("packageNative") {
    group = "distribution"
    description = "Builds the native installer supported on the current OS."

    if (isMac) {
        dependsOn(packageDmg)
    } else if (isWindows) {
        dependsOn(packageExe)
    } else {
        doLast {
            error("Native packaging is only configured for macOS (.dmg) and Windows (.exe).")
        }
    }
}
