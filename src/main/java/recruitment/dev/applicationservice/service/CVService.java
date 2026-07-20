package recruitment.dev.applicationservice.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import recruitment.dev.applicationservice.dto.CVDto;



public interface CVService {


    CVDto upload(Long candidateId, MultipartFile file);




    CVDto findById(Long id);




    Resource download(Long id);

}