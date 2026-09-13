package CasaCulturaAPI.feature.alumnos.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AlumnoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no debe superar 80 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 80, message = "El apellido paterno no debe superar 80 caracteres")
    private String apellidoPaterno;

    @Size(max = 80, message = "El apellido materno no debe superar 80 caracteres")
    private String apellidoMaterno;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener exactamente 10 dígitos numéricos")
    private String telefono;

    @NotBlank(message = "El domicilio / dirección es obligatorio")
    @Size(min = 5, max = 255, message = "El domicilio debe tener al menos 5 caracteres (máximo 255)")
    private String direccion;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 160, message = "El correo no debe superar 160 caracteres")
    private String correo;

    @Size(max = 500, message = "La URL de la foto no debe superar 500 caracteres")
    private String fotoUrl;

    @NotBlank(message = "La matrícula es obligatoria")
    @Size(min = 3, max = 30, message = "La matrícula debe tener entre 3 y 30 caracteres")
    private String matricula;
}
