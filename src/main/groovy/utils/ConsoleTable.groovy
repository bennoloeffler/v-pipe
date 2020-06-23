package utils

class ConsoleTable {

    static def test() {
        def header = [
                'name  '         : 'l',
                '   mother'      : 'r',
                'weight'         : 'r',
                'age'            : 'l',
                'pet            ': 'l',
                'money'          : 'r']


        def table = [
                ['Benno', "Ingrid", 89.5, 50, 'cat', 46.5698],
                ['Leo', "Sabine", 55.6, 9, 'rabbitANdRotten', 0.01],
                ['Sabine', 'Maria', 66.6554, 48, 'dog', 5.5]
        ]

        println(tableToString(header, table))
    }


    static String tableToString(Map<String, String> header, List<List> table) {
        StringBuffer result = new StringBuffer()
        //println header
        //println table
        result.append('| ')
        header.keySet().each {result.append(sprintf("${it} | "))}

        def tableWidth = result.size()-3
        result.insert(0, '\n┌' +  ('─' * tableWidth) + '┐\n' )
        result.append( '\n├' +  ('─' * tableWidth) +'┤\n' )

        for (line in table) {
            int column=0
            result.append('| ')
            for (valueToPrint in line) {

                def width = header.keySet()[column].size()
                def rOrL = header.values()[column]
                if(valueToPrint instanceof String) {
                    if (rOrL == 'l') {
                        result.append(valueToPrint.padRight(width))
                    } else {
                        result.append(valueToPrint.padLeft(width))
                    }
                } else if(valueToPrint instanceof Long || valueToPrint instanceof Integer) {
                    if (rOrL == 'l') {
                        result.append(valueToPrint.toString().padRight(width))
                    } else {
                        def s = sprintf("%${width}d", valueToPrint).padRight(width)
                        result.append(s)
                    }
                } else if(valueToPrint instanceof BigDecimal) {
                    if (rOrL == 'l') {
                        result.append(sprintf("%.1f", valueToPrint).padRight(width))
                    } else {
                        result.append(sprintf("%${width}.1f", valueToPrint).padRight(width))
                    }

                } else {
                    result.append('Problem...')
                }
                column++
                result.append(' | ')

            }
            result.append('\n')

        }
        result.append( '└' +  ('─' * tableWidth) +'┘\n' )
        result.toString()

    }

    static void main(String[] args) {
        test()
    }

}
