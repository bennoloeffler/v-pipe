
/**
 * Readable Date format to toString of Date.
 * See class DateExtension and resources/META-INF/services/org.codehaus.groovy.runtime.ExtensionModule
 */
class DateExtensionTest extends GroovyTestCase {

    void testToString() {
        Date d = HelperFunctions.sToD("1.1.2020")
        assert d.toString() == "01.01.2020"
    }
}
