package com.systems.mcp.server

import com.systems.mcp.domain.PlanOfAction
import com.systems.mcp.hitl.ApprovalGate
import com.systems.mcp.hitl.ExecutionService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/approvals")
class AdminController(
    private val approvalGate: ApprovalGate,
    private val executionService: ExecutionService,
    private val metrics: MeterRegistry,
    private val registry: McpToolRegistry
) {


    @GetMapping
    fun getPending(): List<PlanOfAction> {
        return approvalGate.getPendingActions()
    }

    @PostMapping("/{actionId}/resolve")
    fun resolveAction(
        @PathVariable actionId: String,
        @RequestParam approved: Boolean,
        @RequestParam operatorId: String = "admin-ui"
    ): String {
        // 1. Process the human's decision
        val poa = approvalGate.reviewAction(actionId, approved, operatorId)

        // 2. Telemetry tracking
        val metricName = if (approved) "mcp_hitl_approved_total" else "mcp_hitl_rejected_total"
        metrics.counter(metricName, "tool", poa.toolName).increment()

        // 3. Execute if approved
        return if (approved) {
            executionService.executeApprovedAction(poa)
        } else {
            "Action $actionId was rejected and discarded."
        }
    }

    // --- TEMPORARY ENDPOINT TO SIMULATE THE LLM ---
    @PostMapping("/simulate-agent")
    fun simulateAgent(): String {
        return registry.dockerStopContainer("test-nginx", "Testing HITL flow from terminal")
    }
}