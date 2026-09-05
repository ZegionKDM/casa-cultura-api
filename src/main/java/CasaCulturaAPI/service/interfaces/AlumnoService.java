package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.request.AlumnoRequest;
import CasaCulturaAPI.dto.response.AlumnoCredentialResponse;
import CasaCulturaAPI.dto.response.AlumnoResponse;

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
