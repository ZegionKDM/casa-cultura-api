package CasaCulturaAPI.feature.docentes.service;

import CasaCulturaAPI.feature.docentes.dto.request.AsignacionDocenteRequest;
import CasaCulturaAPI.feature.docentes.dto.response.AsignacionDocenteResponse;

import java.util.List;

public interface AsignacionDocenteService {
    AsignacionDocenteResponse asignar(AsignacionDocenteRequest request);
    List<AsignacionDocenteResponse> listarPorGrupo(Long grupoId);
    List<AsignacionDocenteResponse> listarPorUsuario(String nombreUsuario);
}
