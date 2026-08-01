package recruitment.dev.applicationservice.web;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.dto.ApplicationDashboardCounts;
import recruitment.dev.applicationservice.dto.CreateApplicationRequest;
import recruitment.dev.applicationservice.entities.ApplicationStatus;
import recruitment.dev.applicationservice.service.ApplicationService;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    @PreAuthorize("hasRole('CANDIDATE')")
    @PostMapping("/create")
    public ResponseEntity<ApplicationDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateApplicationRequest dto) {
        ApplicationDto created = applicationService.create(dto, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    @GetMapping("/mine")
    public ResponseEntity<Page<ApplicationDto>> getMine(@AuthenticationPrincipal Jwt jwt, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getMine(jwt.getSubject(), pageable));
    }
    @PreAuthorize("hasRole('CANDIDATE')")


    @PostMapping("/{id}/submit")
    public ResponseEntity<ApplicationDto> submit(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                applicationService.submit(id)
        );
    }
    @PreAuthorize("hasRole('CANDIDATE')")

    @PatchMapping("/update/{id}")
    public ResponseEntity<ApplicationDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationDto dto) {
        return ResponseEntity.ok(applicationService.update(id, dto));
    }
    @PreAuthorize("hasRole('HR')")

    @GetMapping("/get/{id}")
    public ResponseEntity<ApplicationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }
    @PreAuthorize("hasRole('HR')")


    @GetMapping("/getall")
    public ResponseEntity<Page<ApplicationDto>> getAll(Pageable pageable) {
        return ResponseEntity.ok(applicationService.getAll(pageable));
    }
    @PreAuthorize("hasRole('HR')")

    @GetMapping("/getbycandidate/{candidateId}")
    public ResponseEntity<Page<ApplicationDto>> getByCandidateId(
            @PathVariable Long candidateId, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getByCandidateId(candidateId, pageable));
    }
    @PreAuthorize("hasRole('HR')")

    @GetMapping("/getby-job-offer/{jobOfferId}")
    public ResponseEntity<Page<ApplicationDto>> getByJobOfferId(
            @PathVariable Long jobOfferId, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getByJobOfferId(jobOfferId, pageable));
    }
    @PreAuthorize("hasRole('HR')")

    @GetMapping("/get-by-status/{status}")
    public ResponseEntity<Page<ApplicationDto>> getByStatus(
            @PathVariable ApplicationStatus status, Pageable pageable) {
        return ResponseEntity.ok(applicationService.getByStatus(status, pageable));
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/dashboard-counts")
    public ResponseEntity<ApplicationDashboardCounts> getDashboardCounts() {
        return ResponseEntity.ok(applicationService.getDashboardCounts());
    }
    @PreAuthorize("hasRole('HR')")

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
