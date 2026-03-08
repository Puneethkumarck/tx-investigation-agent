package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.LedgerSnapshot;

public interface LedgerStateProvider {

    LedgerSnapshot fetchLedgerEntries(String paymentId);
}
