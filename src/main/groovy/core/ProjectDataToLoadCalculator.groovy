package core

import groovy.transform.CompileStatic
import utils.FileSupport
import groovy.time.TimeCategory
import groovy.transform.ToString
import transform.Transformer
import utils.RunTimer

import static core.TaskInProject.WeekOrMonth.WEEK

/**
 * this does the raw data calculation:
 * 1. set a list of core.TaskInProject
 * 2. then call calcDepartmentWeekLoad
 * 3. now you may get the departments and the loads per week
 */
@ToString
class ProjectDataToLoadCalculator {

    /**
     * from where to read data
     */
    public static String FILE_NAME = "Projekt-Start-End-Abt-Kapa.txt"
    static def SILENT = true // during reading: show all data read?

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

        def t = new RunTimer()
        transformers.each {
            taskList = it.transform()
        }
        t.stop("Transformers")
        t.go()

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
        t.stop("getCapaDemandSplitIn ($weekOrMonth)")
        load as Map<String, Map<String, Double>>
    }

    /**
     * hint, that config may have changed and to urgently re-read it before next operation
     * @return
     */
    def updateConfiguration() {
        taskList = dataFromFile
        transformers.each() {
            it.updateConfiguration()
        }
    }


    /**
     * reads data lines into a list of core.TaskInProject
     * Default file name: Projekt-Start-End-Abt-Capa.txt
     * This reflects the order of the data fields.
     * Start and End are Dates, formatted like: dd.MM.yyyy
     * Capa is capacityNeededByThisTask and is a float (NO comma - but point for decimal numbers)
     * Default separator: any number of spaces, tabs, commas, semicolons (SEPARATOR_ALL)
     * If whitespace is in project names or department names, semicolon or comma is needed (SEPARATOR_SC)
     */
    //@CompileStatic
    static List<TaskInProject> getDataFromFile() {
        List<TaskInProject> taskList = []
        def i = 0 // count the lines
        SILENT?:println("\nstart reading data file:   " + FILE_NAME)
        def errMsg = {""}
        def f = new File(FILE_NAME).eachLine {
            if(it.trim()) {
                try {
                    i++
                    String line = it.trim()
                    SILENT?:println("\n$i raw-data:   " + line)
                    String[] strings = line.split(FileSupport.SEPARATOR)
                    errMsg = {"$FILE_NAME\nZeile $i: $line\nZerlegt: $strings\n"}
                    if(strings.size() != 5){
                        throw new VpipeDataException(errMsg() +
                            "Keine 5 Daten-Felder gefunden mit Regex-SEPARATOR = "+FileSupport.SEPARATOR +
                            "\nSoll-Format: 'Projekt-Name Task-Start Task-Ende Abteilungs-Name Kapa-Bedarf'")}
                    SILENT?:println("$i split-data: " + strings)

                    Date start = strings[1].toDate()
                    Date end = strings[2].toDate()
                    if( ! start.before(end)) {
                        throw new VpipeDataException(errMsg() + "Start (${start.toString()}) liegt nicht vor Ende: (${end.toString()})")
                    }
                    use (TimeCategory) {
                        if (end - start > 20.years) {
                            throw new VpipeDataException(errMsg() + "Task zu lang! ${start.toString()} " +
                                    "bis ${end.toString()}\nist mehr als 20 Jahre")
                        }
                    }

                    def tip = new TaskInProject(
                            project: strings[0],
                            starting: strings[1].toDate(),
                            ending: strings[2].toDate(),
                            department: strings[3],
                            capacityNeeded: Double.parseDouble(strings[4])
                    )
                    taskList << tip
                    SILENT?:println("$i task-data:  " + tip)
                } catch (VpipeDataException v) {
                    throw v
                } catch (Exception e) {
                    throw new VpipeDataException(errMsg() + "\nVermutlich Fehler beim parsen von \nDatum (--> 22.4.2020) oder Kommazahl (--> 4.5 Punkt statt Komma!)\n Grund: ${e.getMessage()}")
                }
            }
        }
        if( ! taskList){throw new VpipeDataException("$FILE_NAME enthält keine Daten")}
        return taskList
    }

}
