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

            Contractor c1 = new Contractor("ООО \"СтройТрейд\"", "7810998877", "г. Санкт-Петербург, ул. Ленина, д. 5");
            Contractor c2 = new Contractor("ИП Иванов И.И.", "503211223344", "г. Москва, ул. Тверская, д. 12");
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

        model.addAttribute("invoices", invoiceRepository.findAll());
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
        Invoice invoice = invoiceRepository.findById(id).orElseThrow();
        model.addAttribute("invoice", invoice);
        model.addAttribute("organizations", organizationRepository.findAll());
        model.addAttribute("contractors", contractorRepository.findAll());
        model.addAttribute("nomenclatures", nomenclatureRepository.findAll());
        return "edit-invoice";
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
                InvoiceItem item = new InvoiceItem(invoice, nomenclatureRepository.findById(nomenclatureIds.get(i)).orElseThrow(), quantities.get(i), prices.get(i));
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

    @PostMapping("/contractors/quick-add")
    @ResponseBody
    public Map<String, Object> quickAddContractor(@RequestParam String name,
                                                  @RequestParam(required = false, defaultValue = "—") String taxNumber) {
        Contractor contractor = new Contractor(name, taxNumber, "Не указан");
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

    @GetMapping("/invoice/export/pdf/{id}")
    public void exportToPdf(@PathVariable Long id, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow();

        // 1. Считаем математику сумм и НДС 22% для вывода на бланке
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getQuantity() != null && item.getPrice() != null) {
                total = total.add(item.getQuantity().multiply(item.getPrice()));
            }
        }
        java.math.BigDecimal vat = total.multiply(new java.math.BigDecimal("22")).divide(new java.math.BigDecimal("122"), 2, java.math.RoundingMode.HALF_UP);

        // 2. Переводим число суммы в читаемый текст на русском языке
        long rubles = total.longValue();
        int kopecks = total.subtract(java.math.BigDecimal.valueOf(rubles)).multiply(java.math.BigDecimal.valueOf(100)).intValue();
        String totalInWords = NumberToWordsRu.convert(rubles) + " " + NumberToWordsRu.getRublesDeclension(rubles) + " " + String.format("%02d", kopecks) + " коп.";

        // 3. Подготавливаем контекст для Thymeleaf рендеринга
        org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
        context.setVariable("invoice", invoice);
        context.setVariable("total", total);
        context.setVariable("vat", vat);
        context.setVariable("totalInWords", totalInWords);

        // Настраиваем автономный изолированный движок рендеринга шаблона в строку
        org.thymeleaf.spring6.SpringTemplateEngine templateEngine = new org.thymeleaf.spring6.SpringTemplateEngine();
        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver templateResolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateEngine.setTemplateResolver(templateResolver);

        String htmlContent = templateEngine.process("invoice-pdf", context);

        // 4. Формируем заголовки HTTP ответа для скачивания файла браузером
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Invoice_" + invoice.getInvoiceNumber() + ".pdf");

        // 5. Запуск Flying Saucer рендерера для конвертации HTML + Картинки -> PDF
        // 5. Запуск Flying Saucer рендерера для конвертации HTML + Картинки -> PDF
        try (java.io.OutputStream os = response.getOutputStream()) {
            org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();

            // СТРОГО ОБЯЗАТЕЛЬНО: Регистрируем русский шрифт Arial для поддержки кириллицы
            renderer.getFontResolver().addFont("templates/fonts/Arial.ttf", "Identity-H", true);


            // Задаем корень, чтобы движок понимал, где искать папку static/ с картинками
            String baseUrl = getClass().getClassLoader().getResource("").toString();
            renderer.setDocumentFromString(htmlContent, baseUrl);

            renderer.layout();
            renderer.createPDF(os);
            os.flush();
        }
    }
}
