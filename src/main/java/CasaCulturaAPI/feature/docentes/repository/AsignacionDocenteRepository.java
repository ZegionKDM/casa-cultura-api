package CasaCulturaAPI.feature.docentes.repository;

import CasaCulturaAPI.shared.entity.AsignacionDocente;
import CasaCulturaAPI.shared.entity.Docente;
import CasaCulturaAPI.shared.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, Long> {
    List<AsignacionDocente> findByGrupoOrderByFechaInicioDesc(Grupo grupo);
    List<AsignacionDocente> findByDocenteOrderByFechaInicioDesc(Docente docente);
}
