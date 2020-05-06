package core

import fileutils.FileEvent
import fileutils.FileWatcherDeamon
import transform.DateShiftTransformer


// TODO: to 15 min groovy
// https://e.printstacktrace.blog/groovy-regular-expressions-the-definitive-guide/
//

/**
 * reads from standard file: Projekt-Start-End-Abt-Capa.txt (see core.ProjectDataReader)
 * writes to standarf file: Department-Load-Result.txt (see core.ProjectDataWriter)
 */
class  Main {

    static VERSION_STRING ='0.1.0-Vor-Ruhlamat'

    static void main(String[] args) {

        println "starting v-pipe-release: $VERSION_STRING"

        // TODO: add levels... https://signalw.github.io/2019/04/09/study-notes-an-example-of-intercept-cache-invoke-in-groovy.html
        // https://mrhaki.blogspot.com/2011/04/groovy-goodness-inject-logging-using.html
        //FileHandler handler = new FileHandler("v-pipe.log", true)
        //handler.setFormatter(new SimpleFormatter())
        //log.addHandler(handler)



        try {

            // args from apache commons
            if(args && args[0].contains('-s')) { // single mode
                println "V-PIPE going to read file: " + ProjectDataReader.FILE_NAME // todo
                processData()
                println "V-PIPE finished writing file: " + ProjectDataWriter.FILE_NAME // todo

            } else { // deamon mode

                def fwd = new FileWatcherDeamon(".")
                fwd.filter = [ProjectDataReader.FILE_NAME, DateShiftTransformer.FILE_NAME]

                while(true) {
                    try {
                        boolean pdfExists = new File(ProjectDataReader.FILE_NAME).exists()
                        if (pdfExists) {
                            println 'Found file changes. Going to processing data...'
                            processData()
                            println ("Processing finished at time " + new Date().format("hh:mm:ss"))
                        }
                        println 'waiting for file changes...'

                        List<FileEvent> fileEvents = null
                        if (!fwd.isRunning()) {
                            fwd.startReceivingEvents()
                        }
                        while (!fileEvents) {
                            fileEvents = fwd.extractEvents()
                            sleep(200)
                        }

                    }catch(VpipeException e) {
                        println "ERROR: " + e.getMessage()
                       sleep(5000)
                    } catch(Exception e) {
                        println "java Exception: " + e.getMessage()
                        //todo logile
                        sleep(5000)
                    }
                }

            }
        } catch (VpipeException e) {
            println("ERROR: " + e.getMessage()) // todo
        }
    }

    private static void processData() {
        ProjectDataToLoadCalculator pt = new ProjectDataToLoadCalculator()
        pt.transformers << new DateShiftTransformer(pt)
        pt.updateConfiguration()
        ProjectDataWriter.writeToFile(pt, null)
    }
}
