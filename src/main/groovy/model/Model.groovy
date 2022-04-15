package model

import extensions.DateExtension
import extensions.StringExtension
import groovy.beans.Bindable
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.TypeCheckingMode
import groovy.yaml.YamlSlurper
import org.pcollections.HashTreePSet
import org.pcollections.PSet
import transform.DateShiftTransformer
import transform.ScenarioTransformer
import utils.RunTimer

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors

import static extensions.DateHelperFunctions.*
import static model.WeekOrMonth.WEEK

@CompileStatic
class Model {

    @Bindable
    boolean projectsAndTemplatesSwapped = false

    @Bindable
    boolean dirty = false

    PSet<String> set = HashTreePSet.empty();

    @Bindable
    boolean updateToggle

    @Bindable
    String currentDir // = new File(".").getCanonicalPath().toString()

    void setCurrentDir(String currentDir) {
        DataReader.currentDir = new File(currentDir).getCanonicalPath().toString()
        File path = new File(currentDir)
        if (!(path.exists() && path.isDirectory())) {
            path.mkdirs()
        }
        firePropertyChange('currentDir', this.currentDir, this.currentDir = new File(currentDir).getCanonicalPath().toString())
    }

    //--------------------------------
    //
    //   DATA of the MODEL
    //
    //--------------------------------

    // all project tasks data
    List<TaskInProject> taskList = []

    // the corresponding delivery date
    Map<String, Date> deliveryDates = [:]

    Map<String, String> projectComments = [:]

    /**
     * @see ScenarioTransformer: this copies and moves the projects from taskList
     */
    List<List> scenarioProjects = []

    // maximum number of parallel slots in pipeline
    int maxPipelineSlots = 0

    // the pipeline elements (to put into the pipeline). One for every project.
    List<PipelineElement> pipelineElements = []

    // those are the pipeline elements for templates
    List<PipelineElement> templatePipelineElements = []

    /**
     * @see DateShiftTransformer: just moves projects
     * key: project name
     * value: integer, representing the shift of the project:
     * +7 = all tasks one week to the future.
     * 0 = no shift.
     * -7 = all dates one week earlier.
     */
    Map<String, Integer> projectDayShift = [:]

    /**
     * Cache for recalc of capaAvailable.
     * this is needed, because the size of time keys depend on
     * the position of tasks - and that changes when projects are moved
     */
    def capaFileRawYamlSlurp = ""

    /**
     * key: department -->
     * key: year-week-string 2020-W04 -->
     * value: the capaAvailable
     */
    Map<String, Map<String, YellowRedLimit>> capaAvailable = [:]

    // all project template data
    List<TaskInProject> templateList = []

    // the sequence (e.g. if the user moves a project up or down)
    List<String> projectSequence = []
    List<String> templateSequence = [] //just for swapping templates and projects


    def fireUpdate() {
        setDirty(true)
        setUpdateToggle(!updateToggle)
    }


    Date getDeliveryDate(String project) {
        def date = deliveryDates[project]
        if (!date) {
            def projectTasks = getProject(project)
            date = (projectTasks*.ending).max()
            deliveryDates[project] = date
        }
        date
    }


    /**
     * @param project
     * @return List of all Tasks with project name project
     */
    List<TaskInProject> getProject(String project) {
        def r
        r = taskList.findAll { it.project == project }
        def departments = getAllDepartments()
        r = r.sort { a, b ->
            departments.indexOf(a.department) - departments.indexOf(b.department)
        }
        r
    }

    def addProject(List<TaskInProject> tasks) {
        assert (tasks && tasks.size() > 0)
        projectSequence.add(0, tasks[0].project)
        taskList.addAll(tasks)
        reCalcCapaAvailableIfNeeded()
        fireUpdate()
    }


    def deleteProjectTask(int idx, project) {
        def r = taskList.findAll { it.project == project }
        if (r.size() > 1) {
            def departments = getAllDepartments()
            r = r.sort { a, b ->
                departments.indexOf(a.department) - departments.indexOf(b.department)
            }
            TaskInProject t = r[idx]
            taskList.remove(t)
        }
    }


    def copyProjectTask(int idx, project) {
        def r = taskList.findAll { it.project == project }
        def departments = getAllDepartments()
        r = r.sort { a, b ->
            departments.indexOf(a.department) - departments.indexOf(b.department)
        }
        TaskInProject t = r[idx]
        TaskInProject copy = new TaskInProject(t.project, t.starting, t.ending, t.department, t.capacityNeeded, t.description)
        taskList.add(copy)
    }


    PipelineElement getPipelineElement(String project) {
        PipelineElement e = pipelineElements.find {
            it.project == project
        }
        e
    }

    static List<TaskInProject> deepClone(List<TaskInProject> originals) {
        def result = []
        for (o in originals) {
            result.add(o.clone())
        }
        result
    }


    PipelineElement copyPipelineFromTemplate(String templateName, String copyName, Date copyEndDate) {
        PipelineElement pe = templatePipelineElements.find { templateName == it.project }.clone()
        assert pe
        pe.project = copyName
        def t = getTemplate(templateName)
        def templateEndDate = (t*.ending).max()
        def diff = copyEndDate - templateEndDate
        pe.startDate += diff
        pe.endDate += diff
        pe
    }


    List<TaskInProject> copyFromTemplate(String templateName, String copyName, Date copyEndDate) {
        if (getProject(copyName)) throw new RuntimeException("Projektname schon vorhanden: " + copyName)

        def now = new Date()
        if (Math.abs(now - copyEndDate) > 20 * 365) throw new RuntimeException("Projekt-End-Datum ist 20 Jahre entfernt" + copyName)
        def t = getTemplate(templateName)
        def copy = deepClone(t)
        copy*.project = copyName
        def templateEndDate = (copy*.ending).max()
        def diff = copyEndDate - templateEndDate
        for (task in copy) {
            task.starting += diff
            task.ending += diff
        }
        copy
    }


    PipelineElement createPipelineForProject(List<TaskInProject> project) {

        // exactly ONE element in a newly created project!
        assert project
        assert project[0]
        assert project.size() == 1

        PipelineElement pe = new PipelineElement(
                project: project[0].project,
                startDate: project[0].starting,
                endDate: project[0].ending,
                pipelineSlotsNeeded: 1
        )
        pe
    }

    List<TaskInProject> createProject(String copyName, Date copyEndDate) {
        if (getProject(copyName)) throw new RuntimeException("Projektname schon vorhanden: " + copyName)
        if (Math.abs(new Date() - copyEndDate) > 20 * 365) throw new RuntimeException("Projekt-End-Datum ist 20 Jahre entfernt" + copyName)
        //def result = [] // List<TaskInProject>
        def d = "dummy-dep"
        if (getAllDepartments().size() > 0) {
            d = getAllDepartments()[0]
        }
        def task = new TaskInProject(copyName, copyEndDate - 28, copyEndDate, d, 10, "")
        [task]
    }


    List<String> getAllTemplates() {
        (templateList*.project).unique()
    }


    List<TaskInProject> getTemplate(String template) {
        def t = templateList.findAll { it.project == template }
        def departments = getAllDepartments()
        t.sort { a, b ->
            departments.indexOf(a.department) - departments.indexOf(b.department)
        }
    }


    /**
     * @return List of Strings with all projectNames found in
     */
    List<String> _getAllProjects() {
        RunTimer.getTimerAndStart('getAllProjects').withCloseable {
            (taskList*.project).unique()
        }
    }

    /**
     * @return the minimum time of all tasks
     */
    Date getStartOfTasks() {
        RunTimer.getTimerAndStart('getStartOfTasks').withCloseable {
            (taskList*.starting).min()
        }
    }


    /**
     * @return the maximum time of all tasks
     */
    Date getEndOfTasks() {
        RunTimer.getTimerAndStart('getEndOfTasks').withCloseable {
            (taskList*.ending).max()
        }
    }


    Date getStartOfProjects() {
        Date minDelDate = deliveryDates.values().min()
        Date start = getStartOfTasks()
        def result = (minDelDate && minDelDate < start) ? minDelDate : start
        if (result) {
            //result = result - 5 * 7
        }
        result
    }


    Date getEndOfProjects() {
        Date maxDelDate = deliveryDates.values().max()
        Date end = getEndOfTasks()
        def result = maxDelDate && maxDelDate > end ? maxDelDate : end
        if (result) {
            //result = result + 5 * 7
        }
        result
    }


    /**
     * @return even if data is sparce, deliver continous list of timekey strings. Every week.
     */
    Duration years20 = Duration.of(20 * 365 * 20, ChronoUnit.DAYS)

    boolean tooFarAway20Y(Date d) {
        Math.abs(new Date() - d) > years20.toDays()
    }


    List<String> getFullSeriesOfTimeKeys(WeekOrMonth weekOrMonth) {
        Date s = getStartOfProjects()
        Date e = getEndOfProjects()

        getFullSeriesOfTimeKeysInternal(weekOrMonth, s, e)
    }


    @Memoized
    @CompileDynamic
    List<String> getFullSeriesOfTimeKeysInternal(WeekOrMonth weekOrMonth, Date s, Date e) {
        def result = []
        RunTimer.getTimerAndStart('getFullSeriesOfTimeKeys').withCloseable {

            if (s && e) {
                if (e - s > years20.toDays()) {
                    throw new VpipeDataException("Dauer von Anfang bis Ende\n" +
                            "der Tasks zu lange ( > 20 Jahre ): ${s.toString()} bis ${e.toString()}")
                }
                if (weekOrMonth == WEEK) {
                    s = DateExtension.getStartOfWeek(s)
                    while (s < e) {
                        result << DateExtension.getWeekYearStr(s)
                        s = DateExtension.convertToDate(DateExtension.convertToLocalDate(s).plusDays(7))
                    }
                } else {
                    //use (TimeCategory) {
                    s = DateExtension.getStartOfMonth(s)
                    while (s < e) {
                        result << DateExtension.getMonthYearStr(s)
                        s = DateExtension.convertToDate(DateExtension.convertToLocalDate(s).plusMonths(1))
                        //result << DateExtension.getMonthYearStr(s)
                    }
                    //}
                }
            }
        }
        result
    }


    long departmentSizeLastAccess = -1
    List<String> allDepartmentsCache = []

    List<String> getAllDepartments() {
        if (capaAvailable) {
            capaAvailable.keySet().toList()
        } else {
            if (taskList.size() != departmentSizeLastAccess) {
                allDepartmentsCache = taskList*.department.unique()
                departmentSizeLastAccess = taskList.size()
            }
            allDepartmentsCache
        }
    }


    void deleteProject(String projectToDelete) {
        taskList.removeIf {
            it.project == projectToDelete
        }
        if (pipelineElements) {
            pipelineElements.removeIf {
                it.project == projectToDelete
            }
        }

        projectSequence.remove(projectToDelete)

        fireUpdate()
    }


    Date cachedStartOfTasks
    Date cachedEndOfTasks

    def reCalcCapaAvailableIfNeeded() {
        RunTimer.getTimerAndStart('reCalcCapaAvailableIfNeeded').withCloseable {
            def currentStart = getStartOfProjects()
            def currentEnd = getEndOfProjects()
            if (!(currentStart >= cachedStartOfTasks && currentEnd <= cachedEndOfTasks) && capaFileRawYamlSlurp) {
                capaAvailable = calcCapa(capaFileRawYamlSlurp)
                calcAndInsertMonthlyCapaAvailable(capaAvailable)
            }
            cachedStartOfTasks = currentStart
            cachedEndOfTasks = currentEnd
        }
    }


    void calcAndInsertMonthlyCapaAvailable(Map<String, Map<String, YellowRedLimit>> capaAvailable) {

        // ALGORITHM of available capa

        // WEEKS
        //
        // Feiertage:
        // data: companyHolidays = coHoDayPerc[week] = calced on basis of public holidays (each reduces the week capacity by 1/5, so dont take Sa So)
        // Kapa_Profil:
        // data: companyWeekProfile coWeProf[week] = percentage to reduce the capa (e.g. for sesonal holiday time)
        // calc: companyWeekPercentage coWePerc[week] =  coHoDayPerc * coWeProf (of that week)
        //
        // Kapa_Abteilungen, Key: Resourcen-name (zB IBN)
        // Kapa:
        // data: ressourceCapa(yellow, red) = resCapa
        // Kapa_Profil:
        // data: resourceWeekCapa(y, r) = resWeekCapa starting from that week another base value
        // data: ressourceWeekProfile resWeProf[week] --> totally overwrites coWePerc
        //
        // calc realResWeekCapa = current resWeekCapa or ResCapa   *   exists?(resWeProf) or resWeProf or 100%

        // MONTHS
        // init all structure with zero? --> no. weeks are not sparse
        // for each resource, for each week:
        //   split the red and yellow numbers according to Mo - Fr that belong to which month
        //   and put it to the map of resource and according months

        def timeKeysWeek = getFullSeriesOfTimeKeys(WEEK)
        def departments = getAllDepartments()
        departments.each { dep ->
            timeKeysWeek.each { week ->
                Map<String, YellowRedLimit> monthPartsToAdd = getMonthParts(capaAvailable[dep][week], dep, week)
                monthPartsToAdd.each { monthPart ->
                    String month = monthPart.key
                    YellowRedLimit yellowRedLimit = capaAvailable[dep][month]
                    double oldYellow = 0
                    double oldRed = 0
                    if (yellowRedLimit) {
                        oldRed = yellowRedLimit.red
                        oldYellow = yellowRedLimit.yellow
                    }
                    capaAvailable[dep][month] =
                            new YellowRedLimit(yellow: oldYellow + monthPart.value.yellow,
                                    red: oldRed + monthPart.value.red)
                }

            }
        }
        /*
        //check if all months are there
        def timeKeysMonth = getFullSeriesOfTimeKeys(MONTH)
        departments.each {dep ->
            timeKeysMonth.each {month ->
                if(! capaAvailable[dep][month]){
                    //println "missing: " + dep + " " + month
                    capaAvailable[dep][month] = new YellowRedLimit(1,2) //kind of bugfix... to avoid infinity
                }
            }
        }
         */
    }

    static Map<String, YellowRedLimit> getMonthParts(YellowRedLimit yellowRedLimit, String dep, String week) {
        // start from the beginning of week and count to end (5) or until month changes.
        int countFirstMonth = 0
        Date startOfWeek = _getStartOfWeek(_wToD(week))
        Date endOfWeek = startOfWeek + 5
        Map<String, Double> months = [:] as Map<String, Double>
        for (day in startOfWeek..endOfWeek) {
            String month = _getMonthYearStr(day)
            months[month] = months[month] ? months[month] + 0.2d : 0.2d
        }
        Map<String, YellowRedLimit> result = [:]
        months.each {
            String month = it.key
            double percentage = months[month]
            result[month] = new YellowRedLimit(percentage * yellowRedLimit.yellow, percentage * yellowRedLimit.red)
        }
        result
    }


    Closure<GString> fileErr = { "" as GString }

    @CompileStatic(TypeCheckingMode.SKIP)
    Map<String, Map<String, YellowRedLimit>> calcCapa(def yamlSlurp, boolean withFileNameInErrorMessage = true) {
        Map<String, Map<String, YellowRedLimit>> result = [:]
        RunTimer.getTimerAndStart('calcCapa').withCloseable {
            this.capaFileRawYamlSlurp = yamlSlurp
            if (withFileNameInErrorMessage) {
                fileErr = { "Fehler beim Lesen der Datei ${DataReader.get_CAPA_FILE_NAME()}\n" }
            } else {
                fileErr = { "" }
            }
            def timeKeys = getFullSeriesOfTimeKeys(WEEK)
            //println ("calcCapa von ${timeKeys[0]} to ${timeKeys[timeKeys.size()-1]}")

            // get the public holiday of that week and create a percentage based on 5 days (5-h)/5 (ph)
            //if (!yamlSlurp.Kapa_Gesamt) {
            //    throw new VpipeDataException("${fileErr()}Eintrag 'Kapa_Gesamt' fehlt.")
            //}
            //if (!yamlSlurp.Kapa_Gesamt.Feiertage) {
            //    throw new VpipeDataException("${fileErr()}Eintrag 'Feiertage' in 'Kapa_Gesamt' fehlt.")
            //}
            List publicHolidays = []
            if (yamlSlurp.Kapa_Gesamt?.Feiertage) {
                publicHolidays = yamlSlurp.Kapa_Gesamt.Feiertage
            }
            // get the company percentage profile from holidayPercentProfile (ch)
            //if (!yamlSlurp.Kapa_Gesamt.Kapa_Profil) {
            //    throw new VpipeDataException("${fileErr()}Eintrag 'Kapa_Profil' in 'Kapa_Gesamt' fehlt.")
            //}

            Map percentProfile = [:]
            if (yamlSlurp.Kapa_Gesamt?.Kapa_Profil) {
                percentProfile = yamlSlurp.Kapa_Gesamt.Kapa_Profil
                percentProfile.keySet().each {
                    checkWeekPattern(it)
                }
            }

            // create a map of year-week-strings with a percentage based
            // percentOverall = profilePercentLeft * pubHolPercentLeft
            Map<String, Double> overallPercentageProfile = [:]
            for (week in timeKeys) {
                Double percentagePubHol = percentageLeftAfterPublicHoliday(week, publicHolidays)
                Double percentageProfile = (Double) (percentProfile[week] == null ? 1.0 : (Double) (percentProfile[week] / 100))
                overallPercentageProfile[week] = percentagePubHol * percentageProfile
            }
            Map departments = yamlSlurp.Kapa_Abteilungen
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
                    if (dep.value['Kapa_Profil']) {
                        dep.value['Kapa_Profil'].keySet().each {
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
        }
        //calcAndInsertMonthlyCapaAvailable(result)
        result
    }


    static def checkWeekPattern(String week) {
        def weekPattern = /\d\d\d\d-W\d\d/
        if (!(week =~ weekPattern)) {
            throw new VpipeDataException("Wochen-Bezeichner falsch: $week\nSieht z. B. so aus: 2020-W02")
        }
    }

    def checkPipelineInProject() {
        if (pipelineElements) {
            checkOnePipelineToEachProject(taskList, pipelineElements, "PROJEKTE")
            fileErr = { " beim Lesen der Datei ${DataReader.get_PIPELINING_FILE_NAME()}\n" }
            pipelineElements.each {
                List<TaskInProject> p = getProject(it.project)
                if (p) {
                    Date startOfPipeline = _getStartOfWeek(it.startDate)
                    Date endOfPipeline = _getStartOfWeek(it.endDate) + 7
                    Date startOfProject = _getStartOfWeek(p*.starting.min())
                    Date endOfProject = _getStartOfWeek(p*.ending.max()) + 7
                    if (startOfPipeline < startOfProject) {
                        println("WARNUNG ${fileErr()}Projekt: $it.project -> Integrations-Phase außerhalb des Projektes")
                    }
                    if (endOfPipeline > endOfProject) {
                        println("WARNUNG ${fileErr()}Projekt: $it.project -> Integrations-Phase außerhalb des Projektes")
                    }
                } else {
                    throw new VpipeDataException("Fehler ${fileErr()}Projekt: $it.project existiert nicht in Projekt-Daten")
                }
            }
        }
    }


    def checkPipelineTemplatesInTemplates() {
        if (templatePipelineElements) {
            checkOnePipelineToEachProject(templateList, templatePipelineElements, "VORLAGEN")
            fileErr = { " beim Lesen der Datei ${DataReader.get_TEMPLATE_PIPELINING_FILE_NAME()}\n" }
            templatePipelineElements.each {
                List<TaskInProject> p = getTemplate(it.project)
                if (p) {
                    Date startOfPipeline = _getStartOfWeek(it.startDate)
                    Date endOfPipeline = _getStartOfWeek(it.endDate) + 7
                    Date startOfProject = _getStartOfWeek(p*.starting.min())
                    Date endOfProject = _getStartOfWeek(p*.ending.max()) + 7
                    if (startOfPipeline < startOfProject) {
                        println("WARNUNG ${fileErr()}Projekt: $it.project -> Integrations-Phase außerhalb des Projektes")
                    }
                    if (endOfPipeline > endOfProject) {
                        println("WARNUNG ${fileErr()}Projekt: $it.project -> Integrations-Phase außerhalb des Projektes")
                    }
                } else {
                    throw new VpipeDataException("Fehler ${fileErr()}Projekt: $it.project existiert nicht in Template-Daten")
                }
            }
        }
    }

    static def checkOnePipelineToEachProject(List<TaskInProject> projects, List<PipelineElement> pipelineElements, String templateOrData) {

        Set<String> pipProjects = pipelineElements*.project.unique().toSet()
        Set<String> projProjects = projects*.project.unique().toSet()
        Collection<String> shouldBeAll = pipProjects.intersect(projProjects)

        Set<String> projectsWithoutPipeline = new HashSet<String>(projProjects)
        projectsWithoutPipeline.removeAll(shouldBeAll)
        Set<String> pipelineWithoutProject = new HashSet<String>(pipProjects)
        pipelineWithoutProject.removeAll(shouldBeAll)
        if (projectsWithoutPipeline) throw new VpipeDataException(templateOrData + ": Projekte ohne Pipline-Element: " + projectsWithoutPipeline)
        if (pipelineWithoutProject) throw new VpipeDataException(templateOrData + ": Pipeline-Element ohne Projekt: " + pipelineWithoutProject)
    }

    def check(List<TaskInProject> tasks) {
        List<String> depsTasks = tasks*.department.unique()
        Set<String> depsCapa = capaAvailable.keySet()
        List<String> remain = new ArrayList(depsTasks)
        remain.removeAll(depsCapa)
        if (remain) throw new VpipeDataException("${fileErr()}Für folgende Abteilungen (Projekte oder Vorlagen) ist keine Kapa definiert: $remain")
    }

    static Double percentageLeftAfterPublicHoliday(String week, List listOfPubHoliday) {
        Double p = 1.0d
        Date startOfWeek = StringExtension.toDateFromYearWeek(week) // TODO check if this is Monday
        Date endOfWeek = DateExtension.convertToDate(DateExtension.convertToLocalDate(startOfWeek).plusDays(5))
        // exclude Sa and Su
        for (String day in listOfPubHoliday) {
            def date = _sToD(day)
            if (date >= startOfWeek && date < endOfWeek) {
                p -= 0.2d
            }
        }
        p
    }


    //
    // loading & saving
    //

    def emptyTheModel() {
        taskList = []
        deliveryDates = [:]
        projectDayShift = [:]
        scenarioProjects = []
        pipelineElements = []
        capaAvailable = [:]
        DataReader.capaTextCache = null
        capaFileRawYamlSlurp = ''
        projectSequence = []
        templateList = []
        templatePipelineElements = []
        projectComments = [:]

        cachedStartOfTasks = null
        cachedEndOfTasks = null

        setDirty(false)
    }


    @CompileStatic(TypeCheckingMode.SKIP)
    def readAllData() {

        def t = RunTimer.getTimerAndStart('Model::readAllData')
        try {

            emptyTheModel()

            //
            // ordinary tasks
            //
            taskList = DataReader.readTasks()

            deliveryDates = DataReader.readPromisedDeliveryDates()


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
            // THEN read and create scenario projects -> on top of shifts of originals with shift
            //
            scenarioProjects = DataReader.readScenario()
            ScenarioTransformer tt = new ScenarioTransformer(this)
            tt.transform()

            //
            // capa
            //
            capaFileRawYamlSlurp = DataReader.readCapa()
            if (capaFileRawYamlSlurp) {
                capaAvailable = calcCapa(capaFileRawYamlSlurp)
                if (capaAvailable) {
                    check(taskList)
                    calcAndInsertMonthlyCapaAvailable(capaAvailable)
                }
            }

            // if sequence has been saved AND project data removed or added: sync and let seqnence stable anyway
            projectSequence = DataReader.readSequence()
            List<String> all = _getAllProjects()
            def intersect = all.intersect(projectSequence)
            projectSequence.retainAll(intersect)
            def added = all - intersect
            projectSequence = added + projectSequence

            //
            // templates - identical to tasks, but in different file and data structure
            //
            templateList = DataReader.readProjectTemplates()
            if (capaAvailable) {
                check(templateList)
            }

            if (templateList && pipelineElements) {
                if (new File(DataReader.get_TEMPLATE_PIPELINING_FILE_NAME()).exists()) {
                    templatePipelineElements = DataReader.readPipeliningTemplates()
                    checkPipelineTemplatesInTemplates()
                } else {
                    throw new VpipeDataException("Wenn die Dateien Integrations-Phasen.txt und\n" +
                            "Vorlagen-Projekt-Start-End-Abt-Kapa.txt vorhanden sind\n " +
                            "dann ist die Datei Vorlagen-Integrations-Phasen.txt notwendig.\n" +
                            "\nETWAS VERSTÄNDLICHER:\nWenn es Integrationsphasen und Vorlagen gibt,\ndann braucht es auch" +
                            " Vorlagen für die Integrationsphasen.")
                }
            }

        projectComments = DataReader.readComments()


        } catch (Exception e) {
            emptyTheModel()
            setVPipeHomeDir()
            throw e
        } finally {
            setDirty(false)
            fireUpdate()
            t.stop()
        }
    }

    void saveRessources(String yaml) {
        DataReader.capaTextCache = yaml
        capaFileRawYamlSlurp = DataReader.readCapa(true)
        capaAvailable = calcCapa(capaFileRawYamlSlurp)
        calcAndInsertMonthlyCapaAvailable(capaAvailable)
        fireUpdate()
    }

    /**
     * dont forget: view.gridPipelineModel.setSelectedProject(null)
     */
    void renameProject(String oldName, String newName) {
        def changeTask = { TaskInProject t -> if (t.project == oldName) t.project = newName }
        taskList.each changeTask
        templateList.each changeTask

        def replaceName = { String n -> n == oldName ? newName : n }
        projectSequence.replaceAll replaceName
        templateSequence.replaceAll replaceName

        def changeTemplElement = { PipelineElement p -> if (p.project == oldName) p.project = newName }
        pipelineElements.each changeTemplElement
        templatePipelineElements.each changeTemplElement

        deliveryDates.put(newName, deliveryDates.get(oldName))
        deliveryDates.remove(oldName)

        // PROBLEM: because current view.gridPipelineModel.selectedProject
        // needs to be set also and THEN fired. so fire from outside... Sorry
        //fireUpdate()
    }

    void renameDepartment(String oldName, String newName) {

        // in order to recalc getAllDepartments(), if no capa is defined
        departmentSizeLastAccess = -1

        taskList.each {
            if (it.department == oldName) {
                it.department = newName
            }
        }

        DataReader.capaTextCache = DataReader.capaTextCache.replace(oldName, newName)
        def slurper = new YamlSlurper()
        capaFileRawYamlSlurp = slurper.parseText(DataReader.capaTextCache)
        capaAvailable = calcCapa(capaFileRawYamlSlurp)
        calcAndInsertMonthlyCapaAvailable(capaAvailable)
        templateList.each { if (it.department == oldName) { it.department = newName } }

        fireUpdate()
    }

    /**
     * states of the model:
     * - loaded = there are Tasks
     * - not_loaded = there are NO tasks
     */
    void newModel() {
        emptyTheModel()
        setCurrentDir("./new-model")
        readAllData()
        setCurrentDir(System.getProperty("user.home") + "/v-pipe-data/modell-neu")
        //setVPipeHomeDir()
        fireUpdate()
    }

    void setVPipeHomeDir() {
        DataReader.currentDir = System.getProperty("user.home") + "/v-pipe-data"
        setCurrentDir(DataReader.currentDir)
    }

    Map<String, Date> swapStoreOfdeliveryDates = [:]

    def swapDeliveryDates() {
        if (projectsAndTemplatesSwapped) { // move back
            deliveryDates = swapStoreOfdeliveryDates
            swapStoreOfdeliveryDates = [:]
        } else { // save
            swapStoreOfdeliveryDates = deliveryDates
            deliveryDates = [:] // clear - in order to let templates "discover" their own
        }
    }


    void swapTemplatesAndProjects() {

        createTemplateSequence() // first!

        swapDeliveryDates()

        def swap = taskList
        taskList = templateList
        templateList = swap

        swap = pipelineElements
        pipelineElements = templatePipelineElements
        templatePipelineElements = swap

        swap = projectSequence
        projectSequence = templateSequence
        templateSequence = swap

        setProjectsAndTemplatesSwapped(!projectsAndTemplatesSwapped)

        reCalcCapaAvailableIfNeeded()
        fireUpdate()
    }

    List<String> createTemplateSequence() {
        templateSequence = templateList.stream().map { it.project }.distinct().collect(Collectors.toList())
    }

    void removePipeline() {
        assert (pipelineElements || templatePipelineElements), "pipline need to be there in templates or tasks"
        pipelineElements = []
        templatePipelineElements = []
        maxPipelineSlots = 0
        fireUpdate()
    }

    void createPipeline() {
        assert !(pipelineElements || templatePipelineElements), "no pipline at all..."

        maxPipelineSlots = 2

        projectSequence.each { project ->
            def tasks = getProject(project)
            Date start = tasks*.starting.min()
            Date end = tasks*.ending.max()
            int lenIP = (int) ((end - start) / 3)
            lenIP = lenIP == 0 ? 1 : lenIP
            start = end - lenIP
            pipelineElements << new PipelineElement(project: project, startDate: start, endDate: end, pipelineSlotsNeeded: 1)
        }
        templateSequence.each { project ->
            def tasks = getTemplate(project)
            Date start = tasks*.starting.min()
            Date end = tasks*.ending.max()
            int lenIP = (int) ((end - start) / 3)
            lenIP = lenIP == 0 ? 1 : lenIP
            start = end - lenIP
            templatePipelineElements << new PipelineElement(project: project, startDate: start, endDate: end, pipelineSlotsNeeded: 1)
        }

        fireUpdate()

    }


    def void saveComment(String projectName, String comment) {
        if (comment) {
            projectComments[projectName] = comment
        } else {
            projectComments.remove(projectName)
        }
        fireUpdate()
    }
}
