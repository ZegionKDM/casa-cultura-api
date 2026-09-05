package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.request.LoginRequest;
import CasaCulturaAPI.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
}
