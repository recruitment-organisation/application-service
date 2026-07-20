package recruitment.dev.applicationservice.mapper;

import org.mapstruct.*;
import recruitment.dev.applicationservice.dto.ApplicationDto;
import recruitment.dev.applicationservice.entities.Application;

@Mapper(componentModel = "spring",
        uses = CVMapper.class
)
public interface ApplicationMapper {

    @Mapping(
            target = "cvId",
            source = "cv.id"
    )
    ApplicationDto toDto(Application entity);



    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cv", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "matchingScore", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Application toEntity(ApplicationDto dto);



    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cv", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "matchingScore", ignore = true)
    @Mapping(target = "appliedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(
            ApplicationDto dto,
            @MappingTarget Application entity
    );
}