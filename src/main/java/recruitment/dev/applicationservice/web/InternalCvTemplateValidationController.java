package recruitment.dev.applicationservice.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import recruitment.dev.applicationservice.dto.CvTemplateValidationResult;
import recruitment.dev.applicationservice.service.CvTemplateValidationService;

@RestController
@RequestMapping("/validation")
@RequiredArgsConstructor
public class InternalCvTemplateValidationController {

    private final CvTemplateValidationService
            cvTemplateValidationService;

    @GetMapping("/cv/{applicationId}")
    public ResponseEntity<CvTemplateValidationResult> validateTemplate(
            @PathVariable Long applicationId
    ) {

        return ResponseEntity.ok(
                cvTemplateValidationService.validate(applicationId)
        );
    }
}