package CasaCulturaAPI.feature.portal.dto.response;

import CasaCulturaAPI.feature.asistencias.dto.response.AsistenciaResponse;
import CasaCulturaAPI.feature.catalogo.dto.response.CatalogResponse;
import CasaCulturaAPI.feature.docentes.dto.response.DocenteResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardResponse {
    private DocenteResponse docente;
    private TeacherStatsResponse estadisticas;
    private List<TeacherGroupResponse> grupos;
    private List<CatalogResponse> clasesHoy;
    private List<CatalogResponse> horariosSemana;
    private List<AsistenciaResponse> asistenciasRecientes;
}
