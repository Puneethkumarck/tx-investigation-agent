package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;

public record SagaEvent(
        String step,
        String status,
        Instant timestamp,
        String detail
) {}
