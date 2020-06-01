package view

import core.LoadCalculator
import model.Model
import model.VpipeDataException
import model.WeekOrMonth
import transform.CapaTransformer
import transform.DateShiftTransformer

import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.SwingUtilities

import java.awt.Dimension

import java.awt.Toolkit








/**
 * Main - Demo and Real Starter (CTRL-O = open real data)
 */
class VpipeGui {


    static JFrame loadFrame
    static LoadPanel loadPanel

    static JFrame pipelineFrame
    static PipelinePanel pipelinePanel

    static JFrame projectFrame
    static ProjectPanel projectPanel

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
            sleep(10000)
            System.exit(-1)
        }
    }

    /**
     * start in DEMO-Mode
     * @param args
     */
    static void main(String[] args) {
        //
        // AWT event dispatch thread: get the exceptions...
        //
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler())
        System.setProperty("sun.awt.exception.handler",
                ExceptionHandler.class.getName())

        try {
            SwingUtilities.invokeLater {
                openGuiOnFile()
            }
        } catch ( VpipeDataException e ) {
            println "\nD A T E N - F E H L E R :\n" + e.getMessage()
            sleep(10000)
        } catch(Exception e) {
            println "PROBLEM. Programm ist gecrasht :-(:\n${e.getMessage()}\n\nSTACKTRACE: (bitte an BEL)\n\n"
            e.printStackTrace()
            //todo logile
            sleep(10000)
        }

        //
        // Demo Panel
        //
        //GridProjectPanel p = new GridProjectPanel()
        //createProjectFrame(p)
    }


    /**
     * start when o is pressed = open data
     * @param p
     */
    def static openGuiOnFile() {

        Model model = new Model()
        model.readAllData()

        LoadCalculator pt = new LoadCalculator(model: model)
        def dst = new DateShiftTransformer(model)
        pt.transformers << dst
        pt.transformers << new CapaTransformer(model)
        pt.calcDepartmentLoad(WeekOrMonth.WEEK)
        pt.transformers.remove(dst)

        loadPanel = new LoadPanel(30)


        ProjectModel projectModel = new ProjectModel(model)
        projectPanel = new ProjectPanel(30,  projectModel, loadPanel, pt)

        PipelineModel m = new PipelineModel(model)
        pipelinePanel = new PipelinePanel(30,  m, loadPanel, projectModel, pt)

        createPipelineFrame()
        createLoadFrame()
        createProjectFrame()

    }


    /**
     * create grid frame for pipelining
     * @param p
     */
    static void createPipelineFrame() {
        pipelineFrame = new JFrame("v-pipe    |  +/- = Zoom  |  Pfeile = Cursor bewegen  |  Shift+Pfeile = Projekt bewegen  | CTRL-o = öffnen  ")
        JScrollPane sp = new JScrollPane(pipelinePanel)
        sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE)
        pipelineFrame.getContentPane().add(sp)
        pipelineFrame.addKeyListener(pipelinePanel) // needs to be here... not in p
        pipelineFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        pipelineFrame.pack()
        //placeWindowTop(projectFrame)
        pipelineFrame.setVisible(true)
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        pipelineFrame.setSize((int)(dimension.width/2), (int)(dimension.height/2))
        pipelineFrame.setLocation(0, 0)
    }

    /**
     * create grid frame for pipelining
     * @param p
     */
    static void createProjectFrame() {

        projectFrame = new JFrame("v-pipe  Projekt")
        JScrollPane sp = new JScrollPane(projectPanel)
        sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE)
        projectFrame.getContentPane().add(sp)
        //projectFrame.addKeyListener(projectPanel) // needs to be here... not in p
        projectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        projectFrame.pack()
        //placeWindowTop(projectFrame)
        projectFrame.setVisible(true)
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        projectFrame.setSize((int)(dimension.width/2), (int)(dimension.height/2))
        projectFrame.setLocation((int)(dimension.width/ 2), 0)
    }


    /**
     * create (invisible) load view
     * @param p
     */
    def static createLoadFrame() {
        loadFrame = new JFrame("v-pipe  Load")
        JScrollPane sp = new JScrollPane(loadPanel)
        sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE)
        loadFrame.getContentPane().add(sp)
        //frame.addKeyListener(p) // needs to be here... not in p
        loadFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE)
        loadFrame.pack()
        //placeWindowBottom(loadFrame)
        loadFrame.setVisible(true)
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        loadFrame.setSize((int)(dimension.width), (int)(dimension.height/2 -50))
        loadFrame.setLocation(0, (int)(dimension.height/2))
        loadPanel.setGridWidth(pipelinePanel.grid) // to make repaint...

    }

    /**
     * make load window visible or invisible
     * @return
     */
    def static openLoad() {
        SwingUtilities.invokeLater {
            loadFrame?.setVisible(!loadFrame?.isVisible())
            if (loadFrame?.isVisible()) {
                loadPanel.setGridWidth(pipelinePanel.grid)
                //loadFrame.pack()
                //loadPanel.setSize(projectFrame.getWidth(), (int)(projectFrame.getHeight()/2))
                loadFrame?.invalidate()
                loadFrame?.repaint()
            }
        }
    }

    /*
    static void placeWindowTop(Window frame) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        int x = (int) ((dimension.getWidth() - frame.getWidth()))
        int y = 0
        frame.setLocation(x, y)
        frame.setLocationRelativeTo(null)
    }*/

    /*
    static void placeWindowBottom(Window frame) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        int x = (int) ((dimension.getWidth() - frame.getWidth()))
        int y = (int) ((dimension.getHeight() ) / 2)
        frame.setSize(projectFrame.getWidth(), (int)(projectFrame.getHeight()/2))
        frame.setLocation(x, y)
        //frame.setLocationRelativeTo(null)
    }*/


}