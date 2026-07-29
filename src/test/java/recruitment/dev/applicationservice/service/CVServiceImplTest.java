package recruitment.dev.applicationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStep;
import recruitment.dev.applicationservice.entities.CV;
import recruitment.dev.applicationservice.mapper.CVMapper;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;
import recruitment.dev.applicationservice.repositories.CVRepository;

import java.io.ByteArrayInputStream;
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
    @Mock private CvTemplateValidationService cvTemplateValidationService;
    @InjectMocks private CVServiceImpl service;

    @Test
    void replacesOldCvAndMarksApplicationAsUploaded() {
        Application application = new Application();
        CV oldCv = CV.builder().fileUrl("old.pdf").build();
        application.setCv(oldCv);
        MultipartFile file = mock(MultipartFile.class);
        CVDto expected = new CVDto();
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(application));
        when(file.getOriginalFilename()).thenReturn("resume.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(minioService.upload(file)).thenReturn("new.pdf");
        when(cvMapper.toDto(any(CV.class))).thenReturn(expected);

        assertThat(service.upload(5L, file)).isSameAs(expected);

        verify(minioService).delete("old.pdf");
        verify(cvRepository).delete(oldCv);
        ArgumentCaptor<CV> created = ArgumentCaptor.forClass(CV.class);
        verify(cvMapper).toDto(created.capture());
        assertThat(created.getValue().getFileUrl()).isEqualTo("new.pdf");
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

        assertThatThrownBy(() -> service.upload(9L, mock(MultipartFile.class)))
                .isInstanceOf(RuntimeException.class).hasMessage("Application not found");
        assertThatThrownBy(() -> service.findById(10L))
                .isInstanceOf(RuntimeException.class).hasMessage("CV not found");
    }
}
