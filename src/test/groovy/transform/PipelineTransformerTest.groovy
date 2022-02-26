package transform


import core.VpipeException
import model.DataReader
import model.Model
import model.PipelineElement
import testdata.TestDataHelper

import static testdata.TestDataHelper.pe
import static testdata.TestDataHelper.t


class PipelineTransformerTest extends GroovyTestCase {

    File f
    List<PipelineElement> listOfPOEs

    void setUp() {
        f = new File(DataReader.get_PIPELINING_FILE_NAME())
        f.delete()
        f.createNewFile()

        listOfPOEs = [pe('p1', '7.1.2020'.toDate(), '9.1.2020'.toDate(), 2), // to block pipeline
                      pe('p2', '7.1.2020'.toDate(), '9.2.2020'.toDate(), 1)]
    }

    void tearDown() { f.delete() }

    void testTransform() {
        Model m = TestDataHelper.getPopulatedModel()
        /*
        t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = t("p2", "8.1.2020", "9.2.2020", "d2", 20.0)
         */

        def pt = new PipelineTransformer(m)
        pt.pipelineElements = listOfPOEs
        pt.maxPipelineSlots = 2
        pt.transform()

        assert m.getProject('p1')[0] == t('p1', '5.1.2020', '10.1.2020', 'd1', 20)
        assert m.getProject('p2')[0] == t('p2', '7.1.2020', '12.1.2020', 'd1', 20)

    }

    void testFailBecauseOfMismatchingProjects() {

        //
        // slots wrong (1 avail, 2 needed)
        //
        Model m = TestDataHelper.getPopulatedModel()
        def pt = new PipelineTransformer(m)
        pt.pipelineElements = listOfPOEs
        pt.maxPipelineSlots = 1
        String msg = shouldFail {
            m.taskList = pt.transform()
        }
        assert msg.contains('Staffelungs-Element braucht mehr Slots')


        //
        // mismatch: lesser projects than in piplining (or typo)
        //
        m = TestDataHelper.getPopulatedModel()
        m.taskList.remove(0) // p1 raus
        m.taskList.remove(0) // p1 raus
        pt = new PipelineTransformer(m)
        pt.pipelineElements = listOfPOEs
        pt.maxPipelineSlots = 2
        msg = shouldFail {
            m.taskList = pt.transform()
        }
        assert msg.contains('Integrations-Phasen.txt enthält Projekte,\ndie nicht in den Grunddaten sind: p1')

        //
        // mismatch: lesser POEs than in projects (or typo)
        //
        m = TestDataHelper.getPopulatedModel()
        pt = new PipelineTransformer(m)
        pt.pipelineElements = listOfPOEs
        pt.pipelineElements.remove(0) // p1 raus

        pt.maxPipelineSlots = 2
        msg = shouldFail {
            m.taskList = pt.transform()
        }
        assert msg.contains('Integrations-Phasen.txt aufgeführt sind: p1')


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

        DataReader.PipelineResult result = DataReader.readPipelining()
        def maxPipelineSlots = result.maxPipelineSlots
        def pipelineElements = result.elements

        assert maxPipelineSlots == 6

        assert '20.02.2020' == pipelineElements[0].startDate.toString()
        assert '20.03.2020' == pipelineElements[0].endDate.toString()
        assert 2 == pipelineElements[0].pipelineSlotsNeeded

        assert '05.03.2020' == pipelineElements[5].startDate.toString()
        assert '26.04.2020' == pipelineElements[5].endDate.toString()
        assert 2 == pipelineElements[5].pipelineSlotsNeeded
    }



    def readShouldFail(String content, String errorMsgContains) {

        f.delete()
        f.createNewFile()
        f << content

        //LoadCalculator plc = new LoadCalculator()
        //def pt = new PipelineTransformer(plc)

        def msg = shouldFail VpipeException, {
            DataReader.readPipelining()
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


        def msg = shouldFail {
            DataReader.readPipelining()
        }
        assert msg.contains("[p2=2]")
    }

    }