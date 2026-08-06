package recruitment.dev.applicationservice.client;

public record WorkflowStartRequest(
        Long applicationId,
        Long candidateId,
        Long jobId,
        Long cvId
) {
}
