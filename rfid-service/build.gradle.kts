plugins {
	java
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.anexya"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

	implementation(platform("software.amazon.awssdk:bom:2.31.0"))
	implementation("software.amazon.awssdk:kinesis")

	runtimeOnly("com.mysql:mysql-connector-j")
	runtimeOnly("com.h2database:h2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName.set("app.jar")
}

tasks.named<Jar>("jar") {
	enabled = false
}

tasks.test {
	useJUnitPlatform {
		excludeTags("aws")
	}
}

tasks.register<Test>("awsTest") {
	description = "Runs integration tests against the service deployed on AWS"
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	useJUnitPlatform {
		includeTags("aws")
	}
	systemProperty("baseUrl", System.getProperty("baseUrl") ?: "")
	outputs.upToDateWhen { false }
}