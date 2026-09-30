package recruitment.dev.applicationservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.dto.ApplicationDashboardCounts;
import recruitment.dev.applicationservice.dto.CreateApplicationRequest;
import recruitment.dev.applicationservice.entities.ApplicationStatus;

public interface ApplicationService {
    @Deprecated
    ApplicationDto create(CreateApplicationRequest dto);
    ApplicationDto create(CreateApplicationRequest dto, String candidateKeycloakId);
    ApplicationDto create(CreateApplicationRequest dto, String candidateKeycloakId, Long companyId);
    ApplicationDto submit(Long id, String candidateKeycloakId, String accessToken);
    ApplicationDto update(Long id, ApplicationDto dto);
    ApplicationDto markHrInterviewScheduled(Long id);
    ApplicationDto updateForCandidate(Long id, ApplicationDto dto, String candidateKeycloakId);
    ApplicationDto getById(Long id);
    Page<ApplicationDto> getAll(Pageable pageable);
    Page<ApplicationDto> getAll(Long companyId, Pageable pageable);
    Page<ApplicationDto> getByCandidateId(Long candidateId, Pageable pageable);
    Page<ApplicationDto> getByCandidateId(Long companyId, Long candidateId, Pageable pageable);
    Page<ApplicationDto> getMine(String candidateKeycloakId, Pageable pageable);
    Page<ApplicationDto> getByJobOfferId(Long jobOfferId, Pageable pageable);
    Page<ApplicationDto> getByJobOfferId(Long companyId, Long jobOfferId, Pageable pageable);
    Page<ApplicationDto> getByStatus(ApplicationStatus status, Pageable pageable);
    Page<ApplicationDto> getByStatus(Long companyId, ApplicationStatus status, Pageable pageable);

    ApplicationDashboardCounts getDashboardCounts();
    ApplicationDashboardCounts getDashboardCounts(Long companyId);
    void delete(Long id);
    ApplicationDto updateMatchingScore(Long id, Double matchingScore);
    ApplicationDto updateStatus(Long id, String status);
    ApplicationDto updateWorkflowState(
            Long id,
            String processInstanceId,
            String currentTaskId,
            String currentTaskDefinitionKey,
            String currentTaskName
    );
}
