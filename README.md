# mcp-systems-controller

Secure infrastructure orchestration for agentic workflows. A high-performance MCP server that bridges LLM agents to Docker and Kubernetes, with a mandatory Human-in-the-Loop approval layer for any destructive operation.

The bottleneck in agentic AI isn't intelligence — it's secure connectivity. This project treats infrastructure tools the same way a production system treats privileged API calls: every destructive action requires explicit sign-off before execution.

---

## What it does

Exposes Docker and Kubernetes operations as MCP tools, with a built-in approval gate:

```
Agent proposes Plan of Action (PoA)
       ↓
HITL approval layer (async, cryptographic sign-off)
       ↓
Execution + immutable audit log entry
       ↓
Telemetry exported to Prometheus
```

Read-only operations (listing containers, reading logs) execute immediately. Destructive operations (stopping containers, deleting namespaces, scaling down) are held in a pending state until the human operator explicitly approves.

---

## Tools exposed via MCP

**Docker**
- `docker_list_containers` — list running containers and status
- `docker_get_logs(container_id, tail)` — tail container logs
- `docker_start_container(container_id)` — start a stopped container
- `docker_stop_container(container_id)` — ⚠️ requires HITL approval
- `docker_remove_container(container_id)` — ⚠️ requires HITL approval

**Kubernetes**
- `k8s_get_pods(namespace)` — list pods and health
- `k8s_scale_deployment(namespace, deployment, replicas)` — ⚠️ requires HITL approval
- `k8s_delete_namespace(namespace)` — ⚠️ requires HITL approval
- `k8s_get_events(namespace)` — fetch recent cluster events

---

## HITL approval flow

Any tool marked ⚠️ triggers the approval protocol:

1. Agent generates a **Plan of Action** describing what will change and why
2. Server holds execution, returns a `pending_approval` response with a unique `action_id`
3. Operator reviews the PoA and signs off via the approval endpoint
4. Server executes, logs the result with the operator's identity and timestamp

No destructive action executes without a matching approval record in the audit log.

---

## Observability

P95/P99 execution latencies exported to Prometheus on `:9090/metrics`. Every MCP tool call is instrumented — not just the destructive ones.

```
mcp_tool_duration_seconds{tool="docker_stop_container", quantile="0.95"}
mcp_tool_duration_seconds{tool="k8s_scale_deployment", quantile="0.99"}
mcp_hitl_pending_approvals_total
mcp_hitl_approved_total
mcp_hitl_rejected_total
```

---

## Stack

| Layer | Technology |
|---|---|
| Language | Kotlin / Spring Boot |
| Protocol | Model Context Protocol (MCP) SDK |
| Docker integration | Docker Engine API (via Fabric8 or native HTTP) |
| Kubernetes integration | Kubernetes Client Java |
| Audit log | Actor model — immutable append-only via Kotlin coroutines |
| Metrics | Micrometer → Prometheus |

---

## Project structure

```
mcp-systems-controller/
├── src/
│   └── main/
│       └── kotlin/
│           └── com/systems/mcp/
│               ├── server/
│               │   └── McpServer.kt         # MCP tool registration and routing
│               ├── hitl/
│               │   ├── ApprovalGate.kt      # Async approval flow
│               │   └── AuditLog.kt          # Immutable audit trail (actor model)
│               ├── tools/
│               │   ├── DockerTools.kt       # Docker MCP tool implementations
│               │   └── KubernetesTools.kt   # K8s MCP tool implementations
│               └── telemetry/
│                   └── MetricsExporter.kt   # Prometheus instrumentation
├── build.gradle.kts
├── decisions.md
└── README.md
```

---

## Architectural decisions

Full reasoning in [`decisions.md`](./decisions.md). Key choices:

- **Why Kotlin/Spring Boot over Python?** — Type safety matters when you're issuing infrastructure commands. A mistyped namespace in Python fails at runtime; Kotlin catches it at compile time.
- **Why actor model for the audit log?** — Guarantees sequential, non-concurrent writes without explicit locking. Same reasoning as event sourcing in production systems.
- **Why async HITL?** — The agent shouldn't block waiting for human approval. The PoA is stored, the agent continues other work, execution resumes when approval arrives.

---

## References

- [Model Context Protocol specification](https://modelcontextprotocol.io)
- [Docker Engine API](https://docs.docker.com/engine/api/)
- [Kubernetes Client Java](https://github.com/kubernetes-client/java)