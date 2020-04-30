import static HelperFunctions.*

class ProjectDataToLoadTransformerTest extends GroovyTestCase {

    ProjectDataToLoadTransformer tr
    TaskInProject t1p1, t2p1, t1p2, t2p2, t3p1

    void setUp() {
        super.setUp()
        tr = new ProjectDataToLoadTransformer()
        t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = t("p2", "8.1.2020", "9.1.2020", "d2", 20.0)
        t3p1 = t("p3", "8.2.2020", "9.2.2020", "d3", 20.0)
        tr.taskList = [t1p1, t2p1, t1p2, t2p2,t3p1]
    }

    //void tearDown() {}

    void testGetProject() {
        def p1 = tr.getProject("p1")
        def p2 = tr.getProject("p2")
        assert p1 == [t1p1, t2p1]
        assert p2 == [t1p2, t2p2]
    }

    void testGetAllProjects() {
        def pList = tr.getAllProjects()
        assert pList == ["p1", "p2", "p3"] || pList == ["p2", "p1", "p3"] // sequence?
    }

    void testGetStartOfTasks() {
        assert tr.startOfTasks == sToD('5.1.2020')
    }

    void testGetEndOfTasks() {
        assert tr.endOfTasks == sToD('9.2.2020')
    }

    void testCalcDepartmentWeekLoad() {

        // date    5  6  7  8  9  10
        // load
        // p1-d1   4  4  4  4  4
        // p2-d1   4  4  4  4  4
        // p1-d2           20
        // p2-d3           20
        //
        def load = tr.calcDepartmentWeekLoad()
        assert load['d1']['2020-W01'] == 8
        assert load['d1']['2020-W02'] == 32
        assert load['d2']['2020-W02'] == 40
    }

    void testCalcDepartmentWeekLoadSparse() {

        // date    5  6  7  8  9  10       9.2
        // load
        // p1-d1   4  4  4  4  4
        // p2-d1   4  4  4  4  4
        // p1-d2           20
        // p2-d2           20
        // p3-d3                            20
        //[W1       ][W2            ][...][W6  ]
        def load = tr.calcDepartmentWeekLoad()
        assert load['d1']['2020-W01'] == 8
        assert load['d1']['2020-W02'] == 32
        assert load['d2']['2020-W02'] == 40
        assert load['d3']['2020-W06'] == 20

        def timeKeys = tr.getFullSeriesOfTimeKeys()
        assert timeKeys.size() == 6
        assert timeKeys[0] == "2020-W01"
        assert timeKeys[5] == "2020-W06"
    }
}
