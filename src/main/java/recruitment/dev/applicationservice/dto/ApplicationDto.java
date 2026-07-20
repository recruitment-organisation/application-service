package recruitment.dev.applicationservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.entities.ApplicationStep;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor

@NoArgsConstructor
@Builder
public class ApplicationDto {

    private Long id;


    private Long candidateId;


    private Long jobOfferId;


    private Long cvId;


    private ApplicationStatus status;


    private ApplicationStep currentStep;


    private Double matchingScore;


    private LocalDateTime appliedAt;


    private LocalDateTime updatedAt;


}
