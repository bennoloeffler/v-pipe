import groovy.transform.Field
import groovy.transform.InheritConstructors

// get libraries if needed for transformations...
//@Grab('info.picocli:picocli:2.0.3')

//
// from where to read and to where to write to
//
@Field String readFromFilePath = './Binding.groovy'
@Field String writeToFilePath ="./newData.txt"

static boolean isDate(String word) { word.matches(/\d+.\d+.\d\d\d\d/) }
static boolean isInteter(String word) { word.isInteger() }
static boolean isFloat(String word) { word.isFloat() }

@InheritConstructors class PluginParseException extends RuntimeException {}

//
// HERE HAPPENS THE REAL TRANSFORMING
//
String transfromLine(String oldLine) {

    List<String> words = oldLine.split(/\s*[;,]+\s*|\s+/) as List<String>
    words.retainAll {it} // keep the non-empty ones
    words.each {it.trim()} // remove space left and right
    println("zerlegte Daten:         $words")

    if(words.size() < 1) {throw new PluginParseException("zu wenig Elemente: ${words.size()}")}
    if(words.size() > 7) {throw new PluginParseException("zu viele Elemente: ${words.size()}")}

    StringBuffer result = new StringBuffer()

    result.append(words[0].toUpperCase())

    return result.toString()
}


//
// prepare files
//
File toFile = new File(writeToFilePath)
if(toFile.exists()){
    println("Zieldatei: $writeToFilePath")
    toFile.delete()
    def created = toFile.createNewFile()
    if( ! created) {
        println("F E H L E R:   kann Ziel-Datei $writeToFilePath nicht lösche und öffnen")
        System.exit(-1)
    }
}

File fromFile = new File(readFromFilePath)
if( ! fromFile.exists()) {
    println("F E H L E R:   kann Quell-Datei $readFromFilePath nicht finden")
    System.exit(-1)
} else {
    println("Quell-Datei: $readFromFilePath")
}
String content = fromFile.text


//
// extract lines from content and log them
//
def lineNumber = 1
content.eachLine { originalLine -> // line by line
    if(originalLine) { // if there is something except "blank & newline"
        try {
            println("going to parse line" + "${lineNumber}: ".padLeft(6) + originalLine)
            String transformedLine = transfromLine(originalLine)
            toFile.append(transformedLine + "\n")
        } catch (PluginParseException e) {
            println("F E H L E R   beim Umwandeln: " + e.message)
        } catch (Exception e) {
            println("C R A S H   beim Umwandeln: " + e.message)
            println()
            e.printStackTrace()
            System.exit(-1)
        }
    }
    lineNumber++
}
