package recruitment.dev.applicationservice.web;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.service.ApplicationService;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/create")
    public ResponseEntity<ApplicationDto> create(@Valid @RequestBody ApplicationDto dto) {
        ApplicationDto created = applicationService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<ApplicationDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationDto dto) {
        return ResponseEntity.ok(applicationService.update(id, dto));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<ApplicationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }

    @GetMapping("/getall")
    public ResponseEntity<Page<ApplicationDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(applicationService.getAll(pageable));
    }

    @GetMapping("/getbycandidate/{candidateId}")
    public ResponseEntity<Page<ApplicationDto>> getByCandidateId(
            @PathVariable Long candidateId, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getByCandidateId(candidateId, pageable));
    }

    @GetMapping("/getby-job-offer/{jobOfferId}")
    public ResponseEntity<Page<ApplicationDto>> getByJobOfferId(
            @PathVariable Long jobOfferId, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getByJobOfferId(jobOfferId, pageable));
    }

    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<Page<ApplicationDto>> getByStatus(
            @PathVariable ApplicationStatus status, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getByStatus(status, pageable));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}