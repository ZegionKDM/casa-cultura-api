package CasaCulturaAPI.service.impl;

import CasaCulturaAPI.dto.request.RolRequest;
import CasaCulturaAPI.dto.response.RolResponse;
import CasaCulturaAPI.entity.Rol;
import CasaCulturaAPI.exception.ResourceNotFoundException;
import CasaCulturaAPI.mapper.RolMapper;
import CasaCulturaAPI.repository.RolRepository;
import CasaCulturaAPI.service.interfaces.RolService;
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