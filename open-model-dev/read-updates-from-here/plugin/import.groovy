println "plugin started"

//
// https://stackoverflow.com/questions/6157114/easiest-way-to-read-excel-files-in-groovy
// http://docs.groovy-lang.org/latest/html/documentation/grape.html


@Grab('org.apache.poi:poi:3.8')
@Grab('org.apache.poi:poi-ooxml:3.8')
@GrabExclude('xml-apis:xml-apis')

import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.*;
import org.apache.poi.ss.usermodel.*;

def fileName = '/Users/benno/projects/v-pipe/open-model-dev/read-updates-from-here/plugin/daten.xlsx'
println "reading: " + fileName
def excelFile = new File(fileName)
excelFile.withInputStream { is ->
    workbook = new XSSFWorkbook(is)
    sheet = workbook.getSheetAt(0)
    row = sheet.getRow(0)
    Cell oneCell = row.getCell(0)
    val = oneCell.getStringCellValue()
    println "one value: " + val

    (0..<workbook.numberOfSheets).each { sheetNumber ->
        XSSFSheet sheet = workbook.getSheetAt( sheetNumber )
        sheet.rowIterator().each { row ->
            row.cellIterator().each { cell ->
                println cell.toString()
            }
        }
    }
}

println "plugin finished"
