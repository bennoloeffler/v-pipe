package model

import spock.lang.Specification

class DataReaderTest extends Specification {



    def "read tasks"() {
    }

    def "read pipelining"() {
    }

    def "read dateShift"() {
    }

    def "read templates one line"() {
        given: 'one line'
        def data = "p2 p1 4"

        when: 'reading'
        def r = DataReader.readScenario(data)

        then: 'two strings and one number'
        r[0][0] == 'p2'
        r[0][1] == 'p1'
        r[0][2] == 4
    }


    def "read templates more lines"() {
        given: 'cluttered but ok data'
        def data = "p2, p1;   4 \n  \np_1, p__1   434 \n"

        when: 'reading'
        def r = DataReader.readScenario(data)

        then: 'works - second line read'
        r[1][0] == 'p_1'
        r[1][1] == 'p__1'
        r[1][2] == 434
    }

    def "read empty"() {
        given: 'empty data'
        def data = ""

        when: 'reading'
        def r = DataReader.readScenario(data)

        then: 'result is empty too - but no exception or null'
        r.size() == 0
    }

    def "read fields too much"() {
        given: 'four fields'
        def data = "p1 p2 p3 p4"

        when: 'reading'
        def r = DataReader.readScenario(data)

        then:
            VpipeDataException ex = thrown()
            // Alternative syntax: def ex = thrown(InvalidDeviceException)
            ex.message.contains ('Es müssen 3 Daten-Felder sein')

    }

    def "read no int number"() {
        given:
        def data = "p1, p2, 5.6"

        when:
        def r = DataReader.readScenario(data)

        then:
        VpipeDataException ex = thrown()
        // Alternative syntax: def ex = thrown(InvalidDeviceException)
        ex.message.contains('Umwandeln in Ganzahl gescheitert. Das ist keine: 5.6')
    }
}
