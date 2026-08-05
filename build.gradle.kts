// build.gradle.kts (Root)
plugins {
    java
    jacoco
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
    apply(plugin = "jacoco")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21)) // Match your Java version
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        // Pass JVM arg to suppress Java 21 dynamic agent loading warnings (Mockito/ByteBuddy/JaCoCo)
        jvmArgs("-XX:+EnableDynamicAgentLoading")

        // Automatically trigger JaCoCo coverage report after running tests
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.withType<Test>())

        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
