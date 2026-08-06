package recruitment.dev.applicationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "workflow-service")
public interface WorkflowClient {

    @PostMapping("/workflow/start")
    WorkflowStartResponse startRecruitment(@RequestBody WorkflowStartRequest request);
}
