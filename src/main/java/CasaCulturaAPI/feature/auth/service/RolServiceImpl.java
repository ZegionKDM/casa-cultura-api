package CasaCulturaAPI.feature.auth.service;

import CasaCulturaAPI.feature.auth.dto.request.RolRequest;
import CasaCulturaAPI.feature.auth.dto.response.RolResponse;
import CasaCulturaAPI.shared.entity.Rol;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.feature.auth.service.RolMapper;
import CasaCulturaAPI.feature.auth.repository.RolRepository;
import CasaCulturaAPI.feature.auth.service.RolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements RolService {

    private final RolRepository repository;
    private final RolMapper mapper;

    @Override
    public RolResponse crear(RolRequest request) {

        if(repository.existsByNombre(request.getNombre())){
            throw new IllegalArgumentException("El rol ya existe.");
        }

        Rol rol = mapper.toEntity(request);

        repository.save(rol);

        return mapper.toResponse(rol);
    }

    @Override
    public List<RolResponse> obtenerTodos() {

        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();

    }

    @Override
    public RolResponse obtenerPorId(Long id) {

        Rol rol = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rol no encontrado."));

        return mapper.toResponse(rol);

    }

    @Override
    public RolResponse actualizar(Long id, RolRequest request) {

        Rol rol = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rol no encontrado."));

        rol.setNombre(request.getNombre());
        rol.setDescripcion(request.getDescripcion());

        repository.save(rol);

        return mapper.toResponse(rol);

    }

    @Override
    public void eliminar(Long id) {

        Rol rol = repository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rol no encontrado."));

        repository.delete(rol);

    }

}