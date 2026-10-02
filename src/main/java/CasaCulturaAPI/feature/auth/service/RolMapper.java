package CasaCulturaAPI.feature.auth.service;

import CasaCulturaAPI.feature.auth.dto.request.RolRequest;
import CasaCulturaAPI.feature.auth.dto.response.RolResponse;
import CasaCulturaAPI.shared.entity.Rol;

public interface RolMapper {

    Rol toEntity(RolRequest request);

    RolResponse toResponse(Rol rol);

}

