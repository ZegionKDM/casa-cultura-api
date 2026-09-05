package CasaCulturaAPI.feature.auth.service;

import CasaCulturaAPI.feature.auth.dto.request.UsuarioRequest;
import CasaCulturaAPI.feature.auth.dto.request.PasswordChangeRequest;
import CasaCulturaAPI.feature.auth.dto.response.UsuarioResponse;

import java.util.List;

public interface UsuarioAccountService {
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse obtenerPorNombre(String nombreUsuario);
    List<UsuarioResponse> listar();
    void desactivar(Long id);
    void cambiarPassword(String nombreUsuario, PasswordChangeRequest request);
}
