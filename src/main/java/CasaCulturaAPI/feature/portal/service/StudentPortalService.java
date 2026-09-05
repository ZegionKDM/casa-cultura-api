package CasaCulturaAPI.feature.portal.service;

import CasaCulturaAPI.shared.dto.*;
import CasaCulturaAPI.feature.alumnos.dto.response.*;
import CasaCulturaAPI.feature.asistencias.dto.response.*;
import CasaCulturaAPI.feature.auth.dto.response.*;
import CasaCulturaAPI.feature.catalogo.dto.response.*;
import CasaCulturaAPI.feature.docentes.dto.response.*;
import CasaCulturaAPI.feature.inscripciones.dto.response.*;
import CasaCulturaAPI.feature.pagos.dto.response.*;
import CasaCulturaAPI.feature.portal.dto.response.*;
import CasaCulturaAPI.feature.reportes.dto.response.*;

import java.util.List;

public interface StudentPortalService {
    StudentDashboardResponse dashboard(String nombreUsuario);
    AlumnoResponse perfil(String nombreUsuario);
    List<CatalogResponse> horarios(String nombreUsuario);
    List<PagoResponse> pagos(String nombreUsuario);
    List<AsistenciaResponse> asistencias(String nombreUsuario);
}
