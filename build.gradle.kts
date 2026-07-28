// build.gradle.kts (Root)
plugins {
    id("com.gradleup.shadow") version "9.5.1" apply false
}
allprojects {
    group = "com.turbotoggle"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21)) // Match your Java version
        }
    }
}
