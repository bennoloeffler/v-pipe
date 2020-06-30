package utils

import org.joda.time.DateTime

class FileSupport {

    final static String SEPARATOR_ALL = /\s*[;,]+\s*|\s+/ // every whitespace, semicolon or comma
    final static String SEPARATOR_SC = /\s*[;,]+\s*/ // can read data with whitespace
    static String SEPARATOR = SEPARATOR_ALL
    static String BACKUP_DIR = "backup"

    static {
        checkBackupDir()
    }

/**
 * @param fileName
 * @return empty List, if file does not exist or is empty. Else, split lines with SEPARATOR (all types of spaces \s , or ;
 */

    static String getTextOrEmpty(String fileName) {
        String result = ''
        File f = new File(fileName)
        if( f.exists() ) {
            f.withReader { reader ->
                result = reader.text
            }
        }
        result
    }

    static List<List<String>> toSplitAndTrimmedLines(String text) {
        List result = []
        text.eachLine { line ->
            if (line.trim()) {
                def words = line.split(SEPARATOR)
                if (words) {
                    def list = words.toList()
                    list.remove('')
                    result << list
                }
            }
        }
        result
    }

    static List<List<String>> getDataLinesSplitTrimmed(String fileName) {
        def text = getTextOrEmpty(fileName)
        toSplitAndTrimmedLines(text)
    }



/*
    static List<List<String>> getDataLinesSplitTrimmed(String fileName) {
        List result = []
        File f = new File(fileName)
        if( f.exists() ) {
            f.eachLine() {
                if ( it.trim() ) {
                    def line = it.split(SEPARATOR)
                    if (line) {
                        def list = line.toList()
                        list.remove('')
                        result << list
                    }
                }
            }
        }
        result
    }
*/

    /**
     * works only for filenames with 3-Letter-Postfix like .txt
     * @param fileName
     * @return
     */
    static String backupFileName(String fileName) {
        assert fileName
        assert fileName[-4] == "."
        assert fileName.length() > 4

        BACKUP_DIR + "/" + fileName[0..-5] + " backup von " + new DateTime().toString("yyyy-MM-dd HH.mm.ss") + fileName[-4..-1]
    }

    /**
     * works only for filenames with 3-Letter-Postfix like .txt
     * @param fileName
     * @return
     */
    static String backupDirName(String dirName) {
        assert dirName
        dirName + '\\' + BACKUP_DIR + "\\" +  new DateTime().toString("yyyy-MM-ddHHmmss")
    }

    static def checkBackupDir() {
        def f = new File(FileSupport.BACKUP_DIR)
        if(!f.exists() && !f.isDirectory()) {f.mkdir()}
    }

}