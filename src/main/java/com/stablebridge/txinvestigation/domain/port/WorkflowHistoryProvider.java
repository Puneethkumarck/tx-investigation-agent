package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.WorkflowSnapshot;

public interface WorkflowHistoryProvider {

    WorkflowSnapshot fetchWorkflowHistory(String paymentId);
}
