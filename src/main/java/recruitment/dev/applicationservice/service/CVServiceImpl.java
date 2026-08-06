package recruitment.dev.applicationservice.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;
import recruitment.dev.applicationservice.client.CandidateProfile;
import recruitment.dev.applicationservice.client.CandidateProfileClient;
import recruitment.dev.applicationservice.client.JobOfferClient;
import recruitment.dev.applicationservice.client.JobOfferSummary;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.entities.ApplicationStep;
import recruitment.dev.applicationservice.entities.CV;
import recruitment.dev.applicationservice.mapper.CVMapper;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;
import recruitment.dev.applicationservice.repositories.CVRepository;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class CVServiceImpl implements CVService {

    private static final long MAX_CV_SIZE_BYTES = 10L * 1024 * 1024;

    private final CVRepository cvRepository;

    private final ApplicationRepository applicationRepository;

    private final CVMapper cvMapper;

    private final MinioService minioService;

    private final CandidateProfileClient candidateProfileClient;

    private final JobOfferClient jobOfferClient;

    private final CvTemplateValidationService
            cvTemplateValidationService;
    @Override
    @Transactional
    public CVDto upload(Long applicationId, MultipartFile file, String candidateKeycloakId) {
        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new RuntimeException("Application not found")
                        );

        if (!canReplaceCv(application)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The CV is locked while or after the HR CV filtering workflow"
            );
        }

        validateCvFile(file);

        if (!candidateKeycloakId.equals(application.getCandidateKeycloakId())) {
            throw new AccessDeniedException("This application does not belong to the authenticated candidate");
        }

        CandidateProfile candidate = candidateProfileClient.getCurrentCandidate();
        if (candidate == null || !application.getCandidateId().equals(candidate.id())) {
            throw new AccessDeniedException("The authenticated candidate does not match this application");
        }
        JobOfferSummary offer = jobOfferClient.getJobOffer(application.getJobOfferId());
        if (offer == null || offer.title() == null || offer.title().isBlank()) {
            throw new IllegalStateException("Unable to resolve the job offer title");
        }
        String fileName = buildFileName(candidate, offer, file.getOriginalFilename());


        if (application.getCv() != null) {

            CV oldCv = application.getCv();


            minioService.delete(oldCv.getFileUrl());


            cvRepository.delete(oldCv);


            application.setCv(null);
        }



        String url = minioService.upload(file, fileName);



        CV cv = CV.builder()

                .fileName(fileName)

                .fileUrl(url)

                .fileType(MediaType.APPLICATION_PDF_VALUE)

                .active(true)

                .uploadedAt(LocalDateTime.now())


                .build();



        application.setCv(cv);
        application.setCurrentStep(
                ApplicationStep.CV_UPLOADED
        );

        application.setUpdatedAt(
                LocalDateTime.now()
        );


        applicationRepository.save(application);



        return cvMapper.toDto(cv);
    }



    @Override
    public CVDto findById(Long id) {

        return cvRepository.findById(id)
                .map(cvMapper::toDto)
                .orElseThrow(
                        () -> new RuntimeException("CV not found")
                );
    }



    @Override
    public Resource download(Long id) {

        CV cv = cvRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("CV not found")
                );


        InputStream stream =
                minioService.getFile(cv.getFileUrl());


        return new InputStreamResource(stream);
    }

    private String buildFileName(CandidateProfile candidate, JobOfferSummary offer, String originalFileName) {
        String extension = "";
        if (originalFileName != null) {
            int extensionIndex = originalFileName.lastIndexOf('.');
            if (extensionIndex >= 0 && extensionIndex < originalFileName.length() - 1) {
                extension = originalFileName.substring(extensionIndex);
            }
        }
        return cleanFileSegment(candidate.firstName() + " " + candidate.lastName())
                + " - "
                + cleanFileSegment(offer.title())
                + extension;
    }

    private boolean canReplaceCv(Application application) {
        return application.getProcessInstanceId() == null
                || application.getStatus() == ApplicationStatus.CV_REVISION_REQUIRED
                || "sid-3140788F-868D-4F20-88A1-D66AF0BA345A".equals(application.getCurrentTaskDefinitionKey());
    }

    private String cleanFileSegment(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void validateCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A CV PDF file is required");
        }
        if (file.getSize() > MAX_CV_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CV must not exceed 10 MB");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The CV must be a PDF file");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String signature = new String(inputStream.readNBytes(5), StandardCharsets.US_ASCII);
            if (!"%PDF-".equals(signature)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a valid PDF");
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read the uploaded CV", exception);
        }
    }


}
