package CasaCulturaAPI.feature.portal.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStatsResponse {
    private int totalGrupos;
    private int totalAlumnos;
    private int clasesHoy;
    private double porcentajeAsistenciaGeneral;
    private int asistenciasRegistradasHoy;
}
