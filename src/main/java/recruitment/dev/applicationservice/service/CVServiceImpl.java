package recruitment.dev.applicationservice.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStep;
import recruitment.dev.applicationservice.entities.CV;
import recruitment.dev.applicationservice.mapper.CVMapper;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;
import recruitment.dev.applicationservice.repositories.CVRepository;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class CVServiceImpl implements CVService {

    private final CVRepository cvRepository;

    private final ApplicationRepository applicationRepository;

    private final CVMapper cvMapper;

    private final MinioService minioService;

    private final CvTemplateValidationService
            cvTemplateValidationService;
    @Override
    @Transactional
    public CVDto upload(Long applicationId, MultipartFile file) {


        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new RuntimeException("Application not found")
                        );


        if (application.getCv() != null) {

            CV oldCv = application.getCv();


            minioService.delete(oldCv.getFileUrl());


            cvRepository.delete(oldCv);


            application.setCv(null);
        }



        String url = minioService.upload(file);



        CV cv = CV.builder()

                .fileName(file.getOriginalFilename())

                .fileUrl(url)

                .fileType(file.getContentType())

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


}