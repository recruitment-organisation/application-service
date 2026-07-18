package recruitment.dev.applicationservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.entities.ApplicationStatus;

public interface ApplicationService {
    ApplicationDto create(ApplicationDto dto);
    ApplicationDto update(Long id, ApplicationDto dto);
    ApplicationDto getById(Long id);
    Page<ApplicationDto> getAll(Pageable pageable);
    Page<ApplicationDto> getByCandidateId(Long candidateId, Pageable pageable);
    Page<ApplicationDto> getByJobOfferId(Long jobOfferId, Pageable pageable);
    Page<ApplicationDto> getByStatus(ApplicationStatus status, Pageable pageable);
    void delete(Long id);
}
