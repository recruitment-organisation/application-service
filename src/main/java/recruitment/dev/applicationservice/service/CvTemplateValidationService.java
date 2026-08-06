package recruitment.dev.applicationservice.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import recruitment.dev.applicationservice.dto.CvTemplateValidationResult;
import recruitment.dev.applicationservice.entities.Application;
import recruitment.dev.applicationservice.entities.CV;
import recruitment.dev.applicationservice.exception.ApplicationNotFoundException;
import recruitment.dev.applicationservice.repositories.ApplicationRepository;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CvTemplateValidationService {

    private final ApplicationRepository applicationRepository;
    private final MinioService minioService;

    @Transactional(readOnly = true)
    public CvTemplateValidationResult validate(Long applicationId) {

        Application application =
                applicationRepository.findById(applicationId)
                        .orElseThrow(() ->
                                new ApplicationNotFoundException(applicationId)
                        );

        CV cv = application.getCv();

        if (cv == null) {
            return CvTemplateValidationResult.builder()
                    .valid(false)
                    .missingSections(List.of("CV_NOT_FOUND"))
                    .build();
        }

        String text = extractText(cv);

        if (text == null) {
            return CvTemplateValidationResult.builder()
                    .valid(false)
                    .missingSections(List.of("UNREADABLE_PDF"))
                    .build();
        }

        String normalizedText = normalize(text);

        List<String> missingSections = new ArrayList<>();

        if (!containsAny(
                normalizedText,
                "competences",
                "skills",
                "technical skills",
                "technologies",
                "expertise"
        )) {
            missingSections.add("SKILLS");
        }

        if (!containsAny(
                normalizedText,
                "experience",
                "experiences",
                "experience professionnelle",
                "experiences professionnelles",
                "professional experience",
                "work experience",
                "employment history"
        )) {
            missingSections.add("EXPERIENCE");
        }

        if (!containsAny(
                normalizedText,
                "formation",
                "formations",
                "education",
                "academic background",
                "academic history",
                "etudes",
                "diplomes"
        )) {
            missingSections.add("EDUCATION");
        }

        if (!containsAny(
                normalizedText,
                "projet",
                "projets",
                "project",
                "projects",
                "academic projects",
                "personal projects"
        )) {
            missingSections.add("PROJECTS");
        }

        return CvTemplateValidationResult.builder()
                .valid(missingSections.isEmpty())
                .missingSections(missingSections)
                .build();
    }

    private String extractText(CV cv) {

        try (
                InputStream inputStream =
                        minioService.getFile(cv.getFileUrl());

                PDDocument document =
                        Loader.loadPDF(inputStream.readAllBytes())
        ) {

            return new PDFTextStripper().getText(document);

        } catch (Exception exception) {
            return null;
        }
    }

    private boolean containsAny(
            String text,
            String... keywords
    ) {

        for (String keyword : keywords) {

            if (text.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }
}
