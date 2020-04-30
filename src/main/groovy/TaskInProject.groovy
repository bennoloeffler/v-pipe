import groovy.transform.Immutable
import org.joda.time.*

import static HelperFunctions.*

/**
 * Represents one entry of a dataset that models a multi-project situation.
 * A TaskInProject belongs to a project, has a start, an end, belongs to a department und needs a certain amount of capacity.
 * The model assumes, that capacity consumption is distributed evenly between start and end.
 * Start and end are days. Start is included - end is EXCLUDED.
 */
@Immutable
class TaskInProject {

    String project
    Date starting
    Date ending
    String department
    double capacityNeeded

    /**
     * in order to print Date properly. The AST does not call the DateExtension
     * @return one line with whitespaces
     */
    @Override
    String toString() {
        "Task:( $project ${starting.toString()} ${ending.toString()} $department $capacityNeeded )"
    }

    /**
     * returns the number of days, the external interval overlaps with this task.
     * The end days of the intervals are excluded.
     * @param intervalStart
     * @param intervalEnd
     * @return
     */
    long getDaysOverlap(Date intervalStart, Date intervalEnd) {
        assert starting < ending
        assert intervalStart < intervalEnd
        Interval task = new Interval(starting.time, ending.time)
        Interval externalInterval = new Interval(intervalStart.time, intervalEnd.time)
        Interval intersection = externalInterval.overlap(task)

        // this is due to the fact, that leap seconds kill days otherwise
        double millisOverlap = intersection.toDuration().getMillis()
        double millisPerDay = 24 * 60 * 60 * 1000
        return Math.round(millisOverlap / millisPerDay)
    }

    /**
     * @return the capa per day, that this task is consuming in average
     */
    double getCapaPerDay() {
        long days = ending - starting
        capacityNeeded / days
    }

    /**
     * @param intervalStart
     * @param intervalEnd
     * @return the capacity, the task is spending in the interval
     */
    double getCapaNeeded(Date intervalStart, Date intervalEnd) {
        getDaysOverlap(intervalStart, intervalEnd) * getCapaPerDay()
    }

    /**
     * returns a map with YYYY-MX or YYYY-WX - Keys and the corresponding capacity split
     * The end days of the intervals are excluded.
     * @param intervalStart
     * @param intervalEnd
     * @return
     */
    Map<String, Double> getCapaDemandSplitInWeeks() {
        assert starting < ending
        assert capacityNeeded > 0

        def resultMap = [:]
        Date week = getStartOfWeek(starting)
        while(week < ending) {
            def capNeededInThatWeek = getCapaNeeded(week, week + 7)
            def key = getWeekYearStr(week)
            resultMap[key] = capNeededInThatWeek
            week += 7
        }
        return resultMap as Map<String, Double>
    }
}
