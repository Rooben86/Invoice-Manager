package ru.a2ps.invoice;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "nomenclatures")
public class Nomenclature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit; // шт, кг, уп, куб.м

    @Column(name = "default_price", nullable = false)
    private BigDecimal defaultPrice;

    // Конструктор без параметров
    public Nomenclature() {
    }

    // Полный конструктор
    public Nomenclature(String name, String unit, BigDecimal defaultPrice) {
        this.name = name;
        this.unit = unit;
        this.defaultPrice = defaultPrice;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getDefaultPrice() { return defaultPrice; }
    public void setDefaultPrice(BigDecimal defaultPrice) { this.defaultPrice = defaultPrice; }
}