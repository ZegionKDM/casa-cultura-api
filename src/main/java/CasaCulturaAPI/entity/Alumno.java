package CasaCulturaAPI.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alumnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alumno extends BaseEntity {
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;
    @Column(nullable = false, unique = true, length = 30)
    private String matricula;
    @Column(nullable = false, unique = true, length = 128)
    private String qrCredentialHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;
}
