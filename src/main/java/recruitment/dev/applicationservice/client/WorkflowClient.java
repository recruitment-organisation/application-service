package recruitment.dev.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "workflow-service")
public interface WorkflowClient {

    @PostMapping("/workflow/start")
    WorkflowStartResponse startRecruitment(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody WorkflowStartRequest request
    );
}
