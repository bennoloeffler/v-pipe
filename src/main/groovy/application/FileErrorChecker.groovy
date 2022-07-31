package application

import groovy.swing.SwingBuilder
import groovy.transform.Synchronized
import model.DataReader
import model.Model
import model.VpipeDataException
import utils.FileEvent
import utils.FileWatcherDeamon

class FileErrorChecker {
    FileWatcherDeamon fileWatcherDeamon
    List<FileEvent> fileEvents = null
    boolean running = false

    @Synchronized
    void stopIt() {
        running = false
    }

    @Synchronized
    boolean isRunning() {
        running
    }

    boolean runIt(String stringPath, SwingBuilder swing) {
        running = true
        boolean errorInModel = true
        try {
            fileWatcherDeamon = createFileWatcherDeamon(stringPath)
            while (isRunning()) {
                try {
                    swing.doLater {
                        processDataAndDoTimingAndUserMessages()
                        errorInModel = false
                    }
                } catch (VpipeDataException e) {
                    println "\nD A T E N - F E H L E R :\n" + e.getMessage() + "\n"
                    errorInModel = true
                } catch (Exception e) {
                    e.printStackTrace()
                    println ":-( CRASH - erst nach der Behebung wird gerechnet...:\n" + e.getMessage()
                    //sleep(100000)
                    //System.exit(-1)
                } finally {
                    if (!fileWatcherDeamon.isRunning()) {
                        fileWatcherDeamon.startReceivingEvents()
                    }
                }
                //print "start waiting for file events..."
                waitForFileEvents()
                //println "got it!"
            }
            fileWatcherDeamon.stopReceivingEvents()
        } catch (Exception e) {
            println("CRASH:\n" + e.getMessage())
        }
        //println "errorInModel: " + errorInModel
        return errorInModel
    }


    static FileWatcherDeamon createFileWatcherDeamon(String pathString) {
        def d = new FileWatcherDeamon(pathString)
        d.filter = [DataReader.TASK_FILE_NAME,
                    DataReader.PIPELINING_FILE_NAME,
                    DataReader.CAPA_FILE_NAME,
                    DataReader.PROJECT_TEMPLATE_FILE_NAME,
                    DataReader.TEMPLATE_PIPELINE_FILE_NAME,
                    DataReader.SEQUENCE_FILE_NAME,
                    DataReader.PROJECT_DELIVERY_DATE_FILE_NAME,
                    DataReader.PROJECT_COMMENTS_FILE_NAME,
                    DataReader.TEMPLATE_SEQUENCE_FILE_NAME,

                    DataReader.DATESHIFT_FILE_NAME,
                    DataReader.SCENARIO_FILE_NAME]
        d
    }


    static void processDataAndDoTimingAndUserMessages() {
        File projectDataFile = new File(DataReader.get_TASK_FILE_NAME())
        if (projectDataFile.exists()) {
            //println '\nDaten lesen & checken...'
            //long startProcessing = System.currentTimeMillis()
            Model m = MainGui.instance.controller.model
            m.readAllData()
            //long endProcessing = System.currentTimeMillis()
            //def d = new TimeDuration(0, 0, 0, endProcessing - startProcessing as int)
            //println("Fertig nach: " + d.toString())
            println '\nE R F O L G :-)    jetzt warte ich, bis sich etwas ändert.'
        } else {
            println "Datei ${projectDataFile.absolutePath} existiert (noch) nicht. Ich warte..."
        }
    }


    void waitForFileEvents() {
        def lastSecond = System.currentTimeSeconds()
        def lastMinute = System.currentTimeSeconds()
        while (!fileEvents && isRunning()) {
            fileEvents = fileWatcherDeamon.extractEvents()
            //println('Events:' +fileEvents)
            sleep(50)
            if (System.currentTimeSeconds() - lastSecond > 1) {
                print '.'
                lastSecond = System.currentTimeSeconds()
                if (System.currentTimeSeconds() - lastMinute > 120) {
                    println ''
                    lastMinute = System.currentTimeSeconds()
                }
            }
        }
        fileEvents = null
    }

}
