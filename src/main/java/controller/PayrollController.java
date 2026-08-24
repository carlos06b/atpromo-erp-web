package controller;

import dao.FinancePromoterDAO;
import dao.PromoterDAO;
import model.FinancePromoter;
import model.PayrollLine;
import model.Promoter;
import model.PromoterPaymentData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PayrollController {

    private final PromoterDAO promoterDAO = new PromoterDAO();
    private final FinancePromoterDAO financePromoterDAO = new FinancePromoterDAO();

    private final Locale BR_LOCALE = new Locale("pt", "BR");
    private final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(BR_LOCALE);

    public List<PayrollLine> generatePayrollLines(LocalDate start, LocalDate end, String promoterType) {
        if (start.isAfter(end)) {
            throw new RuntimeException("Data inicial não pode ser maior que a final.");
        }

        List<Promoter> promoters = promoterDAO.findAll();
        List<PayrollLine> lines = new ArrayList<>();

        for (Promoter promoter : promoters) {
            if (!promoter.isActive()) {
                continue;
            }

            if (!promoterType.equalsIgnoreCase("TODOS")
                    && !promoter.getType().equalsIgnoreCase(promoterType)) {
                continue;
            }

            List<FinancePromoter> launches = financePromoterDAO.findByPromoterAndPeriod(
                    promoter.getId(),
                    start,
                    end
            );

            BigDecimal discounts = BigDecimal.ZERO;

            for (FinancePromoter finance : launches) {
                String type = finance.getType();
                BigDecimal amount = finance.getAmount();

                if (type == null || amount == null) {
                    continue;
                }

                if ("DESCONTO".equalsIgnoreCase(type)) {
                    discounts = discounts.add(amount);
                }
            }

            BigDecimal baseSalary = promoter.getSalary();

            if (baseSalary == null) {
                baseSalary = BigDecimal.ZERO;
            }

            BigDecimal netAmount = baseSalary.subtract(discounts);
            String status = "OK";
            String observation = "Conferido";

            if (baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
                status = "ATENÇÃO";
                observation = "Promotor sem salário/base cadastrado";
            } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
                status = "ATENÇÃO";
                observation = "Descontos maiores que o salário/base";
            }

            lines.add(new PayrollLine(
                    promoter.getId(),
                    promoter.getName(),
                    promoter.getType(),
                    baseSalary,
                    discounts,
                    netAmount,
                    status,
                    observation
            ));
        }

        lines.sort(Comparator.comparing(
                PayrollLine::getPromoterName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));

        return lines;
    }

    public void registerDiscount(int promoterId, BigDecimal amount, LocalDate date, String description) {
        Promoter promoter = promoterDAO.findById(promoterId);

        if (promoter == null) {
            throw new RuntimeException("Promotor não encontrado.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do desconto precisa ser maior que zero.");
        }

        if (date == null) {
            throw new RuntimeException("Informe a data do desconto.");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("Informe o motivo do desconto.");
        }

        FinancePromoter discount = new FinancePromoter();
        discount.setIdPromoter(promoterId);
        discount.setType("DESCONTO");
        discount.setAmount(amount);
        discount.setDescription(description.trim());
        discount.setDate(date);
        discount.setStatus("APLICADO");

        financePromoterDAO.save(discount);
    }

    public void updateDiscount(int discountId, BigDecimal amount, LocalDate date, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("O valor do desconto precisa ser maior que zero.");
        }

        if (date == null) {
            throw new RuntimeException("Informe a data do desconto.");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("Informe o motivo do desconto.");
        }

        financePromoterDAO.updateDiscount(
                discountId,
                amount,
                description.trim(),
                date
        );
    }

    public void deleteDiscount(int discountId) {
        financePromoterDAO.deleteDiscount(discountId);
    }

    public List<String> listDiscounts(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new RuntimeException("Informe a data inicial e final.");
        }

        if (start.isAfter(end)) {
            throw new RuntimeException("Data inicial não pode ser maior que a final.");
        }

        return financePromoterDAO.findDiscountsForPayroll(start, end);
    }



    public String generatePayroll(LocalDate start, LocalDate end, String promoterType) {
        List<PayrollLine> lines = generatePayrollLines(start, end, promoterType);

        if (lines.isEmpty()) {
            return "Nenhum promotor encontrado para esse filtro.";
        }

        StringBuilder report = new StringBuilder();

        report.append("=== FOLHA DE PAGAMENTO ===\n");
        report.append("Período: ")
                .append(formatDate(start))
                .append(" até ")
                .append(formatDate(end))
                .append("\n");

        report.append("Tipo: ").append(promoterType).append("\n");
        report.append("------------------------------------------------------------\n\n");

        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalDesconto = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;

        for (PayrollLine line : lines) {
            totalBase = totalBase.add(line.getBaseSalary());
            totalDesconto = totalDesconto.add(line.getDiscounts());
            totalLiquido = totalLiquido.add(line.getNetAmount());

            report.append("Promotor: ").append(line.getPromoterName()).append("\n");
            report.append("Tipo: ").append(line.getPromoterType()).append("\n");
            report.append("Salário/Base: ").append(formatMoney(line.getBaseSalary())).append("\n");
            report.append("Descontos: ").append(formatMoney(line.getDiscounts())).append("\n");
            report.append("TOTAL LÍQUIDO A PAGAR: ").append(formatMoney(line.getNetAmount())).append("\n");
            report.append("Status: ").append(line.getStatus()).append("\n");
            report.append("Observação: ").append(line.getObservation()).append("\n");
            report.append("------------------------------------------------------------\n");
        }

        report.append("\n=== RESUMO DA FOLHA ===\n");
        report.append("Total Salário/Base: ").append(formatMoney(totalBase)).append("\n");
        report.append("Total Descontos: ").append(formatMoney(totalDesconto)).append("\n");
        report.append("------------------------------------------------------------\n");
        report.append("TOTAL LÍQUIDO DA FOLHA: ").append(formatMoney(totalLiquido)).append("\n");

        return report.toString();
    }

    public List<PromoterPaymentData> getMeiPixBatch(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new RuntimeException("Informe a data de pagamento.");
        }

        List<Promoter> promoters = promoterDAO.findAll();
        List<PromoterPaymentData> payments = new ArrayList<>();

        for (Promoter promoter : promoters) {
            if (!promoter.isActive()) {
                continue;
            }

            if (promoter.getType() == null || !promoter.getType().equalsIgnoreCase("MEI")) {
                continue;
            }

            if (promoter.getPix() == null || promoter.getPix().isBlank()) {
                continue;
            }

            payments.add(
                    new PromoterPaymentData(
                            promoter.getCpf(),
                            promoter.getName(),
                            promoter.getPix(),
                            promoter.getPixType(),
                            null,
                            paymentDate
                    )
            );
        }

        payments.sort(Comparator.comparing(
                PromoterPaymentData::getName,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));

        return payments;
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "R$ 0,00";
        }

        return MONEY_FORMAT.format(value.setScale(2, RoundingMode.HALF_UP))
                .replace('\u00A0', ' ');
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }
}