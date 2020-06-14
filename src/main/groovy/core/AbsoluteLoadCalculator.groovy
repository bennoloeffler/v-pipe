package core

import groovy.transform.EqualsAndHashCode
import groovy.transform.Memoized
import groovy.transform.ToString
import model.TaskInProject
import model.WeekOrMonth

@ToString
@EqualsAndHashCode
class ProjectCapaNeedDetails {
    TaskInProject originalTask
    Double projectCapaNeed
}

@ToString
@EqualsAndHashCode
class CapaNeedDetails {

    BigDecimal totalCapaNeed
    List<ProjectCapaNeedDetails> projects

    boolean asBoolean() {
        ! nullElement.is(this)
    }

    static CapaNeedDetails nullElement = new CapaNeedDetails(totalCapaNeed: 0, projects: [])
}

/**
 * Calculates a sparce matrix of CapaNeedDetails indexed by "department" and "timeKey".
 * Returns CapaNeedDetails.nullElement, when there is nothing to return.
 */
class AbsoluteLoadCalculator {

    WeekOrMonth weekOrMonth = WeekOrMonth.WEEK

    List<TaskInProject> tasks = []
    Map<String, Map<String, CapaNeedDetails>> capaLoadAbsolut = [:]


    AbsoluteLoadCalculator(List<TaskInProject> tasks, WeekOrMonth weekOrMonth = WeekOrMonth.WEEK) {
        this.tasks = tasks
        this.weekOrMonth = weekOrMonth
        calculate()
    }


    AbsoluteLoadCalculator() {
        this([])
    }


    @Memoized
    Double getMax(String department) {
        Map<String, CapaNeedDetails> depMap = capaLoadAbsolut[department]
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
        Map<String, CapaNeedDetails> departmentDetails = capaLoadAbsolut[department]
        CapaNeedDetails details = departmentDetails?.getAt(timeKey)
        if( details ) {
            result = details
        }
        result
    }


    void calculate() {

        Map<String, Map<String, CapaNeedDetails>> result = [:]

        tasks.each {

            Map<String, Double> timekeyCapaMap = it.getCapaDemandSplitIn(weekOrMonth)
            String department = it.department

            timekeyCapaMap.each { String timeKey, double capaValue ->

                // if there is not yet a department key and map: create
                if (!result[department]) {
                    result[department] = [:]
                }

                if(result[department][timeKey]) {
                    result [department][timeKey].totalCapaNeed += capaValue // add value
                    result [department][timeKey].projects << new ProjectCapaNeedDetails(originalTask: it, projectCapaNeed: capaValue) // and details map entry
                } else { // create first entry
                    result[department][timeKey] = new CapaNeedDetails(
                            totalCapaNeed: capaValue,
                            projects: [ new ProjectCapaNeedDetails(originalTask: it, projectCapaNeed: capaValue)  ] ) // otherwise create
                }
            }
        }
        capaLoadAbsolut = result
    }


}
