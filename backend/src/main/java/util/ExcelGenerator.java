package util;

import model.Client;
import model.InvoiceView;
import model.PayrollLine;
import model.PromoterPaymentData;
import model.Promoter;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ExcelGenerator {

    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public static void generatePromoters(List<Promoter> promoters, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Promotores");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            String[] columns = {
                    "Nome",
                    "CPF",
                    "Telefone",
                    "UF",
                    "Cidade",
                    "Tipo",
                    "Salário",
                    "PIX",
                    "Tipo PIX",
                    "Nascimento",
                    "Status"
            };

            Row header = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;

            for (Promoter promoter : promoters) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(nullToBlank(promoter.getName()));
                row.createCell(1).setCellValue(formatDocument(promoter.getCpf()));
                row.createCell(2).setCellValue(formatPhone(promoter.getPhone()));
                row.createCell(3).setCellValue(nullToBlank(promoter.getUf()));
                row.createCell(4).setCellValue(nullToBlank(promoter.getCity()));
                row.createCell(5).setCellValue(nullToBlank(promoter.getType()));

                Cell salaryCell = row.createCell(6);
                salaryCell.setCellValue(promoter.getSalary() != null ? promoter.getSalary().doubleValue() : 0);
                salaryCell.setCellStyle(moneyStyle);

                row.createCell(7).setCellValue(nullToBlank(promoter.getPix()));
                row.createCell(8).setCellValue(nullToBlank(promoter.getPixType()));
                row.createCell(9).setCellValue(
                        promoter.getDateBirth() != null ? promoter.getDateBirth().format(formatter) : ""
                );
                row.createCell(10).setCellValue(promoter.isActive() ? "ATIVO" : "INATIVO");
            }

            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0,
                    Math.max(0, rowNum - 1),
                    0,
                    columns.length - 1
            ));

            sheet.createFreezePane(0, 1);
            autoSizeColumns(sheet, columns.length);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Excel de promotores", e);
        }
    }

    public static void generateInvoices(List<InvoiceView> invoices, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Faturamento");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            Row header = sheet.createRow(0);

            String[] columns = {
                    "Indústria", "Vínculo", "Valor Faturado", "Valor Recebido",
                    "Previsto", "Faturado em", "Recebido em", "Status"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;

            for (InvoiceView inv : invoices) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(nullToBlank(inv.getClientName()));
                row.createCell(1).setCellValue(nullToBlank(inv.getCompanyLink()));

                Cell amountCell = row.createCell(2);
                amountCell.setCellValue(inv.getAmount() != null ? inv.getAmount().doubleValue() : 0);
                amountCell.setCellStyle(moneyStyle);

                Cell receivedCell = row.createCell(3);
                if (inv.getReceivedAmount() != null) {
                    receivedCell.setCellValue(inv.getReceivedAmount().doubleValue());
                    receivedCell.setCellStyle(moneyStyle);
                } else {
                    receivedCell.setCellValue("-");
                }

                row.createCell(4).setCellValue(
                        inv.getDueDate() != null ? inv.getDueDate().format(formatter) : "-"
                );

                row.createCell(5).setCellValue(
                        inv.getIssueDate() != null ? inv.getIssueDate().format(formatter) : "-"
                );

                row.createCell(6).setCellValue(
                        inv.getPaymentDate() != null ? inv.getPaymentDate().format(formatter) : "-"
                );

                row.createCell(7).setCellValue(nullToBlank(inv.getStatus()));
            }

            autoSizeColumns(sheet, columns.length);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Excel de faturamento", e);
        }
    }

    public static void generatePayrollReport(List<PayrollLine> lines, LocalDate start, LocalDate end, String promoterType, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Folha de Pagamento");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            CellStyle totalMoneyStyle = workbook.createCellStyle();
            totalMoneyStyle.cloneStyleFrom(moneyStyle);
            totalMoneyStyle.setFont(totalFont);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("RELATÓRIO DA FOLHA DE PAGAMENTO");
            titleCell.setCellStyle(titleStyle);

            Row periodRow = sheet.createRow(2);
            periodRow.createCell(0).setCellValue("Período:");
            periodRow.createCell(1).setCellValue(start.format(formatter) + " até " + end.format(formatter));

            Row typeRow = sheet.createRow(3);
            typeRow.createCell(0).setCellValue("Tipo:");
            typeRow.createCell(1).setCellValue(promoterType);

            String[] columns = {
                    "Promotor",
                    "Tipo",
                    "Salário/Base",
                    "Descontos",
                    "Líquido a Pagar",
                    "Status",
                    "Observação"
            };

            int headerRowIndex = 5;
            Row header = sheet.createRow(headerRowIndex);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal totalBase = BigDecimal.ZERO;
            BigDecimal totalDiscounts = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;

            int rowNum = headerRowIndex + 1;

            for (PayrollLine line : lines) {
                BigDecimal baseSalary = line.getBaseSalary() != null ? line.getBaseSalary() : BigDecimal.ZERO;
                BigDecimal discounts = line.getDiscounts() != null ? line.getDiscounts() : BigDecimal.ZERO;
                BigDecimal netAmount = line.getNetAmount() != null ? line.getNetAmount() : BigDecimal.ZERO;

                totalBase = totalBase.add(baseSalary);
                totalDiscounts = totalDiscounts.add(discounts);
                totalNet = totalNet.add(netAmount);

                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(nullToBlank(line.getPromoterName()));
                row.createCell(1).setCellValue(nullToBlank(line.getPromoterType()));

                Cell baseCell = row.createCell(2);
                baseCell.setCellValue(baseSalary.doubleValue());
                baseCell.setCellStyle(moneyStyle);

                Cell discountCell = row.createCell(3);
                discountCell.setCellValue(discounts.doubleValue());
                discountCell.setCellStyle(moneyStyle);

                Cell netCell = row.createCell(4);
                netCell.setCellValue(netAmount.doubleValue());
                netCell.setCellStyle(moneyStyle);

                row.createCell(5).setCellValue(nullToBlank(line.getStatus()));
                row.createCell(6).setCellValue(nullToBlank(line.getObservation()));
            }

            Row totalRow = sheet.createRow(rowNum + 1);

            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("TOTAL GERAL");
            totalLabelCell.setCellStyle(totalStyle);

            Cell totalBaseCell = totalRow.createCell(2);
            totalBaseCell.setCellValue(totalBase.doubleValue());
            totalBaseCell.setCellStyle(totalMoneyStyle);

            Cell totalDiscountCell = totalRow.createCell(3);
            totalDiscountCell.setCellValue(totalDiscounts.doubleValue());
            totalDiscountCell.setCellStyle(totalMoneyStyle);

            Cell totalNetCell = totalRow.createCell(4);
            totalNetCell.setCellValue(totalNet.doubleValue());
            totalNetCell.setCellStyle(totalMoneyStyle);

            Row countRow = sheet.createRow(rowNum + 3);
            countRow.createCell(0).setCellValue("Quantidade de promotores:");
            countRow.createCell(1).setCellValue(lines.size());

            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    headerRowIndex,
                    Math.max(headerRowIndex, rowNum - 1),
                    0,
                    columns.length - 1
            ));

            sheet.createFreezePane(0, headerRowIndex + 1);

            autoSizeColumns(sheet, columns.length);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório da folha", e);
        }
    }

    public static void generateTextReport(String content, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Relatório");

            String[] lines = content.split("\\R");

            int rowNum = 0;

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            for (String line : lines) {
                Row row = sheet.createRow(rowNum++);
                Cell cell = row.createCell(0);
                cell.setCellValue(line);

                if (line.contains("RELATÓRIO") || line.contains("FOLHA") || line.contains("RESUMO")) {
                    cell.setCellStyle(titleStyle);
                }
            }

            sheet.autoSizeColumn(0);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Excel", e);
        }
    }

    public static void generatePendingRequests(List<String> requests, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Solicitações Pendentes");

            String[] columns = {
                    "ID", "Promotor", "Tipo", "Valor", "Mensagem", "Status", "Data"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;

            for (String request : requests) {
                String[] parts = request.split("\\|");

                Row row = sheet.createRow(rowNum++);

                for (int i = 0; i < parts.length && i < columns.length; i++) {
                    row.createCell(i).setCellValue(parts[i].trim());
                }
            }

            autoSizeColumns(sheet, columns.length);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório de solicitações pendentes", e);
        }
    }

    public static void generatePixBatch(List<PromoterPaymentData> payments, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("PIX Lote");

            String[] columns = {
                    "Tipo de Inscrição",
                    "Inscrição",
                    "Nome",
                    "Tipo da Chave",
                    "Chave PIX",
                    "Valor",
                    "Data do Pagamento"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            int rowNum = 1;

            for (PromoterPaymentData payment : payments) {
                Row row = sheet.createRow(rowNum++);

                String document = payment.getDocument();
                String name = payment.getName();
                String pix = payment.getPix();
                String pixType = payment.getPixType();

                row.createCell(0).setCellValue(mapRegistrationType(document));
                row.createCell(1).setCellValue(formatDocument(document));
                row.createCell(2).setCellValue(name != null ? name.toUpperCase() : "");
                row.createCell(3).setCellValue(mapPixType(pixType));
                row.createCell(4).setCellValue(formatPix(pix, pixType));
                row.createCell(5).setCellValue(payment.getAmount() != null ? formatMoney(payment.getAmount()) : "");
                row.createCell(6).setCellValue(payment.getPaymentDate().format(formatter));
            }

            autoSizeColumns(sheet, columns.length);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar PIX lote", e);
        }
    }

    public static void generateClients(List<Client> clients, String path) {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Clientes");

            String[] columns = {"Indústria", "CNPJ", "Telefone", "Email", "Vínculo", "Status"};

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row header = sheet.createRow(0);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;

            for (Client client : clients) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(nullToBlank(client.getName()));
                row.createCell(1).setCellValue(nullToBlank(client.getCnpj()));
                row.createCell(2).setCellValue(nullToBlank(client.getPhone()));
                row.createCell(3).setCellValue(nullToBlank(client.getEmail()));
                row.createCell(4).setCellValue(nullToBlank(client.getCompanyLink()));
                row.createCell(5).setCellValue(client.isActive() ? "Ativo" : "Inativo");
            }

            autoSizeColumns(sheet, columns.length);

            try (FileOutputStream fileOut = new FileOutputStream(path)) {
                workbook.write(fileOut);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Excel de clientes", e);
        }
    }

    private static String mapRegistrationType(String document) {
        String clean = onlyNumbers(document);

        if (clean.isBlank()) {
            return "";
        }

        if (clean.length() == 14) {
            return "2";
        }

        if (clean.length() == 11) {
            return "1";
        }

        return "";
    }

    private static String mapPixType(String pixType) {
        if (pixType == null || pixType.isBlank()) {
            return "4";
        }

        return switch (pixType.trim().toUpperCase()) {
            case "TELEFONE" -> "1";
            case "EMAIL" -> "2";
            case "CPF", "CNPJ" -> "3";
            case "ALEATORIA", "ALEATÓRIA" -> "4";
            default -> "4";
        };
    }

    private static String formatDocument(String document) {
        if (document == null) {
            return "";
        }

        String clean = onlyNumbers(document);

        if (clean.length() == 11) {
            return clean.substring(0, 3) + "." +
                    clean.substring(3, 6) + "." +
                    clean.substring(6, 9) + "-" +
                    clean.substring(9);
        }

        if (clean.length() == 14) {
            return clean.substring(0, 2) + "." +
                    clean.substring(2, 5) + "." +
                    clean.substring(5, 8) + "/" +
                    clean.substring(8, 12) + "-" +
                    clean.substring(12);
        }

        return document;
    }

    private static String formatPix(String pix, String pixType) {
        if (pix == null || pix.isBlank()) {
            return "";
        }

        if (pixType == null || pixType.isBlank()) {
            return pix;
        }

        String type = pixType.trim().toUpperCase();
        String clean = onlyNumbers(pix);

        return switch (type) {
            case "TELEFONE" -> formatPhonePix(clean, pix);
            case "CPF", "CNPJ" -> formatDocument(pix);
            case "EMAIL", "ALEATORIA", "ALEATÓRIA" -> pix;
            default -> pix;
        };
    }

    private static String formatPhonePix(String clean, String original) {
        if (clean.length() == 13 && clean.startsWith("55")) {
            return "+" + clean.substring(0, 2) +
                    " (" + clean.substring(2, 4) + ")" +
                    clean.substring(4, 9) +
                    "-" +
                    clean.substring(9);
        }

        if (clean.length() == 11) {
            return "+55 (" + clean.substring(0, 2) + ")" +
                    clean.substring(2, 7) +
                    "-" +
                    clean.substring(7);
        }

        return original;
    }

    private static String formatMoney(BigDecimal value) {
        if (value == null) {
            return "R$ 0,00";
        }

        NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(BR_LOCALE);

        return moneyFormat.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private static String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }

        String clean = onlyNumbers(phone);

        if (clean.length() == 13 && clean.startsWith("55")) {
            return "+" + clean.substring(0, 2) +
                    " (" + clean.substring(2, 4) + ")" +
                    clean.substring(4, 9) +
                    "-" +
                    clean.substring(9);
        }

        if (clean.length() == 11) {
            return "+55 (" + clean.substring(0, 2) + ")" +
                    clean.substring(2, 7) +
                    "-" +
                    clean.substring(7);
        }

        return phone;
    }

    private static String onlyNumbers(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("[^0-9]", "");
    }

    private static String nullToBlank(String value) {
        return value != null ? value : "";
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
        moneyStyle.setDataFormat(dataFormat.getFormat("\"R$\" #,##0.00"));

        return moneyStyle;
    }

    private static void autoSizeColumns(Sheet sheet, int totalColumns) {
        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}