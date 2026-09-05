package CasaCulturaAPI.service.impl;

import CasaCulturaAPI.dto.request.*;
import CasaCulturaAPI.dto.response.CatalogResponse;
import CasaCulturaAPI.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.repository.*;
import CasaCulturaAPI.service.interfaces.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Curso curso = cursoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado."));
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

    private void validateOfferDates(OfertaCursoRequest request) {
        if (!request.getFechaInicio().isBefore(request.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin.");
        }
    }

    private CatalogResponse curso(Curso x) { return CatalogResponse.builder().id(x.getId()).nombre(x.getNombre()).build(); }
    private CatalogResponse categoria(CategoriaEdad x) { return CatalogResponse.builder().id(x.getId()).nombre(x.getNombre()).build(); }
    private CatalogResponse oferta(OfertaCurso x) { return CatalogResponse.builder().id(x.getId()).cursoId(x.getCurso().getId()).tipo(x.getTipo()).fechaInicio(x.getFechaInicio()).fechaFin(x.getFechaFin()).build(); }
    private CatalogResponse grupo(Grupo x) { return CatalogResponse.builder().id(x.getId()).ofertaId(x.getOferta().getId()).categoriaId(x.getCategoria().getId()).nombreGrupo(x.getNombreGrupo()).build(); }
    private CatalogResponse horario(Horario x) { return CatalogResponse.builder().id(x.getId()).grupoId(x.getGrupo().getId()).dia(x.getDia()).horaInicio(x.getHoraInicio()).horaFin(x.getHoraFin()).build(); }
}
