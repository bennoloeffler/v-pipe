package core

import core.TaskInProject.WeekOrMonth
import transform.CapaTransformer
import utils.FileSupport
import groovy.time.TimeCategory
import groovy.transform.ToString
import transform.Transformer
import utils.RunTimer


/**
 * this does the raw data calculation:
 * 1. set a list of core.TaskInProject
 * 2. then call calcDepartmentWeekLoad
 * 3. now you may get the departments and the loads per week
 */
@ToString
class ProjectDataToLoadCalculator implements TaskListPortfolioAccessor {

    /**
     * from where to read data
     */
    public static String FILE_NAME = "Projekt-Start-End-Abt-Kapa.txt"
    static def SILENT = true // during reading: show all data read?

    /**
     * to where to write
     */
    public static def FILE_NAME_WEEK = 'Abteilungs-Kapazitäts-Belastung-Woche.txt'
    public static def FILE_NAME_MONTH = 'Abteilungs-Kapazitäts-Belastung-Monat.txt'
    public static String BACKUP_FILE

    List<Transformer> transformers = []



    /**
     * Returns a map with keys that contains the strings of departments.
     * The values are maps again. They contain a interval-key and the total capacityDemand.
     * Interval = 2020-W1 up to W53 (or 2020-M1 up to M12)
     * ATTENTION: Calcs a "sparce matrix". It will be fully created while writing out in core.ProjectDataWriter
     *
     * @return map of department-strings with a map of intervall-strings with demand of capacity
     */
    Map<String, Map<String, Double>> calcDepartmentLoad(TaskInProject.WeekOrMonth weekOrMonth) {

        def t = new RunTimer(true)
        transformers.each {
            taskList = it.transform()
        }
        t.stop("Transformers")
        t.start()

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
        t.stop("calcDepartmentLoad ($weekOrMonth)")
        load as Map<String, Map<String, Double>>
    }

    /**
     * hint, that config may have changed and to urgently re-read it before next operation
     * @return
     */
    def updateConfiguration() {
        taskList = dataFromFileStatic
        transformers.each() {
            it.updateConfiguration()
        }
    }


    def readFromFile() {
        taskList = getDataFromFileStatic()
    }

    def writeToFile(WeekOrMonth weekOrMonth) {
        writeToFileStatic(this, weekOrMonth)
    }

    CapaTransformer getFilledCapaTransformer() {
        for(t in transformers) {
            if (t instanceof CapaTransformer && t.capaAvailable){return t}
        }
        null
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
    static List<TaskInProject> getDataFromFileStatic() {
        def t = new RunTimer(true)
        List<TaskInProject> taskList = []
        def i = 0 // count the lines
        SILENT?:println("\nstart reading data file:   " + FILE_NAME)
        def errMsg ={""}
        //String all = new File(FILE_NAME).text
        //all.eachLine {
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
        t.stop("reading file $FILE_NAME")
        if( ! taskList){throw new VpipeDataException("$FILE_NAME enthält keine Daten")}
        return taskList
    }


    /**
     *
     * @param tr
     * @param weekOrMonth
     */
    static void writeToFileStatic(ProjectDataToLoadCalculator tr, WeekOrMonth weekOrMonth) {


        def t = new RunTimer(true)

        Map<String, Map<String, Double>> stringMapMap = tr.calcDepartmentLoad(weekOrMonth)

        def fn = weekOrMonth == TaskInProject.WeekOrMonth.WEEK ? FILE_NAME_WEEK : FILE_NAME_MONTH
        File f = new File(fn)
        if(f.exists()) { // BACKUP
            BACKUP_FILE = FileSupport.backupFileName(f.toString())
            f.renameTo(BACKUP_FILE)
        }

        f = new File(fn)
        f.createNewFile()
        t.stop("backupfile $BACKUP_FILE and new file $fn")
        t.start()

        // normalize a maps to contain all time-keys
        List<String> allTimeKeys = tr.getFullSeriesOfTimeKeys(weekOrMonth)
        f << "DEP\t"+allTimeKeys.join("\t") + "\n"
        t.stop("getFullSeriesOfTimeKeys($weekOrMonth)")

        t.start()
        StringBuffer loads = new StringBuffer()
        stringMapMap.each {dep, loadMap ->
            //f << dep
            loads << dep
            allTimeKeys.each { String timeKey ->
                if (loadMap[timeKey]) {
                    def commaNumber = String.format("%.1f", loadMap[timeKey])

                    loads << "\t" +  commaNumber
                } else {
                    loads << "\t0,0"
                }
            }
            loads <<"\n"
        }
        f << loads.toString()
        t.stop("writeToFileStatic $fn")
    }

    List<String> getAllDepartments() {
        taskList*.department.unique()
    }

    Map<String, Map<String, Double>> calcProjectLoad(WeekOrMonth weekOrMonth, String project) {

        List<TaskInProject> projectTaskList = getProject(project)
        def load = [:]
        projectTaskList.each {
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
}
