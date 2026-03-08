# TX Investigation Agent

An AI-powered payment investigation agent built with [Embabel Agent Framework](https://embabel.com) and Spring Boot. It correlates data from 7 sources — microservice APIs, Temporal workflow history, Elasticsearch logs, and Jaeger distributed traces — to produce structured investigation reports for cross-border stablecoin payments.

## Architecture

The agent follows **hexagonal architecture** with strict layer isolation enforced by ArchUnit:

```
src/main/java/com/stablebridge/txinvestigation/
├── agent/                          # Embabel GOAP agent layer
│   ├── InvestigationAgent.java     #   10 GOAP actions (2 LLM + 7 fetch + 1 format)
│   └── InvestigationPersonas.java  #   Senior Investigator persona
├── application/controller/         # REST API layer
│   ├── InvestigationController.java
│   ├── InvestigationRequest.java
│   └── InvestigationResponse.java
├── domain/                         # Pure domain — no framework deps
│   ├── model/                      #   19 records, 6 enums, 2 exceptions
│   ├── port/                       #   7 provider interfaces
│   └── service/                    #   ReportFormatter
├── infrastructure/                 # Adapters & config
│   ├── blockchain/                 #   BlockchainAdapter (WebClient)
│   ├── compliance/                 #   ComplianceAdapter (WebClient)
│   ├── config/                     #   ServiceProperties, WebClientConfig
│   ├── ledger/                     #   LedgerAdapter (WebClient)
│   ├── elasticsearch/              #   ElasticsearchAdapter (WebClient POST)
│   ├── mock/                       #   MockAdaptersConfig (@ConditionalOnMissingBean)
│   ├── orchestrator/               #   OrchestratorAdapter (WebClient)
│   ├── temporal/                   #   TemporalAdapter (WebClient)
│   └── tracing/                    #   TracingAdapter (WebClient)
└── shell/                          # Spring Shell interactive CLI
    └── InvestigationCommands.java
```

### Layer Rules (ArchUnit-enforced)

| Rule | Description |
|------|-------------|
| Domain isolation | Domain must not depend on agent, application, infrastructure, or shell |
| No Spring Web in domain | Domain must not use WebClient or Spring Web annotations |
| Infrastructure isolation | Infrastructure must not depend on agent layer |

## How It Works

### GOAP Agent Pipeline

The Embabel agent uses [Goal-Oriented Action Planning](https://embabel.com/docs/snapshot/concepts/goap/) to chain 10 actions:

```
UserInput
  │
  ▼
parseQuery (LLM)            → InvestigationQuery
  │
  ├─► fetchPaymentState      → PaymentState          (S1 Orchestrator)
  ├─► fetchComplianceStatus  → ComplianceSnapshot    (S2 Compliance)
  ├─► fetchBlockchainStatus  → BlockchainSnapshot    (S4 Blockchain)
  ├─► fetchLedgerEntries     → LedgerSnapshot        (S7 Ledger)
  ├─► fetchWorkflowHistory   → WorkflowSnapshot      (Temporal)
  ├─► searchErrorLogs        → LogSnapshot           (Elasticsearch)
  └─► fetchTrace             → TraceSnapshot         (Jaeger)
  │
  ▼
analyzeTimeline (LLM)       → InvestigationReport    (Senior Investigator persona)
  │
  ▼
formatReport (@AchievesGoal) → CompletedInvestigation
```

- **`parseQuery`** — LLM extracts payment ID, merchant ID, and corridor from natural language input
- **7 fetch actions** — parallel data collection from microservice APIs + observability systems via hexagonal ports
- **`analyzeTimeline`** — LLM with Senior Investigator persona correlates events across all 7 data sources, identifies root cause, produces findings with severity/category
- **`formatReport`** — assembles the final report with markdown timeline, findings, and recommendations

### Domain Model

| Type | Purpose |
|------|---------|
| `InvestigationQuery` | Parsed user input: paymentId, merchantId, corridor |
| `PaymentState` | Orchestrator saga status, workflow ID, saga events |
| `ComplianceSnapshot` | Screening result, travel rule status, risk score, decisions |
| `BlockchainSnapshot` | TX hash, chain, confirmations, amount, sender/receiver |
| `LedgerSnapshot` | Ledger entries, net position, settlement status |
| `WorkflowSnapshot` | Temporal workflow execution: events, activities, retries, task queue |
| `LogSnapshot` | Elasticsearch error/warn logs: entries, stack traces, trace correlation |
| `TraceSnapshot` | Jaeger distributed trace: spans, latency, service-level errors |
| `InvestigationReport` | LLM-generated: severity, root cause, timeline, findings, recommendations |
| `CompletedInvestigation` | Final assembly of all 7 data sources + formatted markdown report |

### Finding Categories

| Category | Description |
|----------|-------------|
| `STUCK_PAYMENT` | Payment stuck at a saga step |
| `COMPLIANCE_BLOCK` | Sanctions/AML screening block |
| `BLOCKCHAIN_DELAY` | On-chain confirmation delay |
| `SETTLEMENT_MISMATCH` | Ledger balance discrepancy |
| `SLA_BREACH` | Processing time exceeded SLA |
| `RECONCILIATION_GAP` | Cross-service data inconsistency |
| `WORKFLOW_FAILURE` | Temporal activity failure or timeout |
| `ERROR_SPIKE` | High error rate detected in logs |
| `LATENCY_ANOMALY` | Abnormal span duration in traces |

### Severity Levels

`CRITICAL` > `HIGH` > `MEDIUM` > `LOW` > `INFO`

## Getting Started

### Prerequisites

- **Java 25**
- **Gradle 9.3.1**
- An OpenAI or Anthropic API key (for LLM actions)

### Build & Test

```bash
# Run all tests (43 unit + 3 integration)
./gradlew check

# Unit tests only
./gradlew test

# Integration tests only
./gradlew integrationTest
```

### Run

```bash
# Start with mock adapters (no live services needed)
./gradlew bootRun

# Start with live service URLs
ORCHESTRATOR_URL=http://localhost:8081 \
COMPLIANCE_URL=http://localhost:8082 \
BLOCKCHAIN_URL=http://localhost:8083 \
LEDGER_URL=http://localhost:8084 \
TEMPORAL_URL=http://localhost:7233 \
ELASTICSEARCH_URL=http://localhost:9200 \
TRACING_URL=http://localhost:16686 \
./gradlew bootRun
```

### Spring Shell (Interactive CLI)

When the application starts, you get an interactive shell:

```
embabel> investigate PAY-abc-123
```

This calls all 4 backend services, aggregates data, and prints a formatted investigation report.

### REST API

```bash
# Investigate a payment
curl -X POST http://localhost:8080/api/v1/investigations \
  -H "Content-Type: application/json" \
  -d '{"paymentId": "PAY-abc-123"}'
```

**Request:**

```json
{
  "paymentId": "PAY-abc-123",
  "merchantId": "MCH-001"
}
```

**Response:**

```json
{
  "paymentId": "PAY-abc-123",
  "status": "BLOCKCHAIN_PENDING",
  "severity": null,
  "rootCause": "Data collected — use GOAP agent for full LLM analysis",
  "findings": [],
  "timeline": [],
  "recommendations": [],
  "formattedReport": "### Timeline\n| Time | Service | Event | Status |\n..."
}
```

> **Note:** The REST endpoint provides a lightweight summary without LLM analysis. For full AI-powered investigation with root cause analysis, use the GOAP agent via Spring Shell or the Embabel platform.

## Configuration

### Service Endpoints

Configure via `application.yml` or environment variables:

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

### Mock Adapters

When no live service is available, `MockAdaptersConfig` provides fallback implementations via `@ConditionalOnMissingBean`. These return realistic sample data so the agent can run standalone for development and demos.

### Adapter Activation

| Condition | Adapter |
|-----------|---------|
| `app.services.orchestrator.enabled=true` | `OrchestratorAdapter` (WebClient) |
| `app.services.compliance.enabled=true` | `ComplianceAdapter` (WebClient) |
| `app.services.blockchain.enabled=true` | `BlockchainAdapter` (WebClient) |
| `app.services.ledger.enabled=true` | `LedgerAdapter` (WebClient) |
| `app.services.temporal.enabled=true` | `TemporalAdapter` (WebClient) |
| `app.services.elasticsearch.enabled=true` | `ElasticsearchAdapter` (WebClient POST) |
| `app.services.tracing.enabled=true` | `TracingAdapter` (WebClient) |
| No real adapter present | `MockAdaptersConfig` fallback |

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 25 |
| Spring Boot | 4.0.3 |
| Embabel Agent Framework | 0.3.4 |
| Spring Shell | 3.4 |
| Spring WebFlux | (via Boot) |
| Lombok | 1.18.42 |
| Jackson 3 | (via Boot) |
| ArchUnit | 1.4.1 |
| WireMock | 3.13.2 |
| JaCoCo | 0.8.14 |
| Spotless | 8.3.0 |
| Gradle | 9.3.1 (Kotlin DSL) |

## Test Suite

**46 tests** across 3 categories:

### Unit Tests (43)

| Test Class | Tests | What It Covers |
|------------|-------|----------------|
| `ArchitectureTest` | 7 | Hexagonal layer isolation rules |
| `InvestigationAgentTest` | 8 | Agent actions with mocked ports (7 fetch + formatReport) |
| `ReportFormatterTest` | 4 | Markdown report generation |
| `OrchestratorAdapterTest` | 3 | WireMock — success, 404, 500 |
| `ComplianceAdapterTest` | 3 | WireMock — success, 404, 500 |
| `BlockchainAdapterTest` | 3 | WireMock — success, 404, 500 |
| `LedgerAdapterTest` | 3 | WireMock — success, 404, 500 |
| `TemporalAdapterTest` | 3 | WireMock — workflow history, 404, 500 |
| `ElasticsearchAdapterTest` | 3 | WireMock — search, index-not-found, 500 |
| `TracingAdapterTest` | 3 | WireMock — trace fetch, empty, 500 |
| `InvestigationControllerTest` | 2 | REST endpoint + validation |
| `InvestigationCommandsTest` | 1 | Shell command with all 7 providers |

### Integration Tests (3)

| Test Class | Tests | What It Covers |
|------------|-------|----------------|
| `InvestigationAgentIntegrationTest` | 3 | Embabel GOAP agent — full pipeline, 7 provider ports, formatted report |

### Test Fixtures

All test data is centralized in `src/testFixtures/java/.../fixtures/`:

| Fixture | Factory Method |
|---------|---------------|
| `InvestigationQueryFixtures` | `anInvestigationQuery()` |
| `PaymentStateFixtures` | `aPaymentState()` |
| `ComplianceSnapshotFixtures` | `aComplianceSnapshot()` |
| `BlockchainSnapshotFixtures` | `aBlockchainSnapshot()` |
| `LedgerSnapshotFixtures` | `aLedgerSnapshot()` |
| `InvestigationReportFixtures` | `anInvestigationReport()` |
| `WorkflowSnapshotFixtures` | `aWorkflowSnapshot()` |
| `LogSnapshotFixtures` | `aLogSnapshot()` |
| `TraceSnapshotFixtures` | `aTraceSnapshot()` |
| `CompletedInvestigationFixtures` | `aCompletedInvestigation()` |

## Sample Report Output

```markdown
## Investigation Report: PAY-abc-123

**Status:** BLOCKCHAIN_PENDING | **Severity:** HIGH

### Root Cause
Blockchain transaction pending — 2 confirmations received, waiting for finality threshold

### Timeline
| Time | Service | Event | Status |
|------|---------|-------|--------|
| 2026-03-08 16:00:00 UTC | S1 Orchestrator | Compliance check passed | COMPLETED |
| 2026-03-08 16:15:00 UTC | S1 Orchestrator | ACH debit confirmed | COMPLETED |
| 2026-03-08 16:30:00 UTC | S1 Orchestrator | Transaction submitted to Base | PENDING |

### Findings
- [HIGH] **[BLOCKCHAIN_DELAY]** Transaction has only 2 of 12 required confirmations after 30 minutes

### Recommendations
1. Monitor Base network for confirmation progress
2. Alert if no progress within 60 minutes
3. Prepare manual intervention workflow if stuck beyond 2 hours

---
*Generated by TX Investigation Agent (Embabel + Spring AI)*
```

## Project Structure

```
tx-investigation-agent/
├── build.gradle.kts              # Kotlin DSL build (Spring Boot 4, Embabel 0.3.4)
├── settings.gradle.kts           # Root project config, build cache
├── gradle.properties             # Version properties
├── src/
│   ├── main/
│   │   ├── java/                 # Production source
│   │   └── resources/
│   │       └── application.yml   # Service URLs, Spring Shell config
│   ├── test/
│   │   ├── java/                 # Unit tests
│   │   └── resources/
│   │       └── archunit.properties
│   ├── testFixtures/
│   │   └── java/                 # Shared test fixtures
│   └── integration-test/
│       ├── java/                 # Integration tests (Embabel GOAP)
│       └── resources/
│           └── application.yml   # Test config (services disabled)
└── services/
    └── 01-architecture-spec.md   # Full architecture specification
```

## License

Proprietary - StableBridge
