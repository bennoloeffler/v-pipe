package core

import model.Model
import model.TaskInProject
import model.WeekOrMonth
import transform.CapaTransformer
import utils.FileSupport
import groovy.transform.ToString
import transform.Transformer
import utils.RunTimer


/**
 * this does the raw data calculation:
 * 1. set a list of model.TaskInProject
 * 2. then call calcDepartmentWeekLoad
 * 3. now you may get the departments and the loads per week
 */
@ToString
class LoadCalculator {


    @Delegate
    Model model = new Model()
    // during reading: show all data read?

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
    Map<String, Map<String, Double>> calcDepartmentLoad(WeekOrMonth weekOrMonth) {

        def t = new RunTimer(true)
        transformers.each {
            model = it.transform()
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



    /*
    def readFromFile() {
        taskList = Model.readTasks()
    }

    def writeToFile(WeekOrMonth weekOrMonth) {
        writeToFileStatic(this, weekOrMonth)
    }
    */

    CapaTransformer getFilledCapaTransformer() {
        for(t in transformers) {
            if (t instanceof CapaTransformer && t.capaAvailable){return t}
        }
        null
    }


    /**
     *
     * @param tr
     * @param weekOrMonth
     */
    static void writeToFileStatic(LoadCalculator tr, WeekOrMonth weekOrMonth) {


        def t = new RunTimer(true)

        Map<String, Map<String, Double>> stringMapMap = tr.calcDepartmentLoad(weekOrMonth)

        def fn = weekOrMonth == WeekOrMonth.WEEK ? FILE_NAME_WEEK : FILE_NAME_MONTH
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
