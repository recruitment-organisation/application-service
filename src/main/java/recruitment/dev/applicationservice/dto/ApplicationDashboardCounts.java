package recruitment.dev.applicationservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationDashboardCounts {
    private final long total;
    private final long pending;
    private final long hired;
    private final long rejected;
}
