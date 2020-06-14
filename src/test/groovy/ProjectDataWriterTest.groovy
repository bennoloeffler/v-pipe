import core.LoadCalculator
import testdata.TestDataHelper

import static model.WeekOrMonth.*

class ProjectDataWriterTest extends GroovyTestCase {

    void testWriteToFileWeek() {

        def tr = TestDataHelper.getPopulatedCalculator()
        LoadCalculator.writeToFileStatic(tr, WEEK)

        File data = new File(LoadCalculator.FILE_NAME_WEEK)
        def lines = data.readLines()
        lines = lines*.trim()
        assert lines[0] == "DEP\t2020-W01\t2020-W02\t2020-W03\t2020-W04\t2020-W05\t2020-W06"
        assert lines[1] == "d1\t8,0\t32,0\t0,0\t0,0\t0,0\t0,0"
        assert lines[2] == "d2\t0,0\t23,1\t4,4\t4,4\t4,4\t3,8"

        data.delete()
    }

    void testWriteToFileMonth() {

        def tr = TestDataHelper.getPopulatedCalculator()
        LoadCalculator.writeToFileStatic(tr, MONTH)

        File data = new File(LoadCalculator.FILE_NAME_MONTH)
        def lines = data.readLines()
        lines = lines*.trim()
        assert lines[0] == "DEP\t2020-M01\t2020-M02"
        assert lines[1] == "d1\t40,0\t0,0"
        assert lines[2] == "d2\t35,0\t5,0"
        data.delete()
    }


    void testWriteToExistingFileBackup() {

        File data = new File(LoadCalculator.FILE_NAME_WEEK)
        data.delete()
        data.createNewFile()
        data << "backup"
        sleep(200)

        def tr = TestDataHelper.getPopulatedCalculator()
        LoadCalculator.writeToFileStatic(tr, WEEK)
        sleep(200)

        def back = new File(LoadCalculator.BACKUP_FILE)
        assert back.exists()
        def backupLines = back.readLines()
        assert backupLines[0] == "backup"
        sleep(200)

        data = new File(LoadCalculator.FILE_NAME_WEEK)
        def lines = data.readLines()
        lines = lines*.trim()
        assert lines[0] == "DEP\t2020-W01\t2020-W02\t2020-W03\t2020-W04\t2020-W05\t2020-W06"

        back.delete()
        data.delete()
    }

}
