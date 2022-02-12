package application


import model.Model
import model.VpipeDataException

import javax.swing.JFrame
import javax.swing.JMenuBar
import javax.swing.JOptionPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.UIDefaults
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
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
                println thrown
                //thrown.printStackTrace(outStream)
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
            getLogArea().append(s+'\n')
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
        la.setFont(new Font("Monospaced", Font.PLAIN, 25))
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
        // configure tooltip manager
        //
        ToolTipManager.sharedInstance().setDismissDelay(600000)
        ToolTipManager.sharedInstance().setInitialDelay(500)


        //
        // glue view and application together
        //
        // File
        view.swing.openAction.closure = controller.&openActionPerformed
        view.swing.saveAction.closure = controller.&saveActionPerformed
        view.swing.saveAsAction.closure = controller.&saveAsActionPerformed
        view.swing.exitAction.closure = controller.&exitActionPerformed

        // Tool
        view.swing.sortPipelineAction.closure = controller.&sortPipelineActionPerformed

        // View
        view.swing.pipelineViewAction.closure = controller.&pipelineViewActionPerformed
        view.swing.loadViewAction.closure = controller.&loadViewActionPerformed
        view.swing.pipelineLoadViewAction.closure = controller.&pipelineLoadViewActionPerformed
        view.swing.projectViewAction.closure = controller.&projectViewActionPerformed

        // Help
        view.swing.helpAction.closure = controller.&helpActionPerformed
        view.swing.printPerformanceAction.closure = controller.&printPerformanceActionPerformed



        //
        // bind components together
        //
        view.swing.build() {
            bind(target: view.swing.currentPath, targetProperty: 'text', source: model, sourceProperty: "currentDir", converter: { v -> v.toUpperCase()})

            // sync pipelineView with loadView and projectView (details of witch project?)
            bind(target: view.gridLoadModel, targetProperty: 'selectedProject', source: view.gridPipelineModel, sourceProperty: 'selectedProject')
            bind(target: view.gridProjectModel, targetProperty: 'projectName', source: view.gridPipelineModel, sourceProperty: 'selectedProject')

            // sync zoom factor of load views
            bind(target: view.pipelineLoadView, targetProperty: 'gridWidth', source: view.pipelineView, sourceProperty: 'gridWidth')
            bind(target: view.loadView, targetProperty: 'gridWidth', source: view.pipelineView, sourceProperty: 'gridWidth')

            // sync cursorX: central node ist the pipelineView
            bind(target: view.projectView, targetProperty: 'cursorX', source: view.pipelineView, sourceProperty: 'cursorX')
            bind(target: view.loadView, targetProperty: 'cursorX', source: view.pipelineView, sourceProperty: 'cursorX')
            bind(target: view.pipelineLoadView, targetProperty: 'cursorX', source: view.pipelineView, sourceProperty: 'cursorX')

            bind(target: view.pipelineView, targetProperty: 'cursorX', source: view.loadView, sourceProperty: 'cursorX')
            bind(target: view.pipelineView, targetProperty: 'cursorX', source: view.projectView, sourceProperty: 'cursorX')
            bind(target: view.pipelineView, targetProperty: 'cursorX', source: view.pipelineLoadView, sourceProperty: 'cursorX')

            // sync indicators in tools and status line
            bind(target: view.pipelineView, targetProperty: 'hightlightLinePattern', source: view.swing.searchTextField, sourceProperty: 'text')
            bind(target: view.swing.timeLabel, targetProperty: 'text', source: view.pipelineView, sourceProperty: 'nowString')
            bind(target: view.swing.projectLabel, targetProperty: 'text', source: view.gridPipelineModel, sourceProperty: 'selectedProject')
            bind(target: view.swing.depLabel, targetProperty: 'text', source: view.gridProjectModel, sourceProperty: 'departmentName')

            // sync scroll-pane values hScrollBarValueZoomingSync
            bind(target: view.loadView, targetProperty: 'hScrollBarValueZoomingSync', source: view.pipelineView, sourceProperty: 'hScrollBarValueZoomingSync')
            bind(target: view.pipelineLoadView, targetProperty: 'hScrollBarValueZoomingSync', source: view.pipelineView, sourceProperty: 'hScrollBarValueZoomingSync')
        }



        //
        // start EDT and init model
        //
        view.start {
            //controller.openDir('.')
            model.currentDir = "."
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

}
