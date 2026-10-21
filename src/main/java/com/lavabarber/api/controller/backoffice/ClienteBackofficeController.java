package com.lavabarber.api.controller.backoffice;

import com.lavabarber.api.dto.backoffice.ClienteBackofficeRequest;
import com.lavabarber.api.dto.backoffice.ClienteBackofficeResponse;
import com.lavabarber.api.dto.backoffice.ClienteBusquedaResponse;
import com.lavabarber.api.dto.backoffice.DocumentoFacturaResponse;
import com.lavabarber.api.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Clientes del backoffice.
 * - Buscar y crear: ADMIN, EMPLEADO y CAJERO (el cajero da de alta al cliente que pide factura).
 * - Listado completo, ver, editar y dar de baja/alta: solo ADMIN.
 */
@RestController
@RequestMapping("/api/backoffice/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CAJERO')")
public class ClienteBackofficeController {

    private final ClienteService clienteService;

    /** Por nombre, apellido, telefono, RUC o cedula exacta. Minimo 2 caracteres, maximo 15 resultados. */
    @GetMapping("/buscar")
    public List<ClienteBusquedaResponse> buscar(@RequestParam String q) {
        return clienteService.buscarParaPos(q);
    }

    @PostMapping
    public ResponseEntity<ClienteBackofficeResponse> crear(@Valid @RequestBody ClienteBackofficeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    /** RUC o cedula del cliente, para prellenar el formulario de factura. */
    @GetMapping("/{id}/documento-factura")
    public DocumentoFacturaResponse documentoParaFactura(@PathVariable UUID id) {
        return clienteService.documentoParaFactura(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ClienteBackofficeResponse> listar(@RequestParam(required = false) String q) {
        return clienteService.listar(q);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteBackofficeResponse obtener(@PathVariable UUID id) {
        return clienteService.obtener(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteBackofficeResponse actualizar(@PathVariable UUID id, @Valid @RequestBody ClienteBackofficeRequest request) {
        return clienteService.actualizar(id, request);
    }

    /** Baja logica: no se borra porque tiene ventas y reservas asociadas. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteBackofficeResponse desactivar(@PathVariable UUID id) {
        return clienteService.cambiarActivo(id, false);
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ClienteBackofficeResponse reactivar(@PathVariable UUID id) {
        return clienteService.cambiarActivo(id, true);
    }
}
