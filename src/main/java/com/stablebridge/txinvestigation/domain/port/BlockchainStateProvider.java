package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.BlockchainSnapshot;

public interface BlockchainStateProvider {

    BlockchainSnapshot fetchBlockchainStatus(String paymentId);
}
