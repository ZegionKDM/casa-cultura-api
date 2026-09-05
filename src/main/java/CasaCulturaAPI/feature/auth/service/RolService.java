package CasaCulturaAPI.feature.auth.service;


import CasaCulturaAPI.feature.auth.dto.request.RolRequest;
import CasaCulturaAPI.feature.auth.dto.response.RolResponse;

import java.util.List;

public interface RolService {

    RolResponse crear(RolRequest request);

    List<RolResponse> obtenerTodos();

    RolResponse obtenerPorId(Long id);

    RolResponse actualizar(Long id, RolRequest request);

    void eliminar(Long id);

}
