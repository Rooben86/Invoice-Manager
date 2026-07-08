package ru.a2ps.invoice;

public class NumberToWordsRu {

    private static final String[] units = {
            "", "один", "два", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять",
            "десять", "одиннадцать", "двенадцать", "тринадцать", "четырнадцать", "пятнадцать",
            "шестнадцать", "семнадцать", "восемнадцать", "девятнадцать"
    };

    private static final String[] unitsFem = {
            "", "одна", "две", "три", "четыре", "пять", "шесть", "семь", "восемь", "девять"
    };

    private static final String[] tens = {
            "", "десять", "двадцать", "тридцать", "сорок", "пятьдесят", "шестьдесят", "семьдесят", "восемьдесят", "девяносто"
    };

    private static final String[] hundreds = {
            "", "сто", "двести", "триста", "четыреста", "пятьсот", "шестьсот", "семьсот", "восемьсот", "девятьсот"
    };

    public static String convert(long number) {
        if (number == 0) return "ноль";

        StringBuilder sb = new StringBuilder();

        long billions = (number / 1000000000L) % 1000;
        long millions = (number / 1000000L) % 1000;
        long thousands = (number / 1000L) % 1000;
        long unitsVal = number % 1000;

        // 1. Обработка класса миллиардов
        if (billions > 0) {
            sb.append(convertThreeDigits(billions, false)).append(" ");
            sb.append(getDeclension(billions, "миллиард", "миллиарда", "миллиардов")).append(" ");
        }

        // 2. Обработка класса миллионов (ФИКС ОШИБКИ)
        if (millions > 0) {
            sb.append(convertThreeDigits(millions, false)).append(" ");
            sb.append(getDeclension(millions, "миллион", "миллиона", "миллионов")).append(" ");
        }

        // 3. Обработка класса тысяч
        if (thousands > 0) {
            sb.append(convertThreeDigits(thousands, true)).append(" ");
            sb.append(getDeclension(thousands, "тысяча", "тысячи", "тысяч")).append(" ");
        }

        // 4. Обработка единиц
        if (unitsVal > 0 || sb.length() == 0) {
            sb.append(convertThreeDigits(unitsVal, false));
        }

        String result = sb.toString().trim();
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    private static String convertThreeDigits(long number, boolean isFemale) {
        StringBuilder sb = new StringBuilder();
        int h = (int)(number / 100);
        int t = (int)((number % 100) / 10);
        int u = (int)(number % 10);

        if (h > 0) sb.append(hundreds[h]).append(" ");

        if (t == 1) {
            sb.append(units[t * 10 + u]).append(" ");
        } else {
            if (t > 0) sb.append(tens[t]).append(" ");
            if (u > 0) {
                if (isFemale) sb.append(unitsFem[u]).append(" ");
                else sb.append(units[u]).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public static String getRublesDeclension(long rubles) {
        return getDeclension(rubles, "рубль", "рубля", "рублей");
    }

    // Универсальный метод склонения существительных после числительных
    private static String getDeclension(long n, String form1, String form2, String form5) {
        long lastDigit = n % 10;
        long lastTwoDigits = n % 100;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return form5;
        if (lastDigit == 1) return form1;
        if (lastDigit >= 2 && lastDigit <= 4) return form2;
        return form5;
    }
}