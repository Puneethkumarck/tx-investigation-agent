package com.stablebridge.txinvestigation.domain.model;

public enum PaymentStatus {
    INITIATED,
    COMPLIANCE_CHECK,
    FIAT_COLLECTED,
    BLOCKCHAIN_PENDING,
    BLOCKCHAIN_CONFIRMED,
    FIAT_DISBURSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
