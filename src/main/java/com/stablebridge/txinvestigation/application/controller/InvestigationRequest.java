package com.stablebridge.txinvestigation.application.controller;

import jakarta.validation.constraints.NotBlank;

public record InvestigationRequest(
        @NotBlank String paymentId,
        String merchantId
) {}
