package application

import core.LoadCalculator
import model.DataReader
import model.Model
import model.VpipeDataException
import model.WeekOrMonth
import org.tinylog.Logger as l
import utils.*
import groovy.time.TimeDuration
import transform.DateShiftTransformer

import java.awt.Desktop

//
// https://e.printstacktrace.blog/groovy-regular-expressions-the-definitive-guide/
//

/**
 * Starting point of v-pipe.
 * see Readme.md
 */
class  Main {

    static VERSION_STRING ='1.5.0-ALPHA-average-load'

    def singleRunMode = false // instead: Deamon is default
    def multiInstanceMode = false // instead SingleInstance is default
    FileWatcherDeamon fileWatcherDeamon
    List<FileEvent> fileEvents = null


    static void main(String[] args) {
        l.info "v-pipe  (release: $VERSION_STRING)"
        def m = new Main()
        m.parseArgs(args)
        m.checkStartAndHelp()
        m.runIt()
    }


    void parseArgs(String[] args) {
        def argsStr = args.join(" ")
        if(argsStr.contains("-s")) {singleRunMode=true}
        if(argsStr.contains("-m")) {multiInstanceMode = true}
    }


    void checkStartAndHelp() {

        //
        // only one instance - typically...
        //
        if( ! multiInstanceMode ) { StartOnlyOneInstance.checkStart() }

        //
        // Browser with quickstart-hints
        //
        def fs = new File("ersterStart.md")
        if(fs.exists()) {
            openBrowserWithHelp()
            fs.delete()
        }
    }


    void runIt() {

        try {

            DataReader.setCurrentDir('.')

            if(singleRunMode) { // single mode

                processData()
                println "E R F O L G :   Ergebnisse geschrieben. " + LoadCalculator.FILE_NAME_WEEK + ' (und -Monat )'

            } else { // deamon mode

                fileWatcherDeamon = createFileWatcherDeamon()

                while(true) {

                    try {
                        processDataAndDoTimingAndUserMessages()
                    }catch(VpipeDataException e) {
                        println "\nD A T E N - F E H L E R :\n" + e.getMessage()+"\n"
                    } catch(Exception e) {
                        e.printStackTrace()
                        println ":-( CRASH - erst nach der Behebung wird gerechnet...:\n" + e.getMessage()
                        sleep(100000)
                        System.exit(-1)
                    } finally {
                        if (!fileWatcherDeamon.isRunning()) {
                            fileWatcherDeamon.startReceivingEvents()
                        }
                    }

                    waitForFileEvents()
                }
            }
        } catch (VpipeDataException e) {
            println("D A T E N - F E H L E R :  erst nach der Behebung wird gerechnet...: \n" + e.getMessage())
        }

    }


    FileWatcherDeamon createFileWatcherDeamon() {
        def d = new FileWatcherDeamon(".")
        d.filter = [DataReader.TASK_FILE_NAME,
                    DataReader.DATESHIFT_FILE_NAME,
                    DataReader.PIPELINING_FILE_NAME,
                    DataReader.CAPA_FILE_NAME,
                    DataReader.TEMPLATE_FILE_NAME, ]
        d
    }


    void processDataAndDoTimingAndUserMessages() {
        File projectDataFile = new File(DataReader.TASK_FILE_NAME)
        if (projectDataFile.exists()) {
            println '\nDaten lesen, rechnen und schreiben...'
            long startProcessing = System.currentTimeMillis()
            processData()
            long endProcessing = System.currentTimeMillis()
            def d = new TimeDuration(0, 0, 0, endProcessing - startProcessing as int)
            println("Fertig nach: " + d.toString())
            println '\nE R F O L G :-)    jetzt warte ich, bis sich etwas ändert.'
        } else {
            println "Datei ${projectDataFile.absolutePath} existiert (noch) nicht. Ich warte..."
        }
    }


    void waitForFileEvents() {
        def lastSecond = System.currentTimeSeconds()
        def lastMinute = System.currentTimeSeconds()
        while (!fileEvents) {
            fileEvents = fileWatcherDeamon.extractEvents()
            //println('Events:' +fileEvents)
            sleep(200)
            if (System.currentTimeSeconds() - lastSecond > 1) {
                print '.'
                lastSecond=System.currentTimeSeconds()
                if (System.currentTimeSeconds() - lastMinute > 120) {
                    println ''
                    lastMinute=System.currentTimeSeconds()
                }
            }
        }
        fileEvents = null
    }


    private static void processData() {
        Model m = new Model()
        m.readAllData()
        LoadCalculator pt = new LoadCalculator(m)
        LoadCalculator.writeToFileStatic(pt, WeekOrMonth.WEEK)
        LoadCalculator.writeToFileStatic(pt, WeekOrMonth.MONTH)
    }


    static void openBrowserWithHelp() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            String path = new File("Referenz.html").absolutePath.replace('\\', ('/'))
            path.replace(' ', '%20')
            //path = java.net.URLEncoder.encode(path, "UTF-8")
            Desktop.getDesktop().browse(new URI("file:/$path"))
        }
    }
}
