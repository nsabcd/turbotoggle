plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.3.5"
	id("io.spring.dependency-management") version "1.1.6"
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
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation(project(":turbotoggle-core"))

	// Database Drivers
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("com.h2database:h2")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	// ADD THIS LINE: Explicitly provide the launcher to Gradle's test runner
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("com.redis:testcontainers-redis:2.2.4")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		csv.required.set(false)
		html.required.set(true)
	}
}

tasks.jacocoTestCoverageVerification {
	violationRules {
		rule {
			limit {
				minimum = "0.80".toBigDecimal() // Enforce 80% coverage
			}
		}
	}
}
