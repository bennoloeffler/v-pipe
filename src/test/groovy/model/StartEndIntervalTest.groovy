package model

import groovy.transform.Canonical

class StartEndIntervalTest extends GroovyTestCase {

    @Canonical
    class StartStopMock implements StartEndInterval {}

    StartEndInterval ci(String start, String end) {
        new StartStopMock(start: start.toDate(), end: end.toDate())
    }

    static void testOverlap(StartEndInterval i1, StartEndInterval i2, int expectedOverlap) {
        assert i1.getOverlapInDays(i2) == expectedOverlap
        assert i2.getOverlapInDays(i1) == expectedOverlap
    }

    void testGetOverlapInside() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("8.2.2020", "9.2.2020"),
                1)
    }

    void testGetOverlapEnd() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("8.2.2020", "15.2.2020"),
                2)
    }

    void testGetOverlapIdentical() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("1.2.2020", "10.2.2020"),
                9)
    }

    void testGetOverlapNoOverlapNoGap() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("10.2.2020", "12.2.2020"),
                0)
    }

    void testGetOverlapDistantZero() {
        testOverlap(
                ci("1.2.2020", "1.2.2020"),
                ci("2.2.2020", "2.2.2020"),
                0)
    }

    void testGetOverlapIdenticalZero() {
        testOverlap(
                ci("1.2.2020", "1.2.2020"),
                ci("1.2.2020", "1.2.2020"),
                0)
    }

    void testGetOverlapLongLeapSecondsProblem() {
        testOverlap(
                ci("1.1.1970", "1.1.1975"),
                ci("1.1.1970", "1.2.2020"),
                365 * 5 + 1) // 1972 is a leap year!
    }
}
