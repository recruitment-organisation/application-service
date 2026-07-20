package recruitment.dev.applicationservice.entities;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "cvs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CV {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String fileName;


    private String fileUrl;


    private String fileType;


    private Boolean active;


    private LocalDateTime uploadedAt;




}