package com.lavabarber.api.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.lavabarber.api.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    Optional<Cliente> findByTelefono(String telefono);

    Optional<Cliente> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<Cliente> findByDocumentoHash(String documentoHash);

    /**
     * Busqueda por nombre, apellido, nombre completo, telefono, RUC (parcial) o cedula (EXACTA,
     * via documentoHash: la cedula esta cifrada y no admite busqueda parcial).
     * hash nunca puede ser null: si no hay documento se pasa un valor que no coincide con ningun hash.
     */
    @Query("""
           select c from Cliente c
           where (:soloActivos = false or c.activo = true)
             and (   lower(c.nombre) like lower(concat('%', :q, '%'))
                  or lower(c.apellido) like lower(concat('%', :q, '%'))
                  or lower(concat(c.nombre, ' ', c.apellido)) like lower(concat('%', :q, '%'))
                  or c.telefono like concat('%', :q, '%')
                  or c.ruc like concat('%', :q, '%')
                  or c.documentoHash = :hash)
           order by c.nombre, c.apellido
           """)
    List<Cliente> buscar(@Param("q") String q, @Param("hash") String hash,
                         @Param("soloActivos") boolean soloActivos, Pageable pageable);
}
