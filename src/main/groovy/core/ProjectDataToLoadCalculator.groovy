package core

import groovy.time.TimeCategory
import groovy.transform.ToString
import transform.Transformer

import static core.TaskInProject.WeekOrMonth.MONTH
import static core.TaskInProject.WeekOrMonth.WEEK

/**
 * this does the raw data calculation:
 * 1. set a list of core.TaskInProject
 * 2. then call calcDepartmentWeekLoad
 * 3. now you may get the departments and the loads per week
 */
@ToString
class ProjectDataToLoadCalculator {

    List<Transformer> transformers = []


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
    List<String> getAllProjects() {
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
    List<String> getFullSeriesOfTimeKeys(TaskInProject.WeekOrMonth weekOrMonth) {

        Date s = getStartOfTasks()
        Date e = getEndOfTasks()
        def result = []

        if (weekOrMonth == WEEK) {
            s = s.getStartOfWeek()
            while (s < e) {
                result << s.getWeekYearStr()
                s += 7
            }
        } else {
            s = s.getStartOfMonth()
            while (s < e) {
                result << s.getMonthYearStr()
                use(TimeCategory) {
                    s = s + 1.month
                }
            }
        }
        return result.sort()
    }


    /**
     * Returns a map with keys that contains the strings of departments.
     * The values are maps again. They contain a interval-key and the total capacityDemand.
     * Interval = 2020-W1 up to W53 (or 2020-M1 up to M12)
     * ATTENTION: Calcs a "sparce matrix". It will be fully created while writing out in core.ProjectDataWriter
     *
     * @return map of department-strings with a map of intervall-strings with demand of capacity
     */
    Map<String, Map<String, Double>> calcDepartmentLoad(TaskInProject.WeekOrMonth weekOrMonth) {

        transformers.each {
            taskList = it.transform()
        }

        def load = [:]
        taskList.each {
            def capaMap = it.getCapaDemandSplitIn(weekOrMonth)
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
     * hint, that config may have changed and to urgently re-read it before next operation
     * @return
     */
    def updateConfiguration() {
        taskList = ProjectDataReader.dataFromFile
        transformers.each() {
            it.updateConfiguration()
        }
    }


}
