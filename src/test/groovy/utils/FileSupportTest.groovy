package utils


import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FileSupportTest extends Assertions {

    @Test
    void testReadFileLines() {

        def fn = 'lines.txt'
        def f = new File(fn)
        f.delete()

        // no file
        def r = FileSupport.getDataLinesSplitTrimmed(fn)
        assert [] == r
        assert ! r

        // total empty file
        f.createNewFile()
        r = FileSupport.getDataLinesSplitTrimmed(fn)
        assert [] == r
        assert ! r

        // spaces in one line
        assert f.delete()
        f.createNewFile()
        f << "   "
        r = FileSupport.getDataLinesSplitTrimmed(fn)
        assert [] == r
        assert ! r

        // linefeeds and spaces and tabs
        assert f.delete()
        f.createNewFile()
        f << "   \n \t \n       \t\n\n\t\t"
        r = FileSupport.getDataLinesSplitTrimmed(fn)
        assert [] == r
        assert ! r

        // one line surrounded by noise
        assert f.delete()
        f.createNewFile()
        f << "   \n \t \n       \t\n\n\tdata\t"
        r = FileSupport.getDataLinesSplitTrimmed(fn)
        assert [['data']] == r
        assert r

        // several lines surrounded by noise and different separators
        // several separators dont lead to empty values! but to nothing, e.g. ;;;
        assert f.delete()
        f.createNewFile()
        f << "   \n s ; b       \n  ;x+x;;; \t a b c\n       \t\n\n\tdata\t"
        r = FileSupport.getDataLinesSplitTrimmed(fn)
        assert [['s','b'],['x+x','a','b','c'],['data']] == r

        f.delete()


    }

    @Test
    void testBackupFileName() {
        def fn = "T 3.3.2020.txt"

        def r = FileSupport.backupFileName(fn)
        r = r[7..-1]

        assert r.length()==fn.length()+ 35
        assert r[0..9] == fn[0..9] // first 10
        assert r[-4..-1] == fn[-4..-1] // last 4

        assert r[-4..-1] == ".txt" // in that case...

    }

    @Test
    void testBackupFileNameFail() {
        def fn = "T 3.3.2020.txtT" // to long

        def exception = assertThrows(Throwable.class)  {
            FileSupport.backupFileName(fn)
        }

        fn = "T 3.3.2020.tx" // to short

        exception = assertThrows(Throwable.class) {
            FileSupport.backupFileName(fn)
        }
        /*
        shouldFail {
            FileSupport.backupFileName(fn)
        }*/

        fn = ".txt" // ?

        exception = assertThrows(Throwable.class) {
            FileSupport.backupFileName(fn)
        }

        fn = "x.txt" // that works
        FileSupport.backupFileName(fn)

        fn = " .txt" // that works
        FileSupport.backupFileName(fn)    }

}
