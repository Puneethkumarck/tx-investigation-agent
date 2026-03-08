# Transaction Investigation Agent — Architecture Spec

## Overview

AI-powered agent that traces a payment's full lifecycle across all stablecoin-payments microservices. Given a payment ID (or natural language query), it fetches state from each service, correlates the data, and produces a structured investigation report with timeline, root cause analysis, and recommended actions.

**Problem:** A single cross-border payment touches 6+ services (orchestrator → compliance → on-ramp → blockchain → off-ramp → ledger). Tracing a stuck or failed payment requires manually querying each service's API/DB — time-consuming and error-prone.

**Solution:** A GOAP agent that automates the investigation pipeline, using LLM to correlate data and produce human-readable reports.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 25 |
| Framework | Spring Boot 4.0.3 |
| AI Agent | Embabel Agent Framework 0.3.4 |
| LLM | Spring AI (Anthropic / OpenAI) |
| HTTP Client | Spring WebFlux (WebClient) |
| Shell | Spring Shell 3.4 |
| Build | Gradle 9.x (Kotlin DSL) |
| Architecture | Hexagonal (ports & adapters) |

## GOAP Flow

```
UserInput
  │
  ▼ parseQuery (LLM)
InvestigationQuery { paymentId, merchantId?, corridor? }
  │
  ▼ fetchPaymentState (no LLM)
PaymentState { status, sagaStep, workflowId, events[], createdAt, updatedAt }
  │
  ▼ fetchComplianceStatus (no LLM)
ComplianceSnapshot { screeningResult, travelRuleStatus, riskScore, decisions[] }
  │
  ▼ fetchBlockchainStatus (no LLM)
BlockchainSnapshot { txHash, chain, confirmations, amount, addresses, blockTimestamp }
  │
  ▼ fetchLedgerEntries (no LLM)
LedgerSnapshot { entries[], netPosition, settlementStatus }
  │
  ▼ analyzeTimeline (LLM) — SENIOR_INVESTIGATOR persona
InvestigationReport { timeline[], rootCause, findings[], recommendations[], severity }
  │
  ▼ formatReport (no LLM) — @AchievesGoal
CompletedInvestigation { query, report, formattedBody }
```

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
| `InvestigationReport` | timeline, rootCause, findings, recommendations, severity | LLM-generated analysis |
| `TimelineEvent` | timestamp, service, description, status | Correlated timeline entry |
| `Finding` | category, severity, description | Individual finding |
| `CompletedInvestigation` | query, paymentState, complianceSnapshot, blockchainSnapshot, ledgerSnapshot, report, formattedBody | Final output |

### Enums

| Enum | Values |
|------|--------|
| `PaymentStatus` | INITIATED, COMPLIANCE_CHECK, FIAT_COLLECTED, BLOCKCHAIN_PENDING, BLOCKCHAIN_CONFIRMED, FIAT_DISBURSED, COMPLETED, FAILED, CANCELLED |
| `InvestigationSeverity` | CRITICAL, HIGH, MEDIUM, LOW, INFO |
| `FindingCategory` | STUCK_PAYMENT, COMPLIANCE_BLOCK, BLOCKCHAIN_DELAY, SETTLEMENT_MISMATCH, SLA_BREACH, RECONCILIATION_GAP |
| `ScreeningResult` | CLEAR, FLAGGED, PENDING, REJECTED |
| `Direction` | DEBIT, CREDIT |

### Domain Ports

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

**Dev Mode:** Mock adapters via `@ConditionalOnProperty` returning fixture data.

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
```

## Application Layer

### REST Endpoint

```
POST /api/v1/investigations
```

**Request:**
```json
{
  "paymentId": "PAY-abc-123",
  "merchantId": "MCH-001"       // optional
}
```

**Response:**
```json
{
  "paymentId": "PAY-abc-123",
  "status": "BLOCKCHAIN_PENDING",
  "severity": "HIGH",
  "rootCause": "Blockchain transaction pending for 47 minutes — exceeds 30-minute SLA",
  "findings": [...],
  "timeline": [...],
  "recommendations": [...],
  "formattedReport": "## Investigation Report: PAY-abc-123\n..."
}
```

### Shell Command

```
embabel> investigate PAY-abc-123
embabel> investigate --payment-id PAY-abc-123 --merchant-id MCH-001
```

### Webhook (Future)

Slack integration: `/investigate PAY-abc-123` triggers investigation and posts report to channel.

## Agent Persona

```java
SENIOR_INVESTIGATOR = new RoleGoalBackstory(
    "Senior Payment Investigator",
    "Identify root cause of payment issues, produce clear actionable reports",
    "Payment operations specialist with 10 years experience in cross-border "
        + "payments, blockchain settlements, and compliance workflows. Expert at "
        + "correlating events across distributed systems and identifying bottlenecks."
);
```

## Architecture Rules (ArchUnit)

| Rule | Description |
|------|-------------|
| Domain ✗ Infrastructure | Domain must not depend on adapters |
| Domain ✗ Agent | Domain must not depend on Embabel agent |
| Domain ✗ Application | Domain must not depend on controllers |
| Domain ✗ Shell | Domain must not depend on shell |
| Infrastructure ✗ Agent | Infrastructure must not depend on agent |

## Test Strategy

| Level | Scope | Tools |
|-------|-------|-------|
| Unit — Domain | Records, formatter, enums | JUnit 5, AssertJ |
| Unit — Agent | GOAP actions (mocked deps) | BDD Mockito |
| Unit — Adapter | HTTP client behavior | WireMock |
| Unit — Controller | REST endpoint | WebTestClient |
| Unit — Shell | Command output | Mockito |
| Architecture | Hexagonal layer rules | ArchUnit |
| Integration | Full GOAP chain | EmbabelMockitoIntegrationTest |

## Milestones

### M1: Domain & Agent Core
- Domain model (records, enums, ports)
- ReportFormatter service
- InvestigationAgent (7 GOAP actions)
- Persona (SENIOR_INVESTIGATOR)
- Test fixtures
- ArchUnit tests
- Unit tests (agent + formatter)

### M2: Infrastructure Adapters
- OrchestratorAdapter (WebClient + WireMock test)
- ComplianceAdapter (WebClient + WireMock test)
- BlockchainAdapter (WebClient + WireMock test)
- LedgerAdapter (WebClient + WireMock test)
- Mock adapters for dev mode
- Service properties configuration

### M3: Integration, API & Shell
- EmbabelMockitoIntegrationTest
- REST endpoint (POST /api/v1/investigations)
- Shell command (investigate)
- Controller + shell tests

## Exit Criteria

- [ ] All GOAP actions unit tested
- [ ] All adapters WireMock tested
- [ ] Integration test exercises full chain
- [ ] ArchUnit enforces hexagonal rules
- [ ] REST endpoint returns structured report
- [ ] Shell command displays formatted report
- [ ] Spotless clean, JaCoCo coverage on unit tests
