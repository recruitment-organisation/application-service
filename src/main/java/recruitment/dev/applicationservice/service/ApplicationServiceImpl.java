package recruitment.dev.applicationservice.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import recruitment.dev.applicationservice.client.WorkflowClient;
import recruitment.dev.applicationservice.client.WorkflowStartRequest;
import recruitment.dev.applicationservice.client.WorkflowStartResponse;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.dto.ApplicationDashboardCounts;
import recruitment.dev.applicationservice.dto.CreateApplicationRequest;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.entities.ApplicationStep;
import recruitment.dev.applicationservice.entities.CV;
import recruitment.dev.applicationservice.exception.ApplicationNotFoundException;
import recruitment.dev.applicationservice.exception.DuplicateApplicationException;
import recruitment.dev.applicationservice.mapper.ApplicationMapper;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;
import recruitment.dev.applicationservice.outbox.OutboxEvent;
import recruitment.dev.applicationservice.outbox.OutboxEventRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowClient workflowClient;
    private final EntityManager entityManager;
    private final MinioService minioService;

    @Override
    @Deprecated
    public ApplicationDto create(CreateApplicationRequest dto) {
        return create(dto, null);
    }

    @Override
    public ApplicationDto create(CreateApplicationRequest dto, String candidateKeycloakId) {

        if(applicationRepository.existsByCandidateIdAndJobOfferId(
                dto.getCandidateId(),
                dto.getJobOfferId())) {

            throw new DuplicateApplicationException(
                    dto.getCandidateId(),
                    dto.getJobOfferId()
            );
        }


        Application entity = new Application();

        entity.setCandidateId(dto.getCandidateId());
        entity.setCandidateKeycloakId(candidateKeycloakId);

        entity.setJobOfferId(dto.getJobOfferId());


        entity.setStatus(ApplicationStatus.CV_REVISION_REQUIRED);


        entity.setCurrentStep(
                ApplicationStep.CREATED
        );


        entity.setAppliedAt(LocalDateTime.now());

        entity.setUpdatedAt(LocalDateTime.now());


        Application saved =
                applicationRepository.save(entity);


        return applicationMapper.toDto(saved);
    }

    @Override
    public ApplicationDto submit(Long id, String candidateKeycloakId, String accessToken) {

        Application application = getOwnedApplication(id, candidateKeycloakId);


        if(application.getCv() == null){

            throw new RuntimeException(
                    "CV must be uploaded before submit"
            );
        }

        if (application.getProcessInstanceId() != null) {
            return applicationMapper.toDto(application);
        }


        WorkflowStartResponse workflow = workflowClient.startRecruitment(
                "Bearer " + accessToken,
                new WorkflowStartRequest(
                        application.getId(),
                        application.getCandidateId(),
                        application.getJobOfferId(),
                        application.getCv().getId()
                )
        );

        // The workflow persists the AI score in its own transaction. Reload the entity
        // before saving the workflow identifiers so Hibernate does not overwrite it.
        entityManager.refresh(application);


        application.setCurrentStep(
                ApplicationStep.COMPLETED
        );


        application.setStatus(statusFromWorkflow(workflow.currentStatus()));


        application.setUpdatedAt(
                LocalDateTime.now()
        );

        application.setProcessInstanceId(workflow.processInstanceId());
        application.setCurrentTaskId(workflow.currentTaskId());
        application.setCurrentTaskDefinitionKey(workflow.currentTaskDefinitionKey());
        application.setCurrentTaskName(workflow.currentTaskName());


        Application submitted = applicationRepository.save(application);
        enqueueApplicationEvent(submitted, "application.submitted");
        return applicationMapper.toDto(submitted);
    }

    private ApplicationStatus statusFromWorkflow(String workflowStatus) {
        if (workflowStatus == null || workflowStatus.isBlank()) {
            return ApplicationStatus.SUBMITTED;
        }
        try {
            return ApplicationStatus.valueOf(workflowStatus);
        } catch (IllegalArgumentException ignored) {
            return ApplicationStatus.SUBMITTED;
        }
    }

    @Override
    public ApplicationDto update(Long id, ApplicationDto dto) {
        Application entity = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        applicationMapper.updateEntityFromDto(dto, entity);
        entity.setUpdatedAt(LocalDateTime.now());

        Application updated = applicationRepository.save(entity);
        enqueueApplicationEvent(updated, "application.updated");
        return applicationMapper.toDto(updated);
    }

    @Override
    public ApplicationDto updateForCandidate(Long id, ApplicationDto dto, String candidateKeycloakId) {
        Application entity = getOwnedApplication(id, candidateKeycloakId);
        entity.setUpdatedAt(LocalDateTime.now());
        Application updated = applicationRepository.save(entity);
        enqueueApplicationEvent(updated, "application.updated");
        return applicationMapper.toDto(updated);
    }

    @Override
    public ApplicationDto markHrInterviewScheduled(Long id) {
        Application entity = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
        if (entity.getStatus() != ApplicationStatus.SUBMITTED
                || !"hrInterview".equals(entity.getCurrentTaskDefinitionKey())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a submitted application awaiting the HR interview can be scheduled"
            );
        }
        entity.setStatus(ApplicationStatus.HR_INTERVIEW);
        entity.setUpdatedAt(LocalDateTime.now());
        Application updated = applicationRepository.save(entity);
        enqueueApplicationEvent(updated, "application.updated");
        return applicationMapper.toDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDto getById(Long id) {
        Application entity = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
        return applicationMapper.toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getAll(Pageable pageable) {
        return applicationRepository.findAll(pageable)
                .map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getByCandidateId(Long candidateId, Pageable pageable) {
        return applicationRepository.findByCandidateId(candidateId, pageable)
                .map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getMine(String candidateKeycloakId, Pageable pageable) {
        return applicationRepository.findByCandidateKeycloakId(candidateKeycloakId, pageable)
                .map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getByJobOfferId(Long jobOfferId, Pageable pageable) {
        return applicationRepository.findByJobOfferId(jobOfferId, pageable)
                .map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDto> getByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable)
                .map(applicationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDashboardCounts getDashboardCounts() {
        long submitted = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED);
        long aiReview = applicationRepository.countByStatus(ApplicationStatus.UNDER_AI_REVIEW);
        return ApplicationDashboardCounts.builder()
                .total(applicationRepository.count())
                .pending(submitted + aiReview)
                .hired(applicationRepository.countByStatus(ApplicationStatus.HIRED))
                .rejected(applicationRepository.countByStatus(ApplicationStatus.REJECTED))
                .build();
    }

    @Override
    public void delete(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        if (isApplicationLockedAfterHrFiltering(application)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A processed application cannot be deleted or used to submit a new application for this job offer"
            );
        }

        deleteCvObject(application.getCv());
        applicationRepository.delete(application);
    }

    private boolean isApplicationLockedAfterHrFiltering(Application application) {
        if (application.getStatus() == null) {
            return false;
        }
        return switch (application.getStatus()) {
            case HR_INTERVIEW, TECHNICAL_INTERVIEW, MANAGER_INTERVIEW, REJECTED, HIRED, CLOSED -> true;
            case SUBMITTED, CV_REVISION_REQUIRED, UNDER_AI_REVIEW -> false;
        };
    }

    private void deleteCvObject(CV cv) {
        if (cv != null && cv.getFileUrl() != null && !cv.getFileUrl().isBlank()) {
            minioService.delete(cv.getFileUrl());
        }
    }

    @Override
    public ApplicationDto updateMatchingScore(Long id, Double matchingScore) {
        Application entity = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        entity.setMatchingScore(matchingScore);
        entity.setUpdatedAt(LocalDateTime.now());

        Application updated = applicationRepository.save(entity);
        enqueueApplicationEvent(updated, "application.updated");
        return applicationMapper.toDto(updated);
    }

    @Override
    public ApplicationDto updateStatus(Long id, String status) {

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        try {
            ApplicationStatus applicationStatus =
                    ApplicationStatus.valueOf(status.toUpperCase());

            app.setStatus(applicationStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid application status: " + status);
        }

        app.setUpdatedAt(LocalDateTime.now());

        Application updated = applicationRepository.save(app);
        enqueueApplicationEvent(updated, "application.updated");
        return applicationMapper.toDto(updated);
    }

    @Override
    public ApplicationDto updateWorkflowState(
            Long id,
            String processInstanceId,
            String currentTaskId,
            String currentTaskDefinitionKey,
            String currentTaskName
    ) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        application.setProcessInstanceId(processInstanceId);
        application.setCurrentTaskId(currentTaskId);
        application.setCurrentTaskDefinitionKey(currentTaskDefinitionKey);
        application.setCurrentTaskName(currentTaskName);
        application.setUpdatedAt(LocalDateTime.now());

        Application updated = applicationRepository.save(application);
        enqueueApplicationEvent(updated, "application.updated");
        return applicationMapper.toDto(updated);
    }

    private Application getOwnedApplication(Long id, String candidateKeycloakId) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
        if (!candidateKeycloakId.equals(application.getCandidateKeycloakId())) {
            throw new AccessDeniedException("This application does not belong to the authenticated candidate");
        }
        return application;
    }

    private void enqueueApplicationEvent(Application application, String eventType) {
        ApplicationEvent event = new ApplicationEvent(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                application.getId(),
                application.getCandidateId(),
                application.getCandidateKeycloakId(),
                application.getJobOfferId(),
                application.getStatus() == null ? null : application.getStatus().name(),
                application.getCurrentStep() == null ? null : application.getCurrentStep().name()
        );
        try {
            outboxEventRepository.save(new OutboxEvent(
                    "recruitment.application.v1",
                    String.valueOf(application.getId()),
                    objectMapper.writeValueAsString(event)
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize " + eventType + " event", exception);
        }
    }

    private record ApplicationEvent(
            String eventId,
            String eventType,
            String occurredAt,
            Long applicationId,
            Long candidateId,
            String candidateKeycloakId,
            Long jobOfferId,
            String status,
            String currentStep
    ) {
    }
}
