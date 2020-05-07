package core

import groovy.time.TimeCategory
import groovy.transform.Immutable
import org.joda.time.*

/**
 * Represents one entry of a dataset that models a multi-project situation.
 * A core.TaskInProject belongs to a project, has a start, an end, belongs to a department und needs a certain amount of capacity.
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
    double getCapaNeeded(Date intervalStart, Date intervalEnd) {
        double perDay = getDaysOverlap(intervalStart, intervalEnd) * getCapaPerDay()
        return perDay
    }

    enum WeekOrMonth {WEEK, MONTH}
    /**
     * returns a map with YYYY-MX or YYYY-WX - Keys and the corresponding capacity split
     * The end days of the intervals are excluded.
     * @param weeks = true means weeks, false means split in months
     * @return map of [ W01:13.7, W04:4] for weeks (or [M1:17, M3,19.5] for months)
     */
    Map<String, Double> getCapaDemandSplitIn(WeekOrMonth weekOrMonth) {
        assert starting < ending
        assert capacityNeeded > 0

        def resultMap = [:]
        if(weekOrMonth == WeekOrMonth.WEEK) {
            Date week = starting.getStartOfWeek()
            while (week < ending) {
                double capNeededInThatWeek = getCapaNeeded(week, week + 7)
                def key = week.getWeekYearStr()
                resultMap[key] = capNeededInThatWeek
                week += 7
            }
        } else {
            Date month = starting.getStartOfMonth()
            Date nextMonth

            while (month < ending) {
                use(TimeCategory) {
                    nextMonth = month + 1.month
                }
                def capNeededInThatMonth = getCapaNeeded(month, nextMonth)
                def key = month.getMonthYearStr()
                resultMap[key] = capNeededInThatMonth
                month = nextMonth
            }
        }
        return resultMap as Map<String, Double>
    }
}
