package CasaCulturaAPI.feature.auth.service;

import CasaCulturaAPI.feature.auth.dto.request.LoginRequest;
import CasaCulturaAPI.feature.auth.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
}
