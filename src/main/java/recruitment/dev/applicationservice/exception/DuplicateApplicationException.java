package recruitment.dev.applicationservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException(Long candidateId, Long jobOfferId) {
        super("Candidate " + candidateId + " has already applied to job offer " + jobOfferId);
    }
}
