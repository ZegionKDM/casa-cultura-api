package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.request.AsignacionDocenteRequest;
import CasaCulturaAPI.dto.response.AsignacionDocenteResponse;

import java.util.List;

public interface AsignacionDocenteService {
    AsignacionDocenteResponse asignar(AsignacionDocenteRequest request);
    List<AsignacionDocenteResponse> listarPorGrupo(Long grupoId);
    List<AsignacionDocenteResponse> listarPorUsuario(String nombreUsuario);
}
