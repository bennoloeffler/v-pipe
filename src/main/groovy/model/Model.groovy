package model

import extensions.DateExtension
import extensions.StringExtension
import groovy.beans.Bindable
import groovy.time.TimeCategory
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import transform.DateShiftTransformer
import transform.TemplateTransformer
import utils.RunTimer

import java.time.Duration
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._sToD
import static model.WeekOrMonth.MONTH
import static model.WeekOrMonth.WEEK

//@CompileStatic
class Model {

    @Bindable
    boolean updateToggle

    @Bindable
    String currentDir // = new File(".").getCanonicalPath().toString()

    void setCurrentDir(String currentDir) {

        DataReader.currentDir = new File(currentDir).getCanonicalPath().toString()
        File path = new File(currentDir)
        if( ! (path.exists() && path.isDirectory()) ) {
            path.mkdirs()
        }
        firePropertyChange('currentDir', this.currentDir, this.currentDir = new File(currentDir).getCanonicalPath().toString())
    }

    // all project tasks data
    List<TaskInProject> taskList =[]

    //
    List<List> templateProjects =[]
    // stable sorted project list
    //List<String> allProjectNames

    //
    // pipeline data
    //

    // maximum number of parallel slots in pipeline
    int maxPipelineSlots = 0

    // the original elements to feed into the pipeline
    List<PipelineOriginalElement> pipelineElements = []


    /**
     * key: project name
     * value: integer, representing the shift of the project:
     * +7 = delay one week to the future.
     * 0 = no shift.
     * -7 = due date one week earlier.
     */
    Map<String, Integer> projectDayShift = [:]

    // capa available data

    /**
     * Cache for recalc of capaAvailable.
     * this is needed, because the size of time keys depend on
     * the position of tasks - and that changes when projects are moved
     */
    private def jsonSlurp = ""

    /**
     * key: department -->
     * key: year-week-string 2020-W04 -->
     * value: the capaAvailable
     */
    Map<String, Map<String, YellowRedLimit>> capaAvailable = [:]



    List<String> projectSequence = []

    /**
     * @param project
     * @return List of all Tasks with project name project
     */
    //@Memoized
    List<TaskInProject> getProject(String project) {
        def t = RunTimer.getTimerAndStart('getProject')
        def r = taskList.findAll {it.project == project}
        t.stop()
        r
    }

    PipelineOriginalElement getPipelineElement(String project) {
        PipelineOriginalElement e = pipelineElements.find {
            it.project == project
        }
        e
    }

    /**
     * @return List of Strings with all projectNames found in
     */
    List<String> _getAllProjects() {
        def t = RunTimer.getTimerAndStart('getAllProjects')
        def r = (taskList*.project).unique()
        t.stop()
        r
    }

    /**
     * @return the minimum time of all tasks
     */
    Date getStartOfTasks() {
        def t = RunTimer.getTimerAndStart('getStartOfTasks')
        def r = (taskList*.starting).min()
        t.stop()
        r
    }

    /**
     * @return the maximum time of all tasks
     */
    Date getEndOfTasks() {
        def t = RunTimer.getTimerAndStart('getEndOfTasks')
        def r = (taskList*.ending).max()
        t.stop()
        r
    }


    /**
     * @return even if data is sparce, deliver continous list of timekey strings. Every week.
     */
    Duration years20 = Duration.of(20 * 365 * 20, ChronoUnit.DAYS)
    //Duration oneMonth = Duration.of(1, ChronoUnit.MONTHS)

    List<String> getFullSeriesOfTimeKeys(WeekOrMonth weekOrMonth) {
        def t = RunTimer.getTimerAndStart('getFullSeriesOfTimeKeys')

        def result = []

        Date s = getStartOfTasks()
        Date e = getEndOfTasks()

        if(s && e) {
            //use(TimeCategory) {
                if (e - s > years20.toDays()) {
                    throw new VpipeDataException("Dauer von Anfang bis Ende\n" +
                            "der Tasks zu lange ( > 20 Jahre ): ${s.toString()} bis ${e.toString()}")
                }
            //}


            if (weekOrMonth == WEEK) {
                s = DateExtension.getStartOfWeek(s)
                while (s < e) {
                    result << DateExtension.getWeekYearStr(s)
                    s = convertToDate(convertToLocalDate(s).plusDays(7))
                }
            } else {
                s = DateExtension.getStartOfMonth(s)
                while (s < e) {
                    result << DateExtension.getMonthYearStr(s)
                    //use(TimeCategory) {
                        s = convertToDate(convertToLocalDate(s).plusMonths(1))
                    //}
                }
            }
            //result.sort()
        }
        t.stop()
        result
    }

    @CompileStatic
    LocalDate convertToLocalDate(Date dateToConvert) {
        dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
    }

    Date convertToDate(LocalDate dateToConvert) {
        Date.from(dateToConvert.atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant())
    }


    long sizeLastAccess = -1
    List<String> allDepartmentsCache = []

    List<String> getAllDepartments() {


        //def t = RunTimer.getTimerAndStart('getAllDepartments')
        if(capaAvailable) {
            capaAvailable.keySet().toList()
        } else {
            if (taskList.size() != sizeLastAccess) {
                allDepartmentsCache = taskList*.department.unique()
                sizeLastAccess = taskList.size()
            }
            allDepartmentsCache
        }
        //t.stop()
    }


    Date cachedStartOfTasks
    Date cachedEndOfTasks

    def reCalcCapaAvailableIfNeeded() {
        def t = RunTimer.getTimerAndStart('reCalcCapaAvailableIfNeeded')
        def currentStart = getStartOfTasks()
        def currentEnd = getEndOfTasks()
        if( ! (currentStart >= cachedStartOfTasks && currentEnd <= cachedEndOfTasks) ) {
            if( jsonSlurp ) {
                capaAvailable = calcCapa(jsonSlurp)
            }
        }
        cachedStartOfTasks = currentStart
        cachedEndOfTasks = currentEnd
        t.stop()
    }

    Closure<GString> fileErr = {"" as GString}

    @CompileStatic(TypeCheckingMode.SKIP)
    Map<String, Map<String, YellowRedLimit>> calcCapa(def jsonSlurp) {

        Map<String, Map<String, YellowRedLimit>> result = [:]

        def t = RunTimer.getTimerAndStart('calcCapa')
        t.withCloseable {


            this.jsonSlurp = jsonSlurp

            fileErr = { "Fehler beim Lesen der Datei ${DataReader.get_CAPA_FILE_NAME()}\n" }


            def timeKeys = getFullSeriesOfTimeKeys(WEEK)

            // get the public holiday of that week and create a percentage based on 5 days (5-h)/5 (ph)
            if (!jsonSlurp.Kapa_Gesamt) {
                throw new VpipeDataException("${fileErr()}Eintrag 'Kapa_Gesamt' fehlt.")
            }
            if (!jsonSlurp.Kapa_Gesamt.Feiertage) {
                throw new VpipeDataException("${fileErr()}Eintrag 'Feiertage' in 'Kapa_Gesamt' fehlt.")
            }
            List publicHolidays = jsonSlurp.Kapa_Gesamt.Feiertage

            // get the company percentage profile from holidayPercentProfile (ch)
            if (!jsonSlurp.Kapa_Gesamt.Kapa_Profil) {
                throw new VpipeDataException("${fileErr()}Eintrag 'Kapa_Profil' in 'Kapa_Gesamt' fehlt.")
            }
            Map percentProfile = jsonSlurp.Kapa_Gesamt.Kapa_Profil
            percentProfile.keySet().each {
                checkWeekPattern(it)
            }

            // create a map of year-week-strings with a percentage based  cp = ch * ph
            Map<String, Double> overallPercentageProfile = [:]
            for (week in timeKeys) {
                Double percentagePubHol = percentageLeftAfterPublicHoliday(week, publicHolidays)
                Double percentageProfile = (Double) (percentProfile[week] == null ? 1.0 : (Double) (percentProfile[week] / 100))
                overallPercentageProfile[week] = percentagePubHol * percentageProfile
            }
            //println (overallPercentageProfile)

            // every department
            //println(jsonSlurp.Kapa_Abteilungen)
            Map departments = jsonSlurp.Kapa_Abteilungen
            if (!departments) {
                throw new VpipeDataException("${fileErr()}Kein Abschnitt 'Kapa_Abteilungen' definiert")
            }
            for (dep in departments) {

                Map<String, YellowRedLimit> capaMap = [:]
                // get the red and green limit
                if (!dep.value['Kapa']) {
                    throw new VpipeDataException("${fileErr()}Kein Abschnitt 'Kapa' in Abteilung '$dep.key' definiert")
                }
                if (!dep.value['Kapa'].rot) {
                    throw new VpipeDataException("${fileErr()}Kein Abschnitt 'rot' in 'Kapa' der Abteilung '$dep.key' definiert")
                }
                if (!dep.value['Kapa'].gelb) {
                    throw new VpipeDataException("${fileErr()}Kein Abschnitt 'gelb' in 'Kapa' der Abteilung '$dep.key' definiert")
                }
                Double norm_red = dep.value['Kapa'].rot as Double
                Double norm_yellow = dep.value['Kapa'].gelb as Double
                //println("Norm-Kapa $dep.key yellow: $norm_yellow red: $norm_red")
                for (timeKey in timeKeys) {
                    /*
                "Kapa_Profil": {
                    "2020-23": { "gelb": 140, "rot": 250 },
                    "2020-24": 100,
                    "2020-25": 80,
                    "2020-26": 100
                   }
                */
                    def p = 1.0
                    if(dep.value['Kapa_Profil']) {
                        dep.value['Kapa_Profil'].keySet().each{
                            checkWeekPattern(it)
                        }
                    }
                    // is a entry in the local Kapa_Profil. No? Then look for a cp --> value p other than 1
                    //if(! dep.value['Kapa_Profil']) {throw new VpipeDataException("${fileErr()}Kein Abschnitt 'Kapa_Profil' in Abteilung '$dep.key' definiert")}
                    def entry = dep.value['Kapa_Profil']?."$timeKey"
                    if (entry) {
                        if (entry instanceof Number) {
                            p = entry / 100
                        } else {
                            // if there is a "increase or decreas of normal capa (gelb!=null) --> set new normal capa
                            if (!entry?.rot) {
                                throw new VpipeDataException("${fileErr()}Kein Abschnitt 'rot' in 'Kapa_Profil'[$timeKey] der Abteilung '$dep.key' definiert")
                            }
                            if (!entry?.gelb) {
                                throw new VpipeDataException("${fileErr()}Kein Abschnitt 'gelb' in 'Kapa_Profil'[$timeKey] der Abteilung '$dep.key' definiert")
                            }
                            norm_red = entry.rot
                            norm_yellow = entry.gelb
                        }
                    } else {
                        p = overallPercentageProfile[timeKey]
                    }

                    // set yellow and red to p * normal
                    capaMap[timeKey] = new YellowRedLimit(yellow: norm_yellow * p, red: norm_red * p)
                }
                //println("$dep.key $capaMap")
                result[(String) (dep.key)] = capaMap
            }
            //t.stop()
        }
        result
    }


    def checkWeekPattern(String week) {
        def weekPattern = /\d\d\d\d-W\d\d/
        if( ! ( week =~ weekPattern) ) {
            throw new VpipeDataException("Wochen-Bezeichner falsch: $week\nSieht z. B. so aus: 2020-W02")
        }
    }

    def checkPipelineInProject() {
        if(pipelineElements) {
            fileErr = { " beim Lesen der Datei ${DataReader.get_PIPELINING_FILE_NAME()}\n" }
            pipelineElements.each {
                List<TaskInProject> p = getProject(it.project)
                if(p) {
                    Date startOfPipeline = _getStartOfWeek(it.startDate)
                    Date endOfPipeline = _getStartOfWeek(it.endDate) + 7
                    Date startOfProject = _getStartOfWeek(p*.starting.min())
                    Date endOfProject = _getStartOfWeek(p*.ending.max()) + 7
                    if(startOfPipeline < startOfProject) {
                        println("WARNUNG ${fileErr()}Projekt: $it.project... Integrations-Phase außerhalb des Projektes")
                    }
                    if(endOfPipeline > endOfProject) {
                        println("WARNUNG ${fileErr()}Projekt: $it.project... Integrations-Phase außerhalb des Projektes")
                    }
                }else{
                    throw new VpipeDataException("Fehler ${fileErr()}Projekt: $it.project existiert nicht in Projekt-Daten")
                }
            }
        }
    }

    def check() {
        List<String> depsTasks = getAllDepartments()
        Set<String> depsCapa= capaAvailable.keySet()
        List<String> remain = (List<String>)(depsTasks.clone())
        remain.removeAll(depsCapa)
        if(remain) throw new VpipeDataException("${fileErr()}Für folgende Abteilungen ist keine Kapa definiert: $remain")
    }

    Double percentageLeftAfterPublicHoliday(String week, List listOfPubHoliday) {
        Double p = 1.0d
        Date startOfWeek = StringExtension.toDateFromYearWeek(week)
        Date endOfWeek = convertToDate(convertToLocalDate(startOfWeek).plusDays(5)) // exclude Sa and Su
        for(String day in listOfPubHoliday) {
            def date = _sToD(day)
            if(date >= startOfWeek && date < endOfWeek) {
                p -= 0.2d
            }
        }
        p
    }




    //
    // loading & saving
    //

    def emtpyTheModel() {
        taskList = []
        projectDayShift = [:]
        templateProjects = []
        pipelineElements = []
        capaAvailable = [:]
        jsonSlurp = ''
        projectSequence = []
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    def readAllData() {

        def t = RunTimer.getTimerAndStart('Model::readAllData')
        try {

            emtpyTheModel()

            //
            // ordinary tasks
            //
            taskList = DataReader.readTasks()

            //
            // read the integrationPhaseData
            //
            DataReader.PipelineResult pr = DataReader.readPipelining()
            maxPipelineSlots = pr.maxPipelineSlots
            pipelineElements = pr.elements
            checkPipelineInProject()


            //
            // FIRST move projects - if needed
            //
            projectDayShift = DataReader.readDateShift()
            DateShiftTransformer dst = new DateShiftTransformer(this)
            dst.transform()

            //
            // THEN read and create templated projects -> on top of shifts of originals with shift
            //
            templateProjects = DataReader.readTemplates()
            TemplateTransformer tt = new TemplateTransformer(this)
            tt.transform()

            //
            // capa
            //
            jsonSlurp = DataReader.readCapa()
            if (jsonSlurp) {
                capaAvailable = calcCapa(jsonSlurp)
                if (capaAvailable) {
                    check()
                }
            }

            // if sequence has been saved AND project data removed or added: sync and let seqnence stable anyway
            projectSequence = DataReader.readSequence()
            List<String> all = _getAllProjects()
            def intersect = all.intersect(projectSequence)
            projectSequence.retainAll(intersect)
            def added = all - intersect
            projectSequence = added + projectSequence

        }catch(Exception e) {
            emtpyTheModel()
            //println(e.message)
            // e.printStackTrace()
            throw e
        } finally {
            setUpdateToggle(!getUpdateToggle())
            t.stop()
        }
    }

}
