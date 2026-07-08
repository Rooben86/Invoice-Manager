package ru.a2ps.invoice;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@Controller
public class InvoiceController {

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
    public String viewDashboard(Model model) {
        if (organizationRepository.count() == 0) {
            MyOrganization org = new MyOrganization("ООО \"Альфа-Плюс\"", "7701234567", "770101001",
                    "ПАО СБЕРБАНК", "40702810900000001234", "044525225", "static/stamp.png", "static/signature.png");
            organizationRepository.save(org);

            Contractor c1 = new Contractor("ООО \"СтройТрейд\"", "7810998877", "", "г. Санкт-Петербург, ул. Ленина, д. 5");
            Contractor c2 = new Contractor("ИП Иванов И.И.", "503211223344", "", "г. Москва, ул. Тверская, д. 12");
            contractorRepository.save(c1);
            contractorRepository.save(c2);

            Nomenclature n1 = new Nomenclature("Фанера ФК 15мм 1525х1525", "лист", new java.math.BigDecimal("1250.00"));
            Nomenclature n2 = new Nomenclature("Доска обрезная 50х150х6000", "куб.м", new java.math.BigDecimal("18500.00"));
            Nomenclature n3 = new Nomenclature("Саморезы по дереву 3.5х41", "кг", new java.math.BigDecimal("450.00"));
            nomenclatureRepository.save(n1);
            nomenclatureRepository.save(n2);
            nomenclatureRepository.save(n3);

            Invoice testInvoice = new Invoice("СЧ-0001", java.time.LocalDateTime.now(), org, c1, "Новый");
            invoiceRepository.save(testInvoice);
        }

        // Сортируем: сначала по дате по убыванию (от новых к старым), затем по номеру счета
//        model.addAttribute("invoices", invoiceRepository.findAllSortedByDateAndNumberDesc(
//                org.springframework.data.domain.Sort.by(
//                        org.springframework.data.domain.Sort.Order.desc("issueDate"),
//                        org.springframework.data.domain.Sort.Order.desc("invoiceNumber")
//                )
//        ));
        model.addAttribute("invoices", invoiceRepository.findAllSortedByDateAndNumberDesc());
        model.addAttribute("contractors", contractorRepository.findAll());
        model.addAttribute("organizations", organizationRepository.findAll());
        return "index";
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
        invoice.setStatus(commentary);

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
        invoice.setStatus(commentary);

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

            org.thymeleaf.spring6.SpringTemplateEngine templateEngine = new org.thymeleaf.spring6.SpringTemplateEngine();
            org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
            templateResolver.setPrefix("templates/");
            templateResolver.setSuffix(".html");
            templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
            templateResolver.setCharacterEncoding("UTF-8");
            templateEngine.setTemplateResolver(templateResolver);

            String htmlContent = templateEngine.process("invoice-pdf", context);

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "inline; filename=Invoice_" + invoice.getInvoiceNumber() + ".pdf");

            // 5. Рендеринг Flying Saucer с принудительной подстановкой динамических картинок
            try (java.io.OutputStream os = response.getOutputStream()) {
                org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();

                if (getClass().getClassLoader().getResource("templates/fonts/Arial.ttf") != null) {
                    renderer.getFontResolver().addFont("templates/fonts/Arial.ttf", "Identity-H", true);
                }

                // Передаем HTML-контент в движок
                String baseUrl = getClass().getClassLoader().getResource("").toString();
                renderer.setDocumentFromString(htmlContent, baseUrl);

                // ХАК ДЛЯ MAC: Принудительно связываем кодовые имена картинок с файлами из PostgreSQL настроек юрлица
                String rawStamp = invoice.getOrganization().getStampPath();
                String rawSig = invoice.getOrganization().getSignaturePath();

                // Если пути не заданы, берем дефолтные файлы
                String stampPath = (rawStamp != null) ? rawStamp : "static/stamp.png";
                String signaturePath = (rawSig != null) ? rawSig : "static/signature.png";

                // Загружаем файлы картинок в память рендерера
                java.net.URL stampUrl = getClass().getClassLoader().getResource(stampPath);
                java.net.URL sigUrl = getClass().getClassLoader().getResource(signaturePath);

                if (stampUrl != null) {
                    renderer.getSharedContext().getUserAgentCallback().setBaseURL(stampUrl.toString());
                    htmlContent = htmlContent.replace("src=\"org-stamp\"", "src=\"" + stampUrl.toString() + "\"");
                }
                if (sigUrl != null) {
                    htmlContent = htmlContent.replace("src=\"boss-sig\"", "src=\"" + sigUrl.toString() + "\"");
                    htmlContent = htmlContent.replace("src=\"book-sig\"", "src=\"" + sigUrl.toString() + "\"");
                }

                // Перезаписываем документ с уже вшитыми полными путями к новым файлам
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

        // Папка на Mac для сохранения картинок внутри ресурсов
        String uploadDir = new java.io.File("src/main/resources/static/").getAbsolutePath();

        if (stampFile != null && !stampFile.isEmpty()) {
            String fileName = "stamp_" + System.currentTimeMillis() + ".png";
            stampFile.transferTo(new java.io.File(uploadDir + "/" + fileName));
            org.setStampPath("static/" + fileName);
        } else if (org.getStampPath() == null) {
            org.setStampPath("static/stamp.png");
        }

        if (signatureFile != null && !signatureFile.isEmpty()) {
            String fileName = "sig_" + System.currentTimeMillis() + ".png";
            signatureFile.transferTo(new java.io.File(uploadDir + "/" + fileName));
            org.setSignaturePath("static/" + fileName);
        } else if (org.getSignaturePath() == null) {
            org.setSignaturePath("static/signature.png");
        }

        MyOrganization savedOrg = organizationRepository.save(org);
        model.addAttribute("org", savedOrg);
        model.addAttribute("successMessage", "Все реквизиты и файлы факсимиле успешно обновлены!");
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

}

