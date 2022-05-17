package model

import groovy.transform.CompileStatic
import groovy.transform.ToString
import utils.RunTimer

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

import static extensions.DateHelperFunctions.*

@CompileStatic
/**
 * Implements the idea of
 * - absolute calender weeks
 * - absolute days
 * starting from 0 at StartOfDates (4.1.2010)
 * ending at week 1565 at endOfDates (including) (1.1.2040)*/
class Weekifyer {

    static final LocalDate startOfDates = _sToLD("4.1.2010")
    static final LocalDate endOfDates = _sToLD("01.01.2040") // startOfDates + 365 * 30 + 4 // 01.01.2040

    @ToString(includeNames = true)
    static class CalWeekAbsWeek {
        int calWeek
        int absWeek
        int absDay
        String yearWeekStr
        LocalDate date
    }

    static rangeCheck(LocalDate d) {
        if (d < startOfDates) return "Date d: $d is earlier than $startOfDates"
        if (d > endOfDates) return "Date d: $d is later than $endOfDates"
        null
    }

    private static List<List> calcCalWeeksMap() {
        (startOfDates..endOfDates).collect { LocalDate d -> return [d, _getCalWeek(d)] }
    }

    private static Map<Integer, CalWeekAbsWeek> calcAbsWeekMap(List<List> allDays) {
        RunTimer t = new RunTimer()

        List lastElement = null
        def absWeek = 0
        for (d in allDays) {
            def currentWeek = d[1]
            def lastWeek = lastElement ?[1]
            def weekJump = lastWeek != currentWeek
            if (weekJump) absWeek++
            d << absWeek
            lastElement = d
        }
        Map<Integer, CalWeekAbsWeek> result = allDays.collectEntries { l ->
            int daysBetween = (int) ((LocalDate) l[0] - startOfDates)
            [(daysBetween): new CalWeekAbsWeek(calWeek: l[1],
                    absWeek: l[2],
                    absDay: daysBetween,
                    date: l[0],
                    yearWeekStr: _getWeekYearStr((LocalDate) (l[0])))]
        } //as TreeMap
        t.stop()
        result
    }


    static final Map<Integer, CalWeekAbsWeek> dateAbsWeekMap = calcAbsWeekMap(calcCalWeeksMap())

    static def weekify(List<TaskInProject> tasks) {
        tasks.each { task ->
            task.startingWeek = dateAbsWeekMap[absDay(convertToLocalDateViaMilisecond(task.starting))].absWeek
            task.endingWeek = dateAbsWeekMap[absDay(convertToLocalDateViaMilisecond(task.ending))].absWeek
        }
    }

    static CalWeekAbsWeek absWeek(int absDay) {
        String err = rangeCheck(startOfDates + absDay)
        if (err) {
            throw new RuntimeException(err)
        }
        def result = dateAbsWeekMap.getAt(absDay)
        result
    }

    static LocalDate convertToLocalDateViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
    /**
     * get day from 0 to 10954 (2040-01-01)
     * @param d
     * @return
     */
    static int absDay(LocalDate d) {
        d - startOfDates
    }

    static int absDay(Date d) {
        absDay(convertToLocalDateViaMilisecond(d))
    }

    static void main(String[] args) {
        //RunTimer t = new RunTimer()
        //def all = calcCalWeeksMap()
        //println all
        //def dateAbsWeekMap = calcAbsWeekMap(all)
        //dateAbsWeekMap.each { println(it.key + " --> " + it.value) }
        //t.stop()
        println absWeek(absDay(LocalDate.now()))
    }

}

