package model

import org.yaml.snakeyaml.Yaml
import utils.FileSupport

class DataWriter {

    Model model

    String getTasks(List<TaskInProject> tasks) {
        StringBuffer result = new StringBuffer()
        for (task in tasks) {
            result.with {
                append(task.project.padRight(20))
                append('  ')
                append(task.starting.toString())
                append('  ')
                append(task.ending.toString())
                append('  ')
                append(task.department.padRight(20))
                append(task.capacityNeeded.toString().padLeft(6))
                append('  ')
                append(task.description)
                append('\n')
            }
        }
        result.toString()
    }

    void writeTasksToFile(List<TaskInProject> tasks, String fileName) {
        String data = getTasks(tasks)
        def f = new File(fileName)//DataReader.get_TASK_FILE_NAME())
        f.delete()
        f << data
    }

    String getCapa() {
        DataReader.capaTextCache
    }

    void writeCapaToFile() {
        String data = getCapa()
        def f = new File(DataReader.get_CAPA_FILE_NAME())
        f.delete()
        f << data
    }

    static List<String> ALL_DATA_FILES = [
            DataReader.TASK_FILE_NAME,
            DataReader.PIPELINING_FILE_NAME,
            DataReader.DATESHIFT_FILE_NAME,
            DataReader.CAPA_FILE_NAME,
            DataReader.SCENARIO_FILE_NAME,
            DataReader.SEQUENCE_FILE_NAME,
            DataReader.PROJECT_TEMPLATE_FILE_NAME,
            DataReader.PROJECT_TEMPLATE_FILE_NAME,
            DataReader.PROJECT_DELIVERY_DATE_FILE_NAME,
            DataReader.PROJECT_COMMENTS_FILE_NAME

    ]

    static def backup() {

        def bDir = FileSupport.backupDirName(DataReader.currentDir)
        println "try to create backup folder: " + bDir
        def sm = SecurityManager.newInstance()
        sm.checkRead(bDir)
        sm.checkWrite(bDir)
        //assert new File(bDir).mkdirs() // this fails on
        def bFile = new File(bDir)
        //def succ = bFile.mkdirs() // TODO: Does mkdir make it any better?
        def succ = bFile.mkdir() // TODO: CHECK WHY ONEDRIVE does not work on win box of Frank
        println "creation worked?" + succ
        if(bFile.exists()) {
            ALL_DATA_FILES.each { fileName ->
                File from = new File(DataReader.path(fileName))
                File to = new File(bDir + '/' + fileName)
                from.renameTo(to) // move without exception, if file is missing - but also ignore fails...
            }
        } else {
            //throw new RuntimeException("creation of backup folder failed: " + bDir)
            println "ATTENTION: no backup saved! cant create backup folder..."
        }
    }


    def writeSequenceToFile(List<String> sequence, String fileName) {
        def f = new File(fileName)
        f.delete()

        sequence.each {
            f << "$it\n"
        }
    }

    def writeDeliveryDatesToFile() {
        def f = new File(DataReader.get_PROJECT_DELIVERY_DATE_FILE_NAME())
        f.delete()

        model.deliveryDates.each {
            f << "$it.key ${it.value.toString()}\n"
        }
    }

    String getPipelining(List<PipelineElement> elements, boolean isTemplatePipeline = false) {
        String slots = isTemplatePipeline ? "" : model.maxPipelineSlots.toString() + '\n'
        StringBuffer result = new StringBuffer(slots)
        elements.each {
            result << "${it.project.padLeft(20)} ${it.startDate.toString()} ${it.endDate.toString()} ${it.pipelineSlotsNeeded}\n"
        }
        result.toString()
    }

    def writePipliningToFile(List<PipelineElement> elements, String fileName, boolean isTemplatePipeline = false) {
        String data = getPipelining(elements, isTemplatePipeline)
        def f = new File(fileName)
        f.delete()
        f << data
    }


    void saveAll() {
        assert !model.projectsAndTemplatesSwapped
        backup()

        writeTasksToFile(model.taskList, DataReader.get_TASK_FILE_NAME())
        if (model.templateList) {
            writeTasksToFile(model.templateList, DataReader.get_PROJECT_TEMPLATE_FILE_NAME())
        }

        writeDeliveryDatesToFile()

        if (model.capaAvailable) {
            writeCapaToFile()
        }
        writeSequenceToFile(model.projectSequence, DataReader.get_SEQUENCE_FILE_NAME())
        if (model.templateSequence) {
            writeSequenceToFile(model.templateSequence, DataReader.get_TEMPLATE_SEQUENCE_FILE_NAME())
        }

        if (model.pipelineElements) {
            writePipliningToFile(model.pipelineElements, DataReader.get_PIPELINING_FILE_NAME())
            if (model.templatePipelineElements) {
                writePipliningToFile(model.templatePipelineElements, DataReader.get_TEMPLATE_PIPELINING_FILE_NAME(), true)
            }
        }

        writeCommentsToFile(model.projectComments, DataReader.get_PROJECT_COMMENTS_FILE_NAME())

    }

    static void writeCommentsToFile(LinkedHashMap<String, String> comments, String fileName) {
        if (comments) {
            Yaml yaml = new Yaml()
            String data = yaml.dump(comments)
            def f = new File(fileName)
            f.delete()
            f << data
        }
    }
}
