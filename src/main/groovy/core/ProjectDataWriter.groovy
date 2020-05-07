package core

import fileutils.FileSupport

import static core.TaskInProject.WeekOrMonth.WEEK


class ProjectDataWriter {

    static def FILE_NAME_WEEK = 'Abteilungs-Kapazitäts-Belastung-Woche.txt'
    static def FILE_NAME_MONTH = 'Abteilungs-Kapazitäts-Belastung-Monat.txt'
    static String BACKUP_FILE

    static void writeToFile(ProjectDataToLoadCalculator tr, TaskInProject.WeekOrMonth weekOrMonth) {

        def stringMapMap = tr.calcDepartmentLoad(weekOrMonth)

        def fn = weekOrMonth == WEEK ? FILE_NAME_WEEK : FILE_NAME_MONTH
        File f = new File(fn)
        if(f.exists()) { // BACKUP
            BACKUP_FILE = FileSupport.backupFileName(f.toString())
            f.renameTo(BACKUP_FILE)
        }

        f = new File(fn)
        f.createNewFile()


        // normalize a maps to contain all time-keys
        List<String> allTimeKeys = tr.getFullSeriesOfTimeKeys(weekOrMonth)
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

