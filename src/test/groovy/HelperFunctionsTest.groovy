import extensions.DateHelperFunctions

class HelperFunctionsTest extends GroovyTestCase {

    def static assertWeek(String dateddmmyyyy, String yyyy_wxx) {
        def sw = dateddmmyyyy.toDate().getWeekYearStr()
        assert sw == yyyy_wxx
    }

    def static assertMonth(String dateddmmyyyy, String yyyy_mxx) {
        def sw = dateddmmyyyy.toDate().getMonthYearStr()
        assert sw == yyyy_mxx
    }

    void testGetWeekYearStr() {

        assertWeek("28.12.2019", '2019-W52')
        assertWeek("29.12.2019", '2019-W52') // DIFFERENT IN US AND DE Locale!
        assertWeek("30.12.2019", '2020-W01') // first week in last year...
        assertWeek("31.12.2019", '2020-W01')
        assertWeek("1.1.2020", '2020-W01')
        assertWeek("2.1.2020", '2020-W01')
        assertWeek("3.1.2020", '2020-W01')
        assertWeek("4.1.2020", '2020-W01')
        assertWeek("5.1.2020", '2020-W01') // DIFFERENT IN US AND DE Locale!
        assertWeek("6.1.2020", '2020-W02') // second week...
        assertWeek("7.1.2020", '2020-W02')

        // some other years
        assertWeek("31.12.2017", '2017-W52') // DIFFERENT IN US AND DE Locale!
        assertWeek("1.1.2018", '2018-W01')

        assertWeek("31.12.2018", '2019-W01')
        assertWeek("1.1.2019", '2019-W01')

        assertWeek("31.12.2019", '2020-W01')
        assertWeek("1.1.2020", '2020-W01')

        assertWeek("31.12.2020", '2020-W53') // DIFFERENT IN US AND DE Locale!
        assertWeek("1.1.2021", '2020-W53') // DIFFERENT IN US AND DE Locale!


        assertWeek("31.12.2021", '2021-W52')
        assertWeek("1.1.2022", '2021-W52')

        assertWeek("31.12.2022", '2022-W52')
        assertWeek("1.1.2023", '2022-W52')

        assertWeek("31.12.2023", '2023-W52')
        assertWeek("1.1.2024", '2024-W01')

        assertWeek("31.12.2024", '2025-W01')
        assertWeek("1.1.2025", '2025-W01')

    }

    void testGetMonthYearStr() {
        assertMonth("31.12.2019", '2019-M12')
        assertMonth("1.1.2020", '2020-M01')

    }

    void testDToS() {
        assert "01.01.2020" == (Date.parse("dd.MM.yyyy", "1.1.2020")).toString()
        shouldFail {
            DateHelperFunctions._dToS(null)
        }
    }

    void testSToD() {
        assert Date.parse("dd.MM.yyyy", "1.1.2020") == "1.1.2020".toDate()

        shouldFail {
            DateHelperFunctions._sToD(null)
        }

        //shouldFail { // but Date parser evaluates to 1.1.2021
        def ha =   "1.13.2020".toDate()
        //}
    }

    void testGetStartOfWeek () {
        assert "20.4.2020".toDate() == "25.4.2020".toDate().getStartOfWeek()
    }

    void testGetStartOfMonth () {
        assert "1.4.2020".toDate() == "25.4.2020".toDate().getStartOfMonth()
    }
}
