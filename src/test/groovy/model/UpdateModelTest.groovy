package model

import spock.lang.Specification

import static testdata.TestDataHelper.t

// TODO: test deleted, updated, new in the two cases (with ID and without)
class UpdateModelTest extends Specification {
    void setup() {}

    void cleanup() {}

    def "test update complete project"() {

        given:
        Model m = new Model()
        m.taskList = [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 20.0)]
        def changedTasks = [t('p1', '15.1.2021', '15.2.2021', 'IBN', 25.0, "noID:17")]

        when:
        m.readUpdatesFromCompleteProjects(changedTasks)

        then:
        m.taskList[0].capacityNeeded == 25
        m.taskList.size() == 1
    }

    def "test update with IDs"() {

        given:
        Model m = new Model()
        m.taskList = [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0, "ID:17"),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 20.0),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 21.0)]
        def changedTasks =
                [t('p1', '15.1.2020', '15.2.2020', 'IBN', 25.0, "ID:17"),
                 t('p1', '15.1.2020', '15.2.2020', 'IBN', 26.0, "ID:26")]

        when:
        m.readUpdatesFromIDs(changedTasks)

        then:
        m.taskList[0].capacityNeeded == 25
        m.taskList[1].capacityNeeded == 26
        m.taskList.size() == 2
    }

    def "test strategy id"() {

        given:
        Model m = new Model()
        m.taskList = [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0, "ID:17"),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 20.0),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 21.0, "ID:19")]
        def changedTasks =
                [t('p1', '15.1.2020', '15.2.2020', 'IBN', 25.0, "ID:17"),
                 t('p1', '15.1.2020', '15.2.2020', 'IBN', 26.0, "ID:26")]

        when:
        def result = m.readUpdatesFromTasks(changedTasks)

        then:
        result.err == null
        m.taskList[0].capacityNeeded == 25
        m.taskList[1].capacityNeeded == 21
        m.taskList[2].capacityNeeded == 26
        m.taskList.size() == 3
    }

    def "test strategy full"() {

        given:
        Model m = new Model()
        m.taskList = [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0, "ID:17"),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 20.0),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 21.0)]
        def changedTasks =
                [t('p1', '15.1.2020', '15.2.2020', 'IBN', 25.0, "noID:17"),
                 t('p1', '15.1.2020', '15.2.2020', 'IBN', 26.0, "noID:26")]

        when:
        def result = m.readUpdatesFromTasks(changedTasks)
        //println result

        then:
        result.err == null
        m.taskList[0].capacityNeeded == 25
        m.taskList[1].capacityNeeded == 26
        m.taskList.size() == 2
    }

    def "test strategy error ID mixed with no-ID"() {

        given:
        Model m = new Model()
        m.taskList = [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0, "ID:17"),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 20.0),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 21.0)]
        def changedTasks =
                [t('p1', '15.1.2020', '15.2.2020', 'IBN', 25.0, "noID:17"),
                 t('p1', '15.1.2020', '15.2.2020', 'IBN', 26.0, "ID:26")]

        when:
        def result = m.readUpdatesFromTasks(changedTasks)
        //println result

        then:
        result.err.contains("Einige Tasks haben keine ID:-Kennzeichnung")
        result.err.contains("noID:17")

    }

    def "test strategy error double id"() {

        given:
        Model m = new Model()
        m.taskList = [t('p1', '15.1.2020', '15.2.2020', 'IBN', 14.0, "ID:17"),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 20.0),
                      t('p1', '15.1.2020', '15.2.2020', 'IBN', 21.0)]
        def changedTasks =
                [t('p1', '15.1.2020', '15.2.2020', 'IBN', 25.0, "ID:26"),
                 t('p1', '15.1.2020', '15.2.2020', 'IBN', 26.0, "ID:26")]

        when:
        def result = m.readUpdatesFromTasks(changedTasks)
        //println result

        then:
        result.err.contains("sind nicht eindeutig:")
        result.err.contains("ID:26")

    }

}
