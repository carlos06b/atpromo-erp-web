package controller;

import dao.FinancePromoterDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class FinanceController {

    private FinancePromoterDAO dao = new FinancePromoterDAO();

    public void listByPeriod(LocalDate start, LocalDate end) {

        List<String> list = dao.findByPeriodWithPromoterName(start, end);

        if (list.isEmpty()) {
            System.out.println("Nenhum registro financeiro nesse período.");
            return;
        }

        for (String line : list) {
            System.out.println(line);
        }
    }

    public void showReportByPeriod(LocalDate start, LocalDate end) {

        System.out.println("\n=== RELATÓRIO FINANCEIRO ===");
        System.out.println("Período: " + formatDate(start) + " até " + formatDate(end));
        System.out.println("--------------------------------");

        listByPeriod(start, end);

        Map<String, BigDecimal> totals = dao.getTotalByTypeAndPeriod(start, end);

        BigDecimal totalGastos = BigDecimal.ZERO;

        String[] tiposGasto = {
                "BONIFICACAO",
                "AJUDA_CUSTO",
                "ASO",
                "EPI"
        };

        for (String type : tiposGasto) {
            totalGastos = totalGastos.add(
                    totals.getOrDefault(type, BigDecimal.ZERO)
            );
        }

        BigDecimal descontos = totals.getOrDefault("DESCONTO", BigDecimal.ZERO);

        System.out.println("--------------------------------");
        System.out.println("Descontos aplicados: R$ " + descontos);
        System.out.println("--------------------------------");
        System.out.println("TOTAL DE GASTOS: R$ " + totalGastos);
    }

    public void showReportByTypeAndPeriod(LocalDate start, LocalDate end) {

        Map<String, BigDecimal> totals = dao.getTotalByTypeAndPeriod(start, end);

        System.out.println("\n=== RELATÓRIO POR TIPO ===");
        System.out.println("Período: " + formatDate(start) + " até " + formatDate(end));
        System.out.println("--------------------------------");

        BigDecimal totalGastos = BigDecimal.ZERO;

        String[] tiposGasto = {
                "BONIFICACAO",
                "AJUDA_CUSTO",
                "ASO",
                "EPI"
        };

        for (String type : tiposGasto) {

            BigDecimal value = totals.getOrDefault(type, BigDecimal.ZERO);

            System.out.println(formatType(type) + ": R$ " + value);

            totalGastos = totalGastos.add(value);
        }

        // 👇 desconto separado (não entra no total)
        BigDecimal descontos = totals.getOrDefault("DESCONTO", BigDecimal.ZERO);

        System.out.println("--------------------------------");
        System.out.println("Descontos aplicados: R$ " + descontos);
        System.out.println("--------------------------------");
        System.out.println("TOTAL DE GASTOS: R$ " + totalGastos);
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    private String formatType(String type) {
        return switch (type) {
            case "BONIFICACAO" -> "Bonificação";
            case "AJUDA_CUSTO" -> "Ajuda de Custo";
            case "DESCONTO" -> "Desconto";
            case "ASO" -> "ASO";
            case "EPI" -> "EPI";
            default -> type;
        };
    }
}