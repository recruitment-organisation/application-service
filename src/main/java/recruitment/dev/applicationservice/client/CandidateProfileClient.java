package recruitment.dev.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "candidate-service", configuration = UserTokenFeignConfig.class)
public interface CandidateProfileClient {

    @GetMapping("/candidate/me")
    CandidateProfile getCurrentCandidate();
}
