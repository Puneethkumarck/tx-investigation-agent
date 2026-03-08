package com.stablebridge.txinvestigation.infrastructure.mock;

import com.stablebridge.txinvestigation.domain.model.BlockchainSnapshot;
import com.stablebridge.txinvestigation.domain.model.ComplianceDecision;
import com.stablebridge.txinvestigation.domain.model.ComplianceSnapshot;
import com.stablebridge.txinvestigation.domain.model.Direction;
import com.stablebridge.txinvestigation.domain.model.LedgerEntry;
import com.stablebridge.txinvestigation.domain.model.LedgerSnapshot;
import com.stablebridge.txinvestigation.domain.model.LogEntry;
import com.stablebridge.txinvestigation.domain.model.LogLevel;
import com.stablebridge.txinvestigation.domain.model.LogSnapshot;
import com.stablebridge.txinvestigation.domain.model.PaymentState;
import com.stablebridge.txinvestigation.domain.model.PaymentStatus;
import com.stablebridge.txinvestigation.domain.model.SagaEvent;
import com.stablebridge.txinvestigation.domain.model.ScreeningResult;
import com.stablebridge.txinvestigation.domain.model.TraceSnapshot;
import com.stablebridge.txinvestigation.domain.model.TraceSpan;
import com.stablebridge.txinvestigation.domain.model.WorkflowEvent;
import com.stablebridge.txinvestigation.domain.model.WorkflowSnapshot;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.LogSearchProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import com.stablebridge.txinvestigation.domain.port.TraceProvider;
import com.stablebridge.txinvestigation.domain.port.WorkflowHistoryProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Configuration
class MockAdaptersConfig {

    @Bean
    @ConditionalOnMissingBean
    PaymentStateProvider mockPaymentStateProvider() {
        log.warn("Using mock PaymentStateProvider — no live orchestrator configured");
        return paymentId -> new PaymentState(
                paymentId,
                PaymentStatus.BLOCKCHAIN_PENDING,
                "BLOCKCHAIN_SUBMIT",
                "wf-mock-001",
                List.of(
                        new SagaEvent("COMPLIANCE_CHECK", "COMPLETED",
                                Instant.now().minusSeconds(2700), "All checks passed"),
                        new SagaEvent("FIAT_COLLECTION", "COMPLETED",
                                Instant.now().minusSeconds(1800), "ACH debit confirmed"),
                        new SagaEvent("BLOCKCHAIN_SUBMIT", "PENDING",
                                Instant.now().minusSeconds(900), "Transaction submitted")
                ),
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(900));
    }

    @Bean
    @ConditionalOnMissingBean
    ComplianceStateProvider mockComplianceStateProvider() {
        log.warn("Using mock ComplianceStateProvider — no live compliance service configured");
        return paymentId -> new ComplianceSnapshot(
                paymentId,
                ScreeningResult.CLEAR,
                "COMPLETED",
                0.12,
                List.of(
                        new ComplianceDecision("SANCTIONS_SCREENING", "CLEAR", "chainalysis",
                                Instant.now().minusSeconds(2700), "No matches found"),
                        new ComplianceDecision("TRAVEL_RULE", "COMPLETED", "notabene",
                                Instant.now().minusSeconds(2640), "IVMS101 exchanged")
                ));
    }

    @Bean
    @ConditionalOnMissingBean
    BlockchainStateProvider mockBlockchainStateProvider() {
        log.warn("Using mock BlockchainStateProvider — no live blockchain service configured");
        return paymentId -> new BlockchainSnapshot(
                paymentId,
                "0xmock123abc456",
                "base",
                0,
                new BigDecimal("1000.00"),
                "USDC",
                "0xMockSender",
                "0xMockReceiver",
                Instant.now().minusSeconds(900),
                "PENDING");
    }

    @Bean
    @ConditionalOnMissingBean
    LedgerStateProvider mockLedgerStateProvider() {
        log.warn("Using mock LedgerStateProvider — no live ledger service configured");
        return paymentId -> new LedgerSnapshot(
                paymentId,
                List.of(
                        new LedgerEntry("LE-MOCK-001", "MERCHANT_USD", Direction.DEBIT,
                                new BigDecimal("1000.00"), "USD",
                                Instant.now().minusSeconds(1800)),
                        new LedgerEntry("LE-MOCK-002", "POOL_USDC", Direction.CREDIT,
                                new BigDecimal("1000.00"), "USDC",
                                Instant.now().minusSeconds(900))
                ),
                BigDecimal.ZERO,
                "PENDING");
    }

    @Bean
    @ConditionalOnMissingBean
    WorkflowHistoryProvider mockWorkflowHistoryProvider() {
        log.warn("Using mock WorkflowHistoryProvider — no live Temporal configured");
        return paymentId -> new WorkflowSnapshot(
                paymentId,
                "wf-mock-" + paymentId,
                "PaymentWorkflow",
                "RUNNING",
                Instant.now().minusSeconds(3600),
                null,
                1,
                "payment-task-queue",
                List.of(
                        new WorkflowEvent(1L, "WorkflowExecutionStarted",
                                Instant.now().minusSeconds(3600), null, "Workflow started"),
                        new WorkflowEvent(2L, "ActivityTaskScheduled",
                                Instant.now().minusSeconds(2700), "ComplianceCheck",
                                "Scheduled compliance check"),
                        new WorkflowEvent(3L, "ActivityTaskCompleted",
                                Instant.now().minusSeconds(2640), "ComplianceCheck",
                                "Compliance check passed"),
                        new WorkflowEvent(4L, "ActivityTaskScheduled",
                                Instant.now().minusSeconds(900), "BlockchainSubmit",
                                "Scheduled blockchain submission")));
    }

    @Bean
    @ConditionalOnMissingBean
    LogSearchProvider mockLogSearchProvider() {
        log.warn("Using mock LogSearchProvider — no live Elasticsearch configured");
        return paymentId -> new LogSnapshot(
                paymentId,
                2,
                List.of(
                        new LogEntry(Instant.now().minusSeconds(850), LogLevel.WARN,
                                "blockchain-custody",
                                "Transaction confirmation timeout approaching SLA",
                                "trace-mock-001", null),
                        new LogEntry(Instant.now().minusSeconds(300), LogLevel.ERROR,
                                "blockchain-custody",
                                "Gas price spike detected — retry pending",
                                "trace-mock-001",
                                "java.lang.RuntimeException: Gas estimation failed\n\tat ...")));
    }

    @Bean
    @ConditionalOnMissingBean
    TraceProvider mockTraceProvider() {
        log.warn("Using mock TraceProvider — no live tracing backend configured");
        return paymentId -> new TraceSnapshot(
                paymentId,
                "trace-mock-001",
                4,
                2700000L,
                List.of(
                        new TraceSpan("span-1", null, "POST /api/v1/payments",
                                "payment-orchestrator",
                                Instant.now().minusSeconds(3600), 2700000L, "OK", null),
                        new TraceSpan("span-2", "span-1", "ComplianceCheck",
                                "compliance-travel-rule",
                                Instant.now().minusSeconds(2700), 60000L, "OK", null),
                        new TraceSpan("span-3", "span-1", "FiatCollection",
                                "fiat-on-ramp",
                                Instant.now().minusSeconds(2640), 1740000L, "OK", null),
                        new TraceSpan("span-4", "span-1", "BlockchainSubmit",
                                "blockchain-custody",
                                Instant.now().minusSeconds(900), 0L, "ERROR",
                                "Transaction pending — awaiting confirmation")));
    }
}
