import org.joda.time.DateTime

class ProjectDataWriter {

    static def FILE_NAME = "Department-Load-Result.txt"

    static void writeToFile(ProjectDataToLoadTransformer tr, File f) {
        def stringMapMap = tr.calcDepartmentWeekLoad()
        if(f==null) {
            f = new File(FILE_NAME)
        }
        if(f.exists()) {
            println("output file exists: " + FILE_NAME)
            def tmpFileName = new DateTime().toString("yyyy-MM-dd HH.mm.ss  ") + FILE_NAME
            println("appending data-time: " + FILE_NAME)
            f = new File(tmpFileName)
            f.createNewFile()
        }

        // normalize a maps to contain all time-keys
        List<String> allTimeKeys = tr.getFullSeriesOfTimeKeys()
        f << "DEP\t"+allTimeKeys.join("\t") + "\n"

        stringMapMap.each {dep, loadMap ->
            f << dep
            allTimeKeys.each { String timeKey ->
                if (loadMap[timeKey]) {
                    def commaNumber = String.format("%.1f", loadMap[timeKey])
                    f << "\t" +  commaNumber
                } else {
                    f << "\t0,0"
                }
            }
            f <<"\n"
        }
    }
}

