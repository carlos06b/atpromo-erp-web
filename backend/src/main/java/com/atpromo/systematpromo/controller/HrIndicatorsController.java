package com.atpromo.systematpromo.controller;

import com.atpromo.systematpromo.model.Loja;
import com.atpromo.systematpromo.model.Promoter;
import com.atpromo.systematpromo.repository.LojaRepository;
import com.atpromo.systematpromo.repository.PromoterRepository;
import com.atpromo.systematpromo.security.AccessControl;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hr-indicators")
public class HrIndicatorsController {

    private final PromoterRepository promoterRepository;
    private final LojaRepository lojaRepository;
    private final AccessControl accessControl;

    public HrIndicatorsController(PromoterRepository promoterRepository, LojaRepository lojaRepository, AccessControl accessControl) {
        this.promoterRepository = promoterRepository;
        this.lojaRepository = lojaRepository;
        this.accessControl = accessControl;
    }

    @GetMapping
    public ResponseEntity<?> getIndicators(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Integer birthdayMonth,
            Authentication authentication) {

        if (accessControl.isFinance(authentication)) {
            return forbidden();
        }

        if (start.isAfter(end)) {
            return badRequest("Data inicial não pode ser maior que a final.");
        }

        int month = birthdayMonth != null ? birthdayMonth : LocalDate.now().getMonthValue();
        if (month < 1 || month > 12) {
            return badRequest("Mês inválido.");
        }

        List<Promoter> promoters = promoterRepository.findAll();
        Map<Integer, String> lojaNomeById = lojaRepository.findAll().stream()
                .collect(Collectors.toMap(Loja::getId, Loja::getNome));

        long totalActive = promoters.stream().filter(Promoter::isActive).count();

        long admittedInPeriod = promoters.stream()
                .filter(p -> p.getAdmissionDate() != null)
                .filter(p -> !p.getAdmissionDate().isBefore(start) && !p.getAdmissionDate().isAfter(end))
                .count();

        long terminatedInPeriod = promoters.stream()
                .filter(p -> p.getTerminationDate() != null)
                .filter(p -> !p.getTerminationDate().isBefore(start) && !p.getTerminationDate().isAfter(end))
                .count();

        long activeAtStart = activeCountAt(promoters, start.minusDays(1));
        long activeAtEnd = activeCountAt(promoters, end);
        double avgHeadcount = (activeAtStart + activeAtEnd) / 2.0;

        BigDecimal turnoverRate = avgHeadcount > 0
                ? BigDecimal.valueOf(terminatedInPeriod / avgHeadcount * 100).setScale(1, RoundingMode.HALF_UP)
                : null;

        Map<String, Long> byType = groupActiveBy(promoters, p -> labelOrDefault(p.getType(), "Sem tipo"));
        Map<String, Long> byStore = groupActiveBy(promoters, p -> labelOrDefault(lojaNomeById.get(p.getLojaId()), "Sem loja"));
        Map<String, Long> byCompanyLink = groupActiveBy(promoters, p -> labelOrDefault(p.getCompanyLink(), "Sem vínculo"));

        List<BirthdayEntry> birthdays = promoters.stream()
                .filter(Promoter::isActive)
                .filter(p -> p.getDateBirth() != null && p.getDateBirth().getMonthValue() == month)
                .sorted(Comparator.comparing(p -> p.getDateBirth().getDayOfMonth()))
                .map(p -> new BirthdayEntry(p.getId(), p.getName(), p.getDateBirth(), lojaNomeById.get(p.getLojaId()), p.getType()))
                .toList();

        return ResponseEntity.ok(new HrIndicatorsResponse(
                totalActive, admittedInPeriod, terminatedInPeriod, turnoverRate,
                byType, byStore, byCompanyLink, birthdays
        ));
    }

    private long activeCountAt(List<Promoter> promoters, LocalDate date) {
        return promoters.stream()
                .filter(p -> p.getAdmissionDate() != null)
                .filter(p -> !p.getAdmissionDate().isAfter(date))
                .filter(p -> p.getTerminationDate() == null || p.getTerminationDate().isAfter(date))
                .count();
    }

    private Map<String, Long> groupActiveBy(List<Promoter> promoters, Function<Promoter, String> classifier) {
        Map<String, Long> totals = new HashMap<>();
        for (Promoter promoter : promoters) {
            if (!promoter.isActive()) continue;
            totals.merge(classifier.apply(promoter), 1L, Long::sum);
        }
        return totals;
    }

    private String labelOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Você não tem permissão para acessar os indicadores de RH."));
    }

    public record BirthdayEntry(Integer id, String name, LocalDate dateBirth, String store, String type) {}

    public record HrIndicatorsResponse(
            long totalActive,
            long admittedInPeriod,
            long terminatedInPeriod,
            BigDecimal turnoverRate,
            Map<String, Long> byType,
            Map<String, Long> byStore,
            Map<String, Long> byCompanyLink,
            List<BirthdayEntry> birthdays
    ) {}
}