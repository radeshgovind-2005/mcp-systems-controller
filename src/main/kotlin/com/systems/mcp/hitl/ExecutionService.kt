package com.systems.mcp.hitl

import com.systems.mcp.domain.ApprovalStatus
import com.systems.mcp.domain.PlanOfAction
import com.systems.mcp.tools.DockerTools
import com.systems.mcp.tools.KubernetesTools
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExecutionService(
    private val dockerTools: DockerTools,
    private val kubernetesTools: KubernetesTools,
    private val auditLog: AuditLog
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun executeApprovedAction(poa: PlanOfAction): String {
        require(poa.status == ApprovalStatus.APPROVED) { "Action must be approved before execution." }
        
        return try {
            log.info("🚀 Executing approved action: ${poa.actionId}")
            
            // Route the execution based on the tool name
            val result = when (poa.toolName) {
                "docker_stop_container" -> dockerTools.stopContainer(poa.targetResource)
                "docker_remove_container" -> dockerTools.removeContainer(poa.targetResource)
                "k8s_delete_namespace" -> kubernetesTools.deleteNamespace(poa.targetResource)
                "k8s_scale_down" -> {
                    // Extract namespace and deployment from targetResource (e.g., "default/my-app")
                    val parts = poa.targetResource.split("/")
                    kubernetesTools.scaleDeployment(parts[0], parts[1], 0)
                }
                else -> throw IllegalArgumentException("Unknown tool: ${poa.toolName}")
            }
            
            poa.status = ApprovalStatus.EXECUTED
            auditLog.record(poa)
            result

        } catch (e: Exception) {
            log.error("❌ Execution failed for ${poa.actionId}: ${e.message}")
            poa.status = ApprovalStatus.FAILED
            auditLog.record(poa)
            "Execution failed: ${e.message}"
        }
    }
}