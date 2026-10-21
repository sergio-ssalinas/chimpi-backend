package com.lavabarber.api.service;

import com.lavabarber.api.dto.backoffice.ClienteBackofficeRequest;
import com.lavabarber.api.dto.backoffice.ClienteBackofficeResponse;
import com.lavabarber.api.dto.backoffice.ClienteBusquedaResponse;
import com.lavabarber.api.dto.backoffice.DocumentoFacturaResponse;
import com.lavabarber.api.entity.Cliente;
import com.lavabarber.api.exception.BusinessException;
import com.lavabarber.api.exception.ResourceNotFoundException;
import com.lavabarber.api.repository.ClienteRepository;
import com.lavabarber.api.security.DocumentoHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ABM de clientes del backoffice. Los clientes cargados aca (por admin, empleado o cajero) no tienen
 * cuenta en la app: reciben un usuario interno "local_xxxx" y una clave aleatoria que nadie conoce,
 * porque la entidad exige ambos. Si la persona quiere usar la app se registra normalmente.
 */
@Service
@RequiredArgsConstructor
public class ClienteService {

    private static final int MIN_CARACTERES = 2;
    private static final int MAX_BUSQUEDA_POS = 15;
    private static final int MAX_LISTADO_ADMIN = 100;
    /** Numero de 5 a 10 digitos con digito verificador opcional: 1234567-8 o 1234567. */
    private static final Pattern FORMATO_RUC = Pattern.compile("^\\d{5,10}(-\\d)?$");

    private final ClienteRepository clienteRepository;
    private final DocumentoHasher documentoHasher;
    private final PasswordEncoder passwordEncoder;

    /** Devuelve el RUC sin espacios, o null si viene vacio. Lanza BusinessException si el formato no sirve. */
    public static String normalizarRuc(String ruc) {
        if (ruc == null || ruc.isBlank()) return null;
        String limpio = ruc.trim().replace(" ", "");
        if (!FORMATO_RUC.matcher(limpio).matches()) {
            throw new BusinessException("El RUC no tiene un formato valido (ejemplo: 1234567-8)");
        }
        return limpio;
    }

    @Transactional(readOnly = true)
    public List<ClienteBusquedaResponse> buscarParaPos(String q) {
        String termino = q == null ? "" : q.trim();
        if (termino.length() < MIN_CARACTERES) return List.of();
        return clienteRepository.buscar(termino, hashOBusquedaVacia(termino), true, PageRequest.of(0, MAX_BUSQUEDA_POS))
                .stream()
                .map(c -> new ClienteBusquedaResponse(c.getId(), c.getNombre(), c.getApellido(), c.getTelefono(), c.getRuc()))
                .toList();
    }

    /** Listado del ABM (solo ADMIN): incluye inactivos. Sin texto devuelve los primeros 100 por nombre. */
    @Transactional(readOnly = true)
    public List<ClienteBackofficeResponse> listar(String q) {
        String termino = q == null ? "" : q.trim();
        return clienteRepository.buscar(termino, hashOBusquedaVacia(termino), false, PageRequest.of(0, MAX_LISTADO_ADMIN))
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteBackofficeResponse obtener(UUID id) {
        return toResponse(buscarPorId(id));
    }

    @Transactional
    public ClienteBackofficeResponse crear(ClienteBackofficeRequest request) {
        Cliente cliente = Cliente.builder()
                .username("local_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12))
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .nombre("")
                .apellido("")
                .build();
        aplicar(cliente, request);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteBackofficeResponse actualizar(UUID id, ClienteBackofficeRequest request) {
        Cliente cliente = buscarPorId(id);
        aplicar(cliente, request);
        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteBackofficeResponse cambiarActivo(UUID id, boolean activo) {
        Cliente cliente = buscarPorId(id);
        cliente.setActivo(activo);
        return toResponse(clienteRepository.save(cliente));
    }

    /** RUC del cliente o, si no tiene, su cedula: lo que se imprime en su factura. */
    @Transactional(readOnly = true)
    public DocumentoFacturaResponse documentoParaFactura(UUID id) {
        return new DocumentoFacturaResponse(documentoParaFactura(buscarPorId(id)));
    }

    public static String documentoParaFactura(Cliente c) {
        if (c.getRuc() != null && !c.getRuc().isBlank()) return c.getRuc();
        return c.getDocumentoIdentidad();
    }

    // ---- internos ----

    private void aplicar(Cliente cliente, ClienteBackofficeRequest r) {
        String telefono = vacioANull(r.telefono());
        if (telefono != null) {
            clienteRepository.findByTelefono(telefono).ifPresent(otro -> {
                if (!otro.getId().equals(cliente.getId())) {
                    throw new BusinessException("Ya existe un cliente con ese telefono");
                }
            });
        }

        String documento = vacioANull(r.documentoIdentidad());
        String hash = documentoHasher.hash(documento);
        if (hash != null) {
            clienteRepository.findByDocumentoHash(hash).ifPresent(otro -> {
                if (!otro.getId().equals(cliente.getId())) {
                    throw new BusinessException("Ya existe un cliente con esa cedula");
                }
            });
        }

        cliente.setNombre(r.nombre().trim());
        cliente.setApellido(r.apellido() == null ? "" : r.apellido().trim());
        cliente.setTelefono(telefono);
        cliente.setDocumentoIdentidad(documento);
        cliente.setDocumentoHash(hash);
        cliente.setRuc(normalizarRuc(r.ruc()));
        cliente.setEmail(vacioANull(r.email()));
        cliente.setDireccion(vacioANull(r.direccion()));
    }

    private Cliente buscarPorId(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + id));
    }

    /** El parametro :hash de la consulta no admite null; este valor no coincide con ningun hash real. */
    private String hashOBusquedaVacia(String termino) {
        String h = documentoHasher.hash(termino);
        return h != null ? h : "-";
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private ClienteBackofficeResponse toResponse(Cliente c) {
        return new ClienteBackofficeResponse(c.getId(), c.getNombre(), c.getApellido(), c.getTelefono(),
                c.getDocumentoIdentidad(), c.getRuc(), c.getEmail(), c.getDireccion(), c.isActivo());
    }
}
