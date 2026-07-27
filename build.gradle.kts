// build.gradle.kts (Root)
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
