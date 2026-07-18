package recruitment.dev.applicationservice.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
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
    public ApplicationDto create(ApplicationDto dto) {
        if (applicationRepository.existsByCandidateIdAndJobOfferId(dto.getCandidateId(), dto.getJobOfferId())) {
            throw new DuplicateApplicationException(dto.getCandidateId(), dto.getJobOfferId());
        }

        Application entity = applicationMapper.toEntity(dto);
        entity.setStatus(ApplicationStatus.SUBMITTED);
        entity.setAppliedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Application saved = applicationRepository.save(entity);
        return applicationMapper.toDto(saved);
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
    public void delete(Long id) {
        if (!applicationRepository.existsById(id)) {
            throw new ApplicationNotFoundException(id);
        }
        applicationRepository.deleteById(id);
    }
}