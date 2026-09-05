package CasaCulturaAPI.feature.asistencias.repository;

import CasaCulturaAPI.shared.entity.Asistencia;
import CasaCulturaAPI.shared.entity.Horario;
import CasaCulturaAPI.shared.entity.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    Optional<Asistencia> findByInscripcionAndHorarioAndFecha(Inscripcion inscripcion, Horario horario, LocalDate fecha);
    List<Asistencia> findByFechaOrderByHoraRegistroAsc(LocalDate fecha);
    List<Asistencia> findByInscripcionAlumnoIdOrderByFechaDescHoraRegistroDesc(Long alumnoId);
    List<Asistencia> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
