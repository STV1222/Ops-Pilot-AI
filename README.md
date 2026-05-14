# OpsPilot AI

> **AI-powered Operations Workflow Platform for SMEs**  
> Automate procurement workflows with a ReAct AI Agent, durable orchestration, and a modular Skill runtime — not a chatbot, not a wrapper. Real agentic AI that autonomously decides which tools to call, in what order, and when it is done.

---

## Table of Contents

- [Overview](#overview)
- [The Problem](#the-problem)
- [How It Works](#how-it-works)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Key Design Concepts](#key-design-concepts)
- [Repository Structure](#repository-structure)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [What This Project Demonstrates](#what-this-project-demonstrates)
- [Production Roadmap](#production-roadmap)

---

## Overview

OpsPilot AI is a full-stack workflow automation platform that gives SMEs the ability to install and run reusable AI **Skills** — modular units of business logic powered by a real AI Agent, document processing, and durable orchestration.

The MVP ships one production-ready skill: **Quotation Comparison** — a fully automated procurement workflow that takes supplier PDFs and Excel files as input and produces an AI-generated recommendation report routed to a manager for approval.

The core execution engine is a **ReAct-pattern AI Agent** (Reason + Act loop). Rather than a hardcoded pipeline, the LLM autonomously decides which tools to call, inspects their results, and loops until analysis is complete. The agent has access to five tools (`extract_items`, `detect_anomalies`, `compare_quotations`, `generate_report`, `submit_report`) and orchestrates them through a multi-turn conversation with the LLM — terminating only when it calls `submit_report` with the finished recommendation.

---

## The Problem

A procurement manager at a small business routinely:

1. Receives 3–5 supplier quotations by email (PDF or Excel)
2. Opens each file and manually reads prices, quantities, and terms
3. Builds a comparison spreadsheet by hand
4. Looks for anomalies — price spikes, missing quantities, inconsistent specs
5. Writes a recommendation memo for their manager
6. Waits for approval before placing an order

**This takes 1–3 hours per procurement cycle and is error-prone.**

OpsPilot AI automates steps 2–6 entirely. The user uploads the files. The platform does the rest in under 30 seconds.

---

## How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│  User uploads 2+ quotation files (PDF / Excel) via dashboard    │
└────────────────────────────┬────────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Spring Backend  │  Reads files, extracts text
                    │  (WebFlux)       │  PDFBox · Apache POI
                    └────────┬────────┘
                             │  Persists docs to PostgreSQL
                             │  Fires Temporal workflow
                             │
                    ┌────────▼────────┐
                    │    Temporal      │  Durable, retry-safe execution
                    │  Workflow Engine │  Full audit trail in UI
                    └────────┬────────┘
                             │
                    ┌────────▼────────────────────────────────┐
                    │         Skill Runtime                    │
                    │  quotation-comparison skill invokes:     │
                    │                                          │
                    │         ┌──────────────────────┐        │
                    │         │  ReAct AI Agent Loop  │        │
                    │         │  (up to 12 iterations)│        │
                    │         │                       │        │
                    │         │  LLM decides which    │        │
                    │         │  tools to call and    │        │
                    │         │  in what order:       │        │
                    │         │                       │        │
                    │         │  → extract_items      │        │
                    │         │  → detect_anomalies   │        │
                    │         │  → compare_quotations │        │
                    │         │  → generate_report    │        │
                    │         │  → submit_report      │        │
                    │         │    (terminates loop)  │        │
                    │         └──────────────────────┘        │
                    └────────┬────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │ PENDING_APPROVAL │  Report saved to PostgreSQL
                    │                 │  Manager notified via dashboard
                    └────────┬────────┘
                             │  Manager clicks Approve / Reject
                    ┌────────▼────────┐
                    │   COMPLETED     │  Workflow closed, audit trail preserved
                    └─────────────────┘
```

---

## System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                         Browser                              │
│          Next.js 14 · React · TypeScript · Tailwind CSS      │
│    Dashboard  /  Upload  /  Workflow Detail  /  Report       │
└─────────────────────────┬────────────────────────────────────┘
                          │  HTTP REST · CORS
┌─────────────────────────▼────────────────────────────────────┐
│                  Spring Boot 3 Backend                        │
│              Java 21 · WebFlux (non-blocking I/O)             │
│                                                               │
│   ┌─────────────────┐      ┌──────────────────────────────┐  │
│   │   Controllers   │      │       Skill Runtime           │  │
│   │   AuthController│      │  FileSystemSkillLoader        │  │
│   │   WorkflowCtrl  │      │  SkillRegistry                │  │
│   └────────┬────────┘      │  QuotationComparisonExecutor  │  │
│            │               └──────────────┬───────────────┘  │
│   ┌────────▼───────────────────────────────▼──────────────┐  │
│   │                 AI Agent (ReAct Loop)                  │  │
│   │   AgentLoop  ·  AgentTool interface  ·  AgentResult    │  │
│   │   ──────────────────────────────────────────────────   │  │
│   │   LLM conversation history → tool call → result →     │  │
│   │   append to history → repeat until submit_report       │  │
│   └────────────────────────┬──────────────────────────────┘  │
│                            │  tool implementations            │
│   ┌────────────────────────▼──────────────────────────────┐  │
│   │                 Application Services                   │  │
│   │   AIExtractionService  ·  AIComparisonService          │  │
│   │   AIReportGenerationService  ·  AnomalyDetectorService  │  │
│   └────────────────────────┬──────────────────────────────┘  │
│                            │                                  │
│   ┌────────────────────────▼──────────────────────────────┐  │
│   │                  MCP Tool Layer                        │  │
│   │          PdfTool · ExcelTool · EmbeddingSearchTool     │  │
│   └───────────────────────────────────────────────────────┘  │
└─────────────────────────┬────────────────────────────────────┘
                          │
          ┌───────────────┼─────────────────┐
          ▼               ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐
│  PostgreSQL  │  │    Redis     │  │       Temporal         │
│  + pgvector  │  │    Cache     │  │   Workflow Engine       │
│              │  │              │  │                        │
│  workflows   │  │              │  │  Worker registered on  │
│  documents   │  │              │  │  quotation-comparison  │
│  reports     │  │              │  │  task queue            │
│  anomalies   │  │              │  │  Temporal UI: :8088    │
└──────────────┘  └──────────────┘  └──────────┬────────────┘
                                               │
                                   ┌───────────▼───────────┐
                                   │     OpenRouter API     │
                                   │   GPT-4o-mini (default)│
                                   │   model-agnostic layer │
                                   └───────────────────────┘
```

---

## Tech Stack

### Backend

| Technology | Version | Role |
|---|---|---|
| **Java** | 21 | Core language — records, sealed types, virtual threads |
| **Spring Boot** | 3.3 | Application framework — DI, security, actuator |
| **Spring WebFlux** | — | Reactive, non-blocking HTTP server — multipart streaming without thread-per-request |
| **Gradle** | 8.10 | Build system |

### Workflow Orchestration

| Technology | Version | Role |
|---|---|---|
| **Temporal** | 1.25.2 | Durable workflow engine — automatic retries, state recovery, execution history, task queues |

### AI & Document Processing

| Technology | Role |
|---|---|
| **OpenRouter** | Unified LLM API gateway — swap models without code changes |
| **GPT-4o-mini** | Default model for extraction, comparison, and report generation |
| **Apache PDFBox** | PDF text extraction |
| **Apache POI** | Excel / XLSX extraction |

### Storage

| Technology | Role |
|---|---|
| **PostgreSQL 16** | Primary store — workflows, documents, reports, quotation items, anomalies |
| **pgvector** | Vector embedding extension — semantic document retrieval (foundation layer) |
| **Redis 7** | Caching and session store |

### Frontend

| Technology | Role |
|---|---|
| **Next.js 14** | React framework — App Router, server components |
| **TypeScript** | Type-safe client code |
| **Tailwind CSS** | Utility-first styling |
| **react-markdown** | Renders AI-generated reports as formatted HTML |

### DevOps & Observability

| Technology | Role |
|---|---|
| **Docker + Compose** | Full local stack — 6 services, one command |
| **OpenTelemetry** | Structured tracing and metrics (wired, exporter-ready) |
| **JSON structured logging** | Machine-readable logs for aggregation pipelines |

---

## Key Design Concepts

### AI Agent — ReAct Loop

The quotation-comparison workflow is executed by a **ReAct-pattern agent** (Reason + Act), not a hardcoded pipeline. The LLM drives execution by deciding which tools to call based on results it has already seen.

```
User prompt + document text
         │
         ▼
   ┌──────────────────────────────────────────────────┐
   │              AgentLoop (max 12 iterations)        │
   │                                                   │
   │  1. Send conversation history + tool definitions  │
   │     to LLM via OpenRouter                         │
   │                                                   │
   │  2a. finish_reason = "tool_calls"                 │
   │      → execute each tool                          │
   │      → append tool result to history              │
   │      → if tool == submit_report → DONE            │
   │      → else → next iteration                      │
   │                                                   │
   │  2b. finish_reason = "stop"                       │
   │      → LLM decided it's finished                  │
   └──────────────────────────────────────────────────┘
```

**Five agent-callable tools:**

| Tool | Type | What it does |
|---|---|---|
| `extract_items` | AI (LLM) | Parses raw document text into structured line items |
| `detect_anomalies` | Rule engine | Flags price deviations >15%, missing quantities |
| `compare_quotations` | AI (LLM) | Ranks suppliers by total cost and value |
| `generate_report` | AI (LLM) | Produces a markdown procurement recommendation |
| `submit_report` | Terminal | Submits the finished report — ends the agent loop |

Each tool is a Spring `@Component` implementing the `AgentTool` interface. The LLM sees tool names, descriptions, and JSON Schema parameter specs — it constructs the correct arguments autonomously. The agent's conversation history grows with each tool call and result, giving the LLM full context to make informed decisions across iterations.

### AI Skill Runtime

The core innovation. Instead of hardcoding automation logic, the platform uses a **modular Skill system** where each skill is a self-contained folder:

```
skills/
└── quotation-comparison/
    ├── config.json        # skill metadata, version, input schema
    ├── workflow.json      # ordered execution steps
    ├── prompts/
    │   └── report.md      # AI prompt templates
    └── SKILL.md           # human-readable documentation
```

| Component | Responsibility |
|---|---|
| `SkillLoader` | Reads skill definitions from the filesystem at startup |
| `SkillRegistry` | Indexes skills by name, available across the application |
| `SkillExecutor` | Executes a skill given a `SkillContext` (input data, org, prompts) |

New business automation capabilities (`invoice-matching`, `contract-review`, `vendor-onboarding`) can be added by dropping a folder — **no Java code changes required**. This is the foundation for a skill marketplace.

### MCP Tool Layer

Skills interact with external resources through a **Model Context Protocol-inspired tool abstraction** that decouples AI reasoning from data access:

| Component | Responsibility |
|---|---|
| `Tool` | Interface — named, executable capability |
| `ToolRegistry` | Auto-wires all `Tool` Spring beans at startup |
| `ToolExecutor` | Dispatches tool calls by name at runtime |

Built-in tools: `PdfTool`, `ExcelTool`, `EmbeddingSearchTool`

Tools are independently testable — mock them without touching AI services.

### Manager Approval Workflow

Workflows do not auto-complete. When AI finishes processing:

```
RUNNING → PENDING_APPROVAL → (manager action) → COMPLETED or FAILED
```

The dashboard surfaces approve/reject controls when a workflow reaches `PENDING_APPROVAL`, giving humans a checkpoint before operational decisions are acted on.

### Why WebFlux?

File uploads are I/O-bound. WebFlux handles multipart streaming reactively without blocking OS threads — critical for concurrent upload throughput. All JDBC calls are dispatched to `Schedulers.boundedElastic()` to stay off reactor event-loop threads.

### Why Temporal?

AI workflows are async, failure-prone, and long-running. Temporal provides:
- Durable execution that survives process restarts
- Automatic activity retries with backoff
- Full execution history visible in the Temporal UI
- Task queue-based worker scaling

---

## Repository Structure

```
.
├── backend/
│   └── src/main/java/com/opspilot/
│       ├── application/
│       │   ├── agent/               # AgentLoop (ReAct engine), AgentTool interface, AgentResult
│       │   │   └── tools/           # ExtractItemsAgentTool, DetectAnomaliesAgentTool, CompareQuotationsAgentTool
│       │   │                        # GenerateReportAgentTool, SubmitReportAgentTool
│       │   ├── ai/                  # AIExtractionService, AIComparisonService, AIReportGenerationService
│       │   ├── anomaly/             # AnomalyDetectorService (rule engine)
│       │   ├── document/            # DocumentExtractor interface
│       │   ├── skill/               # SkillLoader, SkillRegistry, SkillExecutor, QuotationComparisonSkillExecutor
│       │   ├── tool/                # Tool, ToolRegistry, ToolExecutor
│       │   └── workflow/            # QuotationComparisonService
│       ├── domain/
│       │   └── workflow/            # WorkflowExecution record, WorkflowStatus enum
│       ├── infrastructure/
│       │   ├── ai/                  # OpenRouterClient, OpenRouterProperties
│       │   ├── config/              # SecurityConfig (CORS, JWT)
│       │   ├── document/            # PdfExtractor (PDFBox), ExcelExtractor (POI)
│       │   ├── persistence/         # WorkflowExecutionRepository, DocumentRepository, ReportRepository
│       │   ├── skill/               # FileSystemSkillLoader
│       │   ├── temporal/            # WorkflowImpl, ActivitiesImpl, TemporalConfig
│       │   └── tool/                # PdfTool, ExcelTool, EmbeddingSearchTool
│       └── interfaceadapters/
│           └── web/                 # AuthController, WorkflowController
├── frontend/
│   └── app/
│       ├── dashboard/               # Workflow list
│       ├── login/                   # Authentication
│       └── workflows/
│           ├── [id]/                # Workflow detail + approve/reject
│           ├── [id]/report/         # Rendered markdown report
│           ├── [id]/logs/           # Execution stage logs
│           └── upload/              # File upload
├── skills/
│   └── quotation-comparison/        # Skill definition files
├── docker-compose.yml               # Full 6-service local stack
├── .env.example                     # Environment variable template
└── README.md
```

---

## Quick Start

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- An [OpenRouter](https://openrouter.ai/) API key (free tier works)

### Setup

```bash
# 1. Clone the repository
git clone https://github.com/STV1222/Ops-Pilot-AI.git
cd Ops-Pilot-AI

# 2. Configure environment
cp .env.example .env
# Edit .env and add your OPENROUTER_API_KEY

# 3. Start the full stack
docker compose up --build
```

### Access Points

| Service | URL |
|---|---|
| Frontend dashboard | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Backend health | http://localhost:8080/actuator/health |
| Temporal UI | http://localhost:8088 |

### Run a Workflow via the UI

1. Open `http://localhost:3000`
2. Click **Login** → **Continue**
3. Click **New Workflow**
4. Upload 2 supplier quotation files (PDF or Excel)
5. Click **Run Workflow** — you are redirected to the workflow detail page
6. Watch status auto-update from `RUNNING` → `PENDING_APPROVAL` (~15s)
7. Click **View Report** to review the AI-generated comparison
8. Click **Approve & Complete**

### Run via curl

```bash
# Authenticate
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@opspilot.local"}'

# Upload quotation documents and start workflow
curl -X POST http://localhost:8080/api/v1/workflows/quotation-comparison \
  -H "Authorization: Bearer dev-jwt-token-admin@opspilot.local" \
  -F "files=@quote-a.pdf" \
  -F "files=@quote-b.xlsx"

# Poll status
curl http://localhost:8080/api/v1/workflows/{id} \
  -H "Authorization: Bearer dev-jwt-token-admin@opspilot.local"

# Fetch generated report
curl http://localhost:8080/api/v1/workflows/{id}/report \
  -H "Authorization: Bearer dev-jwt-token-admin@opspilot.local"

# Approve
curl -X POST http://localhost:8080/api/v1/workflows/{id}/approve \
  -H "Authorization: Bearer dev-jwt-token-admin@opspilot.local"
```

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Authenticate and receive JWT token |
| `POST` | `/api/v1/workflows/quotation-comparison` | Upload documents and start workflow |
| `GET` | `/api/v1/workflows` | List all workflow executions |
| `GET` | `/api/v1/workflows/{id}` | Get workflow status and metadata |
| `GET` | `/api/v1/workflows/{id}/report` | Get AI-generated markdown report |
| `GET` | `/api/v1/workflows/{id}/logs` | Get execution stage logs |
| `POST` | `/api/v1/workflows/{id}/approve` | Manager approves → `COMPLETED` |
| `POST` | `/api/v1/workflows/{id}/reject` | Manager rejects → `FAILED` |

---

## What This Project Demonstrates

| Area | Signal |
|---|---|
| **AI Agent (ReAct)** | `AgentLoop` — LLM-driven tool orchestration, multi-turn conversation history, autonomous termination via `submit_report` |
| **Agentic tool design** | Five `AgentTool` beans with JSON Schema params — LLM constructs arguments from schema, Spring wires implementations |
| **Reactive backend** | Spring WebFlux — non-blocking multipart streaming, `publishOn` / `subscribeOn` for JDBC offloading |
| **Durable orchestration** | Temporal — registered worker, `@ActivityInterface`, `@WorkflowImpl`, task queue polling, execution history |
| **Modular AI systems** | Skill runtime — filesystem loader, named registry, context-driven executor; skills are config, not code |
| **LLM integration** | Structured JSON prompting, function/tool calling, model-agnostic via OpenRouter, graceful fallbacks |
| **Document AI** | PDF and Excel extraction feeding into structured reasoning and anomaly detection |
| **Clean architecture** | Strict Domain / Application / Infrastructure / Interface layer separation |
| **MCP tool pattern** | Named tool registry with Spring auto-wiring — composable, independently testable |
| **Persistent workflows** | PostgreSQL schema: workflow executions, uploaded documents, generated reports, anomaly records |
| **Human-in-the-loop** | `PENDING_APPROVAL` state — AI produces, human decides |
| **Containerised stack** | Docker Compose — 6 services with env config, DNS, volume mounts |
| **Production patterns** | CORS, JWT auth, OpenTelemetry, pgvector, structured logging, graceful error fallbacks |

---

## Production Roadmap

- [ ] Replace dev JWT with signed tokens + refresh token rotation (KMS-backed)
- [ ] Move file storage from memory to S3-compatible object store
- [ ] Wire OpenTelemetry exporter to Jaeger / Grafana Tempo
- [ ] Add per-organisation rate limits and usage quotas
- [ ] Temporal workflow versioning for zero-downtime deployments
- [ ] Add `invoice-matching` and `contract-review` skills
- [ ] Skill marketplace — organisations browse and install skills from a registry
- [ ] Outbox pattern for durable event publication to downstream systems

---

## License

MIT
