package ru.a2ps.invoice;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class InvoiceNumberFormatter {
    private InvoiceNumberFormatter() {}

    public static String format(BigDecimal value) {
        DecimalFormat format = new DecimalFormat(
                "#,##0.##",
                DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ru-RU"))
        );
        return format.format(value);
    }
}