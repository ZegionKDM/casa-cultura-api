package CasaCulturaAPI.service.interfaces;

import CasaCulturaAPI.dto.response.*;

import java.util.List;

public interface StudentPortalService {
    StudentDashboardResponse dashboard(String nombreUsuario);
    AlumnoResponse perfil(String nombreUsuario);
    List<CatalogResponse> horarios(String nombreUsuario);
    List<PagoResponse> pagos(String nombreUsuario);
    List<AsistenciaResponse> asistencias(String nombreUsuario);
}
