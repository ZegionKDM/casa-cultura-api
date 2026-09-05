package CasaCulturaAPI.dto.response;

import CasaCulturaAPI.entity.EstadoRegistro;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DocenteResponse {
    private Long id;
    private Long personaId;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String telefono;
    private String direccion;
    private String correo;
    private String especialidad;
    private EstadoRegistro estado;
}
