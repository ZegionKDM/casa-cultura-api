package CasaCulturaAPI.feature.inscripciones.repository;

import CasaCulturaAPI.shared.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByAlumnoAndEstado(Alumno alumno, EstadoInscripcion estado);
    List<Inscripcion> findByGrupoAndEstado(Grupo grupo, EstadoInscripcion estado);
    List<Inscripcion> findByGrupoIdInAndEstado(List<Long> grupoIds, EstadoInscripcion estado);
}
