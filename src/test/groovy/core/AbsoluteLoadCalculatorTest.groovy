package core

import model.TaskInProject
import model.WeekOrMonth
import spock.lang.Specification

import static testdata.TestDataHelper.*

class AbsoluteLoadCalculatorTest extends Specification {

    AbsoluteLoadCalculator lc = new AbsoluteLoadCalculator()

    def 'created load calculator has empty data maps and functions can be called anyway'() {

        given: 'new AbsoluteLoadCalculator'

        expect: 'has empty data maps'
            verifyAll(lc) {
                capaLoadAbsolut.size() == 0
                getMax('someDepartment') == 0
                getCapaNeeded('someDepartment','2020-W1') == CapaNeedDetails.nullElement
            }
    }


    def 'absolute load available in one week'() {

        given: 'project data available'
            lc.tasks = [t('p','1.1.2020', '5.1.2020', 'd', 100)]

        when: 'calcing the load in capa'
            lc.calculate()

        then: 'calc delivers right data'

            verifyAll(lc) {
                capaLoadAbsolut.size() == 1
                capaLoadAbsolut['d'].size() == 1
                capaLoadAbsolut['d']['2020-W01'].totalCapaNeed == 100.0
                capaLoadAbsolut['d']['2020-W01'].projects.size() == 1
                capaLoadAbsolut['d']['2020-W01'].projects[0].originalTask instanceof TaskInProject
                capaLoadAbsolut['d']['2020-W01'].projects[0].projectCapaNeed == 100
            }

        then: 'API works'
            verifyAll(lc) {
                getMax('d') == 100
                getCapaNeeded('d', '2020-W01') == capaLoadAbsolut['d']['2020-W01']
            }

    }

    def 'data test weekly'() {

        TaskInProject t1p1, t2p1, t1p2, t2p2

        given:
            t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
            t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
            t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
            t2p2 = t("p2", "8.1.2020", "9.1.2020", "d2", 20.0)
            lc.tasks = [t1p1, t2p1, t1p2, t2p2]

        when: 'calcing the load'
            lc.calculate()

        then: 'data is right and API works'
            verifyAll(lc) {

                // week  W01 |  W02
                //
                // date    5    6   7   8   9   10
                //
                // load
                // p1-d1   4    4   4   4   4
                // p2-d1   4    4   4   4   4
                // p1-d2           20
                // p2-d2           20

                getCapaNeeded('d1','2020-W01').totalCapaNeed == 8
                getCapaNeeded('d1','2020-W02').totalCapaNeed == 32
                getCapaNeeded('d2','2020-W02').totalCapaNeed == 40
                getCapaNeeded('someDep','someTk') == CapaNeedDetails.nullElement
                getMax('d1') == 32
                getMax('d2') == 40
                getMax('domeDepartment') == 0

            }

    }

    def 'data test monthly'() {

        TaskInProject t1p1, t2p1, t1p2, t2p2, t1p3, t1p4

        given:
            lc.weekOrMonth = WeekOrMonth.MONTH
            t1p1 = t("p1", "5.1.2020", "10.1.2020", "d1", 20.0)
            t2p1 = t("p1", "8.1.2020", "9.1.2020", "d2", 20.0)
            t1p2 = t("p2", "5.1.2020", "10.1.2020", "d1", 20.0)
            t2p2 = t("p2", "8.1.2020", "9.1.2020", "d2", 20.0)
            t1p3 = t("p3", "8.2.2020", "9.2.2020", "d3", 20.0)
            t1p4 = t("p4", "1.1.2020", "1.2.2021", "d4", 31+29+31+30+31+30+31+31+30+31+30+31+31)

            lc.tasks = [t1p1, t2p1, t1p2, t2p2, t1p3, t1p4]

        when: 'calcing the load'
            lc.calculate()

        then: 'data is right and API works'

        verifyAll(lc) {

            getCapaNeeded('d1','2020-M01').totalCapaNeed == 40
            getCapaNeeded('d2','2020-M01').totalCapaNeed == 40
            getCapaNeeded('d3','2020-M02').totalCapaNeed == 20

            getCapaNeeded('d4','2020-M01').totalCapaNeed == 31
            getCapaNeeded('d4','2020-M02').totalCapaNeed == 29
            getCapaNeeded('d4','2020-M03').totalCapaNeed == 31
            getCapaNeeded('d4','2020-M04').totalCapaNeed == 30

            getCapaNeeded('someDep','someTk') == CapaNeedDetails.nullElement

            getMax('d3') == 20
            getMax('d4') == 31

            getMax('domeDepartment') == 0

        }

        then: 'project details are right'

        verifyAll(lc) {
            getCapaNeeded('d1', '2020-M01').projects.size() == 2
            getCapaNeeded('d1', '2020-M01').projects[0].projectCapaNeed == 20
            getCapaNeeded('d1', '2020-M01').projects[1].projectCapaNeed == 20
        }

    }


}
