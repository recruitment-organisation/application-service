package recruitment.dev.applicationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import recruitment.dev.applicationservice.client.CandidateProfile;
import recruitment.dev.applicationservice.client.CandidateProfileClient;
import recruitment.dev.applicationservice.client.JobOfferClient;
import recruitment.dev.applicationservice.client.JobOfferSummary;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStep;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.entities.CV;
import recruitment.dev.applicationservice.mapper.CVMapper;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;
import recruitment.dev.applicationservice.repositories.CVRepository;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CVServiceImplTest {

    @Mock private CVRepository cvRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private CVMapper cvMapper;
    @Mock private MinioService minioService;
    @Mock private CandidateProfileClient candidateProfileClient;
    @Mock private JobOfferClient jobOfferClient;
    @Mock private CvTemplateValidationService cvTemplateValidationService;
    @InjectMocks private CVServiceImpl service;

    @Test
    void replacesOldCvAndMarksApplicationAsUploaded() throws Exception {
        Application application = new Application();
        application.setCandidateId(7L);
        application.setCandidateKeycloakId("candidate-keycloak-id");
        application.setJobOfferId(12L);
        CV oldCv = CV.builder().fileUrl("old.pdf").build();
        application.setCv(oldCv);
        MultipartFile file = mock(MultipartFile.class);
        CVDto expected = new CVDto();
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(file.getOriginalFilename()).thenReturn("resume.pdf");
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)));
        when(candidateProfileClient.getCurrentCandidate()).thenReturn(new CandidateProfile(7L, "Jane", "Doe"));
        when(jobOfferClient.getJobOffer(12L)).thenReturn(new JobOfferSummary(12L, "Java Developer"));
        when(minioService.upload(file, "Jane Doe - Java Developer.pdf")).thenReturn("Jane Doe - Java Developer.pdf");
        when(cvMapper.toDto(any(CV.class))).thenReturn(expected);

        assertThat(service.upload(5L, file, "candidate-keycloak-id")).isSameAs(expected);

        verify(minioService).delete("old.pdf");
        verify(cvRepository).delete(oldCv);
        ArgumentCaptor<CV> created = ArgumentCaptor.forClass(CV.class);
        verify(cvMapper).toDto(created.capture());
        assertThat(created.getValue().getFileUrl()).isEqualTo("Jane Doe - Java Developer.pdf");
        assertThat(created.getValue().getFileName()).isEqualTo("Jane Doe - Java Developer.pdf");
        assertThat(created.getValue().getActive()).isTrue();
        assertThat(application.getCv()).isSameAs(created.getValue());
        assertThat(application.getCurrentStep()).isEqualTo(ApplicationStep.CV_UPLOADED);
        verify(applicationRepository).save(application);
    }

    @Test
    void downloadsStoredCvThroughMinio() throws Exception {
        CV cv = CV.builder().fileUrl("cv.pdf").build();
        when(cvRepository.findById(2L)).thenReturn(Optional.of(cv));
        when(minioService.getFile("cv.pdf")).thenReturn(new ByteArrayInputStream("pdf".getBytes()));

        Resource resource = service.download(2L);

        assertThat(resource.getInputStream().readAllBytes()).isEqualTo("pdf".getBytes());
    }

    @Test
    void reportsMissingApplicationOrCv() {
        when(applicationRepository.findById(9L)).thenReturn(Optional.empty());
        when(cvRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(9L, mock(MultipartFile.class), "candidate-keycloak-id"))
                .isInstanceOf(RuntimeException.class).hasMessage("Application not found");
        assertThatThrownBy(() -> service.findById(10L))
                .isInstanceOf(RuntimeException.class).hasMessage("CV not found");
    }

    @Test
    void rejectsFilesThatAreNotPdfDocuments() {
        Application application = new Application();
        MultipartFile file = mock(MultipartFile.class);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("resume.docx");

        assertThatThrownBy(() -> service.upload(5L, file, "candidate-keycloak-id"))
                .hasMessageContaining("The CV must be a PDF file");
        verifyNoInteractions(minioService, cvRepository);
    }

    @Test
    void refusesCvReplacementAfterTheHrCvFilteringWorkflowStarts() {
        Application application = new Application();
        application.setProcessInstanceId("process-5");
        application.setStatus(ApplicationStatus.HR_INTERVIEW);
        MultipartFile file = mock(MultipartFile.class);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.upload(5L, file, "candidate-keycloak-id"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("CV is locked");

        verifyNoInteractions(minioService, cvRepository, candidateProfileClient, jobOfferClient);
    }
}
