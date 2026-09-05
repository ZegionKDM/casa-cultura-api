package CasaCulturaAPI.feature.alumnos.service;

import CasaCulturaAPI.feature.alumnos.dto.request.AlumnoRequest;
import CasaCulturaAPI.feature.alumnos.dto.response.AlumnoCredentialResponse;
import CasaCulturaAPI.feature.alumnos.dto.response.AlumnoResponse;

import java.util.List;

public interface AlumnoService {
    AlumnoCredentialResponse crear(AlumnoRequest request);
    AlumnoCredentialResponse regenerarCredential(Long id);
    List<AlumnoResponse> listar();
    List<AlumnoResponse> listarPorDocente(String nombreUsuario);
    AlumnoResponse obtener(Long id);
    AlumnoResponse actualizar(Long id, AlumnoRequest request);
    void desactivar(Long id);
}
