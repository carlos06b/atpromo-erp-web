package com.atpromo.systematpromo.util;

import com.atpromo.systematpromo.model.PromoterPaymentData;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * ACHADO H5 (CORRIGIDO, dado que o arquivo é enviado a um sistema bancário
 * de pagamento em lote): nome do promotor, chave Pix e documento eram
 * escritos na célula sem nenhum tratamento defensivo contra
 * "fórmula"/CSV-Excel injection (texto começando com =, +, -, @). O
 * cadastro de promotor (PromoterController) aceita qualquer texto em
 * "name"/"pix" sem bloquear esses caracteres.
 *
 * A correção prefixa esses campos com apóstrofo quando começam com um dos
 * quatro caracteres de fórmula, seguindo a mitigação recomendada pela OWASP
 * para CSV/Excel injection — isso força a célula a ser tratada como texto
 * puro por qualquer leitor que respeite esse prefixo (Excel, LibreOffice,
 * Google Sheets), sem alterar o conteúdo visível para quem abrir o arquivo.
 */
class PixBatchExcelGeneratorTest {

    @Test
    void nomeComCaractereDeFormula_recebePrefixoDefensivo() throws IOException {
        PromoterPaymentData pagamentoMalicioso = new PromoterPaymentData(
                "12345678900",
                "=cmd|' /C calc'!A0",
                "chave-pix@exemplo.com",
                "EMAIL",
                new BigDecimal("100.00"),
                LocalDate.now()
        );

        byte[] arquivo = PixBatchExcelGenerator.generate(List.of(pagamentoMalicioso));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row linhaDoPagamento = sheet.getRow(1);
            Cell celulaNome = linhaDoPagamento.getCell(2);

            assertEquals(CellType.STRING, celulaNome.getCellType());

            // CORRIGIDO: o nome malicioso agora chega com um apóstrofo na
            // frente, forçando a célula a ser interpretada como texto puro
            // em vez de fórmula. O texto também vem em caixa alta porque
            // PixBatchExcelGenerator.generate() já maiusculiza todo nome
            // (regra de negócio existente, não relacionada a este achado) —
            // o apóstrofo defensivo é aplicado depois disso.
            assertEquals("'=CMD|' /C CALC'!A0", celulaNome.getStringCellValue(),
                    "O nome que começa com '=' deveria receber o prefixo defensivo (apóstrofo)");
        }
    }

    @Test
    void chavePixComCaractereDeFormula_recebePrefixoDefensivo() throws IOException {
        PromoterPaymentData pagamentoMalicioso = new PromoterPaymentData(
                "12345678900", "Promotor Normal", "+1234567890", "CELULAR",
                new BigDecimal("50.00"), LocalDate.now()
        );

        byte[] arquivo = PixBatchExcelGenerator.generate(List.of(pagamentoMalicioso));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Cell celulaChave = workbook.getSheetAt(0).getRow(1).getCell(4);
            assertEquals("'+1234567890", celulaChave.getStringCellValue(),
                    "A chave Pix que começa com '+' deveria receber o prefixo defensivo");
        }
    }

    @Test
    void nomeSemCaractereDeFormula_naoRecebePrefixo() throws IOException {
        PromoterPaymentData pagamentoNormal = new PromoterPaymentData(
                "12345678900", "Promotor Normal", "chave@pix.com", "EMAIL",
                new BigDecimal("100.00"), LocalDate.now()
        );

        byte[] arquivo = PixBatchExcelGenerator.generate(List.of(pagamentoNormal));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Cell celulaNome = workbook.getSheetAt(0).getRow(1).getCell(2);
            assertEquals("PROMOTOR NORMAL", celulaNome.getStringCellValue(),
                    "Nome sem caractere de fórmula não deveria ser alterado");
        }
    }

    @Test
    void valorEhGravadoComoNumeroCorretamenteFormatado() throws IOException {
        PromoterPaymentData pagamento = new PromoterPaymentData(
                "12345678900", "Promotor Normal", "chave@pix.com", "EMAIL",
                new BigDecimal("1234.56"), LocalDate.now()
        );

        byte[] arquivo = PixBatchExcelGenerator.generate(List.of(pagamento));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Cell celulaValor = workbook.getSheetAt(0).getRow(1).getCell(5);
            assertEquals(CellType.NUMERIC, celulaValor.getCellType());
            assertEquals(1234.56, celulaValor.getNumericCellValue(), 0.001);
        }
    }

    @Test
    void cpfEhClassificadoComoTipo1_eCnpjComoTipo2() throws IOException {
        PromoterPaymentData cpf = new PromoterPaymentData(
                "123.456.789-00", "Pessoa Fisica", "pix", "CPF", BigDecimal.TEN, LocalDate.now());
        PromoterPaymentData cnpj = new PromoterPaymentData(
                "12.345.678/0001-99", "Empresa", "pix", "CNPJ", BigDecimal.TEN, LocalDate.now());

        byte[] arquivo = PixBatchExcelGenerator.generate(List.of(cpf, cnpj));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(arquivo))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("1", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("2", sheet.getRow(2).getCell(0).getStringCellValue());
            // Garante que CPF e CNPJ não foram confundidos entre si.
            assertNotEquals(
                    sheet.getRow(1).getCell(0).getStringCellValue(),
                    sheet.getRow(2).getCell(0).getStringCellValue()
            );
        }
    }
}
