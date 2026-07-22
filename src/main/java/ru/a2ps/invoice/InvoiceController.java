package ru.a2ps.invoice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class InvoiceController {

    @Autowired
    private org.thymeleaf.spring6.SpringTemplateEngine templateEngine;
    private final MyOrganizationRepository organizationRepository;
    private final ContractorRepository contractorRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final InvoiceRepository invoiceRepository;

    public InvoiceController(MyOrganizationRepository organizationRepository,
                             ContractorRepository contractorRepository,
                             NomenclatureRepository nomenclatureRepository,
                             InvoiceRepository invoiceRepository) {
        this.organizationRepository = organizationRepository;
        this.contractorRepository = contractorRepository;
        this.nomenclatureRepository = nomenclatureRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping("/")
    public String viewDashboard(@RequestParam(value = "searchNom", required = false) String searchNom,
                                @RequestParam(value = "contractorId", required = false) Long contractorId,
                                @RequestParam(value = "startDate", required = false) String startDateStr,
                                @RequestParam(value = "endDate", required = false) String endDateStr,
                                Model model) {

        java.time.LocalDate startDate = (startDateStr != null && !startDateStr.isEmpty()) ? java.time.LocalDate.parse(startDateStr) : null;
        java.time.LocalDate endDate = (endDateStr != null && !endDateStr.isEmpty()) ? java.time.LocalDate.parse(endDateStr) : null;

        List<Invoice> invoices = invoiceRepository.findFilteredInvoices(searchNom, contractorId, startDate, endDate);

        java.math.BigDecimal totalFilteredSum = invoices.stream()
                .map(inv -> inv.calculateInvoiceTotal() != null ? inv.calculateInvoiceTotal() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("invoices", invoices);
        model.addAttribute("totalFilteredSum", totalFilteredSum);
        model.addAttribute("currentSearchNom", searchNom);
        model.addAttribute("currentContractorId", contractorId);
        model.addAttribute("currentStartDate", startDateStr); // Возвращаем строку для HTML input
        model.addAttribute("currentEndDate", endDateStr);
        model.addAttribute("contractors", contractorRepository.findAll());
        model.addAttribute("organizations", organizationRepository.findAll());

        return "index";
    }

    // Метод для отдачи списка подсказок номенклатур в формате JSON
    @GetMapping("/api/nomenclatures/search")
    @ResponseBody
    public List<Map<String, Object>> searchNomenclatures(@RequestParam("term") String term) {
        if (term == null || term.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return nomenclatureRepository.findAll().stream()
                .filter(n -> n.getName() != null && n.getName().toLowerCase().contains(term.toLowerCase()))
                .map(n -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", n.getId());
                    map.put("name", n.getName());
                    return map;
                })
                .toList(); // Метод соберет совпадения и вернет JSON-массив для JavaScript
    }

    @GetMapping("/invoice/new")
    public String showCreateInvoiceForm(Model model) {
        long nextId = invoiceRepository.count() + 1;
        String nextInvoiceNumber = String.format("СЧ-%04d", nextId);

        model.addAttribute("nextInvoiceNumber", nextInvoiceNumber);
        model.addAttribute("organizations", organizationRepository.findAll());
        model.addAttribute("contractors", contractorRepository.findAll());
        model.addAttribute("nomenclatures", nomenclatureRepository.findAll());
        return "create-invoice";
    }

    @PostMapping("/invoice/save")
    public String saveNewInvoice(@RequestParam String invoiceNumber,
                                 @RequestParam String issueDate,
                                 @RequestParam Long organizationId,
                                 @RequestParam Long contractorId,
                                 @RequestParam(required = false) String commentary,
                                 @RequestParam(value = "nomenclatureIds", required = false) java.util.List<Long> nomenclatureIds,
                                 @RequestParam(value = "quantities", required = false) java.util.List<java.math.BigDecimal> quantities,
                                 @RequestParam(value = "prices", required = false) java.util.List<java.math.BigDecimal> prices) {

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(java.time.LocalDate.parse(issueDate).atStartOfDay());
        invoice.setOrganization(organizationRepository.findById(organizationId).orElseThrow());
        invoice.setContractor(contractorRepository.findById(contractorId).orElseThrow());
        invoice.setCommentary(commentary);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        if (nomenclatureIds != null) {
            for (int i = 0; i < nomenclatureIds.size(); i++) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(savedInvoice);
                item.setNomenclature(nomenclatureRepository.findById(nomenclatureIds.get(i)).orElseThrow());
                item.setQuantity(quantities.get(i));
                item.setPrice(prices.get(i));
                savedInvoice.getItems().add(item);
            }
            invoiceRepository.save(savedInvoice);
        }

        return "redirect:/invoice/edit/" + savedInvoice.getId();
    }

    @GetMapping("/invoice/edit/{id}")
    public String showEditInvoiceForm(@PathVariable Long id, Model model) {
        try {
            Invoice invoice = invoiceRepository.findById(id).orElseThrow();
            model.addAttribute("invoice", invoice);
            model.addAttribute("organizations", organizationRepository.findAll());
            model.addAttribute("contractors", contractorRepository.findAll());
            model.addAttribute("nomenclatures", nomenclatureRepository.findAll());
            return "edit-invoice";
        } catch (Exception e) {
            System.err.println("❌ Ошибка при открытии страницы редактирования счета:");
            e.printStackTrace();
            return "redirect:/";
        }
    }

    @PostMapping("/invoice/update-saved")
    public String updateExistingInvoice(@RequestParam Long invoiceId,
                                        @RequestParam String invoiceNumber,
                                        @RequestParam String issueDate,
                                        @RequestParam Long organizationId,
                                        @RequestParam Long contractorId,
                                        @RequestParam(required = false) String commentary,
                                        @RequestParam(value = "nomenclatureIds", required = false) java.util.List<Long> nomenclatureIds,
                                        @RequestParam(value = "quantities", required = false) java.util.List<java.math.BigDecimal> quantities,
                                        @RequestParam(value = "prices", required = false) java.util.List<java.math.BigDecimal> prices,
                                        Model model) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(java.time.LocalDate.parse(issueDate).atStartOfDay());
        invoice.setOrganization(organizationRepository.findById(organizationId).orElseThrow());
        invoice.setContractor(contractorRepository.findById(contractorId).orElseThrow());
        invoice.setCommentary(commentary);

        invoice.getItems().clear();
        if (nomenclatureIds != null) {
            for (int i = 0; i < nomenclatureIds.size(); i++) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(invoice);
                item.setNomenclature(nomenclatureRepository.findById(nomenclatureIds.get(i)).orElseThrow());
                item.setQuantity(quantities.get(i));
                item.setPrice(prices.get(i));
                invoice.getItems().add(item);
            }
        }
        Invoice savedInvoice = invoiceRepository.save(invoice);

        model.addAttribute("invoice", savedInvoice);
        model.addAttribute("organizations", organizationRepository.findAll());
        model.addAttribute("contractors", contractorRepository.findAll());
        model.addAttribute("nomenclatures", nomenclatureRepository.findAll());
        model.addAttribute("successMessage", "Изменения успешно сохранены! Вы можете скачать файлы.");
        return "edit-invoice";
    }

    @GetMapping("/invoice/export/pdf/{id}")
    public void exportToPdf(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        try {
            Invoice invoice = invoiceRepository.findById(id).orElseThrow();

            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            for (InvoiceItem item : invoice.getItems()) {
                if (item.getQuantity() != null && item.getPrice() != null) {
                    total = total.add(item.getQuantity().multiply(item.getPrice()));
                }
            }
            java.math.BigDecimal vat = total.multiply(new java.math.BigDecimal("22"))
                    .divide(new java.math.BigDecimal("122"), 2, java.math.RoundingMode.HALF_UP);

            long rubles = total.longValue();
            int kopecks = total.subtract(java.math.BigDecimal.valueOf(rubles)).multiply(java.math.BigDecimal.valueOf(100)).intValue();
            String totalInWords = NumberToWordsRu.convert(rubles) + " " + NumberToWordsRu.getRublesDeclension(rubles) + " " + String.format("%02d", kopecks) + " коп.";

            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("invoice", invoice);
            context.setVariable("total", total);
            context.setVariable("vat", vat);
            context.setVariable("totalInWords", totalInWords);

            String htmlContent = templateEngine.process("invoice-pdf", context);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=Invoice_" + invoice.getInvoiceNumber() + ".pdf");

            // 5. Рендеринг Flying Saucer с подстановкой Base64-картинок из PostgreSQL
            try (java.io.OutputStream os = response.getOutputStream()) {
                org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();

                if (getClass().getClassLoader().getResource("templates/fonts/Arial.ttf") != null) {
                    renderer.getFontResolver().addFont("templates/fonts/Arial.ttf", "Identity-H", true);
                }

                MyOrganization company = invoice.getOrganization();

                // Получаем Base64 строки картинок (эти методы getStampBase64/getSignatureBase64 мы добавили в класс MyOrganization)
                String stampBase64 = company.getStampBase64();
                String signatureBase64 = company.getSignatureBase64();

                // Прямо в HTML-тексте подменяем маркеры на готовые строки Base64
                if (!stampBase64.isEmpty()) {
                    htmlContent = htmlContent.replace("src=\"org-stamp\"", "src=\"" + stampBase64 + "\"");
                }
                if (!signatureBase64.isEmpty()) {
                    htmlContent = htmlContent.replace("src=\"boss-sig\"", "src=\"" + signatureBase64 + "\"");
                    htmlContent = htmlContent.replace("src=\"book-sig\"", "src=\"" + signatureBase64 + "\"");
                }

                String baseUrl = getClass().getClassLoader().getResource("").toString();
                renderer.setDocumentFromString(htmlContent, baseUrl);
                renderer.layout();
                renderer.createPDF(os);
                os.flush();
            }

        } catch (Exception e) {
            System.err.println("❌ Ошибка во время генерации PDF документа:");
            e.printStackTrace();
            throw e;
        }
    }

    @PostMapping("/contractors/quick-add")
    @ResponseBody
    public Map<String, Object> quickAddContractor(@RequestParam String name,
                                                  @RequestParam(required = false, defaultValue = "—") String taxNumber,
                                                  @RequestParam(required = false, defaultValue = "—") String kppOrOgrnip,
                                                  @RequestParam(required = false, defaultValue = "Не указан") String address) {
        // Передаем все 4 параметра в обновленный конструктор сущности
        Contractor contractor = new Contractor(name, taxNumber, address, kppOrOgrnip);
        contractor = contractorRepository.save(contractor);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("id", contractor.getId());
        response.put("name", contractor.getName());
        return response;
    }

    @PostMapping("/nomenclatures/quick-add")
    @ResponseBody
    public Map<String, Object> quickAddNomenclature(@RequestParam String name,
                                                    @RequestParam String unit) {
        Nomenclature nomenclature = new Nomenclature(name, unit, java.math.BigDecimal.ZERO);
        nomenclature = nomenclatureRepository.save(nomenclature);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("id", nomenclature.getId());
        response.put("name", nomenclature.getName());
        response.put("unit", nomenclature.getUnit());
        return response;
    }

    // 1. Открытие страницы управления реквизитами юрлица
    @GetMapping("/organization/edit")
    public String showOrganizationEditForm(Model model) {
        java.util.List<MyOrganization> orgs = organizationRepository.findAll();
        MyOrganization org = orgs.isEmpty() ? null : orgs.get(0);
        model.addAttribute("org", org);
        return "organization-edit";
    }

    @PostMapping("/organization/save")
    public String saveOrganizationSettings(@RequestParam(required = false) Long id,
                                           @RequestParam String name,
                                           @RequestParam String taxNumber,
                                           @RequestParam String kpp,
                                           @RequestParam String bankName,
                                           @RequestParam String checkingAccount,
                                           @RequestParam String bic,
                                           @RequestParam String corrAccount,
                                           @RequestParam String address,
                                           @RequestParam String phone,
                                           @RequestParam String ceoName,
                                           @RequestParam String cfoName,
                                           @RequestParam(required = false) org.springframework.web.multipart.MultipartFile stampFile,
                                           @RequestParam(required = false) org.springframework.web.multipart.MultipartFile signatureFile,
                                           Model model) throws java.io.IOException {
        MyOrganization org;
        if (id != null) {
            org = organizationRepository.findById(id).orElse(new MyOrganization());
        } else {
            org = new MyOrganization();
        }

        org.setName(name);
        org.setTaxNumber(taxNumber);
        org.setKpp(kpp);
        org.setBankName(bankName);
        org.setCheckingAccount(checkingAccount);
        org.setBic(bic);
        org.setCorrAccount(corrAccount);
        org.setAddress(address);
        org.setPhone(phone);
        org.setCeoName(ceoName);
        org.setCfoName(cfoName);

        if (stampFile != null && !stampFile.isEmpty()) {
            org.setStampData(stampFile.getBytes());
        } else if (org.getStampData() == null) {
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("static/stamp.png")) {
                if (is != null) {
                    org.setStampData(is.readAllBytes());
                }
            }
        }

        if (signatureFile != null && !signatureFile.isEmpty()) {
            org.setSignatureData(signatureFile.getBytes());
        } else if (org.getSignatureData() == null) {
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("static/signature.png")) {
                if (is != null) {
                    org.setSignatureData(is.readAllBytes());
                }
            }
        }

        MyOrganization savedOrg = organizationRepository.save(org);
        model.addAttribute("org", savedOrg);
        model.addAttribute("successMessage", "Все реквизиты и файлы факсимиле успешно обновлены в БД!");
        return "organization-edit";
    }

    // 1. Просмотр списка контрагентов
    @GetMapping("/contractors")
    public String viewContractorsList(Model model) {
        model.addAttribute("contractors", contractorRepository.findAll());
        return "contractors";
    }

    // 2. Форма редактирования контрагента
    @GetMapping("/contractors/edit/{id}")
    public String showEditContractorForm(@PathVariable Long id, Model model) {
        Contractor contractor = contractorRepository.findById(id).orElseThrow();
        model.addAttribute("contractor", contractor);
        return "contractor-edit";
    }

    // 3. Сохранение изменений контрагента
    @PostMapping("/contractors/update")
    public String updateContractor(@RequestParam Long id,
                                   @RequestParam String name,
                                   @RequestParam String taxNumber,
                                   @RequestParam String kppOrOgrnip,
                                   @RequestParam String address) {
        Contractor contractor = contractorRepository.findById(id).orElseThrow();
        contractor.setName(name);
        contractor.setTaxNumber(taxNumber);
        contractor.setKppOrOgrnip(kppOrOgrnip);
        contractor.setAddress(address);
        contractorRepository.save(contractor);
        return "redirect:/contractors";
    }

    // 4. Просмотр списка номенклатуры
    @GetMapping("/nomenclatures")
    public String viewNomenclaturesList(Model model) {
        model.addAttribute("nomenclatures", nomenclatureRepository.findAll());
        return "nomenclatures";
    }

    // 5. Форма редактирования номенклатуры
    @GetMapping("/nomenclatures/edit/{id}")
    public String showEditNomenclatureForm(@PathVariable Long id, Model model) {
        Nomenclature nomenclature = nomenclatureRepository.findById(id).orElseThrow();
        model.addAttribute("nomenclature", nomenclature);
        return "nomenclature-edit";
    }

    // 6. Сохранение изменений номенклатуры
    @PostMapping("/nomenclatures/update")
    public String updateNomenclature(@RequestParam Long id,
                                     @RequestParam String name,
                                     @RequestParam String unit,
                                     @RequestParam java.math.BigDecimal defaultPrice) {
        Nomenclature nomenclature = nomenclatureRepository.findById(id).orElseThrow();
        nomenclature.setName(name);
        nomenclature.setUnit(unit);
        nomenclature.setDefaultPrice(defaultPrice);
        nomenclatureRepository.save(nomenclature);
        return "redirect:/nomenclatures";
    }

    @GetMapping("/invoice/update-shipment-date") // Изменили POST на GET
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> updateShipmentDate(@RequestParam Long invoiceId,
                                                                              @RequestParam String shipmentDate) {
        try {
            Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow();
            if (shipmentDate == null || shipmentDate.trim().isEmpty()) {
                invoice.setShipmentDate(null);
            } else {
                invoice.setShipmentDate(java.time.LocalDate.parse(shipmentDate));
            }
            invoiceRepository.save(invoice);
            return org.springframework.http.ResponseEntity.ok("Дата успешно обновлена");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body("Ошибка: " + e.getMessage());
        }
    }
}