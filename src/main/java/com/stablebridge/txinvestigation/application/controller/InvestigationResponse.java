package com.stablebridge.txinvestigation.application.controller;

import com.stablebridge.txinvestigation.domain.model.Finding;
import com.stablebridge.txinvestigation.domain.model.InvestigationSeverity;
import com.stablebridge.txinvestigation.domain.model.PaymentStatus;
import com.stablebridge.txinvestigation.domain.model.TimelineEvent;
import lombok.Builder;

import java.util.List;

@Builder
public record InvestigationResponse(
        String paymentId,
        PaymentStatus status,
        InvestigationSeverity severity,
        String rootCause,
        List<Finding> findings,
        List<TimelineEvent> timeline,
        String recommendation,
        int errorLogCount,
        String traceId,
        String workflowStatus,
        String formattedReport
) {}
