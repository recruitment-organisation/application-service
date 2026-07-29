package recruitment.dev.applicationservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMatchingScoreRequest {

    @NotNull(message = "matchingScore is required")
    @DecimalMin(value = "0.0", message = "matchingScore must be greater than or equal to 0")
    @DecimalMax(value = "100.0", message = "matchingScore must be lower than or equal to 100")
    private Double matchingScore;
}
