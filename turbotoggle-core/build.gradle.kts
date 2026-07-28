plugins {
    `java-library`
}

group = "com.turbotoggle"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

val jacksonVersion = "2.15.2"
val slf4jVersion = "2.0.9"

dependencies {
    // JSON Parsing
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    // Logging API
    api("org.slf4j:slf4j-api:$slf4jVersion")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}