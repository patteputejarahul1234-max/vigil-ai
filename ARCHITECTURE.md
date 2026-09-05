# Vigil AI — Stage 5 Architecture: Microservices & Event-Driven Design

This document describes how Vigil AI's current monolith would evolve
into a microservices architecture with Kafka-based event communication.
It is a design document, not running code — the same reasoning a team
would write up before committing months of engineering time to a
migration, and a normal, legitimate artifact to bring to an interview.

## Why split at all, and why now (not from day one)

Vigil AI started as — and still is — a single Spring Boot monolith.
That was the right call for Stages 1-4: one team (of one), one deploy
target, fast iteration. Splitting into microservices *first* would have
multiplied complexity (auth, logging, transactions, deployment) with
zero payoff at this scale. The point to split is when different parts
of the system have genuinely different scaling needs or release
cadences — which is where Vigil AI is heading once real usage grows.

## Proposed service boundaries

| Service | Owns | Why it's separate |
|---|---|---|
| **Auth Service** | Users, JWT issuance, OAuth | Security-critical, changes rarely, needs to be rock-solid and independently auditable |
| **Task Service** | Workspaces, projects, tasks, files | The core CRUD workload — highest request volume, benefits from independent horizontal scaling |
| **AI Service** | Proof of Execution, AI Assistant, Smart Suggestions | Calls external AI APIs (Gemini) with variable latency — isolating this means a slow AI call never blocks task CRUD operations |
| **Notification Service** | In-app notifications, email, escalation alerts | Fundamentally async and event-driven by nature — the natural first candidate to extract |

## Event flow with Kafka

Instead of the Task Service calling the Notification Service directly
(a synchronous HTTP call that couples their uptime together), services
publish events to Kafka topics and react independently:

```
Task Service                Kafka Topics                 Consumers
─────────────               ────────────                 ─────────
task.status.changed   ──►   "task-events"          ──►   Notification Service
                                                     ──►   AI Service (for Smart
                                                           Suggestion recalculation)

proof.submitted        ──►   "proof-events"         ──►   Notification Service
                                                     ──►   Task Service (mark DONE)

workspace.member.added ──►   "workspace-events"     ──►   Notification Service
```

**Concrete example — the escalation feature from the original product
idea:** when a task is missed repeatedly, instead of the Task Service
directly calling an SMS/email API (tight coupling, and a slow email
provider could back up task updates), it publishes a
`task.missed.repeatedly` event. A dedicated Escalation Service consumes
that event, decides whether to notify a trusted contact, and does so —
completely decoupled from the Task Service's own health and uptime.

## Why this matters over direct HTTP calls between services

- **Resilience**: if the Notification Service is down, task events queue
  up in Kafka and get processed once it recovers — nothing is lost,
  nothing blocks
- **Independent scaling**: the AI Service can be scaled up during peak
  proof-submission times without touching the Task Service's replica count
- **Loose coupling**: new consumers (e.g., a future analytics pipeline)
  can subscribe to existing topics without any change to the publishing
  service

## Honest status

This is the target architecture, not the current state. The current
codebase is a well-structured monolith with clear internal service
boundaries (`WorkspaceService`, `TaskService`, `AiAssistantService`,
etc.) that already map cleanly onto the services proposed above — that
internal separation is what makes an eventual split realistic rather
than a rewrite.

## What CI/CD and monitoring already do today (not just planned)

Unlike Kafka/Kubernetes, two Stage 5 pieces are genuinely running:
- **CI/CD** (`.github/workflows/ci.yml`) runs on every push, on GitHub's
  own infrastructure — no local hardware involved at all
- **Monitoring** (Spring Boot Actuator + Prometheus metrics) runs
  locally today with negligible overhead — see `MONITORING.md`
