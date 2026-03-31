package com.systems.mcp.hitl

import com.systems.mcp.domain.ApprovalStatus
import com.systems.mcp.domain.PlanOfAction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class ApprovalGate(private val auditLog: AuditLog) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Thread-safe map to hold actions waiting for human review
    private val pendingActions = ConcurrentHashMap<String, PlanOfAction>()

    /**
     * Called by the MCP Tool when a destructive action is requested.
     */
    fun submitForApproval(poa: PlanOfAction): String {
        pendingActions[poa.actionId] = poa
        auditLog.record(poa) // Audit the request
        
        log.warn("⚠️ Destructive action suspended. ActionID: ${poa.actionId}")
        
        // This exact string goes back to the LLM agent via MCP
        return "Action requires human approval. Plan of Action created. Status: PENDING. Action ID: ${poa.actionId}. Please move on to other tasks or notify the user."
    }

    /**
     * Called by a human operator (e.g., via a REST endpoint or UI) to approve/reject.
     */
    fun reviewAction(actionId: String, approved: Boolean, operatorId: String): PlanOfAction {
        val poa = pendingActions[actionId] 
            ?: throw IllegalArgumentException("Action ID $actionId not found or already processed.")

        // Update state
        poa.status = if (approved) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED
        poa.resolvedAt = Instant.now()
        poa.executedBy = operatorId

        // Remove from pending queue and log the human's decision
        pendingActions.remove(actionId)
        auditLog.record(poa)

        return poa
    }

    fun getPendingActions(): List<PlanOfAction> = pendingActions.values.toList()
}