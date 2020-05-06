package fileutils

class FileWatcherDeamonTest extends GroovyTestCase {

    def TEST_PATH = 'data\\test'
    def TEST_FILE = 'test.txt'

    void testCreateDirAndGetPath() {
        File f = new File(TEST_PATH)
        if(f.exists() && f.isDirectory()) {
            def done = f.deleteDir()
            assert done
        }

        assert !f.exists()

        def fwd = new FileWatcherDeamon(f.path)
        fwd.startReceivingEvents()
        sleep(100)

        assert fwd.path.toString() == TEST_PATH
        assert f.exists()
        assert f.isDirectory()

        fwd.stopReceivingEvents()
        sleep(1000)

        f.deleteDir()

    }

    void testStartReceivingEventsAndIsRunning() {
        def fwd = new FileWatcherDeamon(TEST_PATH)
        fwd.startReceivingEvents()
        sleep(100)
        assert fwd.isRunning()
        fwd.stopReceivingEvents()
        sleep(1000)

    }

    void testExtractEvents() {

        def testFileStr = "$TEST_PATH/$TEST_FILE"
        def f = new File(testFileStr)
        f.delete() // just in case it survived last test

        def fwd = new FileWatcherDeamon(TEST_PATH)
        fwd.startReceivingEvents()
        sleep(100)
        f.createNewFile()
        sleep(300)

        def events = fwd.extractEvents()
        fwd.stopReceivingEvents()
        sleep(1000)

        assert events
        assert events[0].eventType == EventType.CREATE
        assert events[0].path == TEST_FILE

        f.delete()


    }


    def tf(int num) {
        new File("$TEST_PATH/$num$TEST_FILE")
    }

    void testLiveCycle() {

        (0..5).each {tf(it).delete()} // new File('data/0test.xt').delete()

        def fwd = new FileWatcherDeamon(TEST_PATH)
        fwd.startReceivingEvents()
        sleep(100) // give FileWatcherDeamon chance to start up

        //
        // create files
        //
        (0..4).each {tf(it).createNewFile()}
        sleep(1000) // give FileWatcherDeamon chance to detect and fill list of events

        def events = fwd.extractEvents()

        assert events?.size() == 5
        (0..4).each {
            assert events[it].eventType == EventType.CREATE
            assert tf(it).toString().contains(events[it].path)
        }

        //
        // modify one, delete one, create a new
        //
        tf(0) << "some content"
        tf(1).delete()
        tf(5).createNewFile()

        sleep(1000)

        events = fwd.extractEvents()

        assert events?.size() == 3

        assert events[0].eventType == EventType.MODIFY
        assert tf(0).toString().contains(events[0].path)

        assert events[1].eventType == EventType.DELETE
        assert tf(1).toString().contains(events[1].path)

        assert events[2].eventType == EventType.CREATE
        assert tf(5).toString().contains(events[2].path)

        //
        // delete files
        //
        (0..5).each {tf(it).delete()}
        sleep(2000)

        events = fwd.extractEvents()

        assert events?.size() == 5
        int i
        [0,*(2..5)].each { // second one is already deleted...
            assert tf(it).toString().contains(events[i].path)
            assert events[i++].eventType == EventType.DELETE
        }
        fwd.stopReceivingEvents()
        sleep(1000)
    }

    /**
     * only get two notifications - ignore everything else...
     */
    void testSetFilter() {
        (0..5).each {tf(it).delete()} // new File('data/0test.xt').delete()

        def fwd = new FileWatcherDeamon(TEST_PATH)
        fwd.startReceivingEvents()
        //fwd.setFilter(["0test.txt", '1test.txt'])
        fwd.setFilter([/[01].*/, "someOtherFilterThatNeverMatches", 'andAnother.*'])
        sleep(100) // give FileWatcherDeamon chance to start up

        //
        // create files
        //
        (0..4).each {tf(it).createNewFile()}
        sleep(1000) // give FileWatcherDeamon chance to detect and fill list of events

        def events = fwd.extractEvents()

        assert events?.size() == 2
        (0..1).each {
            assert events[it].eventType == EventType.CREATE
            assert tf(it).toString().contains(events[it].path)
        }

        //
        // modify one, delete one, create a new
        //
        tf(0) << "some content"
        tf(1).delete()
        tf(5).createNewFile()

        sleep(1000)

        events = fwd.extractEvents()

        assert events?.size() == 2

        assert events[0].eventType == EventType.MODIFY
        assert tf(0).toString().contains(events[0].path)

        assert events[1].eventType == EventType.DELETE
        assert tf(1).toString().contains(events[1].path)

        // no third create...

        //
        // delete files
        //
        (0..5).each {tf(it).delete()}
        sleep(2000)

        events = fwd.extractEvents()

        assert events?.size() == 1
        // second one is already deleted...
        assert tf(0).toString().contains(events[0].path)
        assert events[0].eventType == EventType.DELETE

        fwd.stopReceivingEvents()
        sleep(1000)

    }

}
