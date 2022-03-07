package model

import utils.FileSupport

class DataWriter {

    Model model

    String getTasks() {
        StringBuffer result = new StringBuffer()
        for(task in model.taskList) {
            result.with  {
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

    void writeTasksToFile(){
        String data = getTasks()
        def f = new File(DataReader.get_TASK_FILE_NAME())
        f.delete()
        f << data
    }

    String getCapa() {
        DataReader.capaTextCache
    }

    void writeCapaToFile(){
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
            DataReader.PROJECT_DELIVERY_DATE_FILE_NAME
    ]

    def backup() {


        def bDir = FileSupport.backupDirName(DataReader.currentDir)
        assert new File(bDir).mkdirs()

        ALL_DATA_FILES.each { fileName ->
            File from = new File( DataReader.path(fileName))
            File to = new File(bDir + '/' + fileName)
            //def r = from.renameTo(to)
            //println(to.getCanonicalPath())
            //to.createNewFile()
            //Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
            //to << from.text
            //from.delete()
            from.renameTo(to) // move without exception, if file is missing - but also ignore fails...
        }
    }


    def writeSequenceToFile() {
        def f = new File(DataReader.get_SEQUENCE_FILE_NAME())
        f.delete()

        model.projectSequence.each {
            f << "$it\n"
        }
        /*
        f.withObjectOutputStream { out ->
            out.writeObject(model.projectSequence)
        }*/
    }

    def writeDeliveryDatesToFile() {
        def f = new File(DataReader.get_PROJECT_DELIVERY_DATE_FILE_NAME())
        f.delete()

        model.deliveryDates.each {
            f << "$it.key ${it.value.toString()}\n"
        }
        /*
        f.withObjectOutputStream { out ->
            out.writeObject(model.projectSequence)
        }*/
    }

    String getPipelining() {
        StringBuffer result = new StringBuffer(model.maxPipelineSlots.toString()+'\n')
        model.pipelineElements.each {
            result << "${it.project.padLeft(20)} ${it.startDate.toString()} ${it.endDate.toString()} ${it.pipelineSlotsNeeded}\n"
        }
        result.toString()
    }

    def writePipliningToFile() {
        String data = getPipelining()
        def f = new File(DataReader.get_PIPELINING_FILE_NAME())
        f.delete()
        f << data
    }


    void saveAll() {

        backup()

        writeTasksToFile()
        writeDeliveryDatesToFile()
        if(model.capaAvailable) {
            writeCapaToFile()
        }
        writeSequenceToFile()
        if(model.pipelineElements) {
            writePipliningToFile()
        }
        if(model.templatesPlainTextCache) {
            def f = new File(DataReader.get_PROJECT_TEMPLATE_FILE_NAME())
            f.delete()
            f << model.templatesPlainTextCache
        }
        if(model.templatesPipelineElementsPlainTextCache) {
            def f = new File(DataReader.get_PIPELINING_TEMPLATE_FILE_NAME())
            f.delete()
            f << model.templatesPipelineElementsPlainTextCache
        }
        model.setThreadSaveDirty(false)
    }
}
