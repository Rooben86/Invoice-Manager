package ru.a2ps.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT DISTINCT i FROM Invoice i " +
            "LEFT JOIN i.items ii " +
            "WHERE (:searchNom IS NULL OR :searchNom = '' OR LOWER(ii.nomenclature.name) LIKE LOWER(CONCAT('%', :searchNom, '%'))) " +
            "AND (:contractorId IS NULL OR i.contractor.id = :contractorId) " +
            "ORDER BY i.issueDate DESC, i.invoiceNumber DESC")
    List<Invoice> findFilteredInvoices(@Param("searchNom") String searchNom,
                                       @Param("contractorId") Long contractorId);
}
