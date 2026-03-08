package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.ComplianceSnapshot;

public interface ComplianceStateProvider {

    ComplianceSnapshot fetchComplianceStatus(String paymentId);
}
