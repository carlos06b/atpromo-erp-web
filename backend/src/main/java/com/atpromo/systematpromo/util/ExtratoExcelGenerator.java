package com.atpromo.systematpromo.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExtratoExcelGenerator {

    private static final String[] COLUMNS = {
            "Data",
            "Tipo",
            "Descrição",
            "Categoria",
            "Centro de custo",
            "Beneficiário",
            "Valor",
            "Saldo acumulado"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExtratoExcelGenerator() {
    }

    // Uma linha do extrato já resolvida (nomes de categoria/centro de custo/
    // beneficiário já traduzidos do id, saldo acumulado já calculado) -
    // quem monta essa lista é o controller, reaproveitando a mesma lógica
    // de cálculo de saldo que a tela usa (LancamentoExtratoController.mapOne).
    public record ExtratoLinha(
            LocalDate data,
            String tipo,
            String descricao,
            String categoriaNome,
            String centroCustoNome,
            String beneficiarioNome,
            BigDecimal valor,
            BigDecimal saldoAcumulado
    ) {}

    public static byte[] generate(String contaNome, List<ExtratoLinha> linhas) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Extrato");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            int rowIdx = 0;

            if (contaNome != null && !contaNome.isBlank()) {
                Row tituloRow = sheet.createRow(rowIdx++);
                Cell tituloCell = tituloRow.createCell(0);
                tituloCell.setCellValue("Extrato - " + contaNome);
                Font tituloFont = workbook.createFont();
                tituloFont.setBold(true);
                CellStyle tituloStyle = workbook.createCellStyle();
                tituloStyle.setFont(tituloFont);
                tituloCell.setCellStyle(tituloStyle);
                rowIdx++; // linha em branco antes do cabeçalho
            }

            Row header = sheet.createRow(rowIdx++);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ExtratoLinha linha : linhas) {
                Row row = sheet.createRow(rowIdx++);

                Cell dataCell = row.createCell(0);
                if (linha.data() != null) {
                    dataCell.setCellValue(linha.data().format(DATE_FORMATTER));
                }
                dataCell.setCellStyle(dateStyle);

                row.createCell(1).setCellValue(sanitizeForExcel(linha.tipo()));
                row.createCell(2).setCellValue(sanitizeForExcel(linha.descricao()));
                row.createCell(3).setCellValue(sanitizeForExcel(linha.categoriaNome()));
                row.createCell(4).setCellValue(sanitizeForExcel(linha.centroCustoNome()));
                row.createCell(5).setCellValue(sanitizeForExcel(linha.beneficiarioNome()));

                Cell valorCell = row.createCell(6);
                valorCell.setCellValue(linha.valor() != null ? linha.valor().doubleValue() : 0);
                valorCell.setCellStyle(moneyStyle);

                Cell saldoCell = row.createCell(7);
                saldoCell.setCellValue(linha.saldoAcumulado() != null ? linha.saldoAcumulado().doubleValue() : 0);
                saldoCell.setCellStyle(moneyStyle);
            }

            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gerar planilha do extrato", e);
        }
    }

    // Mesma mitigação do achado H5 (PixBatchExcelGenerator): descrição,
    // categoria, centro de custo e beneficiário vêm de texto livre ou de
    // catálogos editáveis pelo Financeiro/Admin, então aplicamos a mesma
    // defesa contra "fórmula"/CSV-Excel injection (OWASP) por consistência -
    // nenhum desses campos tinha essa checagem antes porque essa planilha
    // de extrato não existia.
    private static String sanitizeForExcel(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        return headerStyle;
    }

    private static CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle moneyStyle = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        moneyStyle.setDataFormat(dataFormat.getFormat("[$-409]\"R$\" #,##0.00"));
        return moneyStyle;
    }

    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);
        return dateStyle;
    }
}
