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
}
