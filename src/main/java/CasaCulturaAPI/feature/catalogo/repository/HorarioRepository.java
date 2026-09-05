package CasaCulturaAPI.feature.catalogo.repository;

import CasaCulturaAPI.shared.entity.Grupo;
import CasaCulturaAPI.shared.entity.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    List<Horario> findByGrupoAndDiaAndHoraInicioLessThanEqualAndHoraFinGreaterThan(Grupo grupo, DayOfWeek dia, LocalTime hora, LocalTime inicio);
    List<Horario> findByGrupoId(Long grupoId);
    List<Horario> findByDiaAndHoraFinLessThanEqual(DayOfWeek dia, LocalTime horaFin);
}
