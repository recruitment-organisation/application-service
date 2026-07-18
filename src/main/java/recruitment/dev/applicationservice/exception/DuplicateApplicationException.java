package recruitment.dev.applicationservice.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(Long candidateId, Long jobOfferId) {
        super("Candidate " + candidateId + " has already applied to job offer " + jobOfferId);
    }
}