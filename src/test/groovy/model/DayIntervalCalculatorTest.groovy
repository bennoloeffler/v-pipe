package model

import groovy.transform.Canonical

class DayIntervalCalculatorTest extends GroovyTestCase {

    @Canonical
    class StartEndTester implements StartEndInterval {}

    StartEndInterval ci(String start, String end) {
        new StartEndTester(start: start.toDate(), end: end.toDate())
    }

    DayIntervalCalculator dic

    @Override
    void setUp() throws Exception {
        super.setUp()
        dic = new DayIntervalCalculator()
    }

    void testAdaptMinMaxDateOnlyOne() {
        def startEndIntervals = [ci("1.1.1970", "4.7.2018")]
        dic.adaptMinMaxDate(startEndIntervals)
        assert dic.minDate == "1.1.1970".toDate()
        assert dic.maxDate == "4.7.2018".toDate()
    }

    void testAdaptMinMaxDateNone() {
        shouldFail {dic.adaptMinMaxDate([])}
        shouldFail {dic.adaptMinMaxDate(null)}
    }

    void testAdaptMinMaxDateMany() {
        def startEndIntervals = [
                ci("1.1.1970", "4.7.2018"),
                ci("1.1.1974", "4.7.2019")]
        dic.adaptMinMaxDate(startEndIntervals)
        assert dic.minDate == "1.1.1970".toDate()
        assert dic.maxDate == "4.7.2019".toDate()
    }

    void testAdaptMinMaxReturnValue() {
        def startEndIntervals = [
                ci("1.1.1970", "4.7.2018"),
                ci("1.1.1974", "4.7.2019")]
        def interval = dic.adaptMinMaxDate(startEndIntervals)
        assert interval.start == "1.1.1970".toDate()
        assert interval.end == "4.7.2019".toDate()
    }

    void testAdaptMinMaxAdaptServeralTimes() {
        def startEndIntervals = [
                ci("1.1.1970", "4.7.2018"),
                ci("1.1.1974", "4.7.2019")]
        def interval = dic.adaptMinMaxDate(startEndIntervals)
        assert interval.start == "1.1.1970".toDate()
        assert interval.end == "4.7.2019".toDate()

        // adapt the values
        startEndIntervals = [
                ci("1.1.1969", "4.7.2004"),
                ci("1.1.1974", "4.7.1990")]
        interval = dic.adaptMinMaxDate(startEndIntervals)
        assert interval.start == "1.1.1969".toDate() // adapted
        assert interval.end == "4.7.2019".toDate() // NOT adapted

    }
}
