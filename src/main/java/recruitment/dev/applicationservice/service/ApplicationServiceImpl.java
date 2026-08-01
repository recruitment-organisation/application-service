package recruitment.dev.applicationservice.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;

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
    public ApplicationDto submit(Long id) {


        Application application =
                applicationRepository.findById(id)
                        .orElseThrow(() ->
                                new ApplicationNotFoundException(id)
                        );


        if(application.getCv() == null){

            throw new RuntimeException(
                    "CV must be uploaded before submit"
            );
        }


        application.setCurrentStep(
                ApplicationStep.COMPLETED
        );


        application.setStatus(
                ApplicationStatus.SUBMITTED
        );


        application.setUpdatedAt(
                LocalDateTime.now()
        );


        return applicationMapper.toDto(
                applicationRepository.save(application)
        );
    }

    @Override
    public ApplicationDto update(Long id, ApplicationDto dto) {
        Application entity = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        applicationMapper.updateEntityFromDto(dto, entity);
        entity.setUpdatedAt(LocalDateTime.now());

        Application updated = applicationRepository.save(entity);
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
        if (!applicationRepository.existsById(id)) {
            throw new ApplicationNotFoundException(id);
        }
        applicationRepository.deleteById(id);
    }

    @Override
    public ApplicationDto updateMatchingScore(Long id, Double matchingScore) {
        Application entity = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        entity.setMatchingScore(matchingScore);
        entity.setUpdatedAt(LocalDateTime.now());

        Application updated = applicationRepository.save(entity);
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

        applicationRepository.save(app);

        applicationMapper.toDto(app);
        return  applicationMapper.toDto(app);
    }
}
