# MCP Systems Controller — Presentation Script

---

## Slide 1 — Introduction

Hi there! My name is Radesh Govind, and today I'm going to present to you a project I've been working on lately. It's called **MCP Systems Controller**.

This project is a Proof of Concept designed to bridge the gap between **AI Agents** from our **sensitive infrastructure**, while ensuring a safety layer through what we call Human-in-the-Loop. So, let's deep dive into why this project was built.

---

## Slide 2 — The Problem

The core problem is straightforward. AI Agents are incredibly smart — but they are **non-deterministic**. We cannot be one hundred percent sure of what an agent will do at any given moment. It can have hallucinations, it can misinterpret context, or it can simply make a mistake.

On the other hand, we have our infrastructure — Docker, Kubernetes — which is **highly deterministic** and extremely sensitive to any kind of mistake. Even the smallest wrong command can lead to downtime, data loss, or a cascading failure in production.

So if we give an AI agent direct, unrestricted access to our infrastructure, we are essentially handing it the keys to the entire system, with no safety net. That is the problem this project addresses. We need something in the middle — a layer that keeps a close eye on any operation that can change the state of our infrastructure.

---

## Slide 3 — The Architecture

To solve this, I built a **unidirectional pipeline**. Let me walk you through it.

On one end, we have the **MCP Client** — in practice, something like Claude Desktop — which sends a request through the **Model Context Protocol** to our server. The first thing that request hits is the **Tool Registry**, which acts as a router.

From there, the request goes one of two ways.

If the agent is performing a **read-only operation** — like listing containers or fetching pod logs — it takes the fast path and executes immediately. No gate, no wait. These are safe, non-destructive calls.

But if the agent wants to **change state** — stop a container, scale down a deployment, delete a namespace — the request hits the **Approval Gate**. The action is immediately suspended. A **Plan of Action** is created, capturing what the agent wants to do, on what resource, and why. That plan is sent to the **Audit Log**, and the entire execution is paused until a **human operator** explicitly approves it.

Only after that sign-off does the system reach the actual Docker or Kubernetes APIs. That is the guarantee this pipeline provides: no destructive action ever reaches production infrastructure without a human in the loop.

---

## Slide 4 — The Safety Components

Let's zoom in on the two most important components: the **Approval Gate** and the **Audit Log**.

The **Approval Gate** is what intercepts every destructive tool call. It holds the Plan of Action in a `PENDING` state, returns a response to the agent so it can move on to other tasks, and waits asynchronously for a human decision. The operator can approve or reject the action through a REST endpoint. If approved, the gate hands execution back to the infrastructure layer. If rejected, it's logged and discarded. Either way, nothing happens without an explicit decision.

The **Audit Log** uses an **Actor Model**. Under the hood, it is a single coroutine with an unbounded mailbox channel, processing entries sequentially. This is a critical design choice — because a single writer guarantees no race conditions, no concurrent writes, and a perfectly ordered history. Every intent, approval, execution, and failure is recorded with the action ID, the tool name, the target resource, the status, and the identity of the operator who made the call. This gives us a tamper-resistant, chronologically consistent audit trail.

Together, these two components are what transform a standard MCP server into a **trust layer** between AI agents and production infrastructure.

---

## Slide 5 — Transition to Demo

Now that we've covered the architecture, let's watch the system in action. I'm going to simulate an AI agent trying to stop a Docker container, and show you step by step how the safety layer catches it in real time.

---

## Slide 6 — Live Demo

**Step 1 — Start the server.**
Let's bring the server up. Running `./gradlew bootRun` — you can see it's live on port 8080, with Spring Boot initialized and MCP tools registered.

**Step 2 — A safe, read-only call.**
First, let's call the `dockerListContainers` tool. This executes immediately because it's non-destructive. The agent gets a clean list of all running and stopped containers. No gate, no wait.

**Step 3 — Provoking a destructive operation.**
Now, let's simulate the agent trying to stop a container called `test-nginx`. I'll fire that request through the `simulate-agent` endpoint.

**Step 4 — The system reacts.**
Look at the server logs. We see a warning: *"Destructive action suspended"*. The Audit Log records the Plan of Action with status `PENDING`. The agent receives a response telling it the action is under review and to continue with other tasks. Nothing has touched Docker yet.

**Step 5 — The human approves — and we get an error.**
As the operator, I'll approve this action now via the resolve endpoint. But watch what happens — we get a 404 from Docker. The container doesn't exist. And this is actually the correct behavior. The system didn't fabricate a success. It executed the real Docker API call, got a real error back, and surfaced that error faithfully. That's **reliable execution** — the system always reflects real-world state.

**Step 6 — Fixing the infrastructure.**
Let's create that container now. I'll run `docker run -d --name test-nginx nginx` in the terminal. Container is up and running. We can list containers again and confirm it's there.

**Step 7 — The successful flow.**
Let's re-submit that stop command. Suspended again, as expected. I approve it — and this time we get back: *"Successfully stopped container test-nginx."* Docker Desktop confirms the container is in stopped state. `docker ps` shows it's no longer running.

**Step 8 — The proof.**
Back in the logs, we can trace the full lifecycle: `PENDING` → `APPROVED` → `EXECUTED`. Every state transition is captured in the Audit Log with timestamps and operator identity. That is the complete cycle — from agent intent to safe, traceable execution.

---

## Slide 7 — Tech Stack & Observability

A quick note on why this stack was chosen.

I used **Kotlin and Spring Boot** because infrastructure control demands type safety. A mistyped key in a dynamically typed language fails at runtime, potentially mid-execution. Kotlin catches those errors at compile time before anything reaches a real cluster.

The bridge between the AI agent and the server is the **Model Context Protocol** — an open standard that allows any MCP-compatible client to discover and call the tools exposed by this server, without any custom integration per agent.

For observability, I integrated **Micrometer** with a Prometheus-compatible endpoint. Every tool call is instrumented — read or destructive — with P95 and P99 latency histograms. And for HITL specifically, we track counters for total pending approvals, approved actions, and rejected actions in real time. You can plug this directly into any production monitoring dashboard.

---

## Slide 8 — Conclusion

As the industry moves from LLMs that *talk* to agents that *act*, the question of trust becomes critical. This project is an attempt to answer that question with a concrete, architectural pattern rather than a policy or a guideline.

We don't have to choose between the speed of AI orchestration and the safety that production infrastructure demands. With a structured approval gate, an immutable audit trail, and a protocol like MCP to standardize how agents call tools — we can have both.

This is still a Proof of Concept. There is a lot of room to grow: persistent storage for the audit log, role-based approval routing, multi-step approval chains, policy-as-code for automatic pre-approval of low-risk operations. The foundation is here — and it is open.

If you have ideas, if you see a better way to design the approval gate, if you want to extend this to other infrastructure targets — the repository is live, and I want to hear from you. Open a PR, start a discussion in the issues, challenge the architecture.

I'm Radesh Govind. Thank you for watching.
