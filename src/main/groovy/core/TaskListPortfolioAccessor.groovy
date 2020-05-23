package core

import groovy.time.TimeCategory
import static core.TaskInProject.WeekOrMonth.WEEK

/**
 * manages a portfolio - modelled as taskList
 */
trait TaskListPortfolioAccessor {

    /**
     * Reference to all projects
     */
    List<TaskInProject> taskList


    /**
     * @param project
     * @return List of all Tasks with project name project
     */
    //@Memoized
    //@CompileStatic
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

        use(TimeCategory) {
            if(e - s > 20.years) {
                throw new VpipeDataException("Dauer von Anfang bis Ende\n"+
                        "der Tasks zu lange ( > 20 Jahre ): ${s.toString()} bis ${e.toString()}")
            }
        }

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
        //result.sort()
        result
    }
}
