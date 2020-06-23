package model


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

    void saveAll() {
        writeTasksToFile()
        writeCapaToFile()
    }
}
