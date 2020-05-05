package core

import fileutils.FileEvent
import fileutils.FileWatcherDeamon
import groovy.util.logging.Log

import java.util.logging.FileHandler
import java.util.logging.SimpleFormatter

// TODO: to 15 min groovy
// https://e.printstacktrace.blog/groovy-regular-expressions-the-definitive-guide/
//

/**
 * reads from standard file: Projekt-Start-End-Abt-Capa.txt (see core.ProjectDataReader)
 * writes to standarf file: Department-Load-Result.txt (see core.ProjectDataWriter)
 */
@Log
class  Main {

    static void main(String[] args) {


        // TODO: add levels... https://signalw.github.io/2019/04/09/study-notes-an-example-of-intercept-cache-invoke-in-groovy.html
        FileHandler handler = new FileHandler("v-pipe.log", true)
        handler.setFormatter(new SimpleFormatter())
        log.addHandler(handler)



        try {
/*
            // args from apache commons
            if(args && args[0].contains('-d')) { // demon mode
                def fwd = new FileWatcherDeamon("data")
                while(true) {
                    // try to read files

                    // process

                    List<FileEvent> fileEvents = fwd.extractEvents()
                    // print what will be done now...

                }
            } else { // single mode
                */
                log.info "V-PIPE going to read file: " + ProjectDataReader.FILE_NAME
                ProjectDataToLoadCalculator pt = new ProjectDataToLoadCalculator()
                pt.taskList = ProjectDataReader.dataFromFile
                ProjectDataWriter.writeToFile(pt, null)
                log.info "V-PIPE finished writing file: " + ProjectDataWriter.FILE_NAME
  //          }
        } catch (VpipeException e) {
            log.severe("ERROR: " + e.getMessage())
        }
    }
}
