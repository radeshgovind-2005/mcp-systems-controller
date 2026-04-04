# Slides Specification — MCP Systems Controller
> Rebuild guide for Keynote. Each slide maps 1:1 to the script.
> Suggested theme: dark background (near-black `#0D1117`), white/light-gray text, accent color `#58A6FF` (GitHub blue) or a cool teal. Monospace font for all code/labels.

---

## Slide 1 — Title

**Layout:** Centered, full bleed.

**Content:**
```
MCP Systems Controller

A Human-in-the-Loop Safety Layer for Agentic Infrastructure

Radesh Govind
github.com/[your-handle]/mcp-systems-controller
```

**Visual notes:**
- Title in large, bold sans-serif (e.g. 52–60pt)
- Subtitle in lighter weight, muted color (~30pt)
- Name + GitHub link at the bottom, small (16–18pt)
- Optional: faint grid or circuit-board texture in background to suggest infrastructure

**Speaker cue:** Introduction — say your name, project name, one-line summary.

---

## Slide 2 — The Problem

**Layout:** Two-column split, equal width. Vertical divider line between columns.

**Left column — "AI Agents"**
```
AI Agents

✦ Incredibly capable
✦ Non-deterministic
✦ Prone to hallucinations
✦ Can misinterpret context
✦ Can make mistakes
```

**Right column — "Infrastructure"**
```
Infrastructure (Docker / K8s)

✦ Highly deterministic
✦ Extremely sensitive
✦ One wrong command →
    downtime, data loss,
    cascading failure
```

**Bottom, full width — centered call-out box:**
```
Giving an AI agent unrestricted access to your infrastructure
means handing it the keys to your entire system — with no safety net.
```

**Visual notes:**
- Left column accent: amber/yellow (`#F0A500`) — signals uncertainty/risk
- Right column accent: red (`#E05252`) — signals criticality
- Call-out box: dark border, slightly lighter background, bold italic text
- Vertical divider: thin, muted line

**Speaker cue:** Explain non-determinism vs. determinism, then read the call-out as the punchline.

---

## Slide 3 — The Architecture

**Layout:** Full-width horizontal pipeline diagram. Title at top. Diagram center-stage. One-line caption below.

**Pipeline diagram (left → right, connected by arrows):**

```
[ MCP Client ]  →  [ Tool Registry ]  →  [ Approval Gate ]  →  [ Audit Log ]  →  [ Docker / K8s APIs ]
  Claude Desktop      Router / Dispatcher    HITL Checkpoint       Actor Model       Real Infrastructure
```

**Below the Tool Registry, add a branch downward:**
```
                          ↓ (read-only)
                   [ Direct Execution ]
                   list, logs, get pods
```

**Visual notes:**
- Each node is a rounded rectangle
- Color code:
  - MCP Client: blue (`#58A6FF`)
  - Tool Registry: neutral gray
  - Approval Gate: amber (`#F0A500`) — the checkpoint
  - Audit Log: teal (`#3FB68B`)
  - Docker/K8s APIs: red (`#E05252`) — the sensitive target
  - Direct Execution branch: green (`#2ECC71`) — safe fast path
- Arrows between nodes are solid; the read-only branch arrow is dashed
- Small label under each node in smaller muted text (the subtitle lines above)

**Caption at bottom:**
```
Unidirectional. No destructive action reaches the infrastructure without a human sign-off.
```

**Speaker cue:** Trace the pipeline left to right with your cursor as you speak.

---

## Slide 4 — The Safety Components

**Layout:** Two-column split. Each column is one component. Title at top.

**Title:**
```
The Trust Layer
```

**Left column — Approval Gate**
```
Approval Gate

Intercepts every destructive call.
Holds the Plan of Action → PENDING
Agent continues other tasks (async).
Human approves or rejects via REST.
Only then does execution proceed.
```

**Right column — Audit Log**
```
Audit Log

Actor Model — single coroutine writer.
Unbounded mailbox channel.
No race conditions. No concurrent writes.
Immutable, sequential history.

Records: Action ID · Tool · Target
         Status · Operator · Timestamp
```

**Bottom, full width — centered:**
```
PENDING  →  APPROVED  →  EXECUTED
           (or REJECTED)
```
> Style this as a state machine: three rounded pill shapes connected by arrows.

**Visual notes:**
- Left column accent: amber (Gate = checkpoint)
- Right column accent: teal (Log = record)
- State machine at bottom: PENDING gray → APPROVED green → EXECUTED blue; REJECTED in red branching off APPROVED
- Monospace font for the status labels

**Speaker cue:** Walk left column first, then right, then point at the state machine at the bottom.

---

## Slide 5 — Demo Transition

**Layout:** Centered, minimal. Dark background, large single statement.

**Content:**
```
Let's watch it in action.
```

Sub-line:
```
Simulating an AI agent attempting to stop a Docker container.
```

**Visual notes:**
- Single large statement: 48–52pt, bold
- Sub-line: 22pt, muted/italic
- Optional: subtle animated pulse or blinking cursor on the sub-line if Keynote animations are used
- No bullets, no diagram — let the whitespace breathe

**Speaker cue:** Short bridge. Say one sentence, then switch to screen recording.

---

## Slide 6 — Live Demo

**Layout:** This slide stays up during the screen recording. It should be a simple reference card — visible but not distracting.

**Title:**
```
Live Demo — The Full Cycle
```

**Content (numbered steps, two columns if needed):**
```
1. Start the server           ./gradlew bootRun
2. Read-only call             GET /dockerListContainers   → executes immediately
3. Destructive request        POST /simulate-agent        → suspended, PENDING
4. System reacts              Audit Log: PENDING · No Docker call made yet
5. Approve (container missing) POST /resolve/approve      → 404 from Docker (correct!)
6. Create the container       docker run -d --name test-nginx nginx
7. Re-submit & approve        POST /simulate-agent → approve → SUCCESS
8. Verify the audit trail     Logs: PENDING → APPROVED → EXECUTED
```

**Visual notes:**
- Left column: step number + description
- Right column: command or action in monospace, color-coded:
  - Green for safe/read calls
  - Amber for the submission (pending)
  - Red for the 404 (expected error)
  - Green again for success
- Keep font small enough (16–18pt) so all 8 steps fit without crowding

**Speaker cue:** You don't need to read this slide — it's a roadmap for the audience while they watch the recording.

---

## Slide 7 — Tech Stack & Observability

**Layout:** Two sections stacked. Top: stack table. Bottom: metrics snippet.

**Title:**
```
Why This Stack?
```

**Top — Stack table (3 columns: Layer | Technology | Why)**
```
Language         Kotlin / JVM          Compile-time type safety for infrastructure commands
Framework        Spring Boot 3.4       Production-ready observability out of the box
Protocol         Model Context Protocol  Open standard — any MCP client works without integration
Docker           docker-java client    Native Docker Engine API
Kubernetes       kubernetes-client     Official K8s Java client
Metrics          Micrometer → Prometheus  P95/P99 latency histograms + HITL counters
Audit            Kotlin Coroutines     Actor model — single writer, no locking
```

**Bottom — Metrics snippet (code block style):**
```
mcp_tool_duration_seconds{tool="docker_stop_container", quantile="0.95"}
mcp_hitl_pending_approvals_total
mcp_hitl_approved_total
mcp_hitl_rejected_total
```

**Visual notes:**
- Table: alternating row shading (very subtle), header row in accent color
- Metrics block: dark inset box, monospace font, syntax-highlighted in muted green/teal
- "Why" column in lighter muted text — supporting detail, not headline

**Speaker cue:** Don't read the table row by row. Highlight Kotlin (type safety) and MCP (standardization), then call out the metrics block.

---

## Slide 8 — Conclusion

**Layout:** Three-point structure. Title at top. Three bold statements as the body. Closing line at the bottom.

**Title:**
```
From Chatbots to Action Agents — Safely.
```

**Three statements (large, one per line, with a small supporting line under each):**

```
01  We don't have to choose.
    Speed of AI orchestration + deterministic safety of infrastructure. Both.

02  This is a foundation, not a finish line.
    Persistent audit storage · Role-based approvals · Policy-as-code · Multi-step chains.

03  It's open. Let's build it together.
    Pull Requests · Issues · Architecture debates — all welcome.
```

**Bottom:**
```
Radesh Govind
github.com/[your-handle]/mcp-systems-controller
```

**Visual notes:**
- The three numbered statements should be visually dominant — 26–30pt, bold
- Supporting lines in 16–18pt, muted
- Numbers `01` `02` `03` in accent color, oversized (maybe 48pt), positioned left as a visual anchor
- Name + link same style as Slide 1 footer for consistency
- Optional: subtle horizontal rule between each statement

**Speaker cue:** Slow down here. One statement at a time. End on the GitHub call-to-action, then say your name and sign off.

---

## General Design Rules

| Element | Recommendation |
|---|---|
| Background | `#0D1117` (GitHub dark) or similar near-black |
| Primary text | `#E6EDF3` (off-white) |
| Accent / highlight | `#58A6FF` (blue) |
| Safe / success | `#2ECC71` (green) |
| Warning / gate | `#F0A500` (amber) |
| Danger / destructive | `#E05252` (red) |
| Code / labels | Monospace (JetBrains Mono, Fira Code, or SF Mono) |
| Headings | Bold sans-serif (SF Pro, Inter, or similar) |
| Slide size | 16:9 widescreen |
| Transitions | None or simple cross-dissolve — keep it clean |
| Animations | Only use "appear" for revealing pipeline steps on Slide 3 |
