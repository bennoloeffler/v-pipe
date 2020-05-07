package fileutils

import org.joda.time.DateTime

class FileSupport {
    final static String SEPARATOR_ALL = /\s*[;,]+\s*|\s+/ // every whitespace, semicolon or comma
    final static String SEPARATOR_SC = /\s*[;,]+\s*/ // can read data with whitespace
    static String SEPARATOR = SEPARATOR_ALL


/**
 * @param fileName
 * @return empty List, if file does not exist or is empty. Else, split lines with SEPARATOR (all types of spaces \s , or ;
 */
    static List<List<String>> getDataLinesSplitTrimmed(String fileName) {
        List r = []
        File f = new File(fileName)
        if( f.exists() ) {
            f.eachLine() {
                if ( it.trim() ) {
                    def line = it.split(SEPARATOR)
                    if (line) {
                        def list = line.toList()
                        list.remove('')
                        r << list
                    }
                }
            }
        }
        return r
    }

    /**
     * works only for filenames with 3-Letter-Postfix like .txt
     * @param fileName
     * @return
     */
    static String backupFileName(String fileName) {
        assert fileName
        assert fileName[-4] == "."
        assert fileName.length() > 4
        fileName[0..-5] + " backup von " + new DateTime().toString("yyyy-MM-dd HH.mm.ss") + fileName[-4..-1]
    }
}