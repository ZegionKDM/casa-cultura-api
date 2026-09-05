package CasaCulturaAPI.repository;

import CasaCulturaAPI.entity.AsignacionDocente;
import CasaCulturaAPI.entity.Docente;
import CasaCulturaAPI.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignacionDocenteRepository extends JpaRepository<AsignacionDocente, Long> {
    List<AsignacionDocente> findByGrupoOrderByFechaInicioDesc(Grupo grupo);
    List<AsignacionDocente> findByDocenteOrderByFechaInicioDesc(Docente docente);
}
