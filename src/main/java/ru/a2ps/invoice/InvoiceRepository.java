package ru.a2ps.invoice;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    // Заменили ::date на стандартный CAST(i.issue_date AS date) во избежание конфликта со Spring
    @Query(value = "SELECT * FROM invoices i " +
            "ORDER BY CAST(i.issue_date AS date) DESC, " +
            "CAST(COALESCE(NULLIF(REGEXP_REPLACE(i.invoice_number, '[^0-9]', '', 'g'), ''), '0') AS INTEGER) DESC",
            nativeQuery = true)
    List<Invoice> findAllSortedByDateAndNumberDesc();
}
