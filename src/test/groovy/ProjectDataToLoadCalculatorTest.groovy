import core.LoadCalculator
import model.DataReader
import model.Model
import model.TaskInProject
import testdata.TestDataHelper
import transform.DateShiftTransformer

import static model.WeekOrMonth.MONTH
import static model.WeekOrMonth.WEEK

// TODO delete
class ProjectDataToLoadCalculatorTest  {

    static LoadCalculator tr
    static TaskInProject t1p1, t2p1, t1p2, t2p2, t3p1

    //@BeforeAll
    static void setUp() {
        TestDataHelper.cleanAllDataFiles()
        //super.setUp()
        tr = new LoadCalculator()
        t1p1 = TestDataHelper.t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = TestDataHelper.t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = TestDataHelper.t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = TestDataHelper.t("p2", "8.1.2020", "9.1.2020", "d2", 20.0)
        t3p1 = TestDataHelper.t("p3", "8.2.2020", "9.2.2020", "d3", 20.0)
        tr.model.taskList = [t1p1, t2p1, t1p2, t2p2,t3p1]
    }

    //void tearDown() {}

    //@Test
    void testGetProject() {
        def p1 = tr.model.getProject("p1")
        def p2 = tr.model.getProject("p2")
        assert p1 == [t1p1, t2p1]
        assert p2 == [t1p2, t2p2]
    }

    //@Test
    void testGetAllProjects() {
        def pList = tr.model._getAllProjects()
        assert pList == ["p1", "p2", "p3"] || pList == ["p2", "p1", "p3"] // sequence?
    }

    //@Test
    void testGetStartOfTasks() {
        assert tr.model.startOfTasks == '5.1.2020'.toDate()
    }

    //@Test
    void testGetEndOfTasks() {
        assert tr.model.endOfTasks == '9.2.2020'.toDate()
    }

    //@Test
    void testCalcDepartmentWeekLoad() {

        // date    5  6  7  8  9  10
        // load
        // p1-d1   4  4  4  4  4
        // p2-d1   4  4  4  4  4
        // p1-d2           20
        // p2-d3           20
        //
        def load = tr.calcDepartmentLoad(WEEK)
        assert load['d1']['2020-W01'] == 8
        assert load['d1']['2020-W02'] == 32
        assert load['d2']['2020-W02'] == 40
    }

    //@Test
    void testCalcDepartmentMonthLoad() {

        // date    5  6  7  8  9  10
        // load
        // p1-d1   4  4  4  4  4
        // p2-d1   4  4  4  4  4
        // p1-d2           20
        // p2-d3           20
        //

        tr.model.taskList << TestDataHelper.t("p4", "1.1.2020", "1.2.2021", "d4", 31+29+31+30+31+30+31+31+30+31+30+31+31)

        def load = tr.calcDepartmentLoad(MONTH)
        assert load['d1']['2020-M01'] == 40
        assert load['d2']['2020-M01'] == 40
        assert load['d3']['2020-M02'] == 20

        assert load['d4']['2020-M01'] == 31
        assert load['d4']['2020-M02'] == 29
        assert load['d4']['2020-M03'] == 31
        assert load['d4']['2020-M04'] == 30



    }

    //@Test
    void testCalcDepartmentWeekLoadSparse() {

        // date    5  6  7  8  9  10       9.2
        // load
        // p1-d1   4  4  4  4  4
        // p2-d1   4  4  4  4  4
        // p1-d2           20
        // p2-d2           20
        // p3-d3                            20
        //[W1       ][W2            ][...][W6  ]
        def load = tr.calcDepartmentLoad(WEEK)
        assert load['d1']['2020-W01'] == 8
        assert load['d1']['2020-W02'] == 32
        assert load['d2']['2020-W02'] == 40
        assert load['d3']['2020-W06'] == 20

        def timeKeys = tr.model.getFullSeriesOfTimeKeys(WEEK)
        assert timeKeys.size() == 6
        assert timeKeys[0] == "2020-W01"
        assert timeKeys[5] == "2020-W06"
    }

    //@Test
    void testUpdateConfiguration() {


        def f = new File(DataReader.get_TASK_FILE_NAME())
        f.getParentFile().mkdirs()
        f.createNewFile()
        f << "p1 6.1.2020 12.1.2020 d1 20\n"
        f << "p1 13.1.2020 19.1.2020 d1 20"

        f = new File(DataReader.get_DATESHIFT_FILE_NAME())
        f.delete()
        f.createNewFile()
        f << "p1 7"

        Model m = new Model()
        m.readAllData()
        LoadCalculator pt = new LoadCalculator(m)
        //new DateShiftTransformer(m).transform()
        def load = pt.calcDepartmentLoad(WEEK)


        assert load['d1']['2020-W02'] == null
        assert load['d1']['2020-W03'] == 20
        assert load['d1']['2020-W04'] == 20
        assert load['d1']['2020-W05'] == null

    }


    //@Test
    void testUpdateConfigurationWithNoDateShiftTransformerData() {


        def f = new File(DataReader.get_TASK_FILE_NAME())
        f.delete()
        f.createNewFile()
        f << "p1 6.1.2020 12.1.2020 d1 20\n"
        f << "p1 13.1.2020 19.1.2020 d1 20"

        f = new File(DataReader.get_DATESHIFT_FILE_NAME())
        f.delete()

        f = new File(DataReader.get_CAPA_FILE_NAME())
        f.delete()

        Model m = new Model()
        m.readAllData()
        LoadCalculator pt = new LoadCalculator(m)
        new DateShiftTransformer(m).transform()
        def load = pt.calcDepartmentLoad(WEEK)


        assert load['d1']['2020-W02'] == 20
        assert load['d1']['2020-W03'] == 20
        assert load['d1']['2020-W04'] == null
        assert load['d1']['2020-W05'] == null

    }

}
