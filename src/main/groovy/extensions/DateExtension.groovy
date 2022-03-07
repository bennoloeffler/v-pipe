package extensions

import groovy.transform.CompileStatic

import java.time.LocalDate
import java.time.ZoneId

/**
 * extends date to provide clear "toString".
 * See: https://groovy-lang.org/metaprogramming.html#_extension_modules
 */
class DateExtension {

    static String toString(Date d) {
        return DateHelperFunctions._dToS(d)
    }

    static Date getStartOfWeek(Date d) {
        return DateHelperFunctions._getStartOfWeek(d)
    }

    static Date getStartOfMonth(Date d) {
        return DateHelperFunctions._getStartOfMonth(d)
    }

    /**
     * Creates a unique, sortable key for weeks of year based on days d: e.g. "2020-W11"
     * @param d Date
     * @return String like "2020-W01", with the last days after week 52 in 2019 looking like "2020-W01"
     */
    static String getWeekYearStr(Date d) {
        return DateHelperFunctions._getWeekYearStr(d)
    }

    static String getMonthYearStr(Date d) {
        return DateHelperFunctions._getMonthYearStr(d)
    }

    @CompileStatic
    static LocalDate convertToLocalDate(Date dateToConvert) {
        dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
    }

    static Date convertToDate(LocalDate dateToConvert) {
        Date.from(dateToConvert.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant())
    }
}
