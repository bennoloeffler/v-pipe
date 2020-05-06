package extensions

import groovy.util.logging.Log

import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Logger
import java.util.logging.Level
import groovy.transform.InheritConstructors

import java.util.logging.SimpleFormatter



/**
 * Extend Logger to enable all types of messages - especially:
 * TRACE, DEBUG, INFO, WARNING, ERROR
 */
@InheritConstructors
@Log
class CustomLevel extends Level {

    static int getLevelValue(String name) {

        // OFF = Integer.MAX_VALUE, SEVERE = 1000, WARNING=900, INFO=800, CONFIG=700, FINE = 500, FINER=400, FINEST=300, ALL=MIN_VALUE
        // TRACE = FINEST, DEBUG=FINE, INFO=INFO, WARNING=WARNING, ERROR=SEVERE,
        switch (name.toUpperCase()) {
            case "TRACE":
                return FINEST.intValue()
            case "DEBUG":
                return FINE.intValue()
            case "INFO":
                return INFO.intValue()
            case "WARNING":
                return INFO.intValue()
            case "ERROR":
                return SEVERE.intValue()
            default:
                def level = WARNING.intValue() +
                        (SEVERE.intValue() - WARNING.intValue()) * Math.random()
                return level
        }
    }


    static {

        //System.setProperty("java.util.logging.config.fil", "logging.properties")



        Logger log = Logger.getGlobal()
        System.setProperty("java.util.logging.SimpleFormatter.format",
                '%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %5$s%6$s%n')
        //SimpleFormatter formatter = new SimpleFormatter()


        //FileHandler fh = new FileHandler("log/v-pipe.log")
        //fh.setFormatter(formatter)
        //log.addHandler(fh)

        //ConsoleHandler ch = new ConsoleHandler()
        //ch.setFormatter(formatter)
        //log.addHandler(ch)
        //ch.level = FINEST

        //def handlers = Logger.getGlobal().getHandlers()
        //Logger.getGlobal().setLevel(Level.WARNING)


        // Intercept (using methodMissing)
        Logger.metaClass.methodMissing = { String name, args ->
            //println "debug: inside methodMissing with $name"
            int val = getLevelValue(name)
            def level = new CustomLevel(name.toUpperCase(), val)
            def impl = { Object... varArgs ->
                delegate.log(level, varArgs[0])
            }
            // Cache the implementation on the metaClass
            Logger.metaClass."$name" = impl

            // Invoke the new implementation
            impl(args)
        }
    }


    static void main(String[] args) {

        Logger log = Logger.getGlobal()
        System.setProperty("java.util.logging.SimpleFormatter.format",
                '%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %5$s%6$s%n')
        SimpleFormatter formatter = new SimpleFormatter()


        FileHandler fh = new FileHandler("log/replication.log")
        fh.setFormatter(formatter)
        log.addHandler(fh)

        //ConsoleHandler ch = new ConsoleHandler()
        //ch.setFormatter(formatter)
        //log.addHandler(ch)
        //ch.level = FINEST

        def handlers = Logger.getGlobal().getHandlers()
        Logger.getGlobal().setLevel(Level.FINEST)
        //log.level = FINEST
        //def level = log.getLevel()
        //println "level $level"

        log.severe 'severe'
        log.warning 'warning'
        log.info 'info'
        log.config 'config'
        log.fine 'fine'
        log.finer 'finer'
        log.finest 'finest'

        // new but defined
        log.error 'error'
        //log.warning 'waring'
        //log.info 'info'
        log.debug "debug" // = fine
        log.trace 'trace' // = finest

        // fantasy - between waring and severe
        log.wtf "wtf"
    }


}
