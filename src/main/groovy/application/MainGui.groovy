package application

import core.GlobalController
import gui.View
import model.Model
import model.VpipeDataException
import utils.FileSupport
import utils.UserSettingsStore

import javax.swing.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import static model.DataReader.isValidModelFolder

// HERE is the place for all TODO s (TODO = release, todo = remainder)
// related with the next release
// TODO practice clean code. https://issuu.com/softhouse/docs/cleancode_5minutes_120523/16
//      make tests functional again and part of the createDistribution task

// RELEASE 2.0 do all in Gui (MIK edition) with video starting from help
// ok 1.6 new Model, new Project, rename Project, templates and pipelines in GUI --> YOU DONT NEED THE FILES.
// ok 1.7-ip-in-gui.  idea: showIP - and IPs are created ALWAYS. But they are stored only, when the are shown.
//                      as soon as IPs "there in files", they are shown. showIP = true
//                      as soon as IPs "are missing in files", showIP=false. They are created, when acivated: 1/3rd of project at the end
//                      as soon as they are there, they can be hidden - but they are saved (with hidden flag)
// ok 1.8-beta-all-gui Detail-Fenster sortierten, Namen kürzen, Detail-Pipeline-View: Slots setzen. Pipeline löschen, Pipeline erzeugen.
// 1.9-tidy-code TODO:  month load (probably: make a monthly capa available, too)
//                      win and macos batch and jre in package
//                      packages sorted (application, view, view-model, ...
//                      empty yaml files (Feiertage, Profile, Kapa_Profile),
//                      yaml in (almost) all examples,
//
//                      Gui save As
//
// 2.0 all-analysis-in-gui
// 2.2 watch files (inside v-pipe in a text area, so that scenarios and shifts can be realized)
// 3.0 Operational: create “rueckmeldung” in folder -> verbleibend

class MainGui {

    static VERSION_STRING ='1.9.0-tidy-code'

    Model model
    View view
    GlobalController controller


    static void main(String[] args) {
        //
        // AWT event dispatch thread: get the exceptions...
        //
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler())
        System.setProperty("sun.awt.exception.handler",
                ExceptionHandler.class.getName())

        new MainGui().glueAndStart()
    }


    static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        void handle(Throwable thrown) {
            // for EDT exceptions
            handleException(Thread.currentThread().getName(), thrown)
        }

        void uncaughtException(Thread thread, Throwable thrown) {
            // for other uncaught exceptions
            handleException(thread.getName(), thrown)
        }

        void handleException(String tname, Throwable thrown) {
            if (thrown instanceof VpipeDataException) {
                println "\nD A T E N - F E H L E R :\n" + thrown.getMessage() ?: ''
            } else {
                File f = new File(FileSupport.instantErrorLogFileName)
                println "\n\nABSTURZ!   " +
                        "Fehler:\n${thrown.getMessage() ?: ''}\n" +
                        "Thread-Name: $tname\n" +
                        "Log-File: ${f.getAbsolutePath()}"

                StringWriter sw = new StringWriter()
                PrintWriter pw = new PrintWriter(sw)
                thrown.printStackTrace(pw)
                println sw.toString()
                f.text = sw.toString()
            }
        }
    }


    //
    // connect println with JTextArea right/down in app
    //
    PrintStream outStream = new PrintStream(System.out) {
        @Override
        void println(String s) {
            out.println(s)
            def doc = view.getLogArea().getStyledDocument()
            doc.insertString(doc.length, s + '\n', null)
            view.getLogArea().setCaretPosition(doc.length)
        }
    }


    def glueAndStart() {

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        // todo: add macos gestures for zooming windows:
        // https://stackoverflow.com/questions/48535595/what-replaces-gestureutilities-in-java-9

        model = new Model()
        view = new View(model)
        controller = new GlobalController(model, view)

        //
        // connect println with JTextArea right/down in app
        //
        System.setOut(outStream)
        System.setErr(outStream)

        println("Programm-Version: $VERSION_STRING")
        println "Auflösung in Pixel: x: ${(1000 * View.scaleX) as int}   y: ${(1000 * View.scaleY) as int}"

        //
        // connect exit-action to X-symbol on window
        //
        def disposeCallback = new WindowAdapter() {
            @Override
            void windowClosing(WindowEvent e) {
                super.windowClosing(e)
                controller.exitActionPerformed(null)
            }
        }
        ((JFrame) view.swing.frame).addWindowListener(disposeCallback)

        //
        // start EDT and init model
        //
        view.start {
            String home = System.getProperty("user.home");
            String dirToOpen = "$home/v-pipe-data"
            //String dirToOpen = "./bsp-daten"
            def recent = UserSettingsStore.instance.recentOpenedDataFolders
            if(recent) {dirToOpen =recent.last()}

            // DEV!
            String currentStartPath = new File(".").absolutePath
            if (currentStartPath.contains("projects")) {
                // if in development mode
                dirToOpen = "./open-model-dev"
            }

            boolean vpipeDataExists = isValidModelFolder(dirToOpen)

            if(vpipeDataExists) {
                controller.openDir(dirToOpen)
            } else {
                model.setCurrentDir(home)
            }
            model.setDirty(false)
        }
    }
}
