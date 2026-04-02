package com.systems.mcp.tools

import io.kubernetes.client.custom.V1Patch
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.apis.CoreV1Api
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KubernetesTools(
    private val coreV1Api: CoreV1Api,
    private val appsV1Api: AppsV1Api
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getPods(namespace: String): List<Map<String, String>> {
        val pods = coreV1Api.listNamespacedPod(namespace).execute()
        return pods.items.map { pod ->
            mapOf(
                "name" to (pod.metadata?.name ?: "unknown"),
                "status" to (pod.status?.phase ?: "unknown"),
                "podIP" to (pod.status?.podIP ?: "none")
            )
        }
    }

    fun getEvents(namespace: String): List<String> {
        val events = coreV1Api.listNamespacedEvent(namespace).execute()
        return events.items.takeLast(10).map { event ->
            "[${event.type}] ${event.involvedObject?.kind} ${event.involvedObject?.name}: ${event.message}"
        }
    }

    fun scaleDeployment(namespace: String, deploymentName: String, replicas: Int): String {
        // ⚠️ Destructive/Modifying - We will intercept this in Phase 4
        // Using Strategic Merge Patch to update replicas safely
        val patch = V1Patch("{\"spec\":{\"replicas\":$replicas}}")
        appsV1Api.patchNamespacedDeployment(
            deploymentName,
            namespace,
            patch
        ).execute()
        
        return "Successfully scaled deployment $deploymentName in namespace $namespace to $replicas replicas."
    }

    fun deleteNamespace(namespace: String): String {
        // ⚠️ Highly Destructive
        // We use execute() to perform a standard foreground deletion
        coreV1Api.deleteNamespace(namespace).execute()
        return "Namespace $namespace marked for deletion."
    }
}