package recruitment.dev.applicationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.client.WorkflowClient;
import recruitment.dev.applicationservice.client.WorkflowStartResponse;
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
import recruitment.dev.applicationservice.outbox.OutboxEventRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private WorkflowClient workflowClient;
    @Mock private EntityManager entityManager;
    @Mock private MinioService minioService;
    @InjectMocks private ApplicationServiceImpl service;

    @Test
    void createsApplicationWithInitialWorkflowState() {
        CreateApplicationRequest request = CreateApplicationRequest.builder().candidateId(3L).jobOfferId(9L).build();
        Application saved = new Application();
        saved.setId(11L);
        ApplicationDto expected = new ApplicationDto();
        when(applicationRepository.existsByCandidateIdAndJobOfferId(3L, 9L)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);
        when(applicationMapper.toDto(saved)).thenReturn(expected);

        assertThat(service.create(request, "candidate-keycloak-id")).isSameAs(expected);

        ArgumentCaptor<Application> application = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(application.capture());
        assertThat(application.getValue().getCandidateId()).isEqualTo(3L);
        assertThat(application.getValue().getCandidateKeycloakId()).isEqualTo("candidate-keycloak-id");
        assertThat(application.getValue().getJobOfferId()).isEqualTo(9L);
        assertThat(application.getValue().getStatus()).isEqualTo(ApplicationStatus.CV_REVISION_REQUIRED);
        assertThat(application.getValue().getCurrentStep()).isEqualTo(ApplicationStep.CREATED);
        assertThat(application.getValue().getAppliedAt()).isNotNull();
        assertThat(application.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateApplication() {
        CreateApplicationRequest request = CreateApplicationRequest.builder().candidateId(3L).jobOfferId(9L).build();
        when(applicationRepository.existsByCandidateIdAndJobOfferId(3L, 9L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(DuplicateApplicationException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void submitsApplicationOnlyAfterCvUpload() throws Exception {
        Application application = new Application();
        application.setId(5L);
        application.setCandidateId(3L);
        application.setCandidateKeycloakId("candidate-keycloak-id");
        application.setJobOfferId(9L);
        recruitment.dev.applicationservice.entities.CV cv = new recruitment.dev.applicationservice.entities.CV();
        cv.setId(12L);
        application.setCv(cv);
        ApplicationDto expected = new ApplicationDto();
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(expected);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(workflowClient.startRecruitment(any())).thenReturn(new WorkflowStartResponse(
                "process-5", "recruitment:1:5", "5", "SUBMITTED",
                "task-5", "applicationSubmitted", "Application Submitted"
        ));

        assertThat(service.submit(5L, "candidate-keycloak-id")).isSameAs(expected);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.getCurrentStep()).isEqualTo(ApplicationStep.COMPLETED);
        assertThat(application.getProcessInstanceId()).isEqualTo("process-5");
        assertThat(application.getCurrentTaskId()).isEqualTo("task-5");
        verify(entityManager).refresh(application);
        verify(applicationRepository).save(application);
    }

    @Test
    void refusesSubmissionWithoutCv() {
        Application application = new Application();
        application.setCandidateKeycloakId("candidate-keycloak-id");
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.submit(5L, "candidate-keycloak-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("CV must be uploaded before submit");
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void changesMatchingScoreAndStatus() throws Exception {
        Application application = new Application();
        ApplicationDto expected = new ApplicationDto();
        when(applicationRepository.findById(8L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(expected);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertThat(service.updateMatchingScore(8L, 87.5)).isSameAs(expected);
        assertThat(application.getMatchingScore()).isEqualTo(87.5);
        assertThat(service.updateStatus(8L, "submitted")).isSameAs(expected);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void onlySchedulesTheHrInterviewForThePendingHrWorkflowTask() throws Exception {
        Application application = new Application();
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setCurrentTaskDefinitionKey("hrInterview");
        ApplicationDto expected = new ApplicationDto();
        when(applicationRepository.findById(8L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(expected);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        assertThat(service.markHrInterviewScheduled(8L)).isSameAs(expected);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.HR_INTERVIEW);
    }

    @Test
    void rejectsAnUnknownStatusWithoutPersistingChanges() {
        Application application = new Application();
        when(applicationRepository.findById(8L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.updateStatus(8L, "unknown-status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid application status: unknown-status");

        verify(applicationRepository, never()).save(application);
    }

    @Test
    void reportsMissingApplicationOnReadAndDelete() {
        when(applicationRepository.findById(91L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(91L)).isInstanceOf(ApplicationNotFoundException.class);
        assertThatThrownBy(() -> service.delete(91L)).isInstanceOf(ApplicationNotFoundException.class);
    }

    @Test
    void deletesTheCvObjectBeforeDeletingTheApplication() {
        Application application = new Application();
        application.setId(7L);
        application.setCv(CV.builder().fileUrl("Jane Doe - Java Developer.pdf").build());
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        service.delete(7L);

        verify(minioService).delete("Jane Doe - Java Developer.pdf");
        verify(applicationRepository).delete(application);
    }

    @Test
    void doesNotDeleteAProcessedApplicationBecauseItWouldAllowReapplying() {
        Application application = new Application();
        application.setStatus(ApplicationStatus.REJECTED);
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be deleted");

        verify(applicationRepository, never()).delete(application);
        verifyNoInteractions(minioService);
    }

    @Test
    void aggregatesDashboardCountsFromRepositoryStatuses() {
        when(applicationRepository.count()).thenReturn(18L);
        when(applicationRepository.countByStatus(ApplicationStatus.SUBMITTED)).thenReturn(4L);
        when(applicationRepository.countByStatus(ApplicationStatus.UNDER_AI_REVIEW)).thenReturn(3L);
        when(applicationRepository.countByStatus(ApplicationStatus.HIRED)).thenReturn(5L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(2L);

        ApplicationDashboardCounts counts = service.getDashboardCounts();

        assertThat(counts.getTotal()).isEqualTo(18L);
        assertThat(counts.getPending()).isEqualTo(7L);
        assertThat(counts.getHired()).isEqualTo(5L);
        assertThat(counts.getRejected()).isEqualTo(2L);
    }
}
