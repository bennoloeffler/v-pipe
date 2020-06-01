package model

import groovy.transform.Immutable

/**
 * Those elements read from file eg. The "original planning".
 */
@Immutable
//@ToString
class  PipelineOriginalElement {
    String project
    Date startDate
    Date endDate
    /**
     *  mostly 1 - but sometimes, one task needs several parallel pipelinging slots
     */
    int pipelineSlotsNeeded

    String toString() { // for the Dates to be readable
        "Pipline-Element($project ${startDate.toString()} ${endDate.toString()} $pipelineSlotsNeeded)"
    }
}