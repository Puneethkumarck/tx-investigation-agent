package com.stablebridge.txinvestigation.domain.model;

public record InvestigationQuery(
        String paymentId,
        String merchantId,
        String corridor
) {}
