package model

import groovy.transform.Canonical
import modelNew.DayIntervalCalculator
import modelNew.StartEndInterval
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DayIntervalCalculatorTest extends Assertions {

    @Canonical
    class StartEndTester implements StartEndInterval {}

    StartEndInterval ci(String start, String end) {
        new StartEndTester(start: start.toDate(), end: end.toDate())
    }

    DayIntervalCalculator dic

    @BeforeEach
    void setUp() throws Exception {
        //super.setUp()
        dic = new DayIntervalCalculator()
    }

    @Test
    void testAdaptMinMaxDateOnlyOne() {
        def startEndIntervals = [ci("1.1.1970", "4.7.2018")]
        dic.expandMinMaxDate(startEndIntervals)
        assert dic.minDate == "1.1.1970".toDate()
        assert dic.maxDate == "4.7.2018".toDate()
    }

    @Test
    void testAdaptMinMaxDateNone() {
        assertThrows(Throwable.class) {dic.expandMinMaxDate([])}
        assertThrows(Throwable) {dic.expandMinMaxDate(null)}
    }

    @Test
    void testAdaptMinMaxDateMany() {
        def startEndIntervals = [
                ci("1.1.1970", "4.7.2018"),
                ci("1.1.1974", "4.7.2019")]
        dic.expandMinMaxDate(startEndIntervals)
        assert dic.minDate == "1.1.1970".toDate()
        assert dic.maxDate == "4.7.2019".toDate()
    }

    @Test
    void testAdaptMinMaxReturnValue() {
        def startEndIntervals = [
                ci("1.1.1970", "4.7.2018"),
                ci("1.1.1974", "4.7.2019")]
        def interval = dic.expandMinMaxDate(startEndIntervals)
        assert interval.start == "1.1.1970".toDate()
        assert interval.end == "4.7.2019".toDate()
    }

    @Test
    void testAdaptMinMaxAdaptServeralTimes() {
        def startEndIntervals = [
                ci("1.1.1970", "4.7.2018"),
                ci("1.1.1974", "4.7.2019")]
        def interval = dic.expandMinMaxDate(startEndIntervals)
        assert interval.start == "1.1.1970".toDate()
        assert interval.end == "4.7.2019".toDate()

        // adapt the values
        startEndIntervals = [
                ci("1.1.1969", "4.7.2004"),
                ci("1.1.1974", "4.7.1990")]
        interval = dic.expandMinMaxDate(startEndIntervals)
        assert interval.start == "1.1.1969".toDate() // adapted
        assert interval.end == "4.7.2019".toDate() // NOT adapted

    }
}
