package core

import extensions.DateHelperFunctions
import groovy.time.TimeCategory
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.Immutable
import groovy.transform.TupleConstructor
import org.joda.time.*

import static extensions.DateHelperFunctions.*

/**
 * Represents one entry of a dataset that models a multi-project situation.
 * A core.TaskInProject belongs to a project, has a start, an end, belongs to a department und needs a certain amount of capacity.
 * The model assumes, that capacity consumption is distributed evenly between start and end.
 * Start and end are days. Start is included - end is EXCLUDED.
 */
//@Immutable
@TupleConstructor
@EqualsAndHashCode
class TaskInProject {

    enum WeekOrMonth {WEEK, MONTH}

    String project
    Date starting
    Date ending
    String department
    double capacityNeeded

    /**
     * in order to print Date properly. The AST does not call the extensions.DateExtension
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
     * @return overlap of the interval with this task in days
     */
    //@CompileStatic
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
        getDaysOverlap(intervalStart.toDate(), intervalEnd.toDate())
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
    //@CompileStatic
    double getCapaNeeded(Date intervalStart, Date intervalEnd) {
        double perDay = getDaysOverlap(intervalStart, intervalEnd) * getCapaPerDay()
        return perDay
    }

    /**
     * returns a map with YYYY-MX or YYYY-WX - Keys and the corresponding capacity split
     * The end days of the intervals are excluded.
     * @param weeks = true means weeks, false means split in months
     * @return map of [ W01:13.7, W04:4] for weeks (or [M1:17, M3,19.5] for months)
     */
    //@CompileStatic
    Map<String, Double> getCapaDemandSplitIn(WeekOrMonth weekOrMonth) {
        assert starting < ending
        //assert capacityNeeded > 0
        def resultMap = [:]
        if (capacityNeeded==0){return resultMap as Map<String, Double>}
        if(weekOrMonth == WeekOrMonth.WEEK) {
            Date week = _getStartOfWeek(starting)//starting.getStartOfWeek() // not possible because of static comp
            while (week < ending) {
                double capNeededInThatWeek = getCapaNeeded(week, week + 7)
                def key = _getWeekYearStr(week)//week.getWeekYearStr() // not possible because of static comp
                resultMap[key] = capNeededInThatWeek
                week += 7
            }
        } else {
            Date month = _getStartOfMonth(starting)//starting.getStartOfMonth() // not possible because of static comp
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
                def key = _getMonthYearStr(month) //month.getMonthYearStr() // not possible because of static comp
                resultMap[key] = capNeededInThatMonth
                month = nextMonth
            }
        }
        return resultMap as Map<String, Double>
    }
}
