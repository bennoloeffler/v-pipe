package model

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor
import org.joda.time.Interval
import utils.RunTimer

import static extensions.DateHelperFunctions.*
import static extensions.StringExtension.toDate

/**
 * Represents one entry of a dataset that models a multi-project situation.
 * A model.TaskInProject belongs to a project, has a start, an end, belongs to a department und needs a certain amount of capacity.
 * The model assumes, that capacity consumption is distributed evenly between start and end.
 * Start and end are days. Start is included - end is EXCLUDED.
 */
//@Immutable
@TupleConstructor
@EqualsAndHashCode
@CompileStatic
class TaskInProject {

    Date startingLastTime
    Date endingLastTime
    double capacityNeededLastTime

    String project
    Date starting
    Date ending
    String department
    double capacityNeeded
    String description

    TaskInProject fromTemplate = null

    TaskInProject(
            String project,
            Date starting,
            Date ending,
            String department,
            double capacityNeeded,
            String description) {
        this.project = project
        this.starting = starting
        this.ending = ending
        this.department = department
        this.capacityNeeded = capacityNeeded
        this.description = description
    }

    Object clone() {
        return new TaskInProject(project, starting, ending, department, capacityNeeded, description)
    }

    TaskInProject cloneFromTemplate(String otherProject, int dayShift) {
        def tip = new TaskInProject(otherProject, starting + dayShift, ending+dayShift, department, capacityNeeded, description)
        tip.fromTemplate = this
        tip
    }

    /**
     * in order to print Date properly. The AST does not call the extensions.DateExtension
     * @return one line with whitespaces
     */
    @Override
    String toString() {
        "Task:( $project ${_dToS(starting)} ${_dToS(ending)} $department $capacityNeeded $description)"
    }

    /**
     * returns the number of days, the external interval overlaps with this task.
     * The end days of the intervals are excluded.
     * @param intervalStart
     * @param intervalEnd
     * @return overlap of the interval with this task in days
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
        return Math.round((millisOverlap / millisPerDay)as double)
    }

    /**
     * Format of Strings: dd.MM.yyyy
     * just an abbrevation for getDaysOverlap(Date intervalStart, Date intervalEnd)
     * @param intervalStart
     * @param intervalEnd
     * @return overlap of the interval with this task in days
     */
    long getDaysOverlap(String intervalStart, String intervalEnd) {
        getDaysOverlap(toDate(intervalStart), toDate(intervalEnd))
    }

    /**
     * @return the capa per day, that this task is consuming in average
     */
    double getCapaPerDay() {
        long days = ending - starting
        double r = capacityNeeded / days
        return r
    }

    /**
     * @param intervalStart
     * @param intervalEnd
     * @return the capacity, the task is spending in the interval
     */
    double getCapaNeeded(Date intervalStart, Date intervalEnd) {
        getDaysOverlap(intervalStart, intervalEnd) * getCapaPerDay()
    }


    boolean hasChanged() {
        return ! (
                cache &&
                starting == startingLastTime &&
                ending == endingLastTime &&
                capacityNeeded == capacityNeededLastTime)
    }

    /**
     * returns a map with YYYY-MX or YYYY-WX - Keys and the corresponding capacity split
     * The end days of the intervals are excluded.
     * @param weeks = true means weeks, false means split in months
     * @return map of [ W01:13.7, W04:4] for weeks (or [M1:17, M3,19.5] for months)
     */
    Map<String, Double> cache
    WeekOrMonth weekOrMonthLastTime
    Map<String, Double> getCapaDemandSplitIn(WeekOrMonth weekOrMonth) {
        RunTimer.getTimerAndStart('getCapaDemandSplitIn').withCloseable {

            assert starting < ending
            //assert capacityNeeded > 0
            if (!hasChanged() && weekOrMonth == weekOrMonthLastTime) {
                //t.stop()
                return cache
            } else {
                Map<String, Double> resultMap = [:]
                if (capacityNeeded == 0) {
                    //t.stop()
                    return resultMap as Map<String, Double>
                }
                if (weekOrMonth == WeekOrMonth.WEEK) {
                    Date week = _getStartOfWeek(starting)
                    //starting.getStartOfWeek() // not possible because of static comp
                    while (week < ending) {
                        double capNeededInThatWeek = getCapaNeeded(week, week + 7)
                        def key = _getWeekYearStr(week)//week.getWeekYearStr() // not possible because of static comp
                        resultMap[key] = capNeededInThatWeek
                        week += 7
                    }
                } else {
                    Date month = _getStartOfMonth(starting)
                    //starting.getStartOfMonth() // not possible because of static comp
                    Date nextMonth

                    while (month < ending) {
                        //use(TimeCategory) { // not possible because of static comp
                        Calendar c = Calendar.getInstance()
                        c.setTime(month)
                        c.add(Calendar.MONTH, 1)
                        nextMonth = c.getTime()
                        //nextMonth = month + 1.month // not possible because of static comp
                        //}
                        def capNeededInThatMonth = getCapaNeeded(month, nextMonth)
                        def key = _getMonthYearStr(month)
                        //month.getMonthYearStr() // not possible because of static comp
                        resultMap[key] = capNeededInThatMonth
                        month = nextMonth
                    }
                }

                // only for caching
                cache = resultMap
                startingLastTime = starting
                endingLastTime = ending
                capacityNeededLastTime = capacityNeeded
                weekOrMonthLastTime = weekOrMonth
                //t.stop()
                return resultMap
            }
        }
    }
}
