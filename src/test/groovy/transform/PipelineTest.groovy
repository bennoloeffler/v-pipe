package transform

import model.PipelineElement

import static testdata.TestDataHelper.pe


class PipelineTest extends GroovyTestCase {




    void testNextSlot() {

        Pipeline p = new Pipeline(1)

        List<PipelineElement> l = [pe('p1', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1),
                                   pe('p2', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1)]
        l.each {
            p.addNext(it)
        }

        assert p.pipeline[0]*.startDateCalc == ['22.04.2020'.toDate(), '23.04.2020'.toDate()]
        assert p.pipeline[0]*.endDateCalc == ['23.04.2020'.toDate(), '24.04.2020'.toDate()]

    }


    /**
     * p2 should be moved one day...
     */
    void testGetProjectShift() {
        Pipeline p = new Pipeline(1)
        PipelineElement poe = pe('p1', '22.4.2020'.toDate(),'22.4.2020'.toDate(), 1)

        List<PipelineElement> l = [pe('p1', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1),
                                   pe('p2', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1)]
        l.each {
            p.addNext(it)
        }

        def ps = p.projectShift
        assert ps.p2 == 1
        assert ps.p1 == 0
    }

    /**
     * more than one slot and succcessive filling of those
     */
    void testMultipleSlots() {

        Pipeline p = new Pipeline(2)

        List<PipelineElement> l = [pe('p1', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1),
                                   pe('p2', '22.4.2020'.toDate(),'25.4.2020'.toDate(), 1),
                                   pe('p3', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1),
                                   pe('p4', '22.4.2020'.toDate(),'23.4.2020'.toDate(), 1)]
        l.each {
            p.addNext(it)
        }

        def ce = p.pipeline[0][0]
        assert ce.startDateCalc.toString() == '22.04.2020'
        assert ce.endDateCalc.toString() == '23.04.2020'
        assert ce.originalElement.project == 'p1'

        ce = p.pipeline[1][0]
        assert ce.startDateCalc.toString() == '22.04.2020'
        assert ce.endDateCalc.toString() == '25.04.2020'
        assert ce.originalElement.project == 'p2'

        ce = p.pipeline[0][1]
        assert ce.startDateCalc.toString() == '23.04.2020'
        assert ce.endDateCalc.toString() == '24.04.2020'
        assert ce.originalElement.project == 'p3'

        ce = p.pipeline[0][2]
        assert ce.startDateCalc.toString() == '24.04.2020'
        assert ce.endDateCalc.toString() == '25.04.2020'
        assert ce.originalElement.project == 'p4'


        def ps = p.projectShift
        assert ps.p1 == 0
        assert ps.p2 == 0
        assert ps.p3 == 1
        assert ps.p4 == 2
    }

    /**
     *
     */
    void testSplit() {
        // date 1       2       3       4       5       6
        // slot
        // 0    p1      p3.1    p3.1    p4      p4
        // 1    p2      p2      p3.2    p3.2
        //
        //

        Pipeline p = new Pipeline(2)

        List<PipelineElement> l = [pe('p1', '1.4.2020'.toDate(),'2.4.2020'.toDate(), 1),
                                   pe('p2', '1.4.2020'.toDate(),'3.4.2020'.toDate(), 1),
                                   pe('p3', '1.4.2020'.toDate(),'3.4.2020'.toDate(), 2), // HERE!
                                   pe('p4', '1.4.2020'.toDate(),'3.4.2020'.toDate(), 1)]

        l.each {
            p.addNext(it)
        }

        def ce = p.pipeline[0][0]
        assert ce.startDateCalc.toString() == '01.04.2020'
        assert ce.endDateCalc.toString() == '02.04.2020'
        assert ce.originalElement.project == 'p1'

        ce = p.pipeline[1][0]
        assert ce.startDateCalc.toString() == '01.04.2020'
        assert ce.endDateCalc.toString() == '03.04.2020'
        assert ce.originalElement.project == 'p2'

        ce = p.pipeline[0][1]
        assert ce.startDateCalc.toString() == '02.04.2020'
        assert ce.endDateCalc.toString() == '04.04.2020'
        assert ce.originalElement.project == 'p3' //p3.1

        ce = p.pipeline[1][1]
        assert ce.startDateCalc.toString() == '03.04.2020'
        assert ce.endDateCalc.toString() == '05.04.2020'
        assert ce.originalElement.project == 'p3' // p3.2


        ce = p.pipeline[0][2]
        assert ce.startDateCalc.toString() == '04.04.2020'
        assert ce.endDateCalc.toString() == '06.04.2020'
        assert ce.originalElement.project == 'p4'

        def ps = p.projectShift
        assert ps.p1 == 0
        assert ps.p2 == 0
        assert ps.p3 == 2
        assert ps.p4 == 3
    }

    /**
     *
     */
    void testSplitHighValue() {
        // date 1       2       3       4       5       6
        // slot
        // 0    p1      p1      p1      p1      p1      p4 ...
        // 1    p2      p2      p3.1    p3.1    p3.2    p3.2
        //
        //

        Pipeline p = new Pipeline(2)

        List<PipelineElement> l = [pe('p1', '1.4.2020'.toDate(),'6.4.2020'.toDate(), 1),
                                   pe('p2', '1.4.2020'.toDate(),'3.4.2020'.toDate(), 1),
                                   pe('p3', '1.4.2020'.toDate(),'3.4.2020'.toDate(), 2), // HERE!
                                   pe('p4', '1.4.2020'.toDate(),'30.4.2020'.toDate(), 2)]

        l.each {
            p.addNext(it)
        }

        def ce = p.pipeline[0][0]
        assert ce.startDateCalc.toString() == '01.04.2020'
        assert ce.endDateCalc.toString() == '06.04.2020'
        assert ce.originalElement.project == 'p1'

        ce = p.pipeline[1][0]
        assert ce.startDateCalc.toString() == '01.04.2020'
        assert ce.endDateCalc.toString() == '03.04.2020'
        assert ce.originalElement.project == 'p2'

        ce = p.pipeline[1][1]
        assert ce.startDateCalc.toString() == '03.04.2020'
        assert ce.endDateCalc.toString() == '05.04.2020'
        assert ce.originalElement.project == 'p3' // p3.1

        ce = p.pipeline[1][2]
        assert ce.startDateCalc.toString() == '05.04.2020'
        assert ce.endDateCalc.toString() == '07.04.2020'
        assert ce.originalElement.project == 'p3' // p3.2

        def ps = p.projectShift
        assert ps.p1 == 0
        assert ps.p2 == 0
        assert ps.p3 == 4

        assert p.increaseTaskLenDueToParallel == 1.0 // 100% länger!
    }

}
