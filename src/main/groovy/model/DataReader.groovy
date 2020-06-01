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


    public static String TASK_FILE_NAME = "Projekt-Start-End-Abt-Kapa.txt"
    public static String PIPELINING_FILE_NAME = "Integrations-Phasen.txt"
    public static String DATESHIFT_FILE_NAME = "Projekt-Verschiebung.txt"
    public static String CAPA_FILE_NAME = "Abteilungs-Kapazitaets-Angebot.txt"


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
        String text = FileSupport.getTextOrEmpty(TASK_FILE_NAME)
        return readTasks(text)
    }

    static List<TaskInProject> readTasks(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parseTasks(splitLines)
    }

    static List<TaskInProject> parseTasks(List<List<String>> splitLines) {
        def t = new RunTimer(true)
        List<TaskInProject> taskList = []
        def i = 0 // count the lines
        SILENT?:println("\nstart parsing data file:   " + TASK_FILE_NAME)
        def errMsg ={""}
        //String all = new File(FILE_NAME).text
        //all.eachLine {
        splitLines.each { strings ->
                try {
                    i++
                    SILENT?:println("\n$i raw-data:   " + line)
                    errMsg = {"$TASK_FILE_NAME\nZeile $i: Zerlegt: $strings\n"}
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
        t.stop("parsing file $TASK_FILE_NAME")
        if( ! taskList){throw new VpipeDataException("$TASK_FILE_NAME enthält keine Daten")}
        return taskList
    }


    /**
     * @return int maxPipelineSlots and List<PipelineOriginalElement> pipelineElements
     */
    static Tuple2 readPipelining() {
        String text = FileSupport.getTextOrEmpty(PIPELINING_FILE_NAME)
        return readPipelining(text)
    }

    static Tuple2 readPipelining(String text) {
        List<List<String>> splitLines =  FileSupport.toSplitAndTrimmedLines(text)
        return parsePipelining(splitLines)
    }

    static Tuple2 parsePipelining(List<List<String>> splitLines) {
        int maxPipelineSlots = 0
        List<PipelineOriginalElement> pipelineElements = []
        if(splitLines) {
            try {
                maxPipelineSlots = splitLines[0][0].toInteger()
                if (maxPipelineSlots == 0) {
                    println("WARNUNG: In $PIPELINING_FILE_NAME ist unbegrenzte Kapazität eingestellt (0 = kein Slot = unbegrenzt)...\n Pipelining wird ignoriert!")
                }
            } catch(Exception e) {
                throw new VpipeDataException("Fehler beim Lesen von $PIPELINING_FILE_NAME.\nErste Zeile muss exakt eine Ganzzahl sein: Slots der Pipeline.")
            }
        }
        if(splitLines.size()>1) {
            (1..splitLines.size()-1).each {
                def line = splitLines[it]
                def errMsg = {"Lesen von Datei $PIPELINING_FILE_NAME fehlgeschlagen.\nDatensatz: ${line}"}
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
                    def pe = new PipelineOriginalElement(line[0], start, end, line[3].toInteger() )

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
            throw new VpipeDataException("Lesen von Datei $PIPELINING_FILE_NAME fehlgeschlagen.\nMehrfacheinträge für Projekte: $diff")
        }
        new Tuple2(maxPipelineSlots, pipelineElements)
    }


    /**
     * @return day shift of the projects
     */
    static Map<String, Integer> readDateShift() {
        String text = FileSupport.getTextOrEmpty(DATESHIFT_FILE_NAME)
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
            def errMsg = {"Parsen von Datei $DATESHIFT_FILE_NAME fehlgeschlagen.\nDatensatz: ${line}"}
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


    /**
     *
     * @return def jsonSlurp
     */
    static def  readCapa() {
        def result =[:]
        File f = new File(CAPA_FILE_NAME)
        if(f.exists()) {
            try {
                def slurper = new JsonSlurper()
                result = slurper.parseText(f.text)
            } catch (Exception e) {
                throw new VpipeDataException("Problem in JSON-Format von Datei $CAPA_FILE_NAME:\n${e.getMessage()}")
            }
        }
        result
    }

}
