package recruitment.dev.applicationservice.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidCvTemplateException extends RuntimeException {

    private final List<String> missingSections;

    public InvalidCvTemplateException(
            List<String> missingSections
    ) {
        super(
                "Le CV ne respecte pas le template demandé. "
                        + "Sections absentes : "
                        + String.join(", ", missingSections)
        );

        this.missingSections = missingSections;
    }
}