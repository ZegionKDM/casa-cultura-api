package CasaCulturaAPI.feature.docentes.service;

import CasaCulturaAPI.feature.docentes.dto.request.DocenteRequest;
import CasaCulturaAPI.feature.docentes.dto.response.DocenteResponse;

import java.util.List;

public interface DocenteService {
    DocenteResponse crear(DocenteRequest request);
    List<DocenteResponse> listar();
    DocenteResponse obtener(Long id);
    DocenteResponse actualizar(Long id, DocenteRequest request);
    void desactivar(Long id);
}
