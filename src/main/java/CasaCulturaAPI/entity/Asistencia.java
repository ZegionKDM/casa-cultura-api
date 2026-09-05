package CasaCulturaAPI.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asistencias", uniqueConstraints = @UniqueConstraint(name = "uk_asistencia_inscripcion_horario_fecha", columnNames = {"inscripcion_id", "horario_id", "fecha"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Asistencia extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id", nullable = false)
    private Horario horario;
    @Column(nullable = false)
    private LocalDate fecha;
    @Column(nullable = false)
    private LocalDateTime horaRegistro;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;
}
