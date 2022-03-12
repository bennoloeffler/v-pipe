package model

import spock.lang.Specification

class ModelNewTest extends Specification {

    ModelNew model
    boolean updateCallHappended

    void setup() {
        updateCallHappended = false
        model = new ModelNew()
        model.addPropertyChangeListener {updateCallHappended = true}
    }

    def "test fireUpdate" () {

        given:
        //model
        //updateCallHappended = false

        when:
        model.fireUpdate()

        then:
        updateCallHappended
    }

    def "test setProject" () {

        given:
        def projectsOld = model.projects
        def p = new Project("the-name", new Date(), null, null)

        when:
        model.setProject(p)

        then:
        model.projects.size() == 1
        projectsOld.size() == 0
        updateCallHappended
    }

    def "test removeProject" () {

        given:
        def p = new Project("the-name", new Date(), null, null)
        model.setProject(p)
        updateCallHappended = false
        def projectsOld = model.projects

        when:
        model.removeProject(p)

        then:
        model.projects.size() == 0
        projectsOld.size() == 1
        updateCallHappended
    }

    /*
    def "test overwrite project" () {

        given:
        def p = new Project("theName", new Date(), null, null)
        model.setProject(p)
        assert model.projects.size() == 1
        assert updateCallHappended
        updateCallHappended = false
        def projectsOld = model.projects

        when:
        p = new Project("theName", new Date() + 1, null, null)
        model.setProject(p)

        then:
        model.projects.size() == 1
        model.projects.theName.deliveryDate == new Date() + 1
        updateCallHappended
    }*/

}
