package com.lavabarber.api.config;

import com.lavabarber.api.entity.CategoriaEgreso;
import com.lavabarber.api.entity.Negocio;
import com.lavabarber.api.entity.Rubro;
import com.lavabarber.api.entity.UsuarioInterno;
import com.lavabarber.api.entity.enums.RolInterno;
import com.lavabarber.api.repository.CategoriaEgresoRepository;
import com.lavabarber.api.repository.NegocioRepository;
import com.lavabarber.api.repository.RubroRepository;
import com.lavabarber.api.repository.UsuarioInternoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos minimos para el PRIMER arranque en produccion (el DataSeeder solo corre en dev).
 *
 * Cada paso se hace unicamente si su tabla esta vacia, asi que en los arranques siguientes no
 * toca nada y puede dejarse siempre activo:
 *  - Negocio (nombre en NEGOCIO_NOMBRE) con su punto de expedicion 001-001.
 *  - Rubros base (Barberia, Lavadero, Mixto) y categorias de egreso comunes; todo se edita despues
 *    desde el backoffice.
 *  - Usuario ADMIN, con ADMIN_USERNAME y ADMIN_PASSWORD. Si no hay usuarios y faltan esas
 *    variables, la app no arranca (sin admin nadie podria entrar al backoffice).
 *
 * Una vez creado el admin, conviene sacar ADMIN_PASSWORD del entorno y cambiar la clave desde
 * el sistema.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdBootstrap implements CommandLineRunner {

    private static final int MIN_LARGO_CLAVE = 10;

    private final NegocioRepository negocioRepository;
    private final RubroRepository rubroRepository;
    private final CategoriaEgresoRepository categoriaEgresoRepository;
    private final UsuarioInternoRepository usuarioInternoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.negocio-nombre:Chimpi Hub}")
    private String negocioNombre;

    @Value("${app.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        // Se valida primero, para no dejar el negocio a medio crear si faltan las variables.
        boolean faltaAdmin = usuarioInternoRepository.count() == 0;
        if (faltaAdmin) {
            if (adminUsername.isBlank() || adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "Primer arranque en produccion: defini ADMIN_USERNAME y ADMIN_PASSWORD para crear el usuario administrador.");
            }
            if (adminPassword.length() < MIN_LARGO_CLAVE) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD es muy corta: usa al menos " + MIN_LARGO_CLAVE + " caracteres.");
            }
        }

        Negocio negocio = null;
        if (negocioRepository.count() == 0) {
            negocio = negocioRepository.save(Negocio.builder()
                    .nombre(negocioNombre)
                    .puntoExpedicion("001-001")
                    .proximoNumeroFactura(1)
                    .build());
            for (String categoria : new String[]{"Insumos", "Sueldos", "Servicios publicos", "Alquiler"}) {
                categoriaEgresoRepository.save(CategoriaEgreso.builder().negocio(negocio).nombre(categoria).build());
            }
            System.out.println("[bootstrap] Negocio creado: " + negocioNombre);
        }

        if (rubroRepository.count() == 0) {
            for (String rubro : new String[]{"Barberia", "Lavadero", "Mixto"}) {
                rubroRepository.save(Rubro.builder().nombre(rubro).build());
            }
            System.out.println("[bootstrap] Rubros base creados.");
        }

        if (faltaAdmin) {
            usuarioInternoRepository.save(UsuarioInterno.builder()
                    .username(adminUsername.trim())
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .nombre("Administrador")
                    .apellido("Sistema")
                    .rol(RolInterno.ADMIN)
                    .activo(true)
                    .build());
            System.out.println("[bootstrap] Usuario administrador creado: " + adminUsername.trim());
        }
    }
}
