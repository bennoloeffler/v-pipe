package model

/**
 * start included, end excluded.
 * So start = 1.5.2021, end = 1.5.2021 means: 0 days duration.
 * If you want the whole month: start = 1.7.2020 end = 1.8.2020 --> 31 days.
 * start = 1.7.2020 end = 31.7.2020 --> 30 days. One day missing... 31.7. excluded.
 */
trait StartEndInterval {

    Date start
    Date end

    int getOverlapInDays(Date otherStart, Date otherEnd) {

        assert start && end && start <= end
        assert otherStart && otherEnd && otherStart <= otherEnd

        if( otherEnd <= start || otherStart >= end ) {
            0 // other is before or after this
        } else if ( otherStart >= start && otherEnd <= end ) {
            otherEnd - otherStart // other is included or identical
        } else if ( start >= otherStart && end <= otherEnd ) {
            end - start // this is included or identical
        } else if ( otherStart < end && otherEnd > end) {
            end - otherStart // other overlaps at the end
        } else if ( otherEnd > start && otherStart < start) {
            otherEnd - start // other overlaps at the start
        } else {
            assert false, "error in algorighm: start: $start end: $end otherStart: $otherStart otherEnd: $otherEnd"
            0 //prevent warning... not meant to be returned
        }
    }

    int getOverlapInDays(StartEndInterval otherInterval) {
        getOverlapInDays(otherInterval.start, otherInterval.end)
    }
}


class DayIntervalCalculator {

    Date minDate = "1.1.2100".toDate()
    Date maxDate = "1.1.1970".toDate()

    StartEndInterval adaptMinMaxDate(List<StartEndInterval> intervals) {
        assert intervals && intervals.size() > 0
        intervals.each {
            if (it.start < (minDate as Date)) minDate = it.start
            if (it.end > (maxDate as Date)) maxDate = it.end
        }
        assert minDate < maxDate
        StartEndInterval r = new Object() as StartEndInterval
        r.start = minDate
        r.end = maxDate
        r
    }

    /*
    List<Date> calcFullyMinMaxDate(List<StartEndInterval> intervals){
        minDate = "1.1.2100".toDate()
        maxDate = "1.1.1970".toDate()
        adaptMinMaxDate(intervals)
    }*/

}

