package CasaCulturaAPI.feature.alumnos.repository;

import CasaCulturaAPI.shared.entity.Alumno;
import CasaCulturaAPI.shared.entity.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlumnoRepository extends JpaRepository<Alumno, Long> {
    Optional<Alumno> findByPersonaId(Long personaId);
    Optional<Alumno> findByQrCredentialHashAndEstado(String hash, EstadoRegistro estado);
    boolean existsByMatriculaIgnoreCase(String matricula);
}
