package core

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import model.YellowRedLimit


/**
 * Calculates a sparce matrix of CapaNeedDetails indexed by "department" and "timeKey".
 * Based on AbsoluteLoadCalculator and the Map<String, Map<String, YellowRedLimit>> capaAvailable.
 *
 * Returns CapaNeedDetails.nullElement, when there is nothing to return.
 * Or returns the detailed Numbers RELATIVE to the YellowRedLimit.RED
 */
@CompileStatic
class PercentageLoadCalculator {

    AbsoluteLoadCalculator absoluteLoadCalculator = new AbsoluteLoadCalculator()
    Map<String, Map<String, YellowRedLimit>> capaAvailable = [:]

    Map<String, Map<String, CapaNeedDetails>> capaLoadPercentage = [:]


    PercentageLoadCalculator() {
        this(new AbsoluteLoadCalculator(), [:] as Map<String, Map<String, YellowRedLimit>>)
    }

    PercentageLoadCalculator(AbsoluteLoadCalculator absoluteLoadCalculator,
                           Map<String, Map<String, YellowRedLimit>> capaAvailable) {
        this.absoluteLoadCalculator = absoluteLoadCalculator
        this.capaAvailable = capaAvailable
        calculate()
    }


    @Memoized
    Double getMax(String department) {
        Map<String, CapaNeedDetails> depMap = capaLoadPercentage[department]
        Double max = 0
        if(depMap) {
            max = depMap.max { entry ->
                entry.value.totalCapaNeed
            }.value.totalCapaNeed
        }
        max
    }

    @Memoized
    CapaNeedDetails getCapaNeeded(String department, String timeKey) {
        CapaNeedDetails result = CapaNeedDetails.getNullElement()
        Map<String, CapaNeedDetails> departmentDetails = capaLoadPercentage[department]
        CapaNeedDetails details = departmentDetails?.getAt(timeKey)
        if( details ) {
            result = details
        }
        result
    }


    void calculate() {

        Map<String, Map<String, CapaNeedDetails>> result = [:]

        capaAvailable.each { String department, def capaMap ->

            capaMap.each { String timeKey, YellowRedLimit capaRedYellow ->

                // if there is not yet a department key and map: create
                if (!result[department]) {
                    result[department] = [:] as Map<String, CapaNeedDetails>
                }

                CapaNeedDetails absDetails = absoluteLoadCalculator.getCapaNeeded(department, timeKey)
                if(absDetails) {
                    result[department][timeKey] = new CapaNeedDetails(
                            totalCapaNeed: absDetails.totalCapaNeed / capaRedYellow.red,
                            projects: absDetails.projects.collect {
                                new ProjectCapaNeedDetails(originalTask: it.originalTask, projectCapaNeed: it.projectCapaNeed / capaRedYellow.red)
                            }
                    )
                }
            }
        }
        capaLoadPercentage = result
    }

}
