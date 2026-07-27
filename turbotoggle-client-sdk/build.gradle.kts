plugins {
    `java-library`
}

dependencies {
    // Add lightweight HTTP or SSE client dependencies (e.g., OkHttp, Jackson)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
