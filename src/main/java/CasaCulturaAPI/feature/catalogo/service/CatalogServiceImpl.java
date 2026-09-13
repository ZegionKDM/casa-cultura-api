package CasaCulturaAPI.feature.catalogo.service;

import CasaCulturaAPI.feature.alumnos.dto.request.*;
import CasaCulturaAPI.feature.asistencias.dto.request.*;
import CasaCulturaAPI.feature.auth.dto.request.*;
import CasaCulturaAPI.feature.catalogo.dto.request.*;
import CasaCulturaAPI.feature.docentes.dto.request.*;
import CasaCulturaAPI.feature.inscripciones.dto.request.*;
import CasaCulturaAPI.feature.pagos.dto.request.*;
import CasaCulturaAPI.feature.catalogo.dto.response.CatalogResponse;
import CasaCulturaAPI.shared.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.shared.repository.*;
import CasaCulturaAPI.feature.alumnos.repository.*;
import CasaCulturaAPI.feature.asistencias.repository.*;
import CasaCulturaAPI.feature.auth.repository.*;
import CasaCulturaAPI.feature.catalogo.repository.*;
import CasaCulturaAPI.feature.docentes.repository.*;
import CasaCulturaAPI.feature.inscripciones.repository.*;
import CasaCulturaAPI.feature.pagos.repository.*;
import CasaCulturaAPI.feature.alumnos.repository.*;
import CasaCulturaAPI.feature.asistencias.repository.*;
import CasaCulturaAPI.feature.auth.repository.*;
import CasaCulturaAPI.feature.catalogo.repository.*;
import CasaCulturaAPI.feature.docentes.repository.*;
import CasaCulturaAPI.feature.inscripciones.repository.*;
import CasaCulturaAPI.feature.pagos.repository.*;
import CasaCulturaAPI.feature.catalogo.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {
    private final CursoRepository cursoRepository;
    private final CategoriaEdadRepository categoriaRepository;
    private final OfertaCursoRepository ofertaRepository;
    private final GrupoRepository grupoRepository;
    private final HorarioRepository horarioRepository;

    @Override @Transactional
    public CatalogResponse crearCurso(CursoRequest request) {
        if (cursoRepository.existsByNombreIgnoreCase(request.getNombre())) throw new IllegalArgumentException("El curso ya existe.");
        return curso(cursoRepository.save(Curso.builder().nombre(request.getNombre()).build()));
    }
    @Override @Transactional
    public CatalogResponse actualizarCurso(Long id, CursoRequest request) {
        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado."));
        if (!curso.getNombre().equalsIgnoreCase(request.getNombre())
                && cursoRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalArgumentException("El curso ya existe.");
        }
        curso.setNombre(request.getNombre());
        return curso(cursoRepository.save(curso));
    }
    @Override @Transactional(readOnly = true)
    public List<CatalogResponse> listarCursos() { return cursoRepository.findAll().stream().map(this::curso).toList(); }
    @Override @Transactional
    public CatalogResponse crearCategoria(CategoriaEdadRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCase(request.getNombre())) throw new IllegalArgumentException("La categoría ya existe.");
        return categoria(categoriaRepository.save(CategoriaEdad.builder().nombre(request.getNombre()).build()));
    }
    @Override @Transactional
    public CatalogResponse actualizarCategoria(Long id, CategoriaEdadRequest request) {
        CategoriaEdad categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada."));
        if (!categoria.getNombre().equalsIgnoreCase(request.getNombre())
                && categoriaRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new IllegalArgumentException("La categoría ya existe.");
        }
        categoria.setNombre(request.getNombre());
        return categoria(categoriaRepository.save(categoria));
    }
    @Override @Transactional(readOnly = true)
    public List<CatalogResponse> listarCategorias() { return categoriaRepository.findAll().stream().map(this::categoria).toList(); }
    @Override @Transactional
    public CatalogResponse crearOferta(OfertaCursoRequest request) {
        if (!request.getFechaInicio().isBefore(request.getFechaFin())) throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        Curso curso = cursoRepository.findById(request.getCursoId()).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado."));
        return oferta(ofertaRepository.save(OfertaCurso.builder().curso(curso).tipo(request.getTipo())
                .fechaInicio(request.getFechaInicio()).fechaFin(request.getFechaFin()).build()));
    }
    @Override @Transactional
    public CatalogResponse actualizarOferta(Long id, OfertaCursoRequest request) {
        validateOfferDates(request);
        OfertaCurso oferta = ofertaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada."));
        Curso curso = cursoRepository.findById(request.getCursoId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado."));
        oferta.setCurso(curso);
        oferta.setTipo(request.getTipo());
        oferta.setFechaInicio(request.getFechaInicio());
        oferta.setFechaFin(request.getFechaFin());
        return oferta(ofertaRepository.save(oferta));
    }
    @Override @Transactional(readOnly = true)
    public List<CatalogResponse> listarOfertas() { return ofertaRepository.findAll().stream().map(this::oferta).toList(); }
    @Override @Transactional
    public CatalogResponse crearGrupo(GrupoRequest request) {
        OfertaCurso oferta = ofertaRepository.findById(request.getOfertaId()).orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada."));
        CategoriaEdad categoria = categoriaRepository.findById(request.getCategoriaId()).orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada."));
        return grupo(grupoRepository.save(Grupo.builder().oferta(oferta).categoria(categoria).nombreGrupo(request.getNombreGrupo()).build()));
    }
    @Override @Transactional
    public CatalogResponse actualizarGrupo(Long id, GrupoRequest request) {
        Grupo grupo = grupoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));
        OfertaCurso oferta = ofertaRepository.findById(request.getOfertaId())
                .orElseThrow(() -> new ResourceNotFoundException("Oferta no encontrada."));
        CategoriaEdad categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada."));
        grupo.setOferta(oferta);
        grupo.setCategoria(categoria);
        grupo.setNombreGrupo(request.getNombreGrupo());
        return grupo(grupoRepository.save(grupo));
    }
    @Override @Transactional(readOnly = true)
    public List<CatalogResponse> listarGrupos() { return grupoRepository.findAll().stream().map(this::grupo).toList(); }
    @Override @Transactional
    public CatalogResponse crearHorario(HorarioRequest request) {
        if (!request.getHoraInicio().isBefore(request.getHoraFin())) throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        Grupo grupo = grupoRepository.findById(request.getGrupoId()).orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));
        return horario(horarioRepository.save(Horario.builder().grupo(grupo).dia(request.getDia())
                .horaInicio(request.getHoraInicio()).horaFin(request.getHoraFin()).build()));
    }
    @Override @Transactional
    public List<CatalogResponse> crearHorariosBatch(HorarioBatchRequest request) {
        Grupo grupo = grupoRepository.findById(request.getGrupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));

        List<HorarioSlotRequest> effectiveSlots = new ArrayList<>();
        if (request.getSlots() != null && !request.getSlots().isEmpty()) {
            effectiveSlots.addAll(request.getSlots());
        }
        if (request.getDias() != null && !request.getDias().isEmpty()
                && request.getHoraInicio() != null && request.getHoraFin() != null) {
            effectiveSlots.add(new HorarioSlotRequest(request.getDias(), request.getHoraInicio(), request.getHoraFin()));
        }

        if (effectiveSlots.isEmpty()) {
            throw new IllegalArgumentException("Debe especificar al menos un día y horario.");
        }

        List<Horario> horariosExistentes = horarioRepository.findByGrupoId(grupo.getId());
        List<Horario> nuevosParaGuardar = new ArrayList<>();

        for (HorarioSlotRequest slot : effectiveSlots) {
            if (!slot.getHoraInicio().isBefore(slot.getHoraFin())) {
                throw new IllegalArgumentException(String.format(
                        "La hora de inicio (%s) debe ser anterior a la hora de fin (%s).",
                        slot.getHoraInicio(), slot.getHoraFin()
                ));
            }
            if (slot.getDias() == null || slot.getDias().isEmpty()) {
                throw new IllegalArgumentException("Cada bloque debe tener al menos un día seleccionado.");
            }

            for (DayOfWeek dia : slot.getDias()) {
                boolean overlap = horariosExistentes.stream().anyMatch(h ->
                        h.getDia().equals(dia) &&
                        slot.getHoraInicio().isBefore(h.getHoraFin()) &&
                        slot.getHoraFin().isAfter(h.getHoraInicio())
                );
                if (overlap) {
                    throw new IllegalArgumentException(String.format(
                            "El taller ya cuenta con un horario el día %s que se empalma con %s - %s.",
                            dia, slot.getHoraInicio(), slot.getHoraFin()
                    ));
                }

                boolean internalOverlap = nuevosParaGuardar.stream().anyMatch(h ->
                        h.getDia().equals(dia) &&
                        slot.getHoraInicio().isBefore(h.getHoraFin()) &&
                        slot.getHoraFin().isAfter(h.getHoraInicio())
                );
                if (internalOverlap) {
                    throw new IllegalArgumentException(String.format(
                            "Conflicto interno: se especificó más de un horario simultáneo para el día %s.",
                            dia
                    ));
                }

                nuevosParaGuardar.add(Horario.builder()
                        .grupo(grupo)
                        .dia(dia)
                        .horaInicio(slot.getHoraInicio())
                        .horaFin(slot.getHoraFin())
                        .build());
            }
        }

        return horarioRepository.saveAll(nuevosParaGuardar).stream().map(this::horario).toList();
    }
    @Override @Transactional
    public CatalogResponse actualizarHorario(Long id, HorarioRequest request) {
        if (!request.getHoraInicio().isBefore(request.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin.");
        }
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado."));
        Grupo grupo = grupoRepository.findById(request.getGrupoId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado."));
        horario.setGrupo(grupo);
        horario.setDia(request.getDia());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());
        return horario(horarioRepository.save(horario));
    }
    @Override @Transactional(readOnly = true)
    public List<CatalogResponse> listarHorarios(Long grupoId) { return horarioRepository.findByGrupoId(grupoId).stream().map(this::horario).toList(); }
    @Override @Transactional(readOnly = true)
    public List<CatalogResponse> listarTodosHorarios() { return horarioRepository.findAll().stream().map(this::horario).toList(); }
    @Override @Transactional
    public void eliminarHorario(Long id) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado."));
        horarioRepository.delete(horario);
    }

    private void validateOfferDates(OfertaCursoRequest request) {
        if (!request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }

    private CatalogResponse curso(Curso x) { return CatalogResponse.builder().id(x.getId()).nombre(x.getNombre()).build(); }
    private CatalogResponse categoria(CategoriaEdad x) { return CatalogResponse.builder().id(x.getId()).nombre(x.getNombre()).build(); }
    private CatalogResponse oferta(OfertaCurso x) { return CatalogResponse.builder().id(x.getId()).cursoId(x.getCurso().getId()).tipo(x.getTipo()).fechaInicio(x.getFechaInicio()).fechaFin(x.getFechaFin()).build(); }
    private CatalogResponse grupo(Grupo x) { return CatalogResponse.builder().id(x.getId()).ofertaId(x.getOferta().getId()).categoriaId(x.getCategoria().getId()).nombreGrupo(x.getNombreGrupo()).build(); }
    private CatalogResponse horario(Horario x) {
        String cursoNombre = null;
        Long cursoId = null;
        if (x.getGrupo() != null && x.getGrupo().getOferta() != null && x.getGrupo().getOferta().getCurso() != null) {
            cursoNombre = x.getGrupo().getOferta().getCurso().getNombre();
            cursoId = x.getGrupo().getOferta().getCurso().getId();
        }
        return CatalogResponse.builder()
                .id(x.getId())
                .grupoId(x.getGrupo().getId())
                .nombreGrupo(x.getGrupo().getNombreGrupo())
                .cursoId(cursoId)
                .nombre(cursoNombre)
                .dia(x.getDia())
                .horaInicio(x.getHoraInicio())
                .horaFin(x.getHoraFin())
                .build();
    }
}
