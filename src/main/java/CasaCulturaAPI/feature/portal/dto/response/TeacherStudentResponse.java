package CasaCulturaAPI.feature.portal.dto.response;

import CasaCulturaAPI.shared.entity.EstadoInscripcion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStudentResponse {
    private Long inscripcionId;
    private Long alumnoId;
    private String matricula;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private String correo;
    private String telefono;
    private String fotoUrl;
    private Long grupoId;
    private String nombreGrupo;
    private LocalDate fechaInscripcion;
    private EstadoInscripcion estadoInscripcion;
    private int totalAsistencias;
    private int totalRetardos;
    private int totalFaltas;
    private double porcentajeAsistencia;
}
