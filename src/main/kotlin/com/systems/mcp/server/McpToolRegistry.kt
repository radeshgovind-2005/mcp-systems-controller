package com.systems.mcp.server

import com.systems.mcp.domain.PlanOfAction
import com.systems.mcp.hitl.ApprovalGate
import com.systems.mcp.tools.DockerTools
import com.systems.mcp.tools.KubernetesTools
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service

@Service
class McpToolRegistry(
    private val dockerTools: DockerTools,
    private val kubernetesTools: KubernetesTools,
    private val approvalGate: ApprovalGate,
    private val metrics: MeterRegistry
) {

    // --- READ-ONLY TOOLS (Execute immediately) ---

    @Tool(description = "List all running and stopped Docker containers.")
    fun dockerListContainers(): String {
        return metrics.timer("mcp_tool_duration_seconds", "tool", "docker_list_containers").recordCallable {
            dockerTools.listContainers().toString()
        } ?: "Error: Could not retrieve container list"
    }

    @Tool(description = "Get the latest logs for a specific Docker container.")
    fun dockerGetLogs(containerId: String, tail: Int = 50): String {
        return metrics.timer("mcp_tool_duration_seconds", "tool", "docker_get_logs").recordCallable {
            dockerTools.getLogs(containerId, tail)
        }!!
    }

    @Tool(description = "List Kubernetes pods in a specific namespace.")
    fun k8sGetPods(namespace: String): String {
        return metrics.timer("mcp_tool_duration_seconds", "tool", "k8s_get_pods").recordCallable {
            kubernetesTools.getPods(namespace).toString()
        }!!
    }

    // --- DESTRUCTIVE TOOLS (Require HITL Approval) ---

    @Tool(description = "Stop a running Docker container. MUST provide a valid reason.")
    fun dockerStopContainer(containerId: String, reason: String): String {
        metrics.counter("mcp_hitl_pending_approvals_total", "tool", "docker_stop_container").increment()
        
        val poa = PlanOfAction(
            toolName = "docker_stop_container",
            targetResource = containerId,
            reason = reason
        )
        return approvalGate.submitForApproval(poa)
    }

    @Tool(description = "Scale a Kubernetes deployment down to zero replicas. targetResource format: 'namespace/deploymentName'.")
    fun k8sScaleDown(targetResource: String, reason: String): String {
        metrics.counter("mcp_hitl_pending_approvals_total", "tool", "k8s_scale_down").increment()
        
        val poa = PlanOfAction(
            toolName = "k8s_scale_down",
            targetResource = targetResource,
            reason = reason
        )
        return approvalGate.submitForApproval(poa)
    }
}