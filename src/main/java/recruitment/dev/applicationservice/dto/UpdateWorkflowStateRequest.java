package recruitment.dev.applicationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkflowStateRequest {

    @NotBlank(message = "processInstanceId is required")
    private String processInstanceId;

    private String currentTaskId;
    private String currentTaskDefinitionKey;
    private String currentTaskName;
}
