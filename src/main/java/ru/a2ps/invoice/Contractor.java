package ru.a2ps.invoice;

import jakarta.persistence.*;

@Entity
@Table(name = "contractors")
public class Contractor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_number", nullable = false)
    private String taxNumber; // ИНН

    private String kppOrOgrnip;

    private String address;

    // Конструктор без параметров
    public Contractor() {
    }

    // Полный конструктор
    public Contractor(String name, String taxNumber, String address, String kppOrOgrnip) {
        this.name = name;
        this.taxNumber = taxNumber;
        this.address = address;
        this.kppOrOgrnip = kppOrOgrnip;
    }


    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getKppOrOgrnip() { return kppOrOgrnip; }
    public void setKppOrOgrnip(String kppOrOgrnip) { this.kppOrOgrnip = kppOrOgrnip; }
}