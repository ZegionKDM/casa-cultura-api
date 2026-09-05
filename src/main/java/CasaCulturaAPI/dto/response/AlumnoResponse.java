package CasaCulturaAPI.dto.response;

import CasaCulturaAPI.entity.EstadoRegistro;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AlumnoResponse {
    private Long id;
    private Long personaId;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String direccion;
    private String correo;
    private String fotoUrl;
    private String matricula;
    private EstadoRegistro estado;
}
