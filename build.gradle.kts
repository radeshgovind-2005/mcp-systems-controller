import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

group = "com.systems.mcp"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

// Use enforcedPlatform() so the Spring dependency-management plugin
// does NOT strip the version from the BOM import
extra["springAiVersion"] = "1.0.3"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
    dependencies {
        // Pin httpclient5 to 5.2.x — 5.4+ broke Unix socket support in docker-java
        dependency("org.apache.httpcomponents.client5:httpclient5:5.2.3")
    }
}
dependencies {
    // MCP server starter — includes spring-ai-core transitively, no need to declare it separately
    // You have spring-boot-starter-web, so use the WebMVC variant for HTTP/SSE transport
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    // Spring Boot Core
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Docker Client - httpclient5 with native kqueue for Apple Silicon
    // Docker Client - OkHttp transport works correctly with Unix sockets on macOS
    implementation("com.github.docker-java:docker-java:3.3.6") {
        exclude(group = "com.github.docker-java", module = "docker-java-transport-jersey")
        exclude(group = "com.github.docker-java", module = "docker-java-transport-netty")
    }
    implementation("com.github.docker-java:docker-java-transport-okhttp:3.3.6")

    // Kubernetes
    implementation("io.kubernetes:client-java:20.0.0")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}