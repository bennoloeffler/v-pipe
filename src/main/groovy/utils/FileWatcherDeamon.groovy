package utils

import core.VpipeException
import groovy.transform.Immutable
import groovy.transform.Synchronized
import groovy.util.logging.Log

import java.nio.file.*

import static java.nio.file.StandardWatchEventKinds.*
import static utils.EventType.*

/**
 * make the FileEvents a little bit more verbose
 */
enum EventType {CREATE, MODIFY, DELETE}


/**
 * indication of what happened: created, modified or deleted a file with path...
 */
@Immutable
class FileEvent {
    EventType eventType
    String path
}


/**
 * watch a dir (path). run in a separate thread
 * and fill events in a (synchronized) queue.
 * Client has to consume them from time to time.
 */
@Log
class FileWatcherDeamon {


    /**
     * path to watch
     */
    final Path path

    private WatchService watchService

    private WatchKey key

    final private List<FileEvent> events = [].asSynchronized()

    List<String> filter = ['.'] // default filter matches everything (containing a character)

    private boolean running = false

    /**
     * create and start FileWatcher in path pathString (relative to current or absolute)
     * @param pathString
     */
    FileWatcherDeamon(String pathString) {
        path = Paths.get(pathString)
        if( ! path.toFile().exists()) {
            path.toFile().mkdirs()
        }
    }

    /**
     * @return if the service is running
     */
    boolean isRunning() {
        running
    }

    /**
     * concurrently get events from head - while Deamon is adding potentially at tail...
     * @return as much events as possible
     */
    List<FileEvent> extractEvents() {
        List<FileEvent> result = []
        while(events.size()>0) {
            result << events.remove(0)
        }
        result
    }


    /**
     * starts a loop in a separate thread,
     * receives events and puts them in a queue,
     * where the client can consume them from time to time.
     */
    def startReceivingEvents() {

        assert !running

        new Thread("FileWatcherDeamon: ${path.toAbsolutePath()}").start {


            //
            // SETUP - may block until the first event comes
            //

            running = true
            //println("started FileWatcher: ${path.toAbsolutePath()}")
            watchService = FileSystems.getDefault().newWatchService()
            path.register(
                    watchService,
                    ENTRY_MODIFY,
                    ENTRY_DELETE,
                    ENTRY_CREATE)
            key = watchService.take()
            addShutdownHook {
                key.cancel() // give back upon System.exit
            }

            while (running) {
                //Poll all the events queued for the key
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind kind = event.kind()
                    switch (kind.name()) {
                        case 'ENTRY_CREATE':
                            for (f in getFilter().clone()) { // thread-save
                                if(event.context() =~ f) {
                                    events << new FileEvent(eventType: CREATE, path: event.context())
                                    break
                                }
                            }
                            break
                        case 'ENTRY_MODIFY':
                            // since deletes are notified as "MODIFY and then DELETE": filter out deleted modifys
                            if(new File("$path/${event.context().toString()}").exists()) {
                                for (f in getFilter().clone()) { // thread-save
                                    if(event.context() =~ f) {
                                        events << new FileEvent(eventType: MODIFY, path: event.context())
                                        break
                                    }
                                }
                            }
                            break
                        case 'ENTRY_DELETE':
                            for (f in getFilter().clone()) { // thread-save
                                if(event.context() =~ f) {
                                    events << new FileEvent(eventType: DELETE, path: event.context())
                                    break
                                }
                            }
                    }

                }
                sleep(200)
                //reset is invoked to put the key back to ready state
                boolean valid = key.reset()
                //If the key is invalid, just exit.
                if (!valid) {
                    running = false
                    throw new VpipeException("FileWatcherDeamon did not get the watch key any more... giving up")
                }
            }
            //println("Stopped FileWatcher: ${path.toAbsolutePath()}" )
            key.cancel()
        }
    }

    /**
     * stop the thread - the list of pending events may be consumed afterwards
     * @return
     */
    def stopReceivingEvents() {
        assert running
        running = false
    }

    /**
     * if you set some regex, not every Create/Modify/Delete are delivered any more,
     * but ONLY THOSE, your are specified with the "regex or regex" list.
     * This may just be a list of filenames - ore more specific.
     * if file.toString =~ regexOrRegex[0 to n] then deliver.
     * @param regexOrRegex
     */
    @Synchronized // needed?
    void setFilter(List<String> regexOrRegex) {
        assert regexOrRegex // at least one filter
        filter = regexOrRegex
    }

    @Synchronized
    List<String> getFilter() {
        filter
    }


    /**
     * test run it for a minute in ./data - then stop it
     * @param args
     */
    static void main(String[] args) {
        def fwd = new FileWatcherDeamon("data")
        def start = System.currentTimeSeconds()
        def time = 0
        while (time < 60) {
            def events = fwd.extractEvents()
            sleep(200 )
            events ? println(events) : ""
            time = System.currentTimeSeconds() - start
        }
        fwd.stopReceivingEvents()
    }

}