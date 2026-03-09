# Transaction Investigation Agent — Architecture Spec

## Overview

AI-powered agent that traces a payment's full lifecycle across all stablecoin-payments microservices. Given a payment ID (or natural language query), it fetches state from each service, correlates the data, and produces a structured investigation report with timeline, root cause analysis, and a single actionable recommendation.

**Problem:** A single cross-border payment touches 7+ services (orchestrator → compliance → on-ramp → blockchain → off-ramp → ledger + Temporal workflows + Elasticsearch logs + Jaeger traces). Tracing a stuck or failed payment requires manually querying each service's API/DB — time-consuming and error-prone.

**Solution:** A GOAP agent that automates the investigation pipeline, using an LLM with a Senior Blockchain Payments Engineer persona to correlate data and produce precise, actionable reports.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.3 |
| AI Agent | Embabel Agent Framework 0.3.4 |
| LLM | Spring AI + Ollama (llama3.2 — local, free) |
| HTTP Client | Spring WebFlux (WebClient) |
| Shell | Spring Shell 3.4 |
| Build | Gradle 9.3.1 (Kotlin DSL) |
| Architecture | Hexagonal (ports & adapters) |
| Dependency Updates | Renovate |

## GOAP Flow

```
UserInput
  │
  ▼ parseQuery (LLM)
InvestigationQuery { paymentId, merchantId?, corridor? }
  │
  ├─► fetchPaymentState (no LLM)
  │   PaymentState { status, sagaStep, workflowId, events[], createdAt, updatedAt }
  │
  ├─► fetchComplianceStatus (no LLM)
  │   ComplianceSnapshot { screeningResult, travelRuleStatus, riskScore, decisions[] }
  │
  ├─► fetchBlockchainStatus (no LLM)
  │   BlockchainSnapshot { txHash, chain, confirmations, amount, addresses, blockTimestamp }
  │
  ├─► fetchLedgerEntries (no LLM)
  │   LedgerSnapshot { entries[], netPosition, settlementStatus }
  │
  ├─► fetchWorkflowHistory (no LLM)
  │   WorkflowSnapshot { workflowId, workflowType, status, startTime, events[], attemptCount }
  │
  ├─► searchErrorLogs (no LLM)
  │   LogSnapshot { totalHits, entries[] }
  │
  └─► fetchTrace (no LLM)
      TraceSnapshot { traceId, totalSpans, durationMs, spans[] }
  │
  ▼ analyzeTimeline (LLM) — SENIOR_BLOCKCHAIN_PAYMENTS_ENGINEER persona
InvestigationReport { timeline[], rootCause, findings[], recommendation, severity }
  │
  ▼ formatReport (no LLM) — @AchievesGoal
CompletedInvestigation { query, all 7 snapshots, report, formattedBody }
```

The Embabel GOAP planner automatically determines the optimal execution order and parallelizes the 7 independent fetch actions.

## Domain Model

### Records

| Record | Fields | Description |
|--------|--------|-------------|
| `InvestigationQuery` | paymentId, merchantId (opt), corridor (opt) | Parsed user query |
| `PaymentState` | paymentId, status, sagaStep, workflowId, events, createdAt, updatedAt | S1 orchestrator state |
| `SagaEvent` | step, status, timestamp, detail | Individual saga step event |
| `ComplianceSnapshot` | paymentId, screeningResult, travelRuleStatus, riskScore, decisions | S2 compliance state |
| `ComplianceDecision` | checkType, result, provider, timestamp, detail | Individual compliance decision |
| `BlockchainSnapshot` | paymentId, txHash, chain, confirmations, amount, currency, fromAddress, toAddress, blockTimestamp, status | S4 blockchain state |
| `LedgerSnapshot` | paymentId, entries, netPosition, settlementStatus | S7 ledger state |
| `LedgerEntry` | entryId, account, direction, amount, currency, timestamp | Individual ledger entry |
| `WorkflowSnapshot` | paymentId, workflowId, workflowType, status, startTime, events, attemptCount, taskQueue | Temporal workflow state |
| `WorkflowEvent` | eventType, timestamp, activityName, status, detail | Individual workflow event |
| `LogSnapshot` | paymentId, totalHits, entries | Elasticsearch log results |
| `LogEntry` | timestamp, level, service, message, traceId, stackTrace | Individual log entry |
| `TraceSnapshot` | paymentId, traceId, totalSpans, durationMs, spans | Jaeger trace data |
| `TraceSpan` | spanId, operationName, serviceName, durationMs, status, tags | Individual trace span |
| `InvestigationReport` | timeline, rootCause, findings, recommendation, severity | LLM-generated analysis |
| `TimelineEvent` | timestamp, service, description, status | Correlated timeline entry |
| `Finding` | category, severity, description | Individual finding |
| `CompletedInvestigation` | query, paymentState, complianceSnapshot, blockchainSnapshot, ledgerSnapshot, workflowSnapshot, logSnapshot, traceSnapshot, report, formattedBody | Final output |

### Enums

| Enum | Values |
|------|--------|
| `PaymentStatus` | INITIATED, COMPLIANCE_CHECK, FIAT_COLLECTED, BLOCKCHAIN_PENDING, BLOCKCHAIN_CONFIRMED, FIAT_DISBURSED, COMPLETED, FAILED, CANCELLED |
| `InvestigationSeverity` | CRITICAL, HIGH, MEDIUM, LOW, INFO |
| `FindingCategory` | STUCK_PAYMENT, COMPLIANCE_BLOCK, BLOCKCHAIN_DELAY, SETTLEMENT_MISMATCH, SLA_BREACH, RECONCILIATION_GAP, WORKFLOW_FAILURE, ERROR_SPIKE, LATENCY_ANOMALY |
| `ScreeningResult` | CLEAR, FLAGGED, PENDING, REJECTED |
| `Direction` | DEBIT, CREDIT |

### Domain Ports (7 providers)

```java
interface PaymentStateProvider {
    PaymentState fetchPaymentState(String paymentId);
}

interface ComplianceStateProvider {
    ComplianceSnapshot fetchComplianceStatus(String paymentId);
}

interface BlockchainStateProvider {
    BlockchainSnapshot fetchBlockchainStatus(String paymentId);
}

interface LedgerStateProvider {
    LedgerSnapshot fetchLedgerEntries(String paymentId);
}

interface WorkflowHistoryProvider {
    WorkflowSnapshot fetchWorkflowHistory(String paymentId);
}

interface LogSearchProvider {
    LogSnapshot searchErrorLogs(String paymentId);
}

interface TraceProvider {
    TraceSnapshot fetchTrace(String paymentId);
}
```

### Domain Service

```java
@Service
class ReportFormatter {
    String format(InvestigationReport report, PaymentState payment);
    String formatTimeline(List<TimelineEvent> events);
    String formatFindings(List<Finding> findings);
}
```

## Infrastructure Adapters

Each adapter calls the corresponding microservice REST API:

| Adapter | Service | Endpoint Pattern |
|---------|---------|-----------------|
| `OrchestratorAdapter` | S1 Payment Orchestrator | `GET /api/v1/payments/{id}` |
| `ComplianceAdapter` | S2 Compliance & Travel Rule | `GET /api/v1/compliance/payments/{id}` |
| `BlockchainAdapter` | S4 Blockchain & Custody | `GET /api/v1/blockchain/payments/{id}` |
| `LedgerAdapter` | S7 Ledger | `GET /api/v1/ledger/payments/{id}` |
| `TemporalAdapter` | Temporal Workflow Engine | `GET /api/v1/workflows/payments/{id}` |
| `ElasticsearchAdapter` | Elasticsearch | `POST /payment-logs-*/_search` |
| `TracingAdapter` | Jaeger | `GET /api/traces?service=payment&tags=paymentId:{id}` |

### Adapter Activation

All adapters use `@ConditionalOnProperty(matchIfMissing = false)`. When no live service is configured, `MockAdaptersConfig` provides fallback implementations via `@ConditionalOnMissingBean` returning realistic sample data.

| Property | Adapter |
|----------|---------|
| `app.services.orchestrator.enabled=true` | `OrchestratorAdapter` |
| `app.services.compliance.enabled=true` | `ComplianceAdapter` |
| `app.services.blockchain.enabled=true` | `BlockchainAdapter` |
| `app.services.ledger.enabled=true` | `LedgerAdapter` |
| `app.services.temporal.enabled=true` | `TemporalAdapter` |
| `app.services.elasticsearch.enabled=true` | `ElasticsearchAdapter` |
| `app.services.tracing.enabled=true` | `TracingAdapter` |
| No real adapter present | `MockAdaptersConfig` fallback |

### Adapter Configuration

```yaml
app:
  services:
    orchestrator:
      base-url: ${ORCHESTRATOR_URL:http://localhost:8081}
    compliance:
      base-url: ${COMPLIANCE_URL:http://localhost:8082}
    blockchain:
      base-url: ${BLOCKCHAIN_URL:http://localhost:8083}
    ledger:
      base-url: ${LEDGER_URL:http://localhost:8084}
    temporal:
      base-url: ${TEMPORAL_URL:http://localhost:7233}
    elasticsearch:
      base-url: ${ELASTICSEARCH_URL:http://localhost:9200}
    tracing:
      base-url: ${TRACING_URL:http://localhost:16686}
```

### LLM Configuration

```java
@Configuration
class ChatModelConfig {
    @Bean
    @ConditionalOnMissingBean
    OllamaChatModel ollamaChatModel();  // localhost:11434, llama3.2:latest, 5min timeout
}
```

```yaml
embabel:
  models:
    default-llm: llama3.2:latest
```

## Application Layer

### REST Endpoint

```
POST /api/v1/investigations
```

The controller collects data from all 7 providers, sends it to the LLM with a Senior Blockchain Payments Engineer prompt, and returns the structured report.

**Request:**
```json
{
  "paymentId": "PAY-001",
  "merchantId": "MCH-001"
}
```

**Response:**
```json
{
  "paymentId": "PAY-001",
  "status": "BLOCKCHAIN_PENDING",
  "severity": "HIGH",
  "rootCause": "Custody service failed to submit tx 0xmock123abc456 — gas price spike to 45 gwei exceeded 20 gwei limit at saga step BLOCKCHAIN_SUBMIT",
  "findings": [
    {"category": "COMPLIANCE_BLOCK", "severity": "INFO", "description": "Compliance check passed successfully"},
    {"category": "BLOCKCHAIN_DELAY", "severity": "HIGH", "description": "Blockchain submit step failed due to gas price spike"},
    {"category": "SLA_BREACH", "severity": "MEDIUM", "description": "Transaction confirmation timeout approaching SLA"},
    {"category": "ERROR_SPIKE", "severity": "MEDIUM", "description": "Gas price spike detected — retry pending"},
    {"category": "LATENCY_ANOMALY", "severity": "LOW", "description": "Blockchain custody service latency anomaly detected"}
  ],
  "timeline": [
    {"timestamp": "2026-03-09T05:15:42Z", "service": "payment-orchestrator", "description": "Workflow started", "status": "RUNNING"},
    {"timestamp": "2026-03-09T05:30:42Z", "service": "compliance-travel-rule", "description": "Compliance check scheduled", "status": "PENDING"},
    {"timestamp": "2026-03-09T05:31:42Z", "service": "compliance-travel-rule", "description": "Compliance check completed", "status": "COMPLETED"},
    {"timestamp": "2026-03-09T05:31:42Z", "service": "fiat-on-ramp", "description": "Fiat collection scheduled", "status": "PENDING"},
    {"timestamp": "2026-03-09T05:31:42Z", "service": "fiat-on-ramp", "description": "Fiat collection completed", "status": "COMPLETED"},
    {"timestamp": "2026-03-09T06:00:42Z", "service": "blockchain-custody", "description": "Blockchain submit scheduled", "status": "PENDING"},
    {"timestamp": "2026-03-09T06:00:42Z", "service": "blockchain-custody", "description": "Blockchain submit failed", "status": "FAILED"}
  ],
  "recommendation": "Resubmit transaction 0xmock123abc456 via custody service /api/v1/retry with updated gas fee",
  "errorLogCount": 2,
  "traceId": "trace-mock-001",
  "workflowStatus": "RUNNING",
  "formattedReport": "## Investigation Report: PAY-001 ..."
}
```

### Shell Command

```
embabel> investigate PAY-001
embabel> investigate --payment-id PAY-001 --merchant-id MCH-001
```

## Agent Persona

```java
SENIOR_INVESTIGATOR = new RoleGoalBackstory(
    "Senior Blockchain Payments Engineer",
    "Identify the single root cause of payment failures and provide one specific actionable fix",
    "Backend engineer with 10 years experience building cross-border stablecoin "
        + "payment infrastructure — saga orchestration, custody integration, gas fee "
        + "management, on-chain settlement, and compliance pipelines. Expert at "
        + "reading Temporal workflow histories, Elasticsearch error logs, and Jaeger "
        + "distributed traces to pinpoint exactly which service and step failed."
);
```

### Prompt Constraints

The LLM prompt enforces specific, actionable output:

1. **Root cause** must identify the SINGLE root cause with specific evidence — which service failed, which step, what error. NOT vague like "blockchain error".
2. **Recommendation** must be exactly ONE specific action referencing the exact service, transaction ID, or endpoint. NOT generic advice like "monitor the situation".
3. The `InvestigationReport.recommendation` field is a `String` (not `List<String>`), structurally enforcing a single recommendation at the schema level.

## Architecture Rules (ArchUnit)

| Rule | Description |
|------|-------------|
| Domain ✗ Infrastructure | Domain must not depend on adapters |
| Domain ✗ Agent | Domain must not depend on Embabel agent |
| Domain ✗ Application | Domain must not depend on controllers |
| Domain ✗ Shell | Domain must not depend on shell |
| Domain ✗ Spring Web | Domain must not use WebClient or Spring Web annotations |
| Infrastructure ✗ Agent | Infrastructure must not depend on agent |

## Test Strategy

| Level | Scope | Tools |
|-------|-------|-------|
| Unit — Domain | Records, formatter, enums | JUnit 5, AssertJ |
| Unit — Agent | GOAP actions (mocked deps) | BDD Mockito |
| Unit — Adapter | HTTP client behavior (7 adapters) | WireMock |
| Unit — Controller | REST endpoint + ChatClient mock | WebTestClient, Mockito |
| Unit — Shell | Command output | Mockito |
| Architecture | Hexagonal layer rules (7 rules) | ArchUnit |
| Integration | Full GOAP chain | EmbabelMockitoIntegrationTest |

**43 unit tests + 3 integration tests = 46 total.** All tests use mocked providers and Embabel's test harness — no live services or LLM API keys required.

## Milestones

### M1: Domain & Agent Core ✅
- Domain model (19 records, 6 enums, 2 exceptions, 7 ports)
- ReportFormatter service
- InvestigationAgent (10 GOAP actions: 2 LLM + 7 fetch + 1 format)
- Persona (Senior Blockchain Payments Engineer)
- Test fixtures
- ArchUnit tests (7 rules)
- Unit tests (agent + formatter)

### M2: Infrastructure Adapters ✅
- OrchestratorAdapter (WebClient + WireMock test)
- ComplianceAdapter (WebClient + WireMock test)
- BlockchainAdapter (WebClient + WireMock test)
- LedgerAdapter (WebClient + WireMock test)
- Mock adapters for dev mode (`@ConditionalOnMissingBean`)
- Service properties configuration

### M3: Integration, API & Shell ✅
- EmbabelMockitoIntegrationTest (3 tests)
- REST endpoint (POST /api/v1/investigations)
- Shell command (investigate)
- Controller + shell tests

### M4: Observability Providers ✅
- TemporalAdapter (WebClient + WireMock test)
- ElasticsearchAdapter (WebClient + WireMock test)
- TracingAdapter (WebClient + WireMock test)

### M5: Spring AI + LLM Integration ✅
- Ollama as default LLM provider (llama3.2, local, free)
- ChatClient.Builder wired into InvestigationController
- ChatModelConfig with 5-minute timeout
- Prompt engineering for specific root cause and single recommendation
- `InvestigationReport.recommendation` (String, not List) — schema-enforced

## Exit Criteria

- [x] All 10 GOAP actions unit tested
- [x] All 7 adapters WireMock tested
- [x] Integration test exercises full chain
- [x] ArchUnit enforces hexagonal rules (7 rules)
- [x] REST endpoint returns structured LLM-analyzed report
- [x] Shell command displays formatted report
- [x] Spotless clean, JaCoCo coverage on unit tests
- [x] LLM produces specific root cause and single actionable recommendation
- [x] App runs standalone with mock data + local Ollama (no external dependencies)
- [x] Renovate configured for automated dependency updates
