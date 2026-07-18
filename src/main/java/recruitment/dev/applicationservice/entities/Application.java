package recruitment.dev.applicationservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long candidateId;
    private Long jobOfferId;
    private Long cvId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private Double matchingScore;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}