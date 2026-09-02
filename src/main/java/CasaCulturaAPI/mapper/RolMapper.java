package CasaCulturaAPI.mapper;


import CasaCulturaAPI.dto.request.RolRequest;
import CasaCulturaAPI.dto.response.RolResponse;
import CasaCulturaAPI.entity.Rol;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RolMapper {

    Rol toEntity(RolRequest request);

    RolResponse toResponse(Rol rol);

}
