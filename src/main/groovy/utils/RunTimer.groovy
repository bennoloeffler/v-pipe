package utils

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode


@CompileStatic
class RunTimer implements Closeable{

    static Map<String, RunTimer> timers = [:]
    static long totalTimeStart

    boolean silent
    boolean running
    long start
    long end
    long diff
    long times
    long total

    static RunTimer getTimerAndStart(String category, boolean silent = true) {
        if(totalTimeStart == 0) {totalTimeStart = System.currentTimeMillis()}
        RunTimer t = timers[category]
        if(!t) {
            t = new RunTimer(silent)
            timers[category] = t
        } else {
            t.start()
        }
        t
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    static void printTimerResults() {

        println(getResultTable())
        /*
        println("TOTAL: ${readableTime(total)} 100%")

        sorted.each {
            def percent = sprintf("%4s", ((double)(it.value.total / total*100)).round(1))
            println("$percent% $it.key: ${readableTime(it.value.total)} #$it.value.times")
        }
        */
    }

    static String getResultTable() {
        long total = System.currentTimeMillis() - totalTimeStart

        def sorted = timers.sort {
            - it.value.total
        }

        def header = ["percent"       :'r',
                      "what                                                ":'l',
                      "calls    "     :'l',
                      "time (m:s:ms) " :'l'
        ]
        def data = []

        data[0] = [100.0, 'TOTAL', '-', readableShortTime(total)]
        def line = 1
        sorted.each{ lineData ->
            def percent = lineData.value.total / total*100
            data[line++] = [percent, lineData.key, lineData.value.times, readableShortTime(lineData.value.total)]
        }

        ConsoleTable.tableToString(header, data)

    }

    RunTimer(boolean silent = false) {
        start()
        this.silent = silent
    }

    void start() {
        assert(!running)
        running=true
        start = System.currentTimeMillis()
    }

    void stop() {
        stop(null)
    }

    void stop(String explanation) {
        assert( running )
        end = System.currentTimeMillis()
        running = false
        diff = end - start
        Closure msg = {
            String text = "DURATION"
            if (explanation) {
                text += " of " + explanation
            }
            text += ":  " + readableTime(diff)
            text
        }
        if( ! silent ) { println( msg() ) }
        total += diff
        times ++
    }

    static String readableTime(long millis) {
        long min =  (long)(millis / (1000 * 60))
        long sec =  (long)((millis % (1000 *60)) / 1000)
        long ms = millis % 1000
        return "" + min + " : " + sec + " : " + ms + " ( min : sec : ms )"
    }

    static String readableShortTime(long millis) {
        long min =  (long)(millis / (1000 * 60))
        long sec =  (long)((millis % (1000 *60)) / 1000)
        long ms = millis % 1000
        return sprintf('%03d',min) + " : " + sprintf('%02d',sec) + " : " + sprintf('%03d',ms)
    }

    static void main(String[] args) {
        def t0 = getTimerAndStart("almostNoTime")
        t0.stop()
        def t = getTimerAndStart("longest")
        sleep(500)
        t.stop()
        t = getTimerAndStart("longest")
        sleep(200)
        t.stop()
        def t2 = getTimerAndStart("inBetween")
        sleep(100)
        t2.stop()

        // AutoClose... even with Exception...
        try {
            getTimerAndStart("belsMethodWithException").withCloseable {
                sleep(100)
            }
            getTimerAndStart("belsMethodWithException").withCloseable {
                sleep(100)
                throw new RuntimeException("fuck...")
            }
        } catch(Exception e) {}

        printTimerResults()
    }

    @Override
    void close() {
        stop()
    }
}

