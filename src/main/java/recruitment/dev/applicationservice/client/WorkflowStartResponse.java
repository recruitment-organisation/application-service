package recruitment.dev.applicationservice.client;

public record WorkflowStartResponse(
        String processInstanceId,
        String processDefinitionId,
        String businessKey,
        String currentStatus,
        String currentTaskId,
        String currentTaskDefinitionKey,
        String currentTaskName
) {
}
