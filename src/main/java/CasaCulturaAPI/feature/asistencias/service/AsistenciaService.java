package CasaCulturaAPI.feature.asistencias.service;

import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaQrRequest;
import CasaCulturaAPI.feature.asistencias.dto.request.AsistenciaManualRequest;
import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;

import java.time.LocalDate;
import java.util.List;

public interface AsistenciaService {
    AsistenciaResponse registrarPorQr(AsistenciaQrRequest request);
    AsistenciaResponse registrarManual(AsistenciaManualRequest request, String nombreUsuario, boolean docente);
    List<AsistenciaResponse> listar(LocalDate fecha, Long alumnoId);
    List<AsistenciaResponse> listarPorDocente(LocalDate fecha, String nombreUsuario);
    int generarFaltas(LocalDate fecha);
}
