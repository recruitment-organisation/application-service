package recruitment.dev.applicationservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;


public interface MinioService {


    String upload(MultipartFile file);


    void delete(String fileName);


    String getUrl(String fileName);
    InputStream getFile(String fileName);


}