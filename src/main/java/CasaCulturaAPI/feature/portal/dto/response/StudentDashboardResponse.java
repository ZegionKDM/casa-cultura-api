package CasaCulturaAPI.feature.portal.dto.response;

import CasaCulturaAPI.feature.alumnos.dto.response.AlumnoResponse;
import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;
import CasaCulturaAPI.feature.catalogo.dto.response.CatalogResponse;
import CasaCulturaAPI.feature.inscripciones.dto.response.InscripcionResponse;
import CasaCulturaAPI.feature.pagos.dto.response.PagoResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentDashboardResponse {
    private AlumnoResponse alumno;
    private List<InscripcionResponse> inscripciones;
    private List<CatalogResponse> horarios;
    private List<PagoResponse> pagos;
    private List<AsistenciaResponse> asistencias;
}
