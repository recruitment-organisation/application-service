package recruitment.dev.applicationservice.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CVDto {


    private Long id;

    @NotBlank(message = "Le nom du fichier est obligatoire")

    private String fileName;


    private String fileUrl;

    @NotBlank(message = "Le type de fichier est obligatoire")

    private String fileType;


    private Boolean active;


    private LocalDateTime uploadedAt;

}