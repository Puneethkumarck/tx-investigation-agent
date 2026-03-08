package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.TraceSnapshot;

public interface TraceProvider {

    TraceSnapshot fetchTrace(String paymentId);
}
