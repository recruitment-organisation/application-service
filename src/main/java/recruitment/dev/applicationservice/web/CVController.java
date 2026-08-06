package recruitment.dev.applicationservice.web;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.service.CVService;

@RestController
@RequestMapping("/cv")
@RequiredArgsConstructor
public class CVController {

    private final CVService cvService;
     @PreAuthorize("hasRole('CANDIDATE')")

    @PostMapping("/applications/{applicationId}/cv")
    public ResponseEntity<CVDto> uploadCV(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam MultipartFile file) {

        return ResponseEntity.ok(
                cvService.upload(applicationId, file, jwt.getSubject())
        );
    }


    @PreAuthorize("hasRole('HR')")
    @GetMapping("/get/{id}")

    public ResponseEntity<CVDto> findById(@PathVariable Long id){
        return ResponseEntity.ok(cvService.findById(id));
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadCV(@PathVariable Long id) {
        CVDto cv = cvService.findById(id);
        Resource file = cvService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cv.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(cv.getFileType()))
                .body(file);
    }


}
