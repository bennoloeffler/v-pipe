import static HelperFunctions.*

class TaskInProjectTest extends GroovyTestCase {

    // CUT
    TaskInProject t1, t2, t3, t4

    void setUp() {
        super.setUp()
        t1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2 = t("p1", "29.1.2020", "3.2.2020", "d1", 20.0)
    }

    void tearDown() {
    }

    void testGetDaysOverlap() {
        long overlap = t1.getDaysOverlap(sToD("3.1.2020"), sToD("6.1.2020"))
        assert overlap == 1

        // overlap of external window
        overlap = t1.getDaysOverlap(sToD("3.1.2020"), sToD("12.1.2020"))
        assert overlap == 5

        // startExternal exact, endExternal longer
        overlap = t1.getDaysOverlap(sToD("5.1.2020"), sToD("12.1.2020"))
        assert overlap == 5

        // startExternal earlier, end exactly
        overlap = t1.getDaysOverlap(sToD("1.1.2020"), sToD("10.1.2020"))
        assert overlap == 5

        // startExternal earlier, end exactly
        overlap = t1.getDaysOverlap(sToD("1.1.2020"), sToD("10.1.2020"))
        assert overlap == 5

        // identical
        overlap = t1.getDaysOverlap(sToD("5.1.2020"), sToD("10.1.2020"))
        assert overlap == 5

        // external smaller
        overlap = t1.getDaysOverlap(sToD("6.1.2020"), sToD("9.1.2020"))
        assert overlap == 3
    }

    void testCapacityPerDay() {
        assert 20 / 5 == t1.capaPerDay
    }

    void testGetCapaNeeded() {

        // 20 capa per 5 days (5th inclusive, 10th exclusive) equals 4 capa per day
        // 6 to 9 = 6_7_8 = 3 days. Capa needed = 4 * 3 = 12
        double capaExpected = 20 / 5 * 3
        assert capaExpected == 12
        assert t1.getCapaNeeded(sToD("6.1.2020"), sToD("9.1.2020")) == capaExpected
    }

    void testGetCapaMap () {
        Map<String, Double> map = t1.getCapaDemandSplitInWeeks()
        // t1 = ("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        assert map != null
        assert map["2020-W01"] == 4
        assert map["2020-W02"] == 16

        map = t2.getCapaDemandSplitInWeeks()
        // t1 = ("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        assert map != null
        assert map["2020-W05"] == 20
    }


}
