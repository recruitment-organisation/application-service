package recruitment.dev.applicationservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.entities.ApplicationStep;

@Getter
@Setter
@NoArgsConstructor
public class UpdateApplicationRequest {
    @NotNull
    private ApplicationStatus status;
    private ApplicationStep currentStep;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private Double matchingScore;
}
