package application


import model.DataReader
import model.Model
import model.VpipeDataException
import utils.FileSupport

import javax.swing.*
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

// HERE is the place for all TODO s (TODO = release, todo = remainder)
// related with the next release
// TODO practice clean code. https://issuu.com/softhouse/docs/cleancode_5minutes_120523/16
//      make tests functional again and part of the createDistribution task

// RELEASE 2.0 do all in Gui (MIK edition) with video starting from help
// 2.0 new Model, new Project, rename Project, templates and pipelines in GUI --> YOU DONT NEED THE FILES.
// 2.1 months in Gui
// 2.2 watch files (inside v-pipe in a text area, so that scenarios and shifts can be realized)
// 3.0 Operational: create “rueckmeldung” in folder -> verbleibend

class MainGui {
    Model model
    View view
    GlobalController controller

    static def scaleX = 1.0;
    static def scaleY = 1.0;

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
                println "PROBLEM. Programm ist gecrasht :-(:\n ${thrown.getMessage() ?: ''}\n\nSTACKTRACE: (bitte an BEL)\n\n"

                StringWriter sw = new StringWriter()
                PrintWriter pw = new PrintWriter(sw)
                thrown.printStackTrace(pw)
                println sw.toString()

                File f = new File(FileSupport.instantErrorLogFileName)
                f.text = sw.toString()

            }
        }
    }

    static void main(String[] args) {
        //
        // AWT event dispatch thread: get the exceptions...
        //
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler())
        System.setProperty("sun.awt.exception.handler",
                ExceptionHandler.class.getName())

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        scaleX = screenSize.getWidth() / 1000.0 // 1000 as virt size => xSize = 1000 * scaleX
        scaleY = screenSize.getHeight() / 1000.0

        new MainGui().glueAndStart()
    }

    /*
    PrintStream errStream = new PrintStream(System.err) {
        @Override
        void println(String s) {
            System.err.println(s)
            //Graphics2D g = getLogArea().getGraphics()
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            getLogArea().append(s + '\n')
            getLogArea().setCaretPosition(getLogArea().getDocument().getLength())
        }
    }*/


    PrintStream outStream = new PrintStream(System.out) {
        @Override
        void println(String s) {
            out.println(s)
            //Graphics2D g = getLogArea().getGraphics()
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            getLogArea().append(s + '\n')
            getLogArea().setCaretPosition(getLogArea().getDocument().getLength())
        }
    }

    JTextArea getLogArea() {
        view.swing.textAreaLog
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
        //System.setOut(new PrintStream(new CustomOutputStream()));
        //System.setErr(new PrintStream(new CustomOutputStream()));
        System.setErr(outStream)
        JTextArea la = getLogArea()
        la.setFont(new Font("Monospaced", Font.PLAIN, (int) (scaleX * 8)))
        println("Programm-Version: $Main.VERSION_STRING")
        println "Scalierung: x: ${(1000 * scaleX) as int}   y: ${(1000 * scaleY) as int}"


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
        // configure tooltip manager
        //
        ToolTipManager.sharedInstance().setDismissDelay(600000)
        ToolTipManager.sharedInstance().setInitialDelay(500)



        //
        // bind components together, see View
        //
        view.swing.build() {
        }


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


        SwingUtilities.invokeLater {
            // increase fonts in java 1.8
            /*
            JMenuBar menuBar = view.swing.menuBar
            Font f = new FontUIResource(menuBar.getFont().getFontName(), menuBar.getFont().getStyle(), 25)
            UIManager.put("Menu.font", f)
            UIManager.put("MenuItem.font", f)
            UIManager.put("Label.font", f)
            UIManager.put("TextArea.font", f)
            UIManager.put("Button.font", f)
            */

/*
            int szIncr = 10; // Value to increase the size by
            UIDefaults uidef = UIManager.getLookAndFeelDefaults();
            for (Map.Entry<Object,Object> e : uidef.entrySet()) {
                Object val = e.getValue();
                if (val != null && val instanceof FontUIResource) {
                    FontUIResource fui = (FontUIResource)val;
                    uidef.put(e.getKey(), new FontUIResource(fui.getName(), fui.getStyle(), fui.getSize()+szIncr));
                }
            }
            */
            JFrame frame = view.swing.frame
            SwingUtilities.updateComponentTreeUI(frame)

        }

    }

    static boolean isValidModelFolder(String dirToOpen) {
        new File(dirToOpen + "/" + DataReader.TASK_FILE_NAME).exists()
    }

}
