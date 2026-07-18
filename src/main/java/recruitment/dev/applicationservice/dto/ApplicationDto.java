package recruitment.dev.applicationservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import recruitment.dev.applicationservice.entities.ApplicationStatus;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor

@NoArgsConstructor
@Builder
public class ApplicationDto {

    private Long id;

    @NotNull(message = "Candidate ID is required")
    @Positive(message = "Candidate ID must be positive")
    private Long candidateId;

    @NotNull(message = "Job offer ID is required")
    @Positive(message = "Job offer ID must be positive")
    private Long jobOfferId;

    @NotNull(message = "CV ID is required")
    @Positive(message = "CV ID must be positive")
    private Long cvId;

    private ApplicationStatus status;

    @DecimalMin(value = "0.0", message = "Matching score must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "Matching score must be between 0 and 100")
    private Double matchingScore;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
