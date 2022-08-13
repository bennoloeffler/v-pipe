println "\n\nplugin started"

//
// https://stackoverflow.com/questions/6157114/easiest-way-to-read-excel-files-in-groovy
// http://docs.groovy-lang.org/latest/html/documentation/grape.html


@Grab('org.apache.poi:poi:3.8')
@Grab('org.apache.poi:poi-ooxml:3.8')
@GrabExclude('xml-apis:xml-apis')

import model.DataReader
import model.DataWriter
import static extensions.DateHelperFunctions._sToD
import model.TaskInProject


import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.*
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook

static def readCell(XSSFSheet sheet, int lineNr, int columnNr) {
    def row = sheet.getRow(lineNr)
    Cell oneCell = row.getCell(columnNr)
    def val = oneCell.getStringCellValue()
    val
}

static TaskInProject readLine(XSSFSheet sheet, int lineNr) {
    String project = readCell(sheet, lineNr, 0)
    String startDate = readCell(sheet, lineNr, 1)
    String endDate = readCell(sheet, lineNr, 2)
    String department = readCell(sheet, lineNr, 3)
    String capaNeeded = readCell(sheet, lineNr, 4)
    String descriptionOrId = readCell(sheet, lineNr, 5)
    //List result = [project, startDate, endDate, department, capaNeeded, descriptionOrId]
    //result
    if(project) { // ignore empty lines
        new TaskInProject(project, _sToD(startDate), _sToD(endDate), department, Double.valueOf(capaNeeded), descriptionOrId)
    } else {
        null
    }
}

def readExcelToData(){
    String updateDir = pathToUpdateDir
    println "Import-Script wurde aktiviert: " + updateDir + "/plugin/import.groovy"
    println "Daten von hier werden transformiert und eingelesen:"
    println updateDir
    // pathToUpdateDir is a preset string variable, that points to the current update data directory
    //println "import.groovy is transforming data from here: " + new File(pathToUpdateDir).absolutePath
    String fileName = pathToUpdateDir + "/" + "daten.xlsx"
    println "Einlesen: " + fileName
    def excelFile = new File(fileName)
    List<TaskInProject> allLinesRead = []
    excelFile.withInputStream { is ->
        XSSFWorkbook workbook = new XSSFWorkbook(is)
        XSSFSheet sheet = workbook.getSheetAt(0) // the first worksheet
        int numOfRows = sheet.getLastRowNum()
        (0..numOfRows).each { row ->
            TaskInProject line = readLine(sheet, row)
            if(line) {
                println "Daten in Excel, Zeile $row: -> " + line
                allLinesRead << line
            } else {
                println "leer: Zeile $row"
            }
        }
    }

    println "\nJetzt Text-Daten erzeugen... In Datei:"
    String outputFileName = DataReader.get_UPDATE_TASK_FILE_NAME()
    println outputFileName
    List<TaskInProject> tasks = []

    allLinesRead.each{TaskInProject t ->
        tasks << t
    }
    DataWriter.writeTasksToFile(tasks, outputFileName)

    /* like that, all workbooks and rows, columns may be iterated, discovered
    (0..<workbook.numberOfSheets).each { sheetNumber ->
        XSSFSheet sheet = workbook.getSheetAt(sheetNumber)
        sheet.rowIterator().each { row ->
            row.cellIterator().each { cell ->
                println cell.toString()
            }
        }
    }
    */
}

// now do it...
readExcelToData()

println "plugin finished"
