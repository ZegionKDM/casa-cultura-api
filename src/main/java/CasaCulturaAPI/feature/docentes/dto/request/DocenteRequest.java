package CasaCulturaAPI.feature.docentes.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DocenteRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no debe superar 80 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 80, message = "El apellido paterno no debe superar 80 caracteres")
    private String apellidoPaterno;

    @Size(max = 80, message = "El apellido materno no debe superar 80 caracteres")
    private String apellidoMaterno;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener exactamente 10 dígitos numéricos")
    private String telefono;

    @NotBlank(message = "El domicilio / dirección es obligatorio")
    @Size(min = 5, max = 255, message = "El domicilio debe contener al menos 5 caracteres (máximo 255)")
    private String direccion;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico debe ser válido")
    @Size(max = 160, message = "El correo no debe superar 160 caracteres")
    private String correo;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 120, message = "La especialidad no debe superar 120 caracteres")
    private String especialidad;

    @Size(max = 500, message = "La URL de la foto no debe superar 500 caracteres")
    private String fotoUrl;
}
