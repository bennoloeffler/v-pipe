package extensions

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

    /**
     * @param s e.g. "2020-W02"
     * @return Date
     */
    static Date toDateFromYearWeek(String s) {
        return DateHelperFunctions._wToD(s)
    }

}
