package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.LogSnapshot;

public interface LogSearchProvider {

    LogSnapshot searchErrorLogs(String paymentId);
}
