package testdata

import core.LoadCalculator
import model.DataReader
import model.Model
import model.TaskInProject
import model.PipelineOriginalElement

class TestDataHelper {

    /**
     * @return populated data
     */
    static Model getPopulatedModel() {
        Model m
        TaskInProject t1p1, t2p1, t1p2, t2p2
        m = new Model()
        t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = t("p2", "8.1.2020", "9.2.2020", "d2", 20.0)
        m.taskList = [t1p1, t2p1, t1p2, t2p2]
        m
    }


    /**
     * @return populated data
     */
    static LoadCalculator getPopulatedCalculator() {
        LoadCalculator tr
        TaskInProject t1p1, t2p1, t1p2, t2p2
        tr = new LoadCalculator()
        t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = t("p2", "8.1.2020", "9.2.2020", "d2", 20.0)
        tr.taskList = [t1p1, t2p1, t1p2, t2p2]
        tr
    }

    /**
     * Factory for model.TaskInProject, mainly for testing
     * @param pro
     * @param sta "dd.MM.yyyy"
     * @param end "dd.MM.yyyy"
     * @param dep
     * @param cap
     * @return new model.TaskInProject
     */
    static TaskInProject t(String pro, String sta, String end, String dep, Double cap) {
        new TaskInProject(
                 pro,
                sta.toDate(),
                end.toDate(),
                dep,
                cap,
                '',
        )
    }

    static def pe(pr, s, e, c) {
        new PipelineOriginalElement(project: pr, startDate: s, endDate: e, pipelineSlotsNeeded: c)
    }

    static def cleanAllDataFiles() {
        List files = [DataReader.get_TASK_FILE_NAME(),
                     DataReader.get_PIPELINING_FILE_NAME(),
                     DataReader.get_DATESHIFT_FILE_NAME(),
                     DataReader.get_CAPA_FILE_NAME(),
                     DataReader.get_TEMPLATE_FILE_NAME(),
        ]
        files.each {
            File f = new File(it)
            f.delete()
        }
    }
}
