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
import recruitment.dev.applicationservice.client.JobOfferClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JobOfferClient jobOfferClient;
    @PreAuthorize("hasRole('CANDIDATE')")
    @PostMapping("/create")
    public ResponseEntity<ApplicationDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateApplicationRequest dto) {
        Long companyId = jobOfferClient.getJobOffer(dto.getJobOfferId()).companyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is not associated with a company");
        ApplicationDto created = applicationService.create(dto, jwt.getSubject(), companyId);
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
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                applicationService.submit(id, jwt.getSubject(), jwt.getTokenValue())
        );
    }
    @PreAuthorize("hasRole('CANDIDATE')")

    @PatchMapping("/update/{id}")
    public ResponseEntity<ApplicationDto> update(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplicationDto dto) {
        return ResponseEntity.ok(applicationService.updateForCandidate(id, dto, jwt.getSubject()));
    }

    @PreAuthorize("hasRole('HR')")
    @PatchMapping("/{id}/hr-interview-scheduled")
    public ResponseEntity<ApplicationDto> markHrInterviewScheduled(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        ensureTenant(jwt, applicationService.getById(id));
        return ResponseEntity.ok(applicationService.markHrInterviewScheduled(id));
    }
    @GetMapping("/get/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApplicationDto> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        ApplicationDto application = applicationService.getById(id);
        ensureTenant(jwt, application);
        return ResponseEntity.ok(application);
    }
    @GetMapping("/getall")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Page<ApplicationDto>> getAll(Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(isAdmin(jwt) ? applicationService.getAll(pageable) : applicationService.getAll(requiredCompany(jwt), pageable));
    }
    @GetMapping("/getbycandidate/{candidateId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Page<ApplicationDto>> getByCandidateId(
            @PathVariable Long candidateId, Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(isAdmin(jwt) ? applicationService.getByCandidateId(candidateId, pageable) : applicationService.getByCandidateId(requiredCompany(jwt), candidateId, pageable));
    }
    @GetMapping("/getby-job-offer/{jobOfferId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Page<ApplicationDto>> getByJobOfferId(
            @PathVariable Long jobOfferId, Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(isAdmin(jwt) ? applicationService.getByJobOfferId(jobOfferId, pageable) : applicationService.getByJobOfferId(requiredCompany(jwt), jobOfferId, pageable));
    }
    @GetMapping("/get-by-status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<Page<ApplicationDto>> getByStatus(
            @PathVariable ApplicationStatus status, Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(isAdmin(jwt) ? applicationService.getByStatus(status, pageable) : applicationService.getByStatus(requiredCompany(jwt), status, pageable));
    }

    @GetMapping("/dashboard-counts")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    public ResponseEntity<ApplicationDashboardCounts> getDashboardCounts(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(isAdmin(jwt) ? applicationService.getDashboardCounts() : applicationService.getDashboardCounts(requiredCompany(jwt)));
    }
    @PreAuthorize("hasRole('HR')")

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        ensureTenant(jwt, applicationService.getById(id));
        applicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long requiredCompany(Jwt jwt) {
        Object claim = jwt == null ? null : jwt.getClaim("companyId");
        if (claim instanceof Number n) return n.longValue();
        if (claim instanceof String value) try { return Long.valueOf(value); } catch (NumberFormatException ignored) { }
        throw new AccessDeniedException("No company is associated with this account");
    }
    private void ensureTenant(Jwt jwt, ApplicationDto application) {
        if (!isAdmin(jwt) && (application.getCompanyId() == null || !application.getCompanyId().equals(requiredCompany(jwt)))) throw new AccessDeniedException("Cross-company access denied");
    }
    @SuppressWarnings("unchecked")
    private boolean isAdmin(Jwt jwt) {
        if (jwt == null || jwt.getClaim("realm_access") == null) return false;
        Object roles = ((java.util.Map<String, Object>) jwt.getClaim("realm_access")).get("roles");
        return roles instanceof java.util.Collection<?> values && values.contains("ADMIN");
    }
}
