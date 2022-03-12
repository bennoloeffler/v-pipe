package model

import spock.lang.Specification

import static testdata.TestDataHelper.t

class DataWriterTest extends Specification {
    void setup() {
    }

    void cleanup() {
    }

    def "WriteTasks"() {
        given:
        Model m = new Model()
        m.taskList =  [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0)]

        when:
        DataWriter dw = new DataWriter(model: m)
        def tasksString = dw.getTasks(m.taskList)

        then:
        tasksString == "p1                    15.01.2020  15.02.2020  IBN                   14.0  \n" // descritption missing...

    }
}
