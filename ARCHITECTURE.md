# OpsPilot AI Architecture

## System Design

- Style: modular monolith with clear bounded modules
- Pattern: clean architecture (`interfaceadapters -> application -> domain -> infrastructure`)
- Work orchestration: Temporal workflow engine
- Skill platform: dynamically loaded skills from `skills/*`

## Backend Modules

- `auth` JWT + org membership
- `workflow` workflow execution lifecycle and tracking
- `skill-runtime` loader/registry/executor/context/result
- `document-processing` PDF/Excel extraction abstractions
- `ai-services` extraction/comparison/report generation with structured outputs
- `anomaly` rule engine + AI explanation layering
- `tooling` MCP-compatible tool registry/executor
- `observability` tracing/logging/latency and token metrics

## Temporal Workflow

`QuotationComparisonWorkflow`

Activities:
- `ExtractDocumentActivity`
- `ParseQuotationActivity`
- `NormalizeDataActivity`
- `CompareQuotationActivity`
- `DetectAnomalyActivity`
- `GenerateReportActivity`

## Data Model

Core entities:

- User
- Organization
- WorkflowExecution
- UploadedDocument
- SupplierQuotation
- QuotationItem
- AnomalyDetection
- GeneratedReport
- SkillDefinition

Schema bootstrap is in `backend/src/main/resources/schema.sql`.

## Security

- JWT auth (MVP scaffolding currently permissive; harden for production)
- Org isolation at repository/service boundaries
- Role-based action checks at controller/application layers
- File content-type and size validation

## Observability

- OpenTelemetry instrumentation point for workflows and AI calls
- Structured JSON logs (add `logback-spring.xml` for production profile)
- Workflow stage logs returned via `/workflows/{id}/logs`

## Deployment

- `docker-compose` for local
- split services later if needed (workflow worker, API, AI gateway) without codebase rewrite
