package com.systems.mcp.domain

import java.time.Instant
import java.util.UUID

// Defines the status of a destructive action
enum class ApprovalStatus {
    PENDING, APPROVED, REJECTED, EXECUTED, FAILED
}

// The core object that the Agent submits for human review
data class PlanOfAction(
    val actionId: String = UUID.randomUUID().toString(),
    val toolName: String,
    val targetResource: String, // e.g., "container-123" or "namespace-dev"
    val reason: String,         // Why the agent wants to do this
    var status: ApprovalStatus = ApprovalStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    var resolvedAt: Instant? = null,
    var executedBy: String? = null // To track which human approved it
)