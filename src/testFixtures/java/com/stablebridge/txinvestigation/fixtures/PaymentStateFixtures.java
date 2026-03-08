package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.PaymentState;
import com.stablebridge.txinvestigation.domain.model.SagaEvent;

import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.domain.model.PaymentStatus.BLOCKCHAIN_PENDING;

public final class PaymentStateFixtures {

    private PaymentStateFixtures() {}

    public static SagaEvent aSagaEvent() {
        return new SagaEvent(
                "BLOCKCHAIN_SUBMIT",
                "PENDING",
                Instant.parse("2026-03-08T10:30:00Z"),
                "Transaction submitted to Base chain");
    }

    public static PaymentState aPaymentState() {
        return new PaymentState(
                InvestigationQueryFixtures.PAYMENT_ID,
                BLOCKCHAIN_PENDING,
                "BLOCKCHAIN_SUBMIT",
                "wf-abc-123",
                List.of(
                        new SagaEvent("COMPLIANCE_CHECK", "COMPLETED",
                                Instant.parse("2026-03-08T10:00:00Z"), "All checks passed"),
                        new SagaEvent("FIAT_COLLECTION", "COMPLETED",
                                Instant.parse("2026-03-08T10:15:00Z"), "ACH debit confirmed"),
                        aSagaEvent()
                ),
                Instant.parse("2026-03-08T09:45:00Z"),
                Instant.parse("2026-03-08T10:30:00Z"));
    }
}
