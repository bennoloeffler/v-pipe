package model

import groovy.json.JsonSlurper
import groovy.time.TimeCategory
import utils.FileSupport
import utils.RunTimer

/**
 * reading and parsing data
 */
class DataReader {

    static def SILENT = true

    static String currentDir = "test" // overwrite this for application!

     static String TASK_FILE_NAME = "Projekt-Start-End-Abt-Kapa.txt"
     static String PIPELINING_FILE_NAME = "Integrations-Phasen.txt"
     static String DATESHIFT_FILE_NAME = "Projekt-Verschiebung.txt"
     static String CAPA_FILE_NAME = "Abteilungs-Kapazitaets-Angebot.txt"
     static String SCENARIO_FILE_NAME = "Szenario-Kopie-Original-Verschiebung.txt"
     static String SEQUENCE_FILE_NAME = "Projekt_Sequenz.txt"
     static String PROJECT_TEMPLATE_FILE_NAME = "Vorlagen-Projekt-Start-End-Abt-Kapa.txt"
     static String PIPELINE_TEMPLATE_FILE_NAME = "Vorlagen-Integrations-Phasen.txt"
    static String PROJECT_DELIVERY_DATE_FILE_NAME = "Projekt-Liefertermin.txt"

    static String path(String fileName){
        currentDir + "/" + fileName
    }

    static String get_PROJECT_TEMPLATE_FILE_NAME() {
        path PROJECT_TEMPLATE_FILE_NAME
    }

    static String get_PROJECT_DELIVERY_DATE_FILE_NAME() {
        path PROJECT_DELIVERY_DATE_FILE_NAME
    }

    static String get_TASK_FILE_NAME() {
        path TASK_FILE_NAME
    }

    static String get_PIPELINING_FILE_NAME() {
        path PIPELINING_FILE_NAME
    }

    static String get_PIPELINING_TEMPLATE_FILE_NAME() {
        path PIPELINE_TEMPLATE_FILE_NAME
    }

    static String get_DATESHIFT_FILE_NAME() {
        path DATESHIFT_FILE_NAME
    }

    static String get_CAPA_FILE_NAME() {
        path CAPA_FILE_NAME
    }

    static String get_SCENARIO_FILE_NAME() {
        path SCENARIO_FILE_NAME
    }

    static String get_SEQUENCE_FILE_NAME() {
        path SEQUENCE_FILE_NAME
    }


    /**
     * read templates
     * @return
     */
    static List<TaskInProject> readProjectTemplates() {
        String text = FileSupport.getTextOrEmpty(get_PROJECT_TEMPLATE_FILE_NAME())
        return readTasks(text, true)
    }


    /**
     * reads data lines into a list of model.TaskInProject
     * Default file name: Projekt-Start-End-Abt-Capa.txt
     * This reflects the order of the data fields.
     * Start and End are Dates, formatted like: dd.MM.yyyy
     * Capa is capacityNeededByThisTask and is a float (NO comma - but point for decimal numbers)
     * Default separator: any number of spaces, tabs, commas, semicolons (SEPARATOR_ALL)
     * If whitespace is in project names or department names, semicolon or comma is needed (SEPARATOR_SC)
     */
    static List<TaskInProject> readTasks() {
        String text = FileSupport.getTextOrEmpty(get_TASK_FILE_NAME())
        return readTasks(text, false)
    }

    static List<TaskInProject> readTasks(String text, boolean templates) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parseTasks(splitLines, templates)
    }

    static List<TaskInProject> parseTasks(List<List<String>> splitLines, boolean templates) {
        List<TaskInProject> taskList = []
        new RunTimer(true).withCloseable {
            def i = 0 // count the lines

            SILENT ?: println("\nstart parsing data file:   " + (templates ? get_PROJECT_TEMPLATE_FILE_NAME() : get_TASK_FILE_NAME()))
            def errMsg = { "" }
            //String all = new File(FILE_NAME).text
            //all.eachLine {
            splitLines.each { strings ->
                try {
                    i++
                    SILENT ?: println("\n$i raw-data:   " + line)
                    errMsg = {
                        "${(templates ? get_PROJECT_TEMPLATE_FILE_NAME() : get_TASK_FILE_NAME())}\nZeile $i: Zerlegt: $strings\n"
                    }
                    if (strings.size() != 5 && strings.size() != 6) {
                        throw new VpipeDataException(errMsg() +
                                "Keine 5 bzw. 6 Daten-Felder gefunden mit Regex-SEPARATOR = " + FileSupport.SEPARATOR +
                                "\nSoll-Format: 'Projekt-Name Task-Start Task-Ende Abteilungs-Name Kapa-Bedarf'")
                    }
                    SILENT ?: println("$i split-data: " + strings)

                    Date start = strings[1].toDate()
                    Date end = strings[2].toDate()
                    Date pastOrTwoDigitsDateTest = "1.1.1970".toDate()
                    if (start < pastOrTwoDigitsDateTest || end < pastOrTwoDigitsDateTest) {
                        throw new VpipeDataException(errMsg() + "Start oder Ende liegen dramatisch vor 1970 (${start.toString()}, ${end.toString()})")
                    }
                    if (!start.before(end)) {
                        throw new VpipeDataException(errMsg() + "Start (${start.toString()}) liegt nicht vor Ende: (${end.toString()})")
                    }
                    use(TimeCategory) {
                        if (end - start > 20.years) {
                            throw new VpipeDataException(errMsg() + "Task zu lang! ${start.toString()} " +
                                    "bis ${end.toString()}\nist mehr als 20 Jahre")
                        }
                    }

                    def tip = new TaskInProject(
                            strings[0],
                            strings[1].toDate(),
                            strings[2].toDate(),
                            strings[3],
                            Double.parseDouble(strings[4]),
                            strings.size() > 5 ? strings[5] : ''
                    )
                    taskList << tip
                    SILENT ?: println("$i task-data:  " + tip)
                } catch (VpipeDataException v) {
                    throw v
                } catch (Exception e) {
                    throw new VpipeDataException(errMsg() + "\nVermutlich Fehler beim parsen von \nDatum (--> 22.4.2020) oder Kommazahl (--> 4.5 Punkt statt Komma!)\n Grund: ${e.getMessage()}")
                }
            }
        }//t.stop("parsing file ${(templates ? get_PROJECT_TEMPLATE_FILE_NAME(): get_TASK_FILE_NAME())}")
        if(!templates && !taskList){throw new VpipeDataException("${get_TASK_FILE_NAME()} enthält keine Daten")}
        if( taskList.size() > 3000 ){println("W A R N U N G:\n${(templates ? get_PROJECT_TEMPLATE_FILE_NAME(): get_TASK_FILE_NAME())} enthält ${taskList.size()} Datensätze. GUI wird seeeehr LANGSAM...")}
        return taskList
    }

    static Map<String, Date> readPromisedDeliveryDates() {
        def result = new HashMap<String, Date>()
        try {
            String text = FileSupport.getTextOrEmpty(get_PROJECT_DELIVERY_DATE_FILE_NAME())
            List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
            splitLines.each {
                result.put(it[0], it[1].toDate())
            }
        } catch (Exception e) {
            throw new VpipeDataException(get_PROJECT_DELIVERY_DATE_FILE_NAME() + "\nVermutlich Fehler beim parsen von \nDatum (--> 22.4.2020)\n Grund: ${e.getMessage()}")

        }
        result
    }


    /**
     * @return int maxPipelineSlots and List<PipelineOriginalElement> pipelineElements
     */
    static class PipelineResult {
        Integer maxPipelineSlots
        List<PipelineOriginalElement> elements
    }
    static PipelineResult readPipelining() {
        String text = FileSupport.getTextOrEmpty(get_PIPELINING_FILE_NAME())
        return readPipelining(text)
    }

    static PipelineResult readPipelining(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parsePipelining(splitLines)
    }

    static PipelineResult parsePipelining(List<List<String>> splitLines) {
        int maxPipelineSlots = 0
        List<PipelineOriginalElement> pipelineElements = []
        if(splitLines) {
            try {
                maxPipelineSlots = splitLines[0][0].toInteger()
                if (maxPipelineSlots == 0) {
                    println("WARNUNG: In ${get_PIPELINING_FILE_NAME()} ist unbegrenzte Kapazität eingestellt (0 = kein Slot = unbegrenzt)...\n Pipelining wird ignoriert!")
                }
            } catch(Exception e) {
                throw new VpipeDataException("Fehler beim Lesen von ${get_PIPELINING_FILE_NAME()}.\nErste Zeile muss exakt eine Ganzzahl sein: Slots der Pipeline.")
            }
        }
        if(splitLines.size()>1) {
            (1..splitLines.size()-1).each {
                def line = splitLines[it]
                def errMsg = {"Lesen von Datei ${get_PIPELINING_FILE_NAME()} fehlgeschlagen.\nDatensatz: ${line}"}
                if(line.size() != 4) {
                    throw new VpipeDataException(errMsg() + "\nEs sind keine 4 Elemente.")
                }
                try {
                    //def pe = new PipelineOriginalElement(project: line[0], startDate: line[1].toDate(), endDate: line[2].toDate(), pipelineSlotsNeeded: line[3].toInteger() )
                    Date start = line[1].toDate()
                    Date end = line[2].toDate()
                    if( ! start.before(end)) {
                        throw new VpipeDataException(errMsg() + "\nStart liegt nicht vor Ende.")
                    }
                    if(line[3].toInteger() > maxPipelineSlots) {
                        throw new VpipeDataException(errMsg() +"\nerforderliche Pipeline-Slots (${line[3].toInteger()}) größer als Maximum($maxPipelineSlots)")
                    }
                    def pe = new PipelineOriginalElement(
                            project: line[0],
                            startDate: start,
                            endDate: end,
                            pipelineSlotsNeeded: line[3].toInteger()
                    )

                    pipelineElements << pe
                } catch (VpipeDataException v) {
                    throw v
                }catch (Exception e) {
                    throw new VpipeDataException(errMsg() + '\nVermutung:\na) Datum falsch (dd.MM.yyyy). Oder\nb) Pipeline-Bedarf ist keine Zahl', e)
                }
            }
        }
        List<String> projects = pipelineElements*.project
        def diff = projects.countBy{it}.grep{it.value > 1}.collect{it}
        if(diff) {
            throw new VpipeDataException("Lesen von Datei ${get_PIPELINING_FILE_NAME()} fehlgeschlagen.\nMehrfacheinträge für Projekte: $diff")
        }
        new PipelineResult(maxPipelineSlots: maxPipelineSlots, elements: pipelineElements)
    }


    /**
     * pipeline templates
     */
    static List<PipelineOriginalElement> readPipeliningTemplates() {
        String text = FileSupport.getTextOrEmpty(get_PIPELINING_TEMPLATE_FILE_NAME())
        return readPipeliningTemplates(text)
    }

    static List<PipelineOriginalElement>  readPipeliningTemplates(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parsePipeliningTemplates(splitLines)
    }

    static List<PipelineOriginalElement>  parsePipeliningTemplates(List<List<String>> splitLines) {
        List<PipelineOriginalElement> pipelineElements = []

        if(splitLines.size()>0) {
            (0..splitLines.size()-1).each {
                def line = splitLines[it]
                def errMsg = {"Lesen von Datei ${get_PIPELINING_FILE_NAME()} fehlgeschlagen.\nDatensatz: ${line}"}
                if(line.size() != 4) {
                    throw new VpipeDataException(errMsg() + "\nEs sind keine 4 Elemente.")
                }
                try {
                    //def pe = new PipelineOriginalElement(project: line[0], startDate: line[1].toDate(), endDate: line[2].toDate(), pipelineSlotsNeeded: line[3].toInteger() )
                    Date start = line[1].toDate()
                    Date end = line[2].toDate()
                    if( ! start.before(end)) {
                        throw new VpipeDataException(errMsg() + "\nStart liegt nicht vor Ende.")
                    }
                    /*
                    if(line[3].toInteger() > maxPipelineSlots) {
                        throw new VpipeDataException(errMsg() +"\nerforderliche Pipeline-Slots (${line[3].toInteger()}) größer als Maximum($maxPipelineSlots)")
                    }*/
                    def pe = new PipelineOriginalElement(
                            project: line[0],
                            startDate: start,
                            endDate: end,
                            pipelineSlotsNeeded: line[3].toInteger()
                    )

                    pipelineElements << pe
                } catch (VpipeDataException v) {
                    throw v
                }catch (Exception e) {
                    throw new VpipeDataException(errMsg() + '\nVermutung:\na) Datum falsch (dd.MM.yyyy). Oder\nb) Pipeline-Bedarf ist keine Zahl', e)
                }
            }
        }
        List<String> projects = pipelineElements*.project
        def diff = projects.countBy{it}.grep{it.value > 1}.collect{it}
        if(diff) {
            throw new VpipeDataException("Lesen von Datei ${get_PIPELINING_FILE_NAME()} fehlgeschlagen.\nMehrfacheinträge für Projekte: $diff")
        }
        pipelineElements
    }


    /**
     * @return day shift of the projects
     */
    static Map<String, Integer> readDateShift() {
        String text = FileSupport.getTextOrEmpty(get_DATESHIFT_FILE_NAME())
        return readDateShift(text)
    }

    static Map<String, Integer> readDateShift(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parseDateShift(splitLines)
    }


    static Map<String, Integer> parseDateShift(List<List<String>> splitLines) {
        Map<String, Integer> projectDayShift = [:]
        //List<String[]> lines = FileSupport.getDataLinesSplitTrimmed(DataReader.DATESHIFT_FILE_NAME)
        splitLines.each { line ->
            def errMsg = {"Parsen von Datei ${get_DATESHIFT_FILE_NAME()} fehlgeschlagen.\nDatensatz: ${line}"}
            if(line?.size() != 2) {
                throw new VpipeDataException(errMsg()+"\nJede Zeile braucht zwei Einträge")
            }
            try {
                def project = line[0]
                def days = line[1] as Integer
                projectDayShift[project] = days
            } catch (Exception e) {
                throw new VpipeDataException(errMsg() + '\nVermutung... Der Tagesversatz ist keine Ganz-Zahl.', e)
            }
        }
        projectDayShift
    }


    static String capaTextCache

    /**
     * @return def jsonSlurp
     */
    static def  readCapa() {
        def result =[:]
        File f = new File(get_CAPA_FILE_NAME())
        if(f.exists()) {
            try {
                def slurper = new JsonSlurper()
                result = slurper.parseText(f.text)
                capaTextCache = f.text
            } catch (Exception e) {
                throw new VpipeDataException("Problem in JSON-Format von Datei ${get_CAPA_FILE_NAME()}:\n${e.getMessage()}")
            }
        }
        result
    }


    /**
     * @return List of Scenario-Projects: "copyProject originalProject daysShift"
     */
    static List<List> readScenario() {
        String text = FileSupport.getTextOrEmpty(get_SCENARIO_FILE_NAME())
        return readScenario(text)
    }

    static List<List> readScenario(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parseScenario(splitLines)
    }

    static List<List> parseScenario(List<List> splitLines) {
        List result = []
        def line = 0
        def errMsg = ''
        splitLines.each { words ->
            errMsg = {"Problem in Datei ${get_SCENARIO_FILE_NAME()}\nZeile ${line}: Zerlegt: ${words}\n"}
            if (words.size() != 3) {
                throw new VpipeDataException("${errMsg()}Es müssen 3 Daten-Felder sein")
            }
            def days = 0
            try {
                days = words[2].toInteger()
            } catch(Exception e) {
                throw new VpipeDataException("${errMsg()}Umwandeln in Ganzahl gescheitert. Das ist keine: ${words[2]}")
            }
            if(days > 20*365) {
                throw new VpipeDataException("${errMsg()}Verschiebung in Tagen größer 20 Jahre: ${words[2]}")
            }
            result[line++] = [words[0], words[1], days]
        }
        // find double entries
        Set<String> doublettes = []
        result.each { l ->
            if (result.count{ it[0] == l[0] } > 1) {
                doublettes << l[0]
            }
        }
        if(doublettes) {
            throw new VpipeDataException("Problem in Datei ${get_SCENARIO_FILE_NAME()}\n Doubletten: $doublettes ")
        }


        result
    }



    static List<String>  readSequence() {
        String text = FileSupport.getTextOrEmpty(get_SEQUENCE_FILE_NAME())
        return readSequence(text)
    }

    static List<String> readSequence(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parseSequence(splitLines)
    }

    static List<String> parseSequence(List<List<String>> splitLines) {
        List<String> result = []
        for(List<String> line in splitLines) {
            assert line.size() == 1
            result << line[0]
        }
        result
    }

    public static void main(String[] args) {
        def templates = readProjectTemplates()
        println templates
    }


}
