import groovy.transform.ToString

import static HelperFunctions.getStartOfWeek
import static HelperFunctions.t

/**
 * this does the raw data calculation:
 * 1. set a list of TaskInProject
 * 2. then call calcDepartmentWeekLoad
 * 3. now you may get the departments and the loads per week
 */
@ToString
class ProjectDataToLoadTransformer {


    /**
     * Data of all projects
     */
    List<TaskInProject> taskList = []

    /**
     * @param project
     * @return List of all Tasks with project name project
     */
    List<TaskInProject> getProject(String project) {
        taskList.findAll {it.project == project}
    }

    /**
     * @return List of Strings with all projectNames found in
     */
    def getAllProjects() {
        (taskList*.project).unique()
    }

    /**
     * @return the minimum time of all tasks
     */
    Date getStartOfTasks() {
        (taskList*.starting).min()
    }

    /**
     * @return the maximum time of all tasks
     */
    Date getEndOfTasks() {
        (taskList*.ending).max()
    }

    /**
     * @return even if data is sparce, deliver continous list of timekey strings. Every week.
     */
    List<String> getFullSeriesOfTimeKeys() {
        Date s = getStartOfWeek(getStartOfTasks())
        Date e = getEndOfTasks()
        def result =[]
        while(s < e) {
            result << HelperFunctions.getWeekYearStr(s)
            s += 7
        }
        return result.sort()
    }


    /**
     * Returns a map with keys that contains the strings of departments.
     * The values are maps again. They contain a interval-key and the total capacityDemand.
     * Interval = 2020-W1 up to W53
     * ATTENTION: Calcs a "sparce matrix". It will be fully created while writing out in ProjectDataWriter
     *
     * @return map of department-strings with a map of intervall-strings with demand of capacity
     */
    Map<String, Map<String, Double>> calcDepartmentWeekLoad() {
        def load = [:]
        taskList.each {
            def capaMap = it.getCapaDemandSplitInWeeks()
            capaMap.each { key, value ->

                // if there is not yet a department key and map: create
                if (!load[it.department]) {
                    load[it.department] = [:]
                }
                if(load[it.department][key]) { // if department and week-key are available
                    load[it.department][key]+=value // add
                } else {
                    load[it.department][key]=value // otherwise create
                }

            }
        }
        load as Map<String, Map<String, Double>>
    }


    /**
     * @return populated data
     */
    static ProjectDataToLoadTransformer getPopulatedTransformer() {
        ProjectDataToLoadTransformer tr
        TaskInProject t1p1, t2p1, t1p2, t2p2
        tr = new ProjectDataToLoadTransformer()
        t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
        t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
        t2p2 = t("p2", "8.1.2020", "9.2.2020", "d2", 20.0)
        tr.taskList = [t1p1, t2p1, t1p2, t2p2]
        tr
    }

}
