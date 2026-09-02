package CasaCulturaAPI.service.interfaces;


import CasaCulturaAPI.dto.request.RolRequest;
import CasaCulturaAPI.dto.response.RolResponse;

import java.util.List;

public interface RolService {

    RolResponse crear(RolRequest request);

    List<RolResponse> obtenerTodos();

    RolResponse obtenerPorId(Long id);

    RolResponse actualizar(Long id, RolRequest request);

    void eliminar(Long id);

}
