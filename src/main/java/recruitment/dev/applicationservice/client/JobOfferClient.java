package recruitment.dev.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "job-offer-service", configuration = UserTokenFeignConfig.class)
public interface JobOfferClient {

    @GetMapping("/job-offers/get/{id}")
    JobOfferSummary getJobOffer(@PathVariable("id") Long id);
}
