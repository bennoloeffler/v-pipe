import model.DataReader
import model.TaskInProject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import utils.FileSupport

/**
 * Test reading different formats and separators for model.TaskInProject-Data
 */
class ProjectDataReaderTest extends GroovyTestCase {

    static def f = new File(DataReader.get_TASK_FILE_NAME())

    void createDataFile(String data) {
        f.with {
            createNewFile()
            write(data)
        }
    }

    @AfterAll
    static void tearDown() {
        f.delete()
    }

    @Test
    void testGetDataFromFile() {
        createDataFile("""
            p1 01.01.2020 20.01.2020 d1 20\n
            p1 01.01.2020 20.01.2020 d2 20\n
            p2 01.01.2020 20.01.2020 d1 20\n
            p3 31.12.2020 20.01.2021 d2 20\n
            p3 01.01.2020 20.01.2020 d1 20\n""")

        List<TaskInProject> data = DataReader.readTasks()

        assert data?.size() == 5
        assert data[4].department == "d1"
    }

    @Test
    void testSemicolonSeparator() {
        createDataFile("""
            p2 01.01.2020 20.01.2020 d1 20\n
            p3 31.12.2020;20.01.2021 d2 20\n
            p3 01.01.2020 20.01.2020; d1 20\n""")

        List<TaskInProject> data = DataReader.readTasks()
        assert data?.size() == 3
    }

    @Test
    void testFailCommaCapa() {
        createDataFile("""
            p1 01.01.2020 20.01.2020 d1 20.5\n
            p1 01.01.2020 20.01.2020 d2 20,5\n
            p1 01.01.2020 20.01.2020 d2 20,5 7""")

        def msg = shouldFail {
            List<TaskInProject> data = DataReader.readTasks()
        }
        assert msg.contains("Keine 5 bzw. 6 Daten-Felder gefunden mit Regex-SEPARATOR")
    }

    @Test
    void testSpacesAndTabs() {
        createDataFile("""
            p1 01.01.2020\t20.01.2020;\t\t d1 20.5\n
            p1 01.01.2020    20.01.2020; d2, 20\n""")

        List<TaskInProject> data = DataReader.readTasks()
        assert data?.size() == 2
    }

    /**
     * SWITCH TO SEMICOLON AND COMMA - in order to enable whitespace in Names (project, department)
     */
    @Test
    void testSpacesInData() {
        createDataFile("""
            p1, 01.01.2020,\t20.01.2020;\t\t d1, 20.5\n
            p1 \t;  01.01.2020;    20.01.2020;    dep 2   , 20\n""")

        FileSupport.SEPARATOR = FileSupport.SEPARATOR_SC
        List<TaskInProject> data = DataReader.readTasks()
        FileSupport.SEPARATOR = FileSupport.SEPARATOR_ALL

        assert data?.size() == 2
        assert data[1].department == "dep 2" // ATTENTION: removing all surounding spaces!
    }

}
