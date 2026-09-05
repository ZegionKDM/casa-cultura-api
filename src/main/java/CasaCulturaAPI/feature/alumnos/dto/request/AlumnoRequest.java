package CasaCulturaAPI.feature.alumnos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AlumnoRequest {
    @NotBlank @Size(max = 80)
    private String nombre;
    @NotBlank @Size(max = 80)
    private String apellidoPaterno;
    @Size(max = 80)
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    @Size(max = 20)
    private String telefono;
    @Size(max = 255)
    private String direccion;
    @Email @Size(max = 160)
    private String correo;
    @Size(max = 500)
    private String fotoUrl;
    @NotBlank @Size(max = 30)
    private String matricula;
}
