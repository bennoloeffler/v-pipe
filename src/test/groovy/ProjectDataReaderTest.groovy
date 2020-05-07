import core.ProjectDataReader
import core.TaskInProject
import fileutils.FileSupport

/**
 * Test reading different formats and separators for core.TaskInProject-Data
 */
class ProjectDataReaderTest extends GroovyTestCase {

    def f = new File(ProjectDataReader.FILE_NAME)

    void createDataFile(String data) {
        f.with {
            createNewFile()
            write(data)
        }
    }

    void tearDown() {
        f.delete()
    }

    void testGetDataFromFile() {
        createDataFile("""
            p1 01.01.2020 20.01.2020 d1 20\n
            p1 01.01.2020 20.01.2020 d2 20\n
            p2 01.01.2020 20.01.2020 d1 20\n
            p3 31.12.2020 20.01.2021 d2 20\n
            p3 01.01.2020 20.01.2020 d1 20\n""")

        List<TaskInProject> data = ProjectDataReader.getDataFromFile()

        assert data?.size() == 5
        assert data[4].department == "d1"
    }

    void testSemicolonSeparator() {
        createDataFile("""
            p2 01.01.2020 20.01.2020 d1 20\n
            p3 31.12.2020;20.01.2021 d2 20\n
            p3 01.01.2020 20.01.2020; d1 20\n""")

        List<TaskInProject> data = ProjectDataReader.getDataFromFile()
        assert data?.size() == 3
    }

    void testFailCommaCapa() {
        createDataFile("""
            p1 01.01.2020 20.01.2020 d1 20.5\n
            p1 01.01.2020 20.01.2020 d2 20,5\n""")

        def msg = shouldFail {
            List<TaskInProject> data = ProjectDataReader.getDataFromFile()
        }
        assert msg.contains("did not find 5")
    }

    void testSpacesAndTabs() {
        createDataFile("""
            p1 01.01.2020\t20.01.2020;\t\t d1 20.5\n
            p1 01.01.2020    20.01.2020; d2, 20\n""")

        List<TaskInProject> data = ProjectDataReader.getDataFromFile()
        assert data?.size() == 2
    }

    /**
     * SWITCH TO SEMICOLON AND COMMA - in order to enable whitespace in Names (project, department)
     */
    void testSpacesInData() {
        createDataFile("""
            p1, 01.01.2020,\t20.01.2020;\t\t d1, 20.5\n
            p1 \t;  01.01.2020;    20.01.2020;    dep 2   , 20\n""")

        FileSupport.SEPARATOR = FileSupport.SEPARATOR_SC
        List<TaskInProject> data = ProjectDataReader.getDataFromFile()
        FileSupport.SEPARATOR = FileSupport.SEPARATOR_ALL

        assert data?.size() == 2
        assert data[1].department == "dep 2" // ATTENTION: removing all surounding spaces!
    }

}
