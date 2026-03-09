package com.stablebridge.txinvestigation.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.aFinding;
import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.aTimelineEvent;
import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.anInvestigationReport;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static org.assertj.core.api.Assertions.assertThat;

class ReportFormatterTest {

    private final ReportFormatter formatter = new ReportFormatter();

    @Test
    void shouldFormatFullReportWithAllSections() {
        // given
        var report = anInvestigationReport();
        var payment = aPaymentState();

        // when
        var result = formatter.format(report, payment);

        // then
        assertThat(result)
                .contains("## Investigation Report: PAY-abc-123")
                .contains("BLOCKCHAIN_PENDING")
                .contains("HIGH")
                .contains("### Root Cause")
                .contains("pending for 47 minutes")
                .contains("### Timeline")
                .contains("### Findings")
                .contains("### Recommendation");
    }

    @Test
    void shouldFormatTimelineAsMarkdownTable() {
        // given
        var events = List.of(aTimelineEvent());

        // when
        var result = formatter.formatTimeline(events);

        // then
        assertThat(result)
                .contains("### Timeline")
                .contains("| Time | Service | Event | Status |")
                .contains("S4 Blockchain")
                .contains("PENDING");
    }

    @Test
    void shouldFormatFindingsWithSeverityBadges() {
        // given
        var findings = List.of(aFinding());

        // when
        var result = formatter.formatFindings(findings);

        // then
        assertThat(result)
                .contains("### Findings")
                .contains("[HIGH]")
                .contains("[BLOCKCHAIN_DELAY]")
                .contains("pending for 47 minutes");
    }

    @Test
    void shouldIncludeSingleRecommendation() {
        // given
        var report = anInvestigationReport();
        var payment = aPaymentState();

        // when
        var result = formatter.format(report, payment);

        // then
        assertThat(result)
                .contains("### Recommendation")
                .contains("Resubmit blockchain transaction 0xabc123def456 with higher gas fee via custody service /api/v1/retry");
    }
}
