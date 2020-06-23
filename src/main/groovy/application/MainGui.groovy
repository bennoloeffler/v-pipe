package application


import model.Model
import model.VpipeDataException

import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JTextArea
import javax.swing.ToolTipManager
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

class MainGui {


    Model model
    View view
    GlobalController controller

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
            if(thrown instanceof VpipeDataException) {
                println "\nD A T E N - F E H L E R :\n" + thrown.getMessage()?:''
            } else {
                println "PROBLEM. Programm ist gecrasht :-(:\n${thrown.getMessage()?:''}\n\nSTACKTRACE: (bitte an BEL)\n\n"
                thrown.printStackTrace()
            }
            //todo logile
            //sleep(10000)
            //System.exit(-1)
        }
    }

    static void main(String[] args) {
        //
        // AWT event dispatch thread: get the exceptions...
        //
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler())
        System.setProperty("sun.awt.exception.handler",
                ExceptionHandler.class.getName())

        new MainGui().glueAndStart()
    }


    PrintStream outStream = new PrintStream(System.out) {
        @Override
        void println(String s) {
            out.println(s)
            //Graphics2D g = getLogArea().getGraphics()
            //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            getLogArea().append('\n'+s+'\n')
            getLogArea().setCaretPosition(getLogArea().getDocument().getLength())
        }
    }

    JTextArea getLogArea() {
        view.swing.textAreaLog
    }


    def glueAndStart() {


        model = new Model()
        view = new View(model)
        controller = new GlobalController(model, view)


        //
        // connect println with JTextArea right/down in app
        //
        System.setOut(outStream)
        //System.setErr(errStream)
        JTextArea la = getLogArea()
        la.setFont(new Font("Monospaced", Font.PLAIN, 18))
        println("Programm-Version: $Main.VERSION_STRING")


        //
        // connect exit-action to X - symbol on window
        //
        def disposeCallback = new WindowAdapter() {
            @Override
            void windowClosing(WindowEvent e) {
                super.windowClosing(e)
                controller.exitActionPerformed(null)
            }
        }
        ((JFrame)view.swing.frame).addWindowListener(disposeCallback)


        //
        // glue view and application together
        //
        view.swing.openAction.closure = controller.&openActionPerformed
        view.swing.saveAction.closure = controller.&saveActionPerformed
        view.swing.saveAsAction.closure = controller.&saveAsActionPerformed
        view.swing.exitAction.closure = controller.&exitActionPerformed

        view.swing.sortPipelineAction.closure = controller.&sortPipelineActionPerformed

        view.swing.printPerformanceAction.closure = controller.&printPerformanceActionPerformed



        //
        // config components
        //
        view.swing.build() {
            bind(target: view.swing.currentPath, targetProperty: 'text', source: model, sourceProperty: "currentDir", converter: { v -> v.toUpperCase()})
            bind(target: view.gridLoadModel, targetProperty: 'selectedProject', source: view.gridPipelineModel, sourceProperty: 'selectedProject')
            bind(target: view.loadView, targetProperty: 'gridWidth', source: view.pipelineView, sourceProperty: 'gridWidth')
            bind(target: view.gridProjectModel, targetProperty: 'projectName', source: view.gridPipelineModel, sourceProperty: 'selectedProject')
            //bind(target: view.gridProjectModel, targetProperty: 'updateToggle', source: view.gridPipelineModel, sourceProperty: 'updateToggle')
        }

        ToolTipManager.sharedInstance().setDismissDelay(15000) // 15 seconds


        //
        // start EDT and init model
        //
        try {
            view.start {
                model.setCurrentDir('.')
                model.readAllData() // in EDT
            }
        } catch (VpipeDataException vde) {
            JOptionPane.showMessageDialog(null,
                    vde.message,
                    "DATEN-FEHLER beim Start",
                    JOptionPane.WARNING_MESSAGE)
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Stacktrace speichern. Bitte.\nNochmal in Console starten.\nDann speichern.\nFehler: $e.message",
                    "F I E S E R   FEHLER beim Start  :-(",
                    JOptionPane.ERROR_MESSAGE)
            throw e // to produce stacktrace on console...
        }
    }
}
