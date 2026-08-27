package com.atpromo.systematpromo.repository;

import com.atpromo.systematpromo.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}