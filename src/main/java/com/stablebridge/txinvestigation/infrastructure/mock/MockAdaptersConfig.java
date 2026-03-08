package com.stablebridge.txinvestigation.infrastructure.mock;

import com.stablebridge.txinvestigation.domain.model.BlockchainSnapshot;
import com.stablebridge.txinvestigation.domain.model.ComplianceDecision;
import com.stablebridge.txinvestigation.domain.model.ComplianceSnapshot;
import com.stablebridge.txinvestigation.domain.model.Direction;
import com.stablebridge.txinvestigation.domain.model.LedgerEntry;
import com.stablebridge.txinvestigation.domain.model.LedgerSnapshot;
import com.stablebridge.txinvestigation.domain.model.PaymentState;
import com.stablebridge.txinvestigation.domain.model.PaymentStatus;
import com.stablebridge.txinvestigation.domain.model.SagaEvent;
import com.stablebridge.txinvestigation.domain.model.ScreeningResult;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
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
}
