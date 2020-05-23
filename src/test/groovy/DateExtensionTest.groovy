import extensions.DateHelperFunctions

/**
 * Readable Date format to toString of Date.
 * See class extensions.DateExtension and resources/META-INF/services/org.codehaus.groovy.runtime.ExtensionModule
 */
class DateExtensionTest extends GroovyTestCase {

    void testToString() {
        Date d = DateHelperFunctions._sToD("1.1.2020")
        assert d.toString() == "01.01.2020"
    }

    void testYearWeekToDate() {
        Date d = DateHelperFunctions._wToD("2020-W21")
        assert d.toString() == "18.05.2020"
        d = DateHelperFunctions._wToD("2020-W20")
        assert d.toString() == "11.05.2020"
        d = DateHelperFunctions._wToD("2020-W22")
        assert d.toString() == "25.05.2020"
    }
}
