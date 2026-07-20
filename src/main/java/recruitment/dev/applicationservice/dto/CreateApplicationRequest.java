package recruitment.dev.applicationservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApplicationRequest {


    @NotNull(message = "Candidate ID is required")
    @Positive(message = "Candidate ID must be positive")
    private Long candidateId;


    @NotNull(message = "Job offer ID is required")
    @Positive(message = "Job offer ID must be positive")
    private Long jobOfferId;

}