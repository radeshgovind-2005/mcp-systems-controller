package com.systems.mcp.config

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.util.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.Duration

@Configuration
class InfrastructureConfig {

    @Bean
    @Primary
    fun dockerClient(): DockerClient {
        val socketPath = resolveDockerSocket()
        println(">>> Using Docker socket: $socketPath")

        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(socketPath)
            .build()

        val httpClient = OkDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .connectTimeout(Duration.ofSeconds(30).toMillis().toInt())
            .readTimeout(Duration.ofSeconds(45).toMillis().toInt())
            .build()

        return DockerClientImpl.getInstance(config, httpClient)
    }

    private fun resolveDockerSocket(): String {
        val candidates = listOf(
            "/Users/${System.getProperty("user.name")}/.docker/run/docker.sock",
            "/var/run/docker.sock",
            "/run/user/1000/docker.sock",
        )
        return "unix://" + (candidates.firstOrNull { java.io.File(it).exists() }
            ?: error("No Docker socket found. Is Docker running?"))
    }

    @Bean
    fun k8sApiClient(): ApiClient {
        val client = Config.defaultClient()
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client)
        return client
    }

    @Bean
    fun coreV1Api(apiClient: ApiClient) = CoreV1Api(apiClient)

    @Bean
    fun appsV1Api(apiClient: ApiClient) = AppsV1Api(apiClient)
}