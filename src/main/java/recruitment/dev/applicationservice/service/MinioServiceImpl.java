package recruitment.dev.applicationservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {


    private final MinioClient minioClient;


    @Value("${minio.bucket}")
    private String bucketName;



    @Override
    public String upload(MultipartFile file, String objectName) {

        try {

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(
                                    file.getContentType()
                            )
                            .build()
            );


            return objectName;


        } catch (Exception e) {

            throw new RuntimeException(
                    "MinIO upload error",
                    e
            );
        }
    }



    @Override
    public void delete(String fileName) {

        try {

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );


        } catch (Exception e) {

            throw new RuntimeException(
                    "MinIO delete error",
                    e
            );
        }
    }



    @Override
    public String getUrl(String fileName) {

        try {

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .method(Method.GET)
                            .expiry(
                                    1,
                                    TimeUnit.HOURS
                            )
                            .build()
            );


        } catch (Exception e) {

            throw new RuntimeException(
                    "MinIO url error",
                    e
            );
        }
    }

    @Override
    public InputStream getFile(String fileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "MinIO get file error",
                    e
            );
        }    }
}
