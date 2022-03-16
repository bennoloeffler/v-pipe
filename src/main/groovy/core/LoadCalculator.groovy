package core

import groovy.transform.CompileStatic
import model.Model
import model.TaskInProject
import model.WeekOrMonth
import utils.FileSupport
import groovy.transform.ToString
import utils.RunTimer

/**
 * this does the raw data calculation:
 * call calcDepartmentWeekLoad
 * you may get the departments and the loads per week
 */
@ToString
@CompileStatic
class LoadCalculator {

    WeekOrMonth weekOrMonth = WeekOrMonth.WEEK

    Map<String, Map<String, Double>> projectTimeLoadMap = [:]
    // compared to maxRed, if capaAvailable // CURRENT PROJECT OR [:]
    Map<String, Map<String, Double>> depTimeLoadMap // compared to maxRed, if capaAvailable
    Map<String, Double> maxRed // the maximum red value (> red limit) or red limit (if value < red limit)

    Model model


    LoadCalculator(Model model = null) {
        this.model = model?:new Model()
    }

    // during reading: show all data read?

    /**
     * to where to write
     */
    public static def FILE_NAME_WEEK = 'Abteilungs-Kapazitäts-Belastung-Woche.txt'
    public static def FILE_NAME_MONTH = 'Abteilungs-Kapazitäts-Belastung-Monat.txt'
    public static String BACKUP_FILE

    //List<Transformer> transformers = []

    /*
    void calcProjectCapaNeeded(String project) {
        projectTimeLoadMap = calcProjectLoad(weekOrMonth, project)
        normalizeTo100Percent()
    }

    void __calcLoadAbsolut() {
        depTimeLoadMap = calcDepartmentLoad(weekOrMonth)
        normalizeTo100Percent()
    }


    void calcLoadPercent() {
        if(capaAvailable) {

            maxPercent = [:]
            capaLoadPercent = [:]

            capaLoadAbsolut.each {

                String dep = it.key

                // key: timeStr
                Map<String, CapaDetailsEntry> depLoad = it.value

                // find maximum absoluteValue of the current load
                Double maxAbsCapaVal = depLoad.max {
                    it.value.value // entry of map, the value (is a CapaDetailsEntry) and the biggest value...
                }.value.value

                Double maxPercentCapaVal = 0
                capaLoadPercent[dep]=[:]
                depLoad.each { String timeStamp, CapaDetailsEntry capaAbs ->
                    //Double availYellow = capaAvailable[dep][timeStamp].yellow
                    Double availRedAbs = capaAvailable[dep][timeStamp].red

                    Double percentageNeeded = capaAbs.value / availRedAbs
                    CapaDetailsEntry detailsPercent = new CapaDetailsEntry(value: percentageNeeded, projectDetails: [:])
                    capaAbs.projectDetails.entrySet().each { String project, Double valueAbs ->
                        detailsPercent.projectDetails[project] = valueAbs / availRedAbs
                    }
                    capaLoadPercent[dep][timeStamp] = new CapaDetailsEntry(value: percentageNeeded, projectDetails: detailsPercent)

                }
                maxPercent[dep] = maxPercentCapaVal


            }
        } else {
            throw new VpipeException('Need available capa data (capaAvailable) to calc load percentage')
        }
    }

     void calcLoadAbsolut() {

        Map<String, Map<String, CapaDetailsEntry>> capaLoadAbsolutLocal = [:]
        Map<String, Double> maxAbsolutLocal = [:]

        taskList.each {
            def capaMap = it.getCapaDemandSplitIn(weekOrMonth)
            capaMap.each { String timeKey, double capaValue ->

                // if there is not yet a department key and map: create
                if (!capaLoadAbsolutLocal[it.department]) {
                    capaLoadAbsolutLocal[it.department] = [:]
                }
                if(capaLoadAbsolutLocal[it.department][timeKey]) { // if department and week-key are available
                    capaLoadAbsolutLocal[it.department][timeKey].value += capaValue // add value
                    capaLoadAbsolutLocal[it.department][timeKey].projectDetails[it.project] = capaValue // and details map entry
                } else { // create first entry
                    def firstEntryMap = [(it.project): capaValue]
                    capaLoadAbsolutLocal[it.department][timeKey] = new CapaDetailsEntry(value: capaValue, projectDetails: firstEntryMap ) // otherwise create
                }

                maxAbsolutLocal[it.department] = Math.max( // finally search for the maximum of the department load
                        (double)(maxAbsolutLocal[it.department]?:0),
                        (double)(capaLoadAbsolutLocal[it.department][timeKey].value)
                )
            }
        }
        maxAbsolut = maxAbsolutLocal
        capaLoadAbsolut = capaLoadAbsolutLocal
    }
*/
    /**
     * Returns a map with keys that contains the strings of departments.
     * The values are maps again. They contain a interval-key and the total capacityDemand.
     * Interval = 2020-W1 up to W53 (or 2020-M1 up to M12)
     * ATTENTION: Calcs a "sparce matrix". It will be fully created while writing out
     *
     * @return map of department-strings with a map of intervall-strings with demand of capacity
     */
    Map<String, Map<String, Double>> calcDepartmentLoad(WeekOrMonth weekOrMonth) {

        def t = new RunTimer(true)
        //transformers.each {
        //    model = it.transform()
        //}
        t.stop("Transformers")
        t.start()

        Map<String, Map<String, Double>> load = [:]
        model.taskList.each { TaskInProject it ->
            def capaMap = it.getCapaDemandSplitIn(weekOrMonth)
            capaMap.each { key, value ->

                // if there is not yet a department key and map: create
                if (!load[it.department]) {
                    load.putAt(it.department, [:])
                }
                if(load[it.department][key]) { // if department and week-key are available
                    //Map<String, Double> departmentLoad = load[it.department]
                    def oldVal = load[it.department][key]
                    load[it.department][key] = oldVal + value // add
                } else {
                    load[it.department][key]=value // otherwise create
                }

            }
        }
        t.stop("calcDepartmentLoad ($weekOrMonth)")
        load as Map<String, Map<String, Double>>
    }


    /**
     * @param weekOrMonth
     * @param project
     * @return load for exactly the project
     */
    Map<String, Map<String, Double>> calcProjectLoad(WeekOrMonth weekOrMonth, String project) {

        List<TaskInProject> projectTaskList = model.getProject(project)
        Map<String, Map<String, Double>> load = [:]
        projectTaskList.each { TaskInProject it ->
            def capaMap = it.getCapaDemandSplitIn(weekOrMonth)
            capaMap.each { String key, Double value ->

                // if there is not yet a department key and map: create
                if (!load[it.department]) {
                    load.putAt(it.department,[:])
                }
                if(load[it.department][key]) { // if department and week-key are available
                    def oldVal = load[it.department][key]
                    //Double val = load[it.department][key]
                    load[it.department][key] = oldVal + value // add
                } else {
                    load[it.department][key]=value // otherwise create
                }

            }
        }
        load as Map<String, Map<String, Double>>
    }


    def normalizeTo100Percent() {
        maxRed = [:]
        if (model.capaAvailable) {
            depTimeLoadMap.each {
                String dep = it.key
                Map<String, Double> load = it.value
                Double maxVal = load.max {
                    it.value
                }.value
                maxRed[dep] = maxVal
                load.each { String timeStamp, Double capa ->
                    //Double avail = capaAvailable[dep][timeStamp].yellow
                    load[timeStamp] = (Double) (capa)
                }
            }
        } else {
            depTimeLoadMap.each {
                //String dep = it.key
                Map<String, Double> load = it.value
                Double maxVal = load.max {
                    it.value
                }.value
                load.each { String timeStamp, Double capa ->
                    load[timeStamp] = (Double) (capa / maxVal)
                }
            }
        }
    }

    /**
     *
     * @param tr
     * @param weekOrMonth
     */
    static void writeToFileStatic(LoadCalculator tr, WeekOrMonth weekOrMonth) {


        def t = new RunTimer(true)

        Map<String, Map<String, Double>> stringMapMap = tr.calcDepartmentLoad(weekOrMonth)

        String fn = weekOrMonth == WeekOrMonth.WEEK ? FILE_NAME_WEEK : FILE_NAME_MONTH
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
        List<String> allTimeKeys = tr.model.getFullSeriesOfTimeKeys(weekOrMonth)
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


}
