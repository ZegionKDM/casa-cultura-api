package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.request.UsuarioRequest;
import CasaCulturaAPI.dto.request.PasswordChangeRequest;
import CasaCulturaAPI.dto.response.UsuarioResponse;

import java.util.List;

public interface UsuarioAccountService {
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse obtenerPorNombre(String nombreUsuario);
    List<UsuarioResponse> listar();
    void desactivar(Long id);
    void cambiarPassword(String nombreUsuario, PasswordChangeRequest request);
}
