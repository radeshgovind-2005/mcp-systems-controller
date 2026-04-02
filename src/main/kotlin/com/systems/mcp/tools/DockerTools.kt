package com.systems.mcp.tools

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class DockerTools(private val dockerClient: DockerClient) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun listContainers(): List<Map<String, String>> {
        return dockerClient.listContainersCmd().withShowAll(true).exec().map { container ->
            mapOf(
                "id" to container.id.take(12),
                "name" to (container.names.firstOrNull()?.removePrefix("/") ?: "unknown"),
                "state" to (container.state ?: "unknown"),
                "status" to (container.status ?: "unknown")
            )
        }
    }

    fun startContainer(containerId: String): String {
        dockerClient.startContainerCmd(containerId).exec()
        return "Successfully started container $containerId"
    }

    fun stopContainer(containerId: String): String {
        // ⚠️ Destructive - We will intercept this in Phase 4
        dockerClient.stopContainerCmd(containerId).exec()
        return "Successfully stopped container $containerId"
    }

    fun removeContainer(containerId: String): String {
        // ⚠️ Destructive
        dockerClient.removeContainerCmd(containerId).withForce(true).exec()
        return "Successfully removed container $containerId"
    }

    fun getLogs(containerId: String, tail: Int): String {
        val logBuilder = StringBuilder()

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                logBuilder.append(String(frame.payload))
            }
        }

        dockerClient.logContainerCmd(containerId)
            .withStdOut(true)
            .withStdErr(true)
            .withTail(tail)
            .exec(callback)
            .awaitCompletion(5, TimeUnit.SECONDS)

        return logBuilder.toString().ifBlank { "No logs found or container is empty." }
    }
}