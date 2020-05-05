import core.ProjectDataToLoadCalculator
import core.TaskInProject

class TestDataHelper {

    /**
     * @return populated data
     */
    static ProjectDataToLoadCalculator getPopulatedCalculator() {
        ProjectDataToLoadCalculator tr
        TaskInProject t1p1, t2p1, t1p2, t2p2
        tr = new ProjectDataToLoadCalculator()
        t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = t("p2", "8.1.2020", "9.2.2020", "d2", 20.0)
        tr.taskList = [t1p1, t2p1, t1p2, t2p2]
        tr
    }

    /**
     * Factory for core.TaskInProject, mainly for testing
     * @param pro
     * @param sta "dd.MM.yyyy"
     * @param end "dd.MM.yyyy"
     * @param dep
     * @param cap
     * @return new core.TaskInProject
     */
    static TaskInProject t(String pro, String sta, String end, String dep, Double cap) {
        new TaskInProject(
                project: pro,
                starting: sta.toDate(),
                ending: end.toDate(), //DateHelperFunctions.sToD(end),
                department: dep,
                capacityNeeded: cap
        )
    }
}
