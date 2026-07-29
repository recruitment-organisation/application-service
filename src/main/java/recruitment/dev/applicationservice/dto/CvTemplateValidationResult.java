package recruitment.dev.applicationservice.dto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CvTemplateValidationResult {

    private boolean valid;


    private List<String> missingSections;
}