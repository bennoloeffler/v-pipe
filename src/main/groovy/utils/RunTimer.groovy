package utils



import java.util.logging.Logger;

/**
 * Timing simple:
 *
 * RunTimer t = new RunTimer(); // starts it first time
 * doSomething();
 * t.stop(); // uses console to log the time
 *
 * t.go(); // starts again with set to 0
 * doSomethingElse();
 * t.stop("doSomethingElse"); // stops and provides timing and information.
 *
 */
class RunTimer {

    boolean running
    long start
    long end
    long diff
    Logger log

    RunTimer() {
        go()
    }

    RunTimer(Logger log) {
        this.log = log
        go()
    }

    void go() {
        assert(!running)
        running=true
        start = System.currentTimeMillis()
    }

    void stop() {
        stop(null)
    }

    void stop(String explanation) {
        assert(running)
        end = System.currentTimeMillis()
        running=false
        diff = end-start
        String text="DURATION"
        if(explanation!=null && !"".equals(explanation)) {
            text+=" of "+explanation
        }
        text+=":  "+ readableTime(diff)

        if(log == null) {
            System.out.println(text)
        } else {
            log.info(text)
        }

    }

    static String readableTime(long millis) {
        long min =  millis / (1000 * 60)
        long sec =  (millis % (1000 *60)) / 1000
        long ms = millis % 1000
        return "" + min + " : " + sec + " : " + ms + " ( min : sec : ms )"

    }

}

