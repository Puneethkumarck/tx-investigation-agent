# TX Investigation Agent — Architecture Diagrams

## 1. High-Level System Architecture

```mermaid
graph TB
    subgraph Clients["Clients"]
        REST["REST API<br/>POST /api/v1/investigations"]
        SHELL["Spring Shell<br/>investigate PAY-001"]
    end

    subgraph Agent["TX Investigation Agent"]
        direction TB
        subgraph App["Application Layer"]
            CTRL["InvestigationController"]
            CMD["InvestigationCommands"]
        end

        subgraph AgentLayer["Agent Layer (Embabel GOAP)"]
            IA["InvestigationAgent<br/>10 GOAP Actions"]
            PERSONA["Senior Blockchain<br/>Payments Engineer<br/>Persona"]
        end

        subgraph Domain["Domain Layer (Pure Java)"]
            direction TB
            MODELS["19 Records · 6 Enums · 2 Exceptions"]
            PORTS["7 Port Interfaces"]
            SVC["ReportFormatter"]
        end

        subgraph Infra["Infrastructure Layer"]
            direction TB
            subgraph Adapters["7 Adapters (WebClient)"]
                A1["OrchestratorAdapter"]
                A2["ComplianceAdapter"]
                A3["BlockchainAdapter"]
                A4["LedgerAdapter"]
                A5["TemporalAdapter"]
                A6["ElasticsearchAdapter"]
                A7["TracingAdapter"]
            end
            MOCK["MockAdaptersConfig<br/>@ConditionalOnMissingBean"]
            CFG["ServiceProperties<br/>WebClientConfig<br/>ChatModelConfig<br/>JacksonConfig"]
        end
    end

    subgraph External["External Microservices"]
        S1["S1 Payment<br/>Orchestrator"]
        S2["S2 Compliance &<br/>Travel Rule"]
        S4["S4 Blockchain<br/>& Custody"]
        S7["S7 Ledger"]
        TMP["Temporal<br/>Workflow Engine"]
        ES["Elasticsearch"]
        JAE["Jaeger<br/>Tracing"]
    end

    LLM["Ollama<br/>llama3.2:latest"]

    REST --> CTRL
    SHELL --> CMD
    CTRL --> AgentLayer
    CMD --> AgentLayer
    IA --> PERSONA
    IA --> PORTS
    PORTS --> Adapters
    PORTS -.-> MOCK
    SVC --> MODELS
    A1 --> S1
    A2 --> S2
    A3 --> S4
    A4 --> S7
    A5 --> TMP
    A6 --> ES
    A7 --> JAE
    AgentLayer --> LLM
    CTRL --> LLM

    style Domain fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style AgentLayer fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    style App fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style Infra fill:#fce4ec,stroke:#c62828,stroke-width:2px
    style External fill:#f3e5f5,stroke:#6a1b9a,stroke-width:1px
    style LLM fill:#fff9c4,stroke:#f57f17,stroke-width:2px
```

## 2. GOAP Pipeline Flow

```mermaid
flowchart TD
    UI["UserInput<br/>(natural language or payment ID)"]
    PQ["parseQuery<br/>🧠 LLM"]
    IQ["InvestigationQuery<br/>{paymentId, merchantId?, corridor?}"]

    UI --> PQ --> IQ

    IQ --> F1 & F2 & F3 & F4 & F5 & F6 & F7

    subgraph Parallel["7 Parallel Fetches (no LLM)"]
        F1["fetchPaymentState<br/>→ PaymentState"]
        F2["fetchComplianceStatus<br/>→ ComplianceSnapshot"]
        F3["fetchBlockchainStatus<br/>→ BlockchainSnapshot"]
        F4["fetchLedgerEntries<br/>→ LedgerSnapshot"]
        F5["fetchWorkflowHistory<br/>→ WorkflowSnapshot"]
        F6["searchErrorLogs<br/>→ LogSnapshot"]
        F7["fetchTrace<br/>→ TraceSnapshot"]
    end

    F1 & F2 & F3 & F4 & F5 & F6 & F7 --> AT

    AT["analyzeTimeline<br/>🧠 LLM + Persona<br/>→ InvestigationReport"]
    FR["formatReport<br/>@AchievesGoal<br/>→ CompletedInvestigation"]

    AT --> FR

    style PQ fill:#fff9c4,stroke:#f57f17,stroke-width:2px
    style AT fill:#fff9c4,stroke:#f57f17,stroke-width:2px
    style Parallel fill:#e3f2fd,stroke:#1565c0,stroke-width:1px
    style FR fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
```

## 3. Adapter Activation Strategy

```mermaid
flowchart TD
    PROP{"app.services.{name}<br/>.enabled=true?"}

    PROP -->|Yes| REAL["Real Adapter<br/>@ConditionalOnProperty"]
    PROP -->|No / Missing| MOCK["MockAdaptersConfig<br/>@ConditionalOnMissingBean<br/>(realistic sample data)"]

    REAL --> WC["WebClient<br/>JdkClientHttpConnector"]
    MOCK --> FALLBACK["In-memory<br/>mock response"]

    WC --> SVC["External Microservice"]

    WC -->|4xx| PNF["PaymentNotFoundException"]
    WC -->|5xx| SUE["ServiceUnavailableException"]
    WC -->|404 on ES/Tracing| EMPTY["Empty snapshot<br/>(graceful degradation)"]

    style PROP fill:#fff3e0,stroke:#e65100,stroke-width:2px
    style REAL fill:#e8f5e9,stroke:#2e7d32
    style MOCK fill:#e3f2fd,stroke:#1565c0
```

## 4. Test Pyramid

```mermaid
graph BT
    subgraph Unit["Unit Tests (43)"]
        UT_AGENT["Agent Actions<br/>BDD Mockito"]
        UT_ADAPTER["7 Adapter Tests<br/>WireMock"]
        UT_DOMAIN["Domain Model &<br/>ReportFormatter"]
        UT_CTRL["Controller<br/>WebTestClient"]
        UT_SHELL["Shell Command<br/>Mockito"]
        UT_ARCH["ArchUnit Rules (7)<br/>Hexagonal Enforcement"]
    end

    subgraph Integration["Integration Tests (3)"]
        IT["EmbabelMockitoIntegrationTest<br/>Full GOAP Chain"]
    end

    Unit --> Integration

    style Unit fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style Integration fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
```

## 5. Domain Model Relationships

```mermaid
classDiagram
    class InvestigationQuery {
        String paymentId
        String merchantId
        String corridor
    }

    class CompletedInvestigation {
        InvestigationQuery query
        PaymentState paymentState
        ComplianceSnapshot complianceSnapshot
        BlockchainSnapshot blockchainSnapshot
        LedgerSnapshot ledgerSnapshot
        WorkflowSnapshot workflowSnapshot
        LogSnapshot logSnapshot
        TraceSnapshot traceSnapshot
        InvestigationReport report
        String formattedBody
    }

    class PaymentState {
        String paymentId
        PaymentStatus status
        String sagaStep
        String workflowId
        List~SagaEvent~ events
    }

    class ComplianceSnapshot {
        String paymentId
        ScreeningResult screeningResult
        String travelRuleStatus
        String riskScore
        List~ComplianceDecision~ decisions
    }

    class BlockchainSnapshot {
        String paymentId
        String txHash
        String chain
        int confirmations
        String amount
    }

    class LedgerSnapshot {
        String paymentId
        List~LedgerEntry~ entries
        String netPosition
        String settlementStatus
    }

    class WorkflowSnapshot {
        String paymentId
        String workflowId
        String status
        List~WorkflowEvent~ events
    }

    class LogSnapshot {
        String paymentId
        int totalHits
        List~LogEntry~ entries
    }

    class TraceSnapshot {
        String paymentId
        String traceId
        int totalSpans
        long durationMs
        List~TraceSpan~ spans
    }

    class InvestigationReport {
        List~TimelineEvent~ timeline
        String rootCause
        List~Finding~ findings
        String recommendation
        InvestigationSeverity severity
    }

    CompletedInvestigation --> InvestigationQuery
    CompletedInvestigation --> PaymentState
    CompletedInvestigation --> ComplianceSnapshot
    CompletedInvestigation --> BlockchainSnapshot
    CompletedInvestigation --> LedgerSnapshot
    CompletedInvestigation --> WorkflowSnapshot
    CompletedInvestigation --> LogSnapshot
    CompletedInvestigation --> TraceSnapshot
    CompletedInvestigation --> InvestigationReport
    PaymentState --> "0..*" SagaEvent
    ComplianceSnapshot --> "0..*" ComplianceDecision
    LedgerSnapshot --> "0..*" LedgerEntry
    WorkflowSnapshot --> "0..*" WorkflowEvent
    LogSnapshot --> "0..*" LogEntry
    TraceSnapshot --> "0..*" TraceSpan
    InvestigationReport --> "0..*" TimelineEvent
    InvestigationReport --> "0..*" Finding

    class SagaEvent { }
    class ComplianceDecision { }
    class LedgerEntry { }
    class WorkflowEvent { }
    class LogEntry { }
    class TraceSpan { }
    class TimelineEvent { }
    class Finding { }
```
