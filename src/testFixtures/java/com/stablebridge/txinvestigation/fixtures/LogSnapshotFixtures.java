package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.LogEntry;
import com.stablebridge.txinvestigation.domain.model.LogLevel;
import com.stablebridge.txinvestigation.domain.model.LogSnapshot;

import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;

public final class LogSnapshotFixtures {

    private LogSnapshotFixtures() {}

    public static LogEntry aWarnLogEntry() {
        return new LogEntry(
                Instant.parse("2026-03-08T10:15:30Z"),
                LogLevel.WARN,
                "blockchain-custody",
                "Transaction confirmation timeout approaching SLA",
                "trace-abc-123",
                null);
    }

    public static LogEntry anErrorLogEntry() {
        return new LogEntry(
                Instant.parse("2026-03-08T10:24:10Z"),
                LogLevel.ERROR,
                "blockchain-custody",
                "Gas price spike detected — retry pending",
                "trace-abc-123",
                "java.lang.RuntimeException: Gas estimation failed\n\tat BlockchainSubmitActivity.execute");
    }

    public static LogSnapshot aLogSnapshot() {
        return new LogSnapshot(
                PAYMENT_ID,
                2,
                List.of(aWarnLogEntry(), anErrorLogEntry()));
    }
}
