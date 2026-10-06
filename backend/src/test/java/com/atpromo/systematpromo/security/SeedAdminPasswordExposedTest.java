package com.atpromo.systematpromo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ACHADO C1 (crítico): backend/schema_postgres.sql continha um INSERT de
 * usuário admin com senha em texto claro documentada no próprio comentário
 * do arquivo (admin@atpromo.com / Teste123!), usado pra popular o banco de
 * PRODUÇÃO no Render.
 *
 * Este teste virou um "guarda de regressão": confirma que o hash antigo
 * (que batia com "Teste123!") NÃO está mais commitado no arquivo, pra
 * ninguém reintroduzir essa credencial por engano no futuro.
 *
 * IMPORTANTE: corrigir este arquivo não troca a senha real em produção —
 * isso só afeta bootstraps futuros de um banco novo. Se o script antigo já
 * rodou contra o Render, a senha real daquele usuário precisa ser trocada
 * manualmente por quem tem acesso ao banco de produção.
 */
class SeedAdminPasswordExposedTest {

    private static final String HASH_ANTIGO_EXPOSTO =
            "$2b$12$rXOaEBy6d5wCkCjrUILzJubM/LehN8Sp5MdDn96cdzOOsUHzWN7mq";

    @Test
    void hashAntigoDaSenhaExposta_naoBateMaisComOConteudoDoArquivo() throws IOException {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // o próprio hash ainda "bate" com a senha — isso é esperado, bcrypt é determinístico
        // na verificação; o que importa é que ele não está mais no arquivo (próximo teste).
        assertTrue(encoder.matches("Teste123!", HASH_ANTIGO_EXPOSTO),
                "sanity check: o hash usado neste teste corresponde à senha 'Teste123!'");
    }

    @Test
    void schemaPostgresSql_naoContemMaisHashNemSenhaEmTextoClaro() throws IOException {
        String conteudo = lerSchemaPostgres();

        assertFalse(conteudo.contains(HASH_ANTIGO_EXPOSTO),
                "REGRESSÃO: o hash bcrypt antigo (correspondente a 'Teste123!') voltou a aparecer "
                        + "em backend/schema_postgres.sql. Isso reintroduz o achado C1 da auditoria.");

        assertFalse(conteudo.toLowerCase().contains("teste123!"),
                "REGRESSÃO: a senha em texto claro 'Teste123!' voltou a aparecer em "
                        + "backend/schema_postgres.sql.");
    }

    private String lerSchemaPostgres() throws IOException {
        // o teste roda a partir de backend/ (raiz do módulo Maven)
        Path path = Paths.get("schema_postgres.sql");
        if (!Files.exists(path)) {
            // fallback caso o working directory dos testes seja a raiz do repositório
            path = Paths.get("backend", "schema_postgres.sql");
        }
        return Files.readString(path);
    }
}
