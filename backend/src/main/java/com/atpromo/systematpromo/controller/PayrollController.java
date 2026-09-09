package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.FinancePromoter;
import com.atpromo.systematpromo.model.PayrollLine;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.model.PromoterPaymentData;
import com.atpromo.systematpromo.repository.FinancePromoterRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PromoterRepository promoterRepository;
    private final FinancePromoterRepository financePromoterRepository;

    public PayrollController(PromoterRepository promoterRepository, FinancePromoterRepository financePromoterRepository) {
        this.promoterRepository = promoterRepository;
        this.financePromoterRepository = financePromoterRepository;
    }

    @GetMapping("/lines")
    public List<PayrollLine> generatePayrollLines(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "TODOS") String type) {

        if (start.isAfter(end)) {
            throw new RuntimeException("Data inicial não pode ser maior que a final.");
        }

        List<Promoter> promoters = promoterRepository.findAll();
        List<PayrollLine> lines = new ArrayList<>();

        for (Promoter promoter : promoters) {
            if (!promoter.isActive()) {
                continue;
            }

            if (!type.equalsIgnoreCase("TODOS") && !type.equalsIgnoreCase(promoter.getType())) {
                continue;
            }

            List<FinancePromoter> launches = financePromoterRepository.findAll().stream()
                    .filter(f -> f.getIdPromoter() == promoter.getId()
                            && f.getDate() != null
                            && !f.getDate().isBefore(start)
                            && !f.getDate().isAfter(end))
                    .toList();

            BigDecimal discounts = BigDecimal.ZERO;
            for (FinancePromoter finance : launches) {
                if ("DESCONTO".equalsIgnoreCase(finance.getType()) && finance.getAmount() != null) {
                    discounts = discounts.add(finance.getAmount());
                }
            }

            BigDecimal baseSalary = promoter.getSalary() != null ? promoter.getSalary() : BigDecimal.ZERO;
            BigDecimal netAmount = baseSalary.subtract(discounts);

            String status = "OK";
            String observation = "Conferido";

            if (baseSalary.compareTo(BigDecimal.ZERO) <= 0) {
                status = "ATENCAO";
                observation = "Promotor sem salario/base cadastrado";
            } else if (netAmount.compareTo(BigDecimal.ZERO) < 0) {
                status = "ATENCAO";
                observation = "Descontos maiores que o salario/base";
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

        lines.sort(Comparator.comparing(PayrollLine::getPromoterName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return lines;
    }

    @GetMapping("/pix-batch")
    public List<PromoterPaymentData> getMeiPixBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate) {

        List<Promoter> promoters = promoterRepository.findAll();
        List<PromoterPaymentData> payments = new ArrayList<>();

        for (Promoter promoter : promoters) {
            if (!promoter.isActive()) continue;
            if (promoter.getType() == null || !promoter.getType().equalsIgnoreCase("MEI")) continue;
            if (promoter.getPix() == null || promoter.getPix().isBlank()) continue;

            payments.add(new PromoterPaymentData(
                    promoter.getCpf(),
                    promoter.getName(),
                    promoter.getPix(),
                    promoter.getPixType(),
                    null,
                    paymentDate
            ));
        }

        payments.sort(Comparator.comparing(PromoterPaymentData::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return payments;
    }
}