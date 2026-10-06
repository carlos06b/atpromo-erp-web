package com.atpromo.systematpromo.util;

import com.atpromo.systematpromo.util.ExtratoExcelGenerator.ExtratoLinha;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da exportação de extrato em Excel (funcionalidade nova, não é um
 * achado da auditoria). Aplica a mesma mitigação de "fórmula"/CSV-Excel
 * injection do achado H5 (ver ExtratoExcelGenerator.sanitizeForExcel), já
 * que descrição, categoria, centro de custo e beneficiário vêm de texto
 * livre ou de catálogos editáveis pelo Financeiro/Admin — por isso os
 * testes cobrem isso também, não só o conteúdo "normal".
 */
class ExtratoExcelGeneratorTest {

    @Test
    void geraUmaLinhaPorLancamento_comValoresNaOrdemCorreta() throws IOException {
        ExtratoLinha linha = new ExtratoLinha(
                LocalDate.of(2026, 3, 15),
                "SAIDA",
                "Pagamento de fornecedor",
                "Despesas Gerais",
                "Administrativo",
                "Fornecedor X",
                new BigDecimal("150.00"),
                new BigDecimal("850.00")
        );

        byte[] arquivo = ExtratoExcelGenerator.generate("Conta Principal", List.of(linha));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet sheet = workbook.getSheetAt(0);

            // linha 0 = título "Extrato - Conta Principal", linha 1 = branco,
            // linha 2 = cabeçalho, linha 3 = primeiro lançamento.
            Row header = sheet.getRow(2);
            assertEquals("Data", header.getCell(0).getStringCellValue());
            assertEquals("Saldo acumulado", header.getCell(7).getStringCellValue());

            Row dados = sheet.getRow(3);
            assertEquals("15/03/2026", dados.getCell(0).getStringCellValue());
            assertEquals("SAIDA", dados.getCell(1).getStringCellValue());
            assertEquals("Pagamento de fornecedor", dados.getCell(2).getStringCellValue());
            assertEquals("Despesas Gerais", dados.getCell(3).getStringCellValue());
            assertEquals("Administrativo", dados.getCell(4).getStringCellValue());
            assertEquals("Fornecedor X", dados.getCell(5).getStringCellValue());

            Cell valorCell = dados.getCell(6);
            assertEquals(CellType.NUMERIC, valorCell.getCellType());
            assertEquals(150.00, valorCell.getNumericCellValue(), 0.001);

            Cell saldoCell = dados.getCell(7);
            assertEquals(CellType.NUMERIC, saldoCell.getCellType());
            assertEquals(850.00, saldoCell.getNumericCellValue(), 0.001);
        }
    }

    @Test
    void camposDeTextoComCaractereDeFormula_recebemPrefixoDefensivo() throws IOException {
        // Mesma mitigação do H5: descrição, categoria, centro de custo e
        // beneficiário podem começar com =, +, - ou @ (texto livre ou vindo
        // de catálogos editáveis) e precisam do apóstrofo defensivo.
        ExtratoLinha linha = new ExtratoLinha(
                LocalDate.of(2026, 3, 15),
                "SAIDA",
                "=cmd|' /C calc'!A0",
                "+categoria maliciosa",
                "-centro malicioso",
                "@beneficiario malicioso",
                BigDecimal.TEN,
                BigDecimal.TEN
        );

        byte[] arquivo = ExtratoExcelGenerator.generate("Conta Principal", List.of(linha));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Row dados = workbook.getSheetAt(0).getRow(3);

            assertEquals(CellType.STRING, dados.getCell(2).getCellType());
            assertTrue(dados.getCell(2).getStringCellValue().startsWith("'="),
                    "Descrição começando com '=' deveria receber o prefixo defensivo");
            assertTrue(dados.getCell(3).getStringCellValue().startsWith("'+"),
                    "Categoria começando com '+' deveria receber o prefixo defensivo");
            assertTrue(dados.getCell(4).getStringCellValue().startsWith("'-"),
                    "Centro de custo começando com '-' deveria receber o prefixo defensivo");
            assertTrue(dados.getCell(5).getStringCellValue().startsWith("'@"),
                    "Beneficiário começando com '@' deveria receber o prefixo defensivo");
        }
    }

    @Test
    void camposNulos_naoQuebramAGeracao() throws IOException {
        ExtratoLinha linha = new ExtratoLinha(
                LocalDate.of(2026, 3, 15), "ENTRADA", null, null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00")
        );

        byte[] arquivo = ExtratoExcelGenerator.generate(null, List.of(linha));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet sheet = workbook.getSheetAt(0);
            // sem contaNome, não tem linha de título/branco - cabeçalho é a linha 0.
            Row dados = sheet.getRow(1);
            assertEquals("", dados.getCell(2).getStringCellValue());
            assertEquals("", dados.getCell(3).getStringCellValue());
        }
    }

    @Test
    void listaVazia_geraApenasCabecalho() throws IOException {
        byte[] arquivo = ExtratoExcelGenerator.generate("Conta Principal", List.of());

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(null, sheet.getRow(3));
        }
    }
}
