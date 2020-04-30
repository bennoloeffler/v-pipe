class ProjectDataWriterTest extends GroovyTestCase {

    void testWriteToFile() {

        def tr = ProjectDataToLoadTransformer.getPopulatedTransformer()
        //def load = tr.calcDepartmentWeekLoad()
        //File f = new File(ProjectDataReader.FILE_NAME)
        //f.delete()
        ProjectDataWriter.writeToFile(tr, null)

        File data = new File(ProjectDataWriter.FILE_NAME)
        def lines = data.readLines()
        lines = lines*.trim()
        assert lines[0] == "DEP\t2020-W01\t2020-W02\t2020-W03\t2020-W04\t2020-W05\t2020-W06"
        assert lines[1] == "d1\t8,0\t32,0\t0,0\t0,0\t0,0\t0,0"
        assert lines[2] == "d2\t0,0\t23,1\t4,4\t4,4\t4,4\t3,8"

        data.delete()
    }

}
