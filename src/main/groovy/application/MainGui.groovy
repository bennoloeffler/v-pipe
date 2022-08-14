package application

import core.GlobalController
import groovy.swing.SwingBuilder
import gui.View
import model.Model
import model.VpipeDataException
import utils.FileSupport
import utils.UserSettingsStore

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import static model.DataReader.isValidModelFolder

// HERE is the place for all TODO s (TODO = release, todo = remainder)
// related with the next release
// practice clean code. https://issuu.com/softhouse/docs/cleancode_5minutes_120523/16

// RELEASE 2.0 do all in Gui (MIK edition) with video starting from help
// ok 1.6 new Model, new Project, rename Project, templates and pipelines in GUI --> YOU DON'T NEED THE FILES.
// ok 1.7-ip-in-gui.  idea: showIP - and IPs are created ALWAYS. But they are stored only, when the are shown.
//                      as soon as IPs "there in files", they are shown. showIP = true
//                      as soon as IPs "are missing in files", showIP=false. They are created, when activated: 1/3rd of project at the end
//                      as soon as they are there, they can be hidden - but they are saved (with hidden flag)
// ok 1.8-beta-all-gui Detail-Fenster sortierten, Namen kürzen, Detail-Pipeline-View: Slots setzen. Pipeline löschen, Pipeline erzeugen.
// 1.9-tidy-code        Month Load
//                      Tooltips off - and bug free
//                      win and macos batch and jre in package
//                      comments and history for Projects: Date, Project, comment
//                      empty yaml files (Feiertage, Profile, Kapa_Profile),
//
// 2.0 all-in-gui
// 2.1 copy-and-update
//  Operational:
//   aktualisierte Projekte einzeln einlesen: read-new-data/Diff-Project-001.txt (lesen in aufsteigender Nummer)
//   Menu-eintrag
//   find files and sort them
//   each: read tasks, delete projects old, put tasks in model
//  Copy Project
//
// noTODO 2.2.0
//  ok - fix bug with "saving wrong date format"
//  ok - commit update commit from all: surfux, belmac and pux
//  ok - build osx, win, linux with java11 jre
//  ok - describe "read updates" in documentation
//  ok - describe in doc "Plugin to read Excel while reading updates"
//  ok - remove all starters, e.g. v-pipe.exe in distribution
//  ok - SWITCH to "correction mode" when error during opening occurs. OPEN FILES IN EDITOR!
//  ok - bat file for win start
//  NO - three different jre for windows/linux/macos into ONE distro
//  ok - deployment from every computer for linux, macos and windows
//  ok - make updates based on IDs in comments of tasks: Project-Name and comment may be the "ID"
//  ok - rename release to update-by-id
//  ok - example 10 bsp-daten with plugin and ID: update
//  ok - put release download page in markdown (link to dropbox)
//  ok - move ALL files in update foler to /done - make sure to move also files with file extension .xlsx (4 instead of 3 .txt)


// NEXT RELEASES

// TODO 2.3.0-good-help
//  - automatic open of file-error-correction-mode is interupted because of savety hint (save...)
//  - add the example models to the v-pipe home folder and make them appear in history
//  - add filmes to loom and links to readme
//  - put .zips in release folder. Place a README.txt there explaining the installation
//  - put a runnable folder for macOS somewhere for V&S
//  - create a script to create a RELEASE
//  - create a SNAPSHOT
//  - a bug when opening bsp-00: empty yaml behaves differently
//  - data-path is centered in gui. should be left

// 2.4 shadow-export-zero-id
// - shadow tasks (tasks that are not saved - but shown as shadow of the original)
// - export results (weekly, monthly, Details) to excel or csv
// - mark tasks with "zero capa" in a different color
// - have a setting: delete tasks with zero capa need during import

// 3.0 Durchsatz in EUR dd
// 4.0 CCPM-planning (kritischer Pfad in Projekten, most penetrating chain,
// 4.2 watch files (inside v-pipe in a text area, so that scenarios and shifts can be realized)

class MainGui {

    static VERSION_STRING = new File('version.txt').text

    Model model
    View view
    GlobalController controller
    static MainGui instance


    SwingBuilder swing

    def build() {

        swing = new SwingBuilder()

        swing.build {
            action(id: 'helpAction',
                    name: "Hilfe...",
                    closure: { println "helpPerformanceAction not connected to application..." })
            frame(title: "This is just a test",
                    size: [200, 200],
                    defaultCloseOperation: JFrame.EXIT_ON_CLOSE,
                    show: true) {
                label("Projekt suchen: ", foreground: Color.GRAY)
                menuBar(id: 'menuBar') {
                    menu(text: 'Help', mnemonic: 'H' ) {
                        menuItem(helpAction)
                    }
                }
            }
        }

    }


    static void main(String[] args) {
/*
        println "started 3"
        new MainGui().build()
        println "gui launched"

 */
        // ExamplesFromWebpage.main()
        // flatlaf: goto lib and start flatlaf-demo-2.4.jar
        // for sourcecode see: https://www.formdev.com/flatlaf

        // AWT event dispatch thread: get the exceptions.


        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler())
        System.setProperty("sun.awt.exception.handler",
                ExceptionHandler.class.getName())
        checkFirstStartAndHelp()
        instance = new MainGui()
        instance.glueAndStart()


    }


    static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        @SuppressWarnings('unused')
        void handle(Throwable thrown) {
            // for EDT exceptions
            handleException(Thread.currentThread().getName(), thrown)
        }

        void uncaughtException(Thread thread, Throwable thrown) {
            // for other uncaught exceptions
            handleException(thread.getName(), thrown)
        }

        @SuppressWarnings('GrMethodMayBeStatic')
        void handleException(String threadName, Throwable thrown) {
            if (thrown instanceof VpipeDataException) {
                println "\nD A T E N - F E H L E R :\n" + thrown.getMessage() ?: ''
            } else {
                File f = new File(FileSupport.instantErrorLogFileName)
                println "\n\nABSTURZ!   " + "Fehler:\n${thrown.getMessage() ?: ''}\n" + "Thread-Name: $threadName\n" + "Log-File:\n${f.getAbsolutePath()}"

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

        System.setProperty("apple.laf.useScreenMenuBar", "true")
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
        //println "Auflösung in Pixel: x: ${(1000 * View.scaleX) as int}   y: ${(1000 * View.scaleY) as int}"

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
            String home = System.getProperty("user.home")
            String dirToOpen = "$home/v-pipe-data"
            //String dirToOpen = "./bsp-daten"
            def recent = UserSettingsStore.instance.recentOpenedDataFolders
            if (recent) {
                dirToOpen = recent.last()
            }

            // DEV!
            /*
            String currentStartPath = new File(".").absolutePath
            if (currentStartPath.contains("projects")) {
                // if in development mode
                dirToOpen = "./open-model-dev"
            }*/

            boolean vpipeDataExists = isValidModelFolder(dirToOpen)

            if (vpipeDataExists) {
                controller.openDir(dirToOpen)
            } else {
                model.setCurrentDir(home)
            }
            model.setDirty(false)
        }
    }

    static void checkFirstStartAndHelp() {
        def fs = new File("ersterStart.md")
        if (fs.exists()) {
            println fs.getAbsolutePath()
            openBrowserWithHelp()
            fs.delete()
        }
    }

    static void openBrowserWithHelp() {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            //String path = new File("Referenz.html").absolutePath.replace('\\', ('/'))
            //path.replace(' ', '%20')
            //path = java.net.URLEncoder.encode(path, "UTF-8")
            //Desktop.getDesktop().browse(new URI("file:/$path"))
            Desktop.getDesktop().browse(new URI("https://github.com/bennoloeffler/v-pipe/blob/master/Readme.md"))
        }
    }
}
