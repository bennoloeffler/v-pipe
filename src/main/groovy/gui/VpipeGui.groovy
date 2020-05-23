package gui

import core.ProjectDataToLoadCalculator
import core.TaskInProject


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

    static JFrame projectFrame
    static JFrame loadFrame
    static LoadGridPanel loadPanel
    static GridProjectPanel projectPanel
    /**
     * start in DEMO-Mode
     * @param args
     */
    static void main(String[] args) {
        GridProjectPanel p = new GridProjectPanel()
        createProjectFrame(p)
    }


    /**
     * start when o is pressed = open data
     * @param p
     */
    def static openGuiOnFile() {
        List<TaskInProject> taskInProjects = ProjectDataToLoadCalculator.dataFromFileStatic
        ProjectGridModel m = new ProjectGridModel(taskInProjects)
        loadPanel = new LoadGridPanel(30)
        projectPanel = new GridProjectPanel(30,  m, loadPanel)
        createProjectFrame(projectPanel)
        createLoadFrame(loadPanel)

    }


    /**
     * create grid frame for pipelining
     * @param p
     */
    static void createProjectFrame(GridProjectPanel p) {
        projectFrame = new JFrame("v-pipe    |  +/- = Zoom  |  Pfeile = Cursor bewegen  |  Shift+Pfeile = Projekt bewegen  | CTRL-o = öffnen  ")
        JScrollPane sp = new JScrollPane(p)
        sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE)
        projectFrame.getContentPane().add(sp)
        projectFrame.addKeyListener(p) // needs to be here... not in p
        projectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        projectFrame.pack()
        //placeWindowTop(projectFrame)
        projectFrame.setVisible(true)
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        projectFrame.setSize((int)(projectFrame.getSize().width), (int)(dimension.height/2))
        projectFrame.setLocation((int)((dimension.width-projectFrame.width) / 2), 0)
    }


    /**
     * create (invisible) load view
     * @param p
     */
    def static createLoadFrame(LoadGridPanel lp) {
        loadFrame = new JFrame("v-pipe  Load")
        JScrollPane sp = new JScrollPane(lp)
        sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE)
        loadFrame.getContentPane().add(sp)
        //frame.addKeyListener(p) // needs to be here... not in p
        loadFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE)
        loadFrame.pack()
        //placeWindowBottom(loadFrame)
        loadFrame.setVisible(true)
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize()
        loadFrame.setSize((int)(projectFrame.getSize().width), (int)(dimension.height/2 -50))
        loadFrame.setLocation((int)((dimension.width-projectFrame.width) / 2), (int)(dimension.height/2))
        loadPanel.setGridWidth(projectPanel.grid) // to make repaint...

    }

    /**
     * make load window visible or invisible
     * @return
     */
    def static openLoad() {
        SwingUtilities.invokeLater {
            loadFrame?.setVisible(!loadFrame?.isVisible())
            if (loadFrame?.isVisible()) {
                loadPanel.setGridWidth(projectPanel.grid)
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