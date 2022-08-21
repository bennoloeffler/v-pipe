package model

import groovy.transform.Canonical
import modelNew.StartEndInterval
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StartEndIntervalTest extends Assertions {

    @Canonical
    class StartStopMock implements StartEndInterval {}

    StartEndInterval ci(String start, String end) {
        new StartStopMock(start: start.toDate(), end: end.toDate())
    }

    static void testOverlap(StartEndInterval i1, StartEndInterval i2, int expectedOverlap) {
        assert i1.getOverlapInDays(i2) == expectedOverlap
        assert i2.getOverlapInDays(i1) == expectedOverlap
    }

    @Test
    void testGetOverlapInside() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("8.2.2020", "9.2.2020"),
                1)
    }

    @Test
    void testGetOverlapEnd() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("8.2.2020", "15.2.2020"),
                2)
    }

    @Test
    void testGetOverlapIdentical() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("1.2.2020", "10.2.2020"),
                9)
    }

    @Test
    void testGetOverlapNoOverlapNoGap() {
        testOverlap(
                ci("1.2.2020", "10.2.2020"),
                ci("10.2.2020", "12.2.2020"),
                0)
    }

    @Test
    void testGetOverlapDistantZero() {
        testOverlap(
                ci("1.2.2020", "1.2.2020"),
                ci("2.2.2020", "2.2.2020"),
                0)
    }

    @Test
    void testGetOverlapIdenticalZero() {
        testOverlap(
                ci("1.2.2020", "1.2.2020"),
                ci("1.2.2020", "1.2.2020"),
                0)
    }

    @Test
    void testGetOverlapLongLeapSecondsProblem() {
        testOverlap(
                ci("1.1.1970", "1.1.1975"),
                ci("1.1.1970", "1.2.2020"),
                365 * 5 + 1) // 1972 is a leap year!
    }
}
