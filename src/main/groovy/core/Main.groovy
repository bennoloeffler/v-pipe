package core


import model.DataReader
import model.Model
import model.TaskInProject
import model.VpipeDataException
import model.WeekOrMonth
import org.tinylog.Logger
import org.tinylog.Logger as l
import transform.CapaTransformer
import utils.*
import groovy.time.TimeDuration
import transform.DateShiftTransformer
import transform.PipelineTransformer

import java.awt.Desktop

// TODO: to 15 min groovy
// https://e.printstacktrace.blog/groovy-regular-expressions-the-definitive-guide/
//

/**
 * Starting point of v-pipe.
 * see Readme.md
 */
class  Main {

    static VERSION_STRING ='0.6.0-GUI-Project-View'

    static void main(String[] args) {

        //
        // parse args
        //
        def argsStr = args.join(" ")
        def singleRunMode = false // instead: Deamon is default
        def multiInstanceMode = false // instead SingleInstance is default
        def commandLineMode = false
        if(argsStr.contains("-s")) {singleRunMode=true}
        if(argsStr.contains("-m")) {multiInstanceMode = true}
        if(argsStr.contains("-c")) {commandLineMode = true}

        l.info "v-pipe  (release: $VERSION_STRING)"
        /*
        Logger.trace("Hello World!")
        Logger.debug("Hello World!")
        Logger.info("Hello World!")
        Logger.warn("Hello World!")
        Logger.error("Hello World!")
        */
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

        //
        // do the job...
        //
        try {

            // args from apache commons
            if(singleRunMode) { // single mode
                println "Daten lesen: $DataReader.TASK_FILE_NAME und $DataReader.DATESHIFT_FILE_NAME" // todo
                processData()
                println "E R F O L G :   Ergebnisse geschrieben. " + LoadCalculator.FILE_NAME_WEEK + ' (und -Monat )' // todo

            } else { // deamon mode

                Thread t = Thread.start {
                    //VpipeGui.main(null)
                }

                def fwd = new FileWatcherDeamon(".")
                fwd.filter = [DataReader.TASK_FILE_NAME, DataReader.DATESHIFT_FILE_NAME, DataReader.PIPELINING_FILE_NAME, DataReader.CAPA_FILE_NAME]

                while(true) {
                    try {
                        File projectDataFile = new File(DataReader.TASK_FILE_NAME)
                        if (projectDataFile.exists()) {
                            println 'Daten lesen, rechnen und schreiben...'
                            long startProcessing = System.currentTimeMillis()
                            processData()
                            long endProcessing = System.currentTimeMillis()
                            def d = new TimeDuration(0,0,0, endProcessing-startProcessing as int)
                            println ("Fertig nach: " + d.toString())
                            println '\nE R F O L G :-)    jetzt warte ich, bis sich etwas ändert.'
                        } else {
                            println "Datei ${projectDataFile.absolutePath} existiert (noch) nicht. Ich warte..."
                        }

                        //
                        // File listener running? No? Start...
                        //
                        List<FileEvent> fileEvents = null
                        if (!fwd.isRunning()) {
                            fwd.startReceivingEvents()
                        }


                        //
                        // Polling loop of v-pipe
                        //
                        def lastSecond = 0
                        def lastMinute = 0
                        while (!fileEvents) {
                            fileEvents = fwd.extractEvents()
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

                    }catch(VpipeDataException e) {
                        println "\nD A T E N - F E H L E R :\n" + e.getMessage()
                       sleep(10000)
                    } catch(Exception e) {
                        println "PROBLEM - erst nach der Behebung wird gerechnet...:\n" + e.getMessage()
                        e.printStackTrace()
                        //todo logile
                        sleep(10000)
                    }
                }

            }
        } catch (VpipeDataException e) {
            println("D A T E N - F E H L E R :  erst nach der Behebung wird gerechnet...: \n" + e.getMessage()) // todo
        }
    }


    /**
     * one processing loop
     */
    private static void processData() {
        Model m = new Model()
        m.readAllData()
        LoadCalculator pt = new LoadCalculator(model: m)
        pt.transformers << new DateShiftTransformer(pt.model)
        pt.transformers << new PipelineTransformer(pt.model)
        pt.transformers << new CapaTransformer(pt.model)
        LoadCalculator.writeToFileStatic(pt, WeekOrMonth.WEEK)
        LoadCalculator.writeToFileStatic(pt, WeekOrMonth.MONTH)
    }


    /**
     * first start: show browser with Quick-Start
     */
    static def openBrowserWithHelp() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            String path = new File("Referenz.html").absolutePath.replace('\\', ('/'))
            Desktop.getDesktop().browse(new URI("file:/$path"))
        }
    }
}
