package core

import model.YellowRedLimit
import spock.lang.Specification

import static testdata.TestDataHelper.*


class PercentageLoadCalculatorTest extends Specification {

    PercentageLoadCalculator plc = new PercentageLoadCalculator()

    def 'calc emty'() {
        expect:
            with(plc) {
                getCapaNeeded('someDep', 'someTimeKey') == CapaNeedDetails.nullElement
                getMax('someDep') == 0
            }
    }

    def 'calc with one peace of data'() {

        // week 1 in 2020: 30.12 - 5.1.
        // week 2 in 2020: 6.1. - 12.1.

        given:
            plc.capaAvailable = ['d': ['2020-W01': new YellowRedLimit(20,30)]]
            plc.absoluteLoadCalculator.tasks =[t('p', '1.1.2020', '5.1.2020', 'd', 60)]

        when:
            plc.absoluteLoadCalculator.calculate()
            plc.calculate()

        then:

        verifyAll(plc) {
            getCapaNeeded('d', '2020-W01').totalCapaNeed == 2.0
            getCapaNeeded('d', '2020-W01').projects[0].projectCapaNeed == 2.0
            getMax('d') == 2.0
        }
    }


    def 'calc with some more data'() {

        // week 1 in 2020: 30.12 - 5.1.
        // week 2 in 2020: 6.1. - 12.1.

        given:
        plc.capaAvailable = ['d': [
                              '2020-W01': new YellowRedLimit(20,30),
                              '2020-W02': new YellowRedLimit(20,60)
        ]]

        plc.absoluteLoadCalculator.tasks =[
                t('p', '1.1.2020', '5.1.2020', 'd', 60),
                t('p', '1.1.2020', '5.1.2020', 'd', 60),
                t('p', '6.1.2020', '7.1.2020', 'd', 60),
                t('p', '6.1.2020', '7.1.2020', 'd', 60),
                t('p', '13.1.2020', '15.1.2020', 'd', 160), // this is ignored, because of capaAvailable
        ]

        when:
        plc.absoluteLoadCalculator.calculate()
        plc.calculate()

        then:

        verifyAll(plc) {
            getCapaNeeded('d', '2020-W01').totalCapaNeed == 4.0
            getCapaNeeded('d', '2020-W01').projects.size() == 2
            getCapaNeeded('d', '2020-W02').totalCapaNeed == 2.0
            getCapaNeeded('d', '2020-W02').projects.size() == 2

            getMax('d') == 4.0

            // W03 is IGNORED
            getCapaNeeded('d', '2020-W03').projects.size() == 0
            getCapaNeeded('d', '2020-W03').totalCapaNeed == 0


        }
    }


}
