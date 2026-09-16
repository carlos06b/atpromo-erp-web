package com.atpromo.systematpromo.util;

import com.atpromo.systematpromo.model.PromoterPaymentData;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PixBatchExcelGenerator {

    private static final String[] COLUMNS = {
            "Tipo de Inscrição*:",
            "Inscrição*:",
            "Nome*:",
            "Tipo de chave*:",
            "Chave*:",
            "Valor do Pagamento*:",
            "Data do Pagamento*:"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PixBatchExcelGenerator() {
    }

    public static byte[] generate(List<PromoterPaymentData> payments) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("PIX Lote");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (PromoterPaymentData payment : payments) {
                Row row = sheet.createRow(rowNum++);

                String document = onlyNumbers(payment.getDocument());
                String pixType = payment.getPixType();

                row.createCell(0).setCellValue(registrationTypeCode(document));
                row.createCell(1).setCellValue(payment.getDocument() != null ? payment.getDocument() : "");
                row.createCell(2).setCellValue(payment.getName() != null ? payment.getName().toUpperCase(Locale.ROOT) : "");
                row.createCell(3).setCellValue(pixKeyTypeCode(pixType));
                row.createCell(4).setCellValue(payment.getPix() != null ? payment.getPix() : "");

                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(payment.getAmount() != null ? payment.getAmount().doubleValue() : 0);
                amountCell.setCellStyle(moneyStyle);

                row.createCell(6).setCellValue(
                        payment.getPaymentDate() != null ? payment.getPaymentDate().format(DATE_FORMATTER) : ""
                );
            }

            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao gerar planilha do lote de Pix", e);
        }
    }

    private static String registrationTypeCode(String documentDigitsOnly) {
        if (documentDigitsOnly == null) {
            return "";
        }
        if (documentDigitsOnly.length() == 14) {
            return "2";
        }
        if (documentDigitsOnly.length() == 11) {
            return "1";
        }
        return "";
    }

    private static String pixKeyTypeCode(String pixType) {
        if (pixType == null || pixType.isBlank()) {
            return "4";
        }

        return switch (pixType.trim().toUpperCase(Locale.ROOT)) {
            case "CELULAR", "TELEFONE" -> "1";
            case "EMAIL" -> "2";
            case "CPF", "CNPJ" -> "3";
            case "ALEATORIA", "ALEATÓRIA" -> "4";
            default -> "4";
        };
    }

    private static String onlyNumbers(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
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
}