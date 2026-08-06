package recruitment.dev.applicationservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.dto.UpdateMatchingScoreRequest;
import recruitment.dev.applicationservice.dto.UpdateWorkflowStateRequest;
import recruitment.dev.applicationservice.service.ApplicationService;
import recruitment.dev.applicationservice.service.CVService;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalApplicationController {

    private final ApplicationService applicationService;
    private final CVService cvService;

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApplicationDto> getApplicationById(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(
                applicationService.getById(
                        applicationId
                )
        );
    }

    @GetMapping("/cv/{cvId}")
    public ResponseEntity<CVDto> getCvById(
            @PathVariable Long cvId
    ) {
        return ResponseEntity.ok(
                cvService.findById(cvId)
        );
    }

    @GetMapping("/cv/{cvId}/download")
    public ResponseEntity<Resource> downloadCv(
            @PathVariable Long cvId
    ) {
        CVDto cv = cvService.findById(cvId);
        Resource resource = cvService.download(cvId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                cv.getFileName() + "\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @PutMapping("/applications/{applicationId}/matching-score")
    public ResponseEntity<ApplicationDto> updateMatchingScore(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateMatchingScoreRequest request
    ) {
        return ResponseEntity.ok(
                applicationService.updateMatchingScore(
                        applicationId,
                        request.getMatchingScore()
                )
        );
    }

    @PutMapping("/applications/{applicationId}/workflow-state")
    public ResponseEntity<ApplicationDto> updateWorkflowState(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateWorkflowStateRequest request
    ) {
        return ResponseEntity.ok(applicationService.updateWorkflowState(
                applicationId,
                request.getProcessInstanceId(),
                request.getCurrentTaskId(),
                request.getCurrentTaskDefinitionKey(),
                request.getCurrentTaskName()
        ));
    }


    @PutMapping("/update-status/{id}")
    public ResponseEntity<ApplicationDto> updateStatus(
            @PathVariable Long id,
            @RequestBody String status) {

        ApplicationDto application =
                applicationService.updateStatus(id, status);

        return ResponseEntity.ok(application);}
    }
