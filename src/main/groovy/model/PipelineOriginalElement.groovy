package model

import groovy.transform.Immutable
import org.joda.time.Interval

/**
 * Those elements read from file eg. The "original planning".
 */
//@Immutable
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

    PipelineOriginalElement cloneFromTemplate(String otherProject, int dayShift) {
        def poe = new PipelineOriginalElement(
                project: otherProject,
                startDate: this.startDate + dayShift,
                endDate: this.endDate + dayShift,
                pipelineSlotsNeeded: this.pipelineSlotsNeeded)
        poe
    }

    // TODO: move to abstract base class eg "StartEndDayElement"
    long getDaysOverlap(Date intervalStart, Date intervalEnd) {
        assert startDate < endDate
        assert intervalStart < intervalEnd
        Interval task = new Interval(startDate.time, endDate.time)
        Interval externalInterval = new Interval(intervalStart.time, intervalEnd.time)
        Interval intersection = externalInterval.overlap(task)
        if(intersection) {
            // this is due to the fact, that leap seconds kill days otherwise
            double millisOverlap = intersection.toDuration().getMillis()
            double millisPerDay = 24 * 60 * 60 * 1000
            return Math.round((millisOverlap / millisPerDay) as double)
        } else {
            return 0
        }
    }

}