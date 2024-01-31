package extensions

import java.time.LocalDate

/**
 * extends String to provide clear "toDate" with e.g. 22.05.2020
 * See: https://groovy-lang.org/metaprogramming.html#_extension_modules
 */
class StringExtension {

    /**
     * @param s e.g. "22.11.2020"
     * @return Date
     */
    static Date toDate(String s) {
        return DateHelperFunctions._sToD(s)
    }

    static LocalDate toLocalDate(String s) {
        return DateHelperFunctions._sToLD(s)
    }

    /**
     * @param s e.g. "2020-W02"
     * @return Date
     */
    static Date toDateFromYearWeek(String s) {
        return DateHelperFunctions._wToD(s)
    }

    static boolean isYearWeek(String s) {
        return DateHelperFunctions._isYearWeek(s)
    }

    static boolean isGermanDate(String s) {
        return DateHelperFunctions._isGermanDate(s)
    }

    /*
    static LocalDate toLocalDateFromYearWeek(String s) {
        return DateHelperFunctions._wToLD(s)
    }*/

}
