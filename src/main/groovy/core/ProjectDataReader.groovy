package core

import extensions.DateHelperFunctions

/**
 * reads data lines into a list of core.TaskInProject
 * Default file name: Projekt-Start-End-Abt-Capa.txt
 * This reflects the order of the data fields.
 * Start and End are Dates, formatted like: dd.MM.yyyy
 * Capa is capacityNeededByThisTask and is a float (NO comma - but point for decimal numbers)
 * Default separator: any number of spaces, tabs, commas, semicolons (SEPARATOR_ALL)
 * If whitespace is in project names or department names, semicolon or comma is needed (SEPARATOR_SC)
 */
class ProjectDataReader {

    static def FILE_NAME = "Projekt-Start-End-Abt-Capa.txt"
    static final def SEPARATOR_ALL = /[;,]+\s*|\s+/ // every whitespace, semicolon or comma
    static final def SEPARATOR_SC = /\s*[;,]+\s*/ // can read data with whitespace
    static def SEPARATOR = SEPARATOR_ALL
    static def SILENT = true

    static List<TaskInProject> getDataFromFile() {
        List<TaskInProject> taskList = []
        def i = 0 // count the lines
        SILENT?:println("\nstart reading data file:   " + FILE_NAME)
        def f = new File(FILE_NAME).eachLine {
            if(it.trim()) {
                try {
                    i++
                    String line = it.trim()
                    SILENT?:println("\n$i raw-data:   " + line)
                    String[] strings = line.split(SEPARATOR)
                    if(strings.size() != 5) throw new RuntimeException("\ndataline $i:\n$line\ndid not find 5 separated data fields SEPARATOR = "+SEPARATOR)
                    SILENT?:println("$i split-data: " + strings)
                    def tip = new TaskInProject(
                            project: strings[0],
                            starting: strings[1].toDate(),
                            ending: strings[2].toDate(),
                            department: strings[3],
                            capacityNeeded: Double.parseDouble(strings[4])
                    )
                    taskList << tip
                    SILENT?:println("$i task-data:  " + tip)
                } catch (Exception e) {
                    throw new VpipeException("DATA-ERROR: Reading data-file $FILE_NAME faild. Reason: ${e.getMessage()}")
                }
            }
        }
        return taskList
    }

}
