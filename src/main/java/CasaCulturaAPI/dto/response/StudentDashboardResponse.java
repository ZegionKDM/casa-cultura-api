package CasaCulturaAPI.dto.response;

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
