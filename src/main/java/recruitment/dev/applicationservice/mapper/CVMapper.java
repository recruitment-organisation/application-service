package recruitment.dev.applicationservice.mapper;



import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import recruitment.dev.applicationservice.dto.CVDto;
import recruitment.dev.applicationservice.entities.CV;


@Mapper(componentModel = "spring")
public interface CVMapper {


    @Mapping(source = "id", target = "id")
    @Mapping(source = "fileName", target = "fileName")
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(source = "fileType", target = "fileType")
    @Mapping(source = "active", target = "active")
    @Mapping(source = "uploadedAt", target = "uploadedAt")
    CVDto toDto(CV cv);



    @Mapping(source = "id", target = "id")
    @Mapping(source = "fileName", target = "fileName")
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(source = "fileType", target = "fileType")
    @Mapping(source = "active", target = "active")
    @Mapping(source = "uploadedAt", target = "uploadedAt")
    CV toEntity(CVDto dto);

}