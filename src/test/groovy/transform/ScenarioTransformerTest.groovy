package transform

import model.Model
import model.VpipeDataException
import spock.lang.Specification
import testdata.TestDataHelper

class ScenarioTransformerTest extends Specification {

    Model m

    def "transform two projects to four"() {

        given:
            m = TestDataHelper.getPopulatedModel()
            m.scenarioProjects = [['copy1', 'p1', 100], ['copy2', 'p2', 10],]
            ScenarioTransformer tt = new ScenarioTransformer(m)

        when:
            tt.transform()

        then:
            m.taskList.size() == 8
            m.getProject('copy1').size() == 2
            m.getProject('copy2').size() == 2
            m.getProject('copy2')[0].ending == "20.1.2020".toDate()

    }


    def "transform is missing original project"() {

        given: 'model with tasks and one template request'
            m = TestDataHelper.getPopulatedModel()
            m.scenarioProjects = [['p1-copy', 'p1-template-original', 17]]
            ScenarioTransformer tt = new ScenarioTransformer(m)

        when: 'transforming'
            tt.transform()

        then: 'ex...'
            VpipeDataException ex = thrown()
            // Alternative syntax: def ex = thrown(InvalidDeviceException)
            ex.message.contains ('Original-Projekt: p1-template-original')
    }

}
