package recruitment.dev.applicationservice.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Page<Application> findByCandidateId(Long candidateId, Pageable pageable);
    Page<Application> findByCandidateKeycloakId(String candidateKeycloakId, Pageable pageable);

    Page<Application> findByJobOfferId(Long jobOfferId, Pageable pageable);

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    boolean existsByCandidateIdAndJobOfferId(Long candidateId, Long jobOfferId);

    long countByStatus(ApplicationStatus status);
}
