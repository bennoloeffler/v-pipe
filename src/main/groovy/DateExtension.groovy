/**
 * extends date to provide clear "toString".
 * See: https://groovy-lang.org/metaprogramming.html#_extension_modules
 */
class DateExtension {
    static String toString(Date d) {
        return HelperFunctions.dToS(d)
    }
}
