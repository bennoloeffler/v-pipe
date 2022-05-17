package extensions

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import org.joda.time.DateTime

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalField
import java.time.temporal.WeekFields

/**
 * Implementation of helper functions.
 * For good accessibility, they are added to Date and String.
 * See DateExtension StringExtension.
 */
@CompileStatic
class DateHelperFunctions {

    static Calendar cal = Calendar.getInstance(Locale.GERMANY)
    static TemporalField woy = WeekFields.of(Locale.GERMANY).weekOfWeekBasedYear();


    @Memoized
    static LocalDate getStartOfWeek(LocalDate d) {
        d.with(DayOfWeek.MONDAY)
    }

    /**
     * Monday of the week of d
     * @param d
     * @return monday
     */
    @Memoized
    static Date _getStartOfWeek(Date d) {
        def dt = new DateTime(d)
        def startOfWeek = dt.withDayOfWeek(1)
        startOfWeek.toDate()
    }


    @Memoized
    static LocalDate getStartOfMonth(LocalDate d) {
        d.withDayOfMonth(1)
    }

    /**
     *
     * @param d
     * @return the first of the month
     */
    @Memoized
    static Date _getStartOfMonth(Date d) {
        def dt = new DateTime(d)
        def startOfMonth = dt.withDayOfMonth(1)
        startOfMonth.toDate()
    }

    @Memoized
    static String _getWeekYearStr(LocalDate d) {
        int year = d.getYear()

        int month = d.getMonthValue() -1
        int week = d.get(woy);

        // correct the last days in december
        // those days belong already to the first week of the next year
        // or sometimes vice verca

        if (month == 11 && week == 1) {
            year++
        }
        if (month == 0 && week == 52) {
            year--
        }
        if (month == 0 && week == 53) {
            year--
        }
        "$year-W${week < 10 ? "0" : ""}$week"
    }

    /**
     * Creates a unique, sortable key for weeks of year based on days d: e.g. "2020-W11"
     * @param d Date
     * @return String like "2020-W01", with the last days after week 52 in 2019 looking like "2020-W01"
     */
    @Memoized
    static String _getWeekYearStr(Date d) {
        cal.setTime(d)
        int year = cal.get(Calendar.YEAR)
        int month = cal.get(Calendar.MONTH)
        int week = cal.get(Calendar.WEEK_OF_YEAR)
        // correct the last days in december
        // those days belong already to the first week of the next year
        // or sometimes vice verca
        if (month == 11 && week == 1) {
            year++
        }
        if (month == 0 && week == 52) {
            year--
        }
        if (month == 0 && week == 53) {
            year--
        }
        "$year-W${week < 10 ? "0" : ""}$week"
    }




    @Memoized
    static int _getCalWeek(Date d) {
        cal.setTime(d)
        cal.get(Calendar.WEEK_OF_YEAR)
    }

    @Memoized
    static int _getCalWeek(LocalDate d) {
        d.get(woy);
    }

    /**
     * Creates a unique, sortable key for months of year based on days d: e.g. "2020-M3"
     * @param d Date
     * @return String like "2020-M11", where 1..Jan, 12..Dec
     */
    @Memoized
    static String _getMonthYearStr(Date d) {
        cal.setTime(d)
        int year = cal.get(Calendar.YEAR)
        int month = cal.get(Calendar.MONTH) + 1
        "$year-M${month < 10 ? "0" : ""}$month"
    }

    @Memoized
    static String _getMonthYearStr(LocalDate d) {
        int year = d.getYear()
        int month = d.getMonthValue()
        "$year-M${month < 10 ? "0" : ""}$month"
    }

    /**
     * @param d
     * @return "dd.MM.yyyy" (01.01.2020)
     */
    @Memoized
    static String _dToS(Date d) {
        assert d != null
        d.format("dd.MM.yyyy")
    }

    /**
     * @param s "dd.MM.yyyy"
     * @return Date
     */
    @Memoized
    static Date _sToD(String s) {
        assert s != null
        Date.parse("dd.MM.yyyy", s)
    }

    @Memoized
    static LocalDate _sToLD(String s) {
        assert s != null
        LocalDate.parse(s, "d.M.yyyy")
    }

    /**
     * @param s "yyyy-Www" 2020-W01
     * @return Date
     */
    @Memoized
    static Date _wToD(String s) {
        assert s != null
        int year = s[0..3].toInteger()
        int week = s[6..7].toInteger()

        cal.setTimeInMillis(0)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.WEEK_OF_YEAR, week)

        _getStartOfWeek(cal.getTime())
    }

}




