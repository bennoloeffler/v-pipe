/**
 * reads from standard file: Projekt-Start-End-Abt-Capa.txt (see ProjectDataReader)
 * writes to standarf file: Department-Load-Result.txt (see ProjectDataWriter)
 */
class Main {

    static void main(String[] args) {
        try {
            println "V-PIPE going to read file: " + ProjectDataReader.FILE_NAME
            ProjectDataToLoadTransformer pt = new ProjectDataToLoadTransformer()
            pt.taskList = ProjectDataReader.dataFromFile
            ProjectDataWriter.writeToFile(pt, null)
            println "V-PIPE finished writing file: " + ProjectDataWriter.FILE_NAME
        } catch (FileNotFoundException e) {
            println("ERROR: " + e.getMessage())
        }
    }
}
