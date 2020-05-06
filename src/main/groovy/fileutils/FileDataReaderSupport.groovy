package fileutils

class FileDataReaderSupport {
    final static String SEPARATOR_ALL = /[;,]+\s*|\s+/ // every whitespace, semicolon or comma
    final static String SEPARATOR_SC = /\s*[;,]+\s*/ // can read data with whitespace
    static String SEPARATOR = SEPARATOR_ALL


/**
 * @param fileName
 * @return empty List, if file does not exist or is empty - else, split lines
 */
    static List<String[]> getDataLinesSplitTrimmed(String fileName) {
        List r = []
        File f = new File(fileName)
        if( ! f.exists()) return r
        f.eachLine() {
            if (it.trim()) {
                def line = it.split(SEPARATOR)
                if(line) {r << line}
            }
        }
        return r
    }
}