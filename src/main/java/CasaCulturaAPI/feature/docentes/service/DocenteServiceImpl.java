package CasaCulturaAPI.feature.docentes.service;

import CasaCulturaAPI.feature.docentes.dto.request.DocenteRequest;
import CasaCulturaAPI.feature.docentes.dto.response.DocenteResponse;
import CasaCulturaAPI.shared.entity.*;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.feature.docentes.repository.DocenteRepository;
import CasaCulturaAPI.shared.repository.PersonaRepository;
import CasaCulturaAPI.feature.docentes.service.DocenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocenteServiceImpl implements DocenteService {
    private final DocenteRepository docenteRepository;
    private final PersonaRepository personaRepository;

    @Override @Transactional
    public DocenteResponse crear(DocenteRequest request) {
        Persona persona = personaRepository.save(Persona.builder().nombre(request.getNombre())
                .apellidoPaterno(request.getApellidoPaterno()).apellidoMaterno(request.getApellidoMaterno())
                .telefono(request.getTelefono()).direccion(request.getDireccion()).correo(request.getCorreo())
                .fotoUrl(request.getFotoUrl()).build());
        return toResponse(docenteRepository.save(Docente.builder().persona(persona)
                .especialidad(request.getEspecialidad()).build()));
    }
    @Override @Transactional(readOnly = true)
    public List<DocenteResponse> listar() { return docenteRepository.findAll().stream().map(this::toResponse).toList(); }
    @Override @Transactional(readOnly = true)
    public DocenteResponse obtener(Long id) { return toResponse(find(id)); }

    @Override @Transactional
    public DocenteResponse actualizar(Long id, DocenteRequest request) {
        Docente docente = find(id);
        Persona persona = docente.getPersona();
        persona.setNombre(request.getNombre());
        persona.setApellidoPaterno(request.getApellidoPaterno());
        persona.setApellidoMaterno(request.getApellidoMaterno());
        persona.setTelefono(request.getTelefono());
        persona.setDireccion(request.getDireccion());
        persona.setCorreo(request.getCorreo());
        if (request.getFotoUrl() != null) {
            persona.setFotoUrl(request.getFotoUrl());
        }
        docente.setEspecialidad(request.getEspecialidad());
        personaRepository.save(persona);
        return toResponse(docenteRepository.save(docente));
    }

    @Override @Transactional
    public void desactivar(Long id) { Docente docente = find(id); docente.setEstado(EstadoRegistro.INACTIVO); docenteRepository.save(docente); }

    private Docente find(Long id) {
        return docenteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Docente no encontrado."));
    }
    private DocenteResponse toResponse(Docente x) {
        Persona p = x.getPersona();
        return DocenteResponse.builder().id(x.getId()).personaId(p.getId()).nombre(p.getNombre())
                .apellidoPaterno(p.getApellidoPaterno()).apellidoMaterno(p.getApellidoMaterno())
                .telefono(p.getTelefono()).direccion(p.getDireccion()).correo(p.getCorreo())
                .fotoUrl(p.getFotoUrl())
                .especialidad(x.getEspecialidad()).estado(x.getEstado()).build();
    }
}
