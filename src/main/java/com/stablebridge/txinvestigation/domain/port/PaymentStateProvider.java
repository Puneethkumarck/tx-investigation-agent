package com.stablebridge.txinvestigation.domain.port;

import com.stablebridge.txinvestigation.domain.model.PaymentState;

public interface PaymentStateProvider {

    PaymentState fetchPaymentState(String paymentId);
}
