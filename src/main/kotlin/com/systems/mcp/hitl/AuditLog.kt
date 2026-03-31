package com.systems.mcp.hitl

import com.systems.mcp.domain.PlanOfAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import jakarta.annotation.PreDestroy

@Service
class AuditLog {
    private val log = LoggerFactory.getLogger(javaClass)
    
    // The "Mailbox" - unlimited capacity for bursts of agent activity
    private val mailbox = Channel<PlanOfAction>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // The Actor Loop: A single coroutine processing messages sequentially
        scope.launch {
            for (poa in mailbox) {
                writeToImmutableLog(poa)
            }
        }
    }

    /**
     * Fire-and-forget submission to the audit log.
     * Safe to call from any thread or coroutine.
     */
    fun record(poa: PlanOfAction) {
        mailbox.trySend(poa.copy()).isSuccess // copy() ensures immutability of the logged state
    }

    private fun writeToImmutableLog(poa: PlanOfAction) {
        // In production, this would append to a WORM (Write Once, Read Many) drive or DB.
        // For this project, a structured standard output log suffices.
        val operator = poa.executedBy ?: "SYSTEM"
        log.info("🔒 [AUDIT] Action: ${poa.actionId} | Tool: ${poa.toolName} | Target: ${poa.targetResource} | Status: ${poa.status} | By: $operator")
    }

    @PreDestroy
    fun shutdown() {
        mailbox.close()
    }
}