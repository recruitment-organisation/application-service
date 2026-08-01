package recruitment.dev.applicationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.dto.ApplicationDashboardCounts;
import recruitment.dev.applicationservice.dto.CreateApplicationRequest;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.entities.ApplicationStep;
import recruitment.dev.applicationservice.exception.ApplicationNotFoundException;
import recruitment.dev.applicationservice.exception.DuplicateApplicationException;
import recruitment.dev.applicationservice.mapper.ApplicationMapper;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationMapper applicationMapper;
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
    void submitsApplicationOnlyAfterCvUpload() {
        Application application = new Application();
        application.setCv(new recruitment.dev.applicationservice.entities.CV());
        ApplicationDto expected = new ApplicationDto();
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(expected);

        assertThat(service.submit(5L)).isSameAs(expected);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.getCurrentStep()).isEqualTo(ApplicationStep.COMPLETED);
        verify(applicationRepository).save(application);
    }

    @Test
    void refusesSubmissionWithoutCv() {
        Application application = new Application();
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.submit(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("CV must be uploaded before submit");
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void changesMatchingScoreAndStatus() {
        Application application = new Application();
        ApplicationDto expected = new ApplicationDto();
        when(applicationRepository.findById(8L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);
        when(applicationMapper.toDto(application)).thenReturn(expected);

        assertThat(service.updateMatchingScore(8L, 87.5)).isSameAs(expected);
        assertThat(application.getMatchingScore()).isEqualTo(87.5);
        assertThat(service.updateStatus(8L, "submitted")).isSameAs(expected);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
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
        when(applicationRepository.existsById(91L)).thenReturn(false);

        assertThatThrownBy(() -> service.getById(91L)).isInstanceOf(ApplicationNotFoundException.class);
        assertThatThrownBy(() -> service.delete(91L)).isInstanceOf(ApplicationNotFoundException.class);
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
