package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.InvestigationQuery;

public final class InvestigationQueryFixtures {

    public static final String PAYMENT_ID = "PAY-abc-123";
    public static final String MERCHANT_ID = "MCH-001";
    public static final String CORRIDOR = "US-DE";

    private InvestigationQueryFixtures() {}

    public static InvestigationQuery anInvestigationQuery() {
        return new InvestigationQuery(PAYMENT_ID, MERCHANT_ID, CORRIDOR);
    }
}
