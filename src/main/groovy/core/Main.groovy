package core

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

    static VERSION_STRING ='0.3.0-Pipeliner'

    static void main(String[] args) {

        //
        // parse args
        //
        def argsStr = args.join(" ")
        def singleRunMode = false // instead: Deamon is default
        def multiInstanceMode = false // instead SingleInstance is default
        if(argsStr.contains("-s")) {singleRunMode=true}
        if(argsStr.contains("-model")) {multiInstanceMode = true}

        println "\n\nv-pipe  (release: $VERSION_STRING)\n\n"

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
                println "Daten lesen: $ProjectDataToLoadCalculator.FILE_NAME und $DateShiftTransformer.FILE_NAME" // todo
                processData()
                println "E R F O L G :   Ergebnisse geschrieben. " + ProjectDataWriter.FILE_NAME_WEEK + ' (und -Monat )' // todo

            } else { // deamon mode

                def fwd = new FileWatcherDeamon(".")
                fwd.filter = [ProjectDataToLoadCalculator.FILE_NAME, DateShiftTransformer.FILE_NAME, PipelineTransformer.FILE_NAME]

                while(true) {
                    try {
                        File projectDataFile = new File(ProjectDataToLoadCalculator.FILE_NAME)
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
                       sleep(5000)
                    } catch(Exception e) {
                        println "PROBLEM - erst nach der Behebung wird gerechnet...:\n" + e.getMessage()
                        e.printStackTrace()
                        //todo logile
                        sleep(5000)
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
        ProjectDataToLoadCalculator pt = new ProjectDataToLoadCalculator()
        pt.transformers << new DateShiftTransformer(pt)
        pt.transformers << new PipelineTransformer(pt)
        pt.updateConfiguration()
        ProjectDataWriter.writeToFile(pt, TaskInProject.WeekOrMonth.WEEK)
        ProjectDataWriter.writeToFile(pt, TaskInProject.WeekOrMonth.MONTH)
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
