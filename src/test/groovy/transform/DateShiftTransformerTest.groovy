package transform

import core.VpipeException
import model.Model
import testdata.TestDataHelper
import transform.DateShiftTransformer

class DateShiftTransformerTest extends GroovyTestCase {

    void testTransform() {
        Model m = TestDataHelper.getPopulatedModel()
        DateShiftTransformer dst = new DateShiftTransformer(m)
        m.projectDayShift = [p1: 2, p2: -1]

        /*
        t1p1 = eventType("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = eventType("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = eventType("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = eventType("p2", "8.1.2020", "9.2.2020", "d2", 20.0)
         */

        dst.transform()

        assert dst.description == "Dates transformed:\np1: 2\np2: -1\n"

        // p1
        assert m.taskList[0].starting == '7.1.2020'.toDate()
        assert m.taskList[0].ending == '12.1.2020'.toDate()
        assert m.taskList[1].starting == '10.1.2020'.toDate()
        assert m.taskList[1].ending == '11.1.2020'.toDate()

        // p2
        assert m.taskList[2].starting == '4.1.2020'.toDate()
        assert m.taskList[2].ending == '9.1.2020'.toDate()
        assert m.taskList[3].starting == '7.1.2020'.toDate()
        assert m.taskList[3].ending == '8.2.2020'.toDate()

    }

    void testFailProjectNotFound() {
        Model m = TestDataHelper.getPopulatedModel()
        DateShiftTransformer dst = new DateShiftTransformer(m)
        m.projectDayShift = [p1: 2, p2: -1, p3: 5]

        def msg = shouldFail VpipeException, {
            def list = dst.transform()
        }
        assert msg == "Es gibt keine Projektdaten,\n"+
                        "um das Projekt p3 zu verschieben.\n"+
                        "Ursache in Projekt-Verschiebung.txt"
    }
}
