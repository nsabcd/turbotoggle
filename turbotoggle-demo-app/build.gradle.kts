plugins {
	java
	//id("org.springframework.boot") version "3.3.0"
	//id("io.spring.dependency-management") version "1.1.5"
}

group = "com.turbotoggle"
version = "0.0.1-SNAPSHOT"

configure<JavaPluginExtension> {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Boot Starters (Versions managed by spring-boot-dependencies BOM)
	//implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	//implementation("org.springframework.boot:spring-boot-starter-web")
	//implementation("org.springframework.boot:spring-boot-starter-data-redis")
	//implementation("org.springframework.boot:spring-boot-starter-validation")

	// Database Drivers
	//runtimeOnly("org.postgresql:postgresql")
	//runtimeOnly("com.h2database:h2")

	// Testing
	//testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}