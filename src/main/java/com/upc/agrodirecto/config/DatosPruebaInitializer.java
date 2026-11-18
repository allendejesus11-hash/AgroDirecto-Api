package com.upc.agrodirecto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Carga los datos de demostracion una sola vez en una BD completamente vacia.
 *
 * El SQL existente contiene TRUNCATE RESTART IDENTITY y referencias a IDs
 * fijos. Por seguridad se omite la carga cuando CUALQUIERA de las 11 tablas
 * ya contiene registros. La transaccion evita dejar datos parciales si falla.
 * No activar app.seed-demo-data fuera del entorno de desarrollo.
 */
@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DatosPruebaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosPruebaInitializer.class);

    private static final String CONTAR_FILAS = """
            SELECT (SELECT COUNT(*) FROM rol)
                 + (SELECT COUNT(*) FROM cultivo)
                 + (SELECT COUNT(*) FROM usuario)
                 + (SELECT COUNT(*) FROM lote)
                 + (SELECT COUNT(*) FROM carga)
                 + (SELECT COUNT(*) FROM solicitud_chat)
                 + (SELECT COUNT(*) FROM mensaje_chat)
                 + (SELECT COUNT(*) FROM calificacion)
                 + (SELECT COUNT(*) FROM precio_mercado)
                 + (SELECT COUNT(*) FROM notificacion)
                 + (SELECT COUNT(*) FROM gasto_viaje)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public DatosPruebaInitializer(JdbcTemplate jdbcTemplate,
                                  PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> {
            Long total = jdbcTemplate.queryForObject(CONTAR_FILAS, Long.class);
            if (total == null || total > 0) {
                log.info("Datos de prueba: se omite la carga. La BD ya contiene registros; no se borrara ni modificara ningun dato.");
                return;
            }

            log.info("Datos de prueba: BD vacia; cargando los ejemplos de AgroDirecto...");
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/datos_prueba_postgresql_bcrypt.sql"));
                return null;
            });
            log.info("Datos de prueba: carga completada. Usuarios y catalogos disponibles para pruebas.");
        });
    }
}
