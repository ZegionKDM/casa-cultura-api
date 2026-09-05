package CasaCulturaAPI.shared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Persona extends BaseEntity {
    @Column(nullable = false, length = 80)
    private String nombre;
    @Column(nullable = false, length = 80)
    private String apellidoPaterno;
    @Column(length = 80)
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    @Column(length = 20)
    private String telefono;
    @Column(length = 255)
    private String direccion;
    @Column(length = 160)
    private String correo;
    @Column(length = 500)
    private String fotoUrl;
}
