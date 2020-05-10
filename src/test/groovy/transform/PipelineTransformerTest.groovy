package transform

import core.ProjectDataToLoadCalculator
import core.VpipeException
import testdata.TestDataHelper

import static testdata.TestDataHelper.pe
import static testdata.TestDataHelper.t


class PipelineTransformerTest extends GroovyTestCase {

    File f
    List<PipelineOriginalElement> listOfPOEs

    void setUp() {
        f = new File(PipelineTransformer.FILE_NAME)
        f.delete()
        f.createNewFile()

        listOfPOEs = [pe('p1', '7.1.2020'.toDate(), '9.1.2020'.toDate(), 2), // to block pipeline
                      pe('p2', '7.1.2020'.toDate(), '9.2.2020'.toDate(), 1)]
    }

    void tearDown() { f.delete() }

    void testTransform() {
        ProjectDataToLoadCalculator plc = TestDataHelper.getPopulatedCalculator()
        /*
                t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
                t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
                t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0) --> 2 days
                t2p2 = t("p2", "8.1.2020", "9.2.2020", "d2", 20.0) --> 2 days
         */

        def pt = new PipelineTransformer(plc)
        pt.pipelineElements = listOfPOEs
        pt.maxPipelineSlots = 2
        plc.taskList = pt.transform()

        assert plc.getProject('p1')[0] == t('p1', '5.1.2020', '10.1.2020', 'd1', 20)
        assert plc.getProject('p2')[0] == t('p2', '7.1.2020', '12.1.2020', 'd1', 20)

    }

    void testFailBecauseOfMismatchingProjects() {

        //
        // slots wrong (1 avail, 2 needed)
        //
        ProjectDataToLoadCalculator plc = TestDataHelper.getPopulatedCalculator()
        def pt = new PipelineTransformer(plc)
        pt.pipelineElements = listOfPOEs
        pt.maxPipelineSlots = 1
        String msg = shouldFail {
            plc.taskList = pt.transform()
        }
        assert msg.contains('Staffelungs-Element braucht mehr Slots')


        //
        // mismatch: lesser projects than in piplining (or typo)
        //
        plc = TestDataHelper.getPopulatedCalculator()
        plc.taskList.remove(0) // p1 raus
        plc.taskList.remove(0) // p1 raus
        pt = new PipelineTransformer(plc)
        pt.pipelineElements = listOfPOEs
        pt.maxPipelineSlots = 2
        msg = shouldFail {
            plc.taskList = pt.transform()
        }
        assert msg.contains('Integrations-Phasen.txt enthält Projekte,\ndie nicht in den Grunddaten sind: p1')

        //
        // mismatch: lesser POEs than in projects (or typo)
        //
        plc = TestDataHelper.getPopulatedCalculator()
        pt = new PipelineTransformer(plc)
        pt.pipelineElements = listOfPOEs
        pt.pipelineElements.remove(0) // p1 raus

        pt.maxPipelineSlots = 2
        msg = shouldFail {
            plc.taskList = pt.transform()
        }
        assert msg.contains('Grunddaten enthalten Projekte,\ndie nicht in Integrations-Phasen.txt aufgeführt sind: p1')


    }


    void testUpdateConfiguration() {

        f << """
            6
            p1 20.2.2020 20.3.2020 2
            p2 20.2.2020 24.3.2020 1
            p3 25.2.2020 26.4.2020 1
            p4 30.2.2020 20.3.2020 2
            p5 2.3.2020 23.3.2020 1
            p6 5.3.2020 26.4.2020 2
            """

        ProjectDataToLoadCalculator plc = new ProjectDataToLoadCalculator()
        def pt = new PipelineTransformer(plc)
        pt.updateConfiguration()

        assert pt.maxPipelineSlots == 6

        assert '20.02.2020' == pt.pipelineElements[0].startDate.toString()
        assert '20.03.2020' == pt.pipelineElements[0].endDate.toString()
        assert 2 == pt.pipelineElements[0].pipelineSlotsNeeded

        assert '05.03.2020' == pt.pipelineElements[5].startDate.toString()
        assert '26.04.2020' == pt.pipelineElements[5].endDate.toString()
        assert 2 == pt.pipelineElements[5].pipelineSlotsNeeded
    }

    def readShouldFail(String content, String errorMsgContains) {

        f.delete()
        f.createNewFile()
        f << content

        ProjectDataToLoadCalculator plc = new ProjectDataToLoadCalculator()
        def pt = new PipelineTransformer(plc)

        def msg = shouldFail VpipeException, {
            pt.updateConfiguration()
        }
        assert msg.contains(errorMsgContains)

    }


    void testFailDataProblems() {


        readShouldFail(
                """
                        p1 20.2.2020 20.3.2020 2
                        """,
                'Fehler beim Lesen von')


        readShouldFail(
                """
                        5
                        p1 20.2.2020 20.2.2020 2
                        """,
                'Start liegt nicht vor Ende')


        readShouldFail(
                """
                        5
                        p1 20.2.2020 21.2.2020 2b
                        """,
                'Vermutung:')

        readShouldFail(
                """
                        5
                        p1 20.2.2020 21.2.2ß20 2
                        """,
                'Start liegt nicht vor Ende.') // strange... But it seems to parse...
    }


    void testFail2ProjectsSame() {

        f << """
            6
            p1 20.2.2020 20.3.2020 2
            p2 20.2.2020 24.3.2020 1
            p3 25.2.2020 26.4.2020 1
            p4 30.2.2020 20.3.2020 2
            p5 2.3.2020 23.3.2020 1
            p2 5.3.2020 26.4.2020 2
            """

        ProjectDataToLoadCalculator plc = new ProjectDataToLoadCalculator()
        def pt = new PipelineTransformer(plc)
        def msg = shouldFail {
            pt.updateConfiguration()
        }
        assert msg.contains("[p2=2]")
    }

    }