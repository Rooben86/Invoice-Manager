package ru.a2ps.invoice;

import jakarta.persistence.*;

@Entity
@Table(name = "my_organizations")
public class MyOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "tax_number", nullable = false)
    private String taxNumber; // ИНН

    private String kpp;       // КПП

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "checking_account")
    private String checkingAccount; // Расчетный счет

    private String bic;       // БИК

    // Вместо старых String imageStampPath, imageSignPath, stampPath, signaturePath:

    @Lob
    @org.hibernate.annotations.JdbcType(org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType.class)
    @Column(name = "stamp_data", columnDefinition = "BYTEA")
    private byte[] stampData; // Бинарные данные печати

    @Lob
    @org.hibernate.annotations.JdbcType(org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType.class)
    @Column(name = "signature_data", columnDefinition = "BYTEA")
    private byte[] signatureData; // Бинарные данные подписи

    private String corrAccount; // Корреспондентский счет
    private String address;     // Юридический адрес
    private String phone;       // Телефон компании
    private String ceoName;     // ФИО Генерального директора
    private String cfoName;     // ФИО Главного бухгалтера


    // Конструктор без параметров (обязателен для JPA)
    public MyOrganization() {
    }

    // Полный конструктор для удобной инициализации
    public MyOrganization(String name, String taxNumber, String kpp, String bankName,
                          String checkingAccount, String bic, byte[] stampData, byte[] signatureData) {
        this.name = name;
        this.taxNumber = taxNumber;
        this.kpp = kpp;
        this.bankName = bankName;
        this.checkingAccount = checkingAccount;
        this.bic = bic;
        this.stampData = stampData;
        this.signatureData = signatureData;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getKpp() { return kpp; }
    public void setKpp(String kpp) { this.kpp = kpp; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getCheckingAccount() { return checkingAccount; }
    public void setCheckingAccount(String checkingAccount) { this.checkingAccount = checkingAccount; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public byte[] getStampData() { return stampData; }
    public void setStampData(byte[] stampData) { this.stampData = stampData; }

    public byte[] getSignatureData() { return signatureData; }
    public void setSignatureData(byte[] signatureData) { this.signatureData = signatureData; }

    public String getCorrAccount() { return corrAccount; }
    public void setCorrAccount(String corrAccount) { this.corrAccount = corrAccount; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCeoName() { return ceoName; }
    public void setCeoName(String ceoName) { this.ceoName = ceoName; }
    public String getCfoName() { return cfoName; }
    public void setCfoName(String cfoName) { this.cfoName = cfoName; }

    public String getStampBase64() {
        if (this.stampData == null) return "";
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(this.stampData);
    }

    public String getSignatureBase64() {
        if (this.signatureData == null) return "";
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(this.signatureData);
    }
}