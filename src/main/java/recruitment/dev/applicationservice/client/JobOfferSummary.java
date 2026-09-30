package recruitment.dev.applicationservice.client;

public record JobOfferSummary(
        Long id,
        String title,
        Long companyId
) {
    public JobOfferSummary(Long id, String title) { this(id, title, null); }
}
