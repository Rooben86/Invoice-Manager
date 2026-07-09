package ru.a2ps.invoice;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDateTime issueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private MyOrganization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private Contractor contractor;

    private String status;

    private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {}

    public Invoice(String invoiceNumber, LocalDateTime issueDate, MyOrganization organization, Contractor contractor, String status) {
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.organization = organization;
        this.contractor = contractor;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public LocalDateTime getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDateTime issueDate) { this.issueDate = issueDate; }
    public MyOrganization getOrganization() { return organization; }
    public void setOrganization(MyOrganization organization) { this.organization = organization; }
    public Contractor getContractor() { return contractor; }
    public void setContractor(Contractor contractor) { this.contractor = contractor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }

    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(java.math.BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}