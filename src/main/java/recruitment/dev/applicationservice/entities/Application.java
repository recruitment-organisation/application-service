package recruitment.dev.applicationservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "application",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_candidate_job_offer",
                columnNames = {"candidate_id", "job_offer_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long candidateId;

    /** Keycloak subject of the candidate who owns this application. */
    private String candidateKeycloakId;

    private Long jobOfferId;

    @Column(name = "company_id")
    private Long companyId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;


    @Enumerated(EnumType.STRING)
    private ApplicationStep currentStep;

    /** Flowable process started when this application is submitted. */
    @Column(length = 255)
    private String processInstanceId;

    /** Current active Flowable task. It is null once the workflow has finished. */
    @Column(length = 255)
    private String currentTaskId;

    @Column(length = 255)
    private String currentTaskDefinitionKey;

    @Column(length = 255)
    private String currentTaskName;

    private Double matchingScore;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    @OneToOne(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JoinColumn(name = "cv_id")
    private CV cv;
}
