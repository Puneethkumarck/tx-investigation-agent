package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.Finding;
import com.stablebridge.txinvestigation.domain.model.InvestigationReport;
import com.stablebridge.txinvestigation.domain.model.TimelineEvent;

import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.domain.model.FindingCategory.BLOCKCHAIN_DELAY;
import static com.stablebridge.txinvestigation.domain.model.FindingCategory.SLA_BREACH;
import static com.stablebridge.txinvestigation.domain.model.InvestigationSeverity.HIGH;
import static com.stablebridge.txinvestigation.domain.model.InvestigationSeverity.MEDIUM;

public final class InvestigationReportFixtures {

    private InvestigationReportFixtures() {}

    public static TimelineEvent aTimelineEvent() {
        return new TimelineEvent(
                Instant.parse("2026-03-08T10:30:00Z"),
                "S4 Blockchain",
                "Transaction submitted to Base chain",
                "PENDING");
    }

    public static Finding aFinding() {
        return new Finding(
                BLOCKCHAIN_DELAY,
                HIGH,
                "Blockchain transaction pending for 47 minutes — exceeds 30-minute SLA");
    }

    public static InvestigationReport anInvestigationReport() {
        return new InvestigationReport(
                List.of(
                        new TimelineEvent(Instant.parse("2026-03-08T10:00:00Z"),
                                "S2 Compliance", "Sanctions screening completed", "CLEAR"),
                        new TimelineEvent(Instant.parse("2026-03-08T10:15:00Z"),
                                "S3 On-Ramp", "ACH debit confirmed", "COMPLETED"),
                        aTimelineEvent()
                ),
                "Blockchain transaction pending for 47 minutes — exceeds 30-minute SLA",
                List.of(
                        aFinding(),
                        new Finding(SLA_BREACH, MEDIUM,
                                "Payment SLA of 60 minutes at risk due to blockchain delay")
                ),
                List.of(
                        "Monitor blockchain transaction 0xabc123def456 for confirmation",
                        "Consider resubmitting with higher gas fee if not confirmed within 60 minutes",
                        "Notify merchant MCH-001 of delay via webhook"
                ),
                HIGH);
    }
}
