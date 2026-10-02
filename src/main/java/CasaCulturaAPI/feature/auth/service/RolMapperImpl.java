package CasaCulturaAPI.feature.auth.service;

import CasaCulturaAPI.feature.auth.dto.request.RolRequest;
import CasaCulturaAPI.feature.auth.dto.response.RolResponse;
import CasaCulturaAPI.shared.entity.Rol;
import org.springframework.stereotype.Component;

@Component
public class RolMapperImpl implements RolMapper {

    @Override
    public Rol toEntity(RolRequest request) {
        if (request == null) {
            return null;
        }

        return Rol.builder()
                .nombre(request.getNombre() != null ? request.getNombre().trim() : null)
                .descripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null)
                .build();
    }

    @Override
    public RolResponse toResponse(Rol rol) {
        if (rol == null) {
            return null;
        }

        return RolResponse.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .build();
    }
}
