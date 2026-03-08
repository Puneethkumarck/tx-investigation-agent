package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.WorkflowEvent;
import com.stablebridge.txinvestigation.domain.model.WorkflowSnapshot;

import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;

public final class WorkflowSnapshotFixtures {

    private WorkflowSnapshotFixtures() {}

    public static WorkflowEvent aWorkflowStartedEvent() {
        return new WorkflowEvent(1L, "WorkflowExecutionStarted",
                Instant.parse("2026-03-08T09:45:00Z"), null, "Workflow started");
    }

    public static WorkflowEvent anActivityCompletedEvent() {
        return new WorkflowEvent(2L, "ActivityTaskCompleted",
                Instant.parse("2026-03-08T09:50:00Z"), "ComplianceCheck",
                "Compliance check passed");
    }

    public static WorkflowEvent anActivityScheduledEvent() {
        return new WorkflowEvent(3L, "ActivityTaskScheduled",
                Instant.parse("2026-03-08T10:15:00Z"), "BlockchainSubmit",
                "Scheduled blockchain submission");
    }

    public static WorkflowSnapshot aWorkflowSnapshot() {
        return new WorkflowSnapshot(
                PAYMENT_ID,
                "wf-abc-123",
                "PaymentWorkflow",
                "RUNNING",
                Instant.parse("2026-03-08T09:45:00Z"),
                null,
                1,
                "payment-task-queue",
                List.of(aWorkflowStartedEvent(), anActivityCompletedEvent(),
                        anActivityScheduledEvent()));
    }
}
