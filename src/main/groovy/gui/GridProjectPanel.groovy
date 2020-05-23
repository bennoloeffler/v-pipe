package gui

import core.ProjectDataToLoadCalculator
import core.TaskInProject
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import utils.RunTimer

import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

/**
 * custom JPanel with a grid and a red cursor in that grid,
 * that is moved with arrow keys: UP, RIGHT, DOWN, LEFT.
 * It has a GridModel to
 */
@CompileStatic
class GridProjectPanel extends JPanel implements MouseWheelListener, MouseMotionListener, MouseListener, KeyListener {

    Color cursorColor = new Color(255, 10, 50, 160)
    Color nowBarColor = new Color(255, 0, 0, 60)
    Color nowBarShadowColor = new Color(50, 50, 50, 30)


    LoadGridPanel lp // notify that one when grid or model changed...

    GridModel model
    int grid
    int borderWidth

    /**
     * the cursor - modeled as position in grid
     */
    int cursorX = 0
    int cursorY = 0

    /**
     * the last mouse position
     */
    int mouseX = 0
    int mouseY = 0

    /**
     * start and end of the last mouse-drag operation
     */
    int startDragX = -1, startDragY = 1, endDragX = 1, endDragY = -1

    /**
     * colors for lines...
     */
    List<Color> colors = createColorList()

    static List<Color> createColorList() {
        def cl = []
        10.times {
            0.step 11, 1,  {
                float h1 = (float)((float)it/10.0f)
                float h2 = (float)(h1 +0.5f)
                h2 = (float)(h2>1?h2-1:h2)
                cl << Color.getHSBColor(h1, 0.2f, 0.9f)
                cl << Color.getHSBColor(h2, 0.2f, 0.9f)
            }
        }
        cl
    }


    /**
     * the Mouse- and KeyListener, that gets all the commands and acts like a controller
     */

    //
    // MouseWheelListener
    //
    @Override
    @CompileStatic(TypeCheckingMode.SKIP) // because of that: scrollPane.processMouseWheelEvent(e)
    void mouseWheelMoved(MouseWheelEvent e) {
        //super?
        if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
            //   negative values if the mouse wheel was rotated up/away from the user,
            //   and positive values if the mouse wheel was rotated down/ towards the user
            int rot = e.getWheelRotation()
            int inc = (int)(grid * rot / 10)
            grid += inc!=0?inc:1 // min one tic bigger
            if(grid < 10){grid=10}
            if(grid > 400){grid=400}
            lp?.setGridWidth(grid)
            invalidateAndRepaint()
        } else {
            scrollPane.getVerticalScrollBar().setUnitIncrement((grid/3) as int)
            scrollPane.processMouseWheelEvent(e)
        }
    }


    //
    // MouseMotionListener
    //

    @Override
    void mouseDragged(MouseEvent e) {
        int x = e.getY()
        int y = e.getX()
        /*
        println("drag: x=$x y=$y")
        int gX = getGridXFromMouseX(x)
        int gY = getGridYFromMouseY(y)
        int swapX = getGridXFromMouseX(startDragX)
        int swapY = getGridYFromMouseY(startDragY)
        */
    }

    @Override
    void mouseMoved(MouseEvent e) {
        mouseX = e.getX()
        mouseY = e.getY()
        MouseEvent phantom = new MouseEvent(
                this,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                mouseX,
                mouseY,
                0,
                false)

        ToolTipManager.sharedInstance().mouseMoved(phantom)
    }

    @Override
    String getToolTipText(MouseEvent event) {
        String html = null

        def gridX = getGridXFromMouseX(event.x)
        def gridY = getGridYFromMouseY(event.y)
        if (gridX >= 0 && gridX < model.sizeX && gridY >= 0 && gridY < model.sizeY) {
            def element = model.getElement(gridX, gridY)
            if (element == GridElement.nullElement) {
                return null
            }

            // <br/>$element.fromToDateString
            html =
                    "<html><p style=\"background-color:white;\"><font color=\"#808080\" " +
                            "size=\"20\" face=\"Verdana\">${element.timeString?:''}<br/>$element.project<br/>$element.department" + // SIZE DOES NOT WORK!
                            "</font></p></html>"
        }
        return html
    }


    //
    // MouseListener
    //

    @Override
    void mouseClicked(MouseEvent e) {
        //def redraw = [new Point(cursorX, cursorY)]
        mouseX = e.getX()
        mouseY = e.getY()
        cursorX = getGridXFromMouseX()
        cursorY = getGridYFromMouseY()
        //redraw << new Point(cursorX, cursorY)
        //revalidate()
        //model.firePointListeners(redraw)
        invalidateAndRepaint()
    }

    @Override
    void mousePressed(MouseEvent e) {
        startDragX = e.getX()
        startDragY = e.getY()
    }

    @Override
    void mouseReleased(MouseEvent e) {
        endDragX = e.getX()
        endDragY = e.getY()
        startDragX = startDragY = endDragX = endDragY = -1
    }

    @Override
    void mouseEntered(MouseEvent e) {}

    @Override
    void mouseExited(MouseEvent e) {}

    //
    // KeyListener
    //

    @Override
    void keyTyped(KeyEvent e) {}

    @Override
    void keyPressed(KeyEvent e){

        if(KeyEvent.VK_I == e.getKeyCode()) {
            model.toggleIntegrationPhase(cursorX, cursorY)
        }

        if(KeyEvent.VK_O == e.getKeyCode()) {
            openFile()
        }

        if(KeyEvent.VK_L == e.getKeyCode()) {
            VpipeGui.openLoad()
        }

        if(KeyEvent.VK_PLUS == e.getKeyCode()) {
            grid = (grid * 1.1) as int
            if(grid > 400){grid=400}
            lp?.setGridWidth(grid)

        }
        if(KeyEvent.VK_MINUS == e.getKeyCode()) {
            grid = (grid / 1.1) as int
            if(grid < 10){grid=10}
            lp?.setGridWidth(grid)
        }

        //def redraw = [new Point(cursorX, cursorY)]
        if(KeyEvent.VK_UP == e.getKeyCode())    {cursorY > 0              ? --cursorY :0}
        if(KeyEvent.VK_DOWN == e.getKeyCode())  {cursorY < model.sizeY-1  ? ++cursorY :0}
        if(KeyEvent.VK_LEFT == e.getKeyCode())  {cursorX > 0              ? --cursorX :0}
        if(KeyEvent.VK_RIGHT == e.getKeyCode()) {cursorX < model.sizeX-1  ? ++cursorX :0}

        if(keyAndShiftPressed(e, KeyEvent.VK_UP)) {
            //println("SHIFT UP x: $cursorX y: $cursorY")
            if(cursorY >= 0) {
                model.swap(cursorY, cursorY + 1)
                colors.swap(cursorY % colors.size(), (cursorY + 1) % colors.size())
            }
        }

        if(keyAndShiftPressed(e, KeyEvent.VK_DOWN)) {
            //println("SHIFT DOWN x: $cursorX y: $cursorY")
            if(cursorY <= model.sizeY-1) {
                model.swap(cursorY - 1, cursorY)
                colors.swap((cursorY - 1) % colors.size(), cursorY % colors.size())
            }
        }

        if(keyAndShiftPressed(e, KeyEvent.VK_LEFT)) {
            //println("SHIFT LEFT x: $cursorX y: $cursorY")
            model.moveLeft(cursorY)
            updateLoadData()
        }

        if(keyAndShiftPressed(e, KeyEvent.VK_RIGHT)) {
            //println("SHIFT RIGHT x: $cursorX y: $cursorY")
            model.moveRight(cursorY)
            updateLoadData()
        }

        SwingUtilities.invokeLater() {
            scrollToCursorXY()
        }

        invalidateAndRepaint()
    }

    void scrollToCursorXY() {
        scrollRectToVisible(new Rectangle(cursorX * grid - grid, cursorY * grid - grid, 3 * grid, 3 * grid))
    }

    @Override
    void keyReleased(KeyEvent e) {}


    /**
     * @param e KeyEvent
     * @param keyCode e.g. KeyEvent.VK_RIGHT
     * @return pressed the keyCode together with SHIFT
     */
    private boolean keyAndShiftPressed(KeyEvent e, keyCode) {
        (keyCode == e.getKeyCode()) && (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) > 0
    }

    /**
     * ask for repaint
     */
    def invalidateAndRepaint() {
        revalidate()
        repaint()
    }


    /**
     * open "the" file
     */
    def openFile() {

        SwingUtilities.invokeLater {
            VpipeGui.openGuiOnFile()
            SwingUtilities.invokeLater {
                setCursorToNow()
                invalidateAndRepaint()
            }
        }


        /*

        JFileChooser fc = new JFileChooser(new File("."))
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)

        int returnVal = fc.showOpenDialog(this)

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile()
            File root = fc.getCurrentDirectory()
            println ("Opening: dir: $dir.name  root: $root.absolutePath" )
            SwingUtilities.invokeLater {
                VpipeGui.openGuiOnFile(dir)
                SwingUtilities.invokeLater {
                    revalidate()
                    repaint()
                }
            }

        } else {
            println("Open command cancelled by user.")
        }
        */
    }




    /**
     * next color of an index mapped to the color list
     * @param idx
     * @return color
     */
    Color getColor(int idx) {
        idx = idx % colors.size()
        return colors[ idx ]
    }


    JScrollPane getScrollPane() {
        getParent().getParent() as JScrollPane
    }

    /**
     * for testing purposes - uses internal, filled GridModel
     */
    GridProjectPanel(int grid = 60) {
        this(grid, new GridDemoModel(), null)
    }


    /**
     * create with custom grid and custom model
     * @param grid
     * @param borderWidth
     * @param model
     */
    GridProjectPanel(int grid, GridModel model, LoadGridPanel lp) {
        this.grid = grid
        this.borderWidth = grid / 3 as int
        addKeyListener(this)
        addMouseMotionListener(this)
        addMouseListener(this)
        addMouseWheelListener(this)
        this.model = model
        this.lp = lp
        setCursorToNow()

        lp?.setGridWidth(width)
        updateLoadData()
    }

    def updateLoadData() {
        if(lp) {
            SwingUtilities.invokeLater {
                ProjectDataToLoadCalculator dc = new ProjectDataToLoadCalculator()
                dc.taskList = ((ProjectGridModel) model).taskList
                Map<String, Map<String, Double>> stringMapMap = dc.calcDepartmentLoad(TaskInProject.WeekOrMonth.WEEK)
                List<String> allKeys = dc.getFullSeriesOfTimeKeys(TaskInProject.WeekOrMonth.WEEK)
                lp.setModelData(stringMapMap, allKeys)
            }
        }
    }

    def setCursorToNow() {
        cursorX = model.nowX
        scrollToCursorXY()
    }

    /**
     * heart of painting
     * @param g1d
     */
    @Override
    protected void paintComponent(Graphics g1d) {

        super.paintComponent(g1d)
        Graphics2D g = g1d as Graphics2D
        Rectangle r = g.getClipBounds()

        RunTimer t = new RunTimer(true)

        //
        // paint only elements inside clipBounds
        //

        for (int x in 0..model.sizeX - 1) {
            for (int y in 0..model.sizeY - 1) {
                int graphX = borderWidth + x * grid
                int graphY = borderWidth + y * grid
                if(graphX >= r.x-2*grid && graphX <= r.x+2*grid + r.width && graphY >= r.y-2*grid && graphY <= r.y+2*grid + r.height) {
                    GridElement e = model.getElement(x, y)
                    if (e != GridElement.nullElement || cursorX == x && cursorY == y) {
                        Color c = getColor(y)
                        g.setColor(c)
                        drawGridElement(c, g, e, x, y)
                    }
                }
            }
        }

        //
        // paint the now-indicator row above everything else
        //

        int offset = (grid/20) as int // shadow and space
        int size = (int)((grid - offset)/3 ) // size of shadow box and element box
        if(grid<15) {size=size*2}
        int round = (size / 2) as int // corner diameter
        int nowGraphX = borderWidth + model.nowX * grid + (int)((grid - size)/2)// position start (left up)
        int nowGraphY = borderWidth + model.sizeY * grid // position end (right down)

        // shadow
        g.setColor(nowBarShadowColor)
        g.fillRoundRect(nowGraphX+offset, +offset, size-4, nowGraphY+borderWidth-4, round, round)
        // element in project color, integration phase color (orange), empty color (white)
        g.setColor( nowBarColor )
        g.fillRoundRect(nowGraphX, 0, size-4 , nowGraphY+borderWidth-4, round, round)

        t.stop("drawing all")
    }


    /**
     * @return size as dimonsion based on model-size
     */
    @Override
    Dimension getPreferredSize() {
        return new Dimension(model.sizeX*grid + 2*borderWidth, model.sizeY*grid + 2*borderWidth)
    }



    /**
     * draw on grid element with shadow and
     * @param c a color for the line
     * @param g graphics
     * @param e gridElement, that is rendered
     * @param x position in the grid
     * @param y position in the grid
     */
    @CompileStatic
    def drawGridElement(Color c, Graphics2D g, GridElement e, int x, int y) {

        int offset = (grid/20) as int // shadow and space
        int size = grid - offset // size of shadow box and element box
        int round = (size / 2) as int // corner diameter
        int graphX = borderWidth + x * grid // position
        int graphY = borderWidth + y * grid // position
        //int gridMouseX = getGridXFromMouseX()
        //int gridMouseY = getGridYFromMouseY()
        //println("gridMouseX=$gridMouseX gridMouseY=$gridMouseY")

        // shadow
        g.setColor(Color.LIGHT_GRAY)
        g.fillRoundRect(graphX+offset, graphY+offset, size-4, size-4, round, round)

        // element in project color, integration phase color (orange), empty color (white)
        g.setColor( e.integrationPhase ? Color.orange : (e == e.nullElement ? Color.WHITE : c) )

        // or current mouse location color (overwrites even more)
        //if(x==gridMouseX && y == gridMouseY) {
        //    g.setColor(makeTransparent(g.getColor(), 120))
        //} //g.setColor(Color.LIGHT_GRAY)}

        g.fillRoundRect(graphX, graphY, size-4 , size-4, round, round)

        // or cursor color (overwrites everything)
        if(x==cursorX && y == cursorY) {
            g.setColor(cursorColor)
            g.fillRoundRect(graphX, graphY, size-4 , size-4, round, round)
        }


        if(grid>30) {
            // write (with shadow) some info
            // TODO: use Clip, Transform, Paint, Font and Composite
            float fontSize = 20.0 * grid / 60
            g.getClipBounds(rBackup)
            g.setClip(graphX, graphY, size - 6, size - 6)
            g.setFont(g.getFont().deriveFont((float) fontSize))
            g.setColor(Color.WHITE)
            g.drawString(e.project, graphX + (int) (grid * 0.2), graphY + (int) (grid / 2))
            g.setColor(Color.BLACK)
            g.drawString(e.project, graphX + (int) (grid * 0.2 - 2), graphY + (int) (grid / 2 - 2))
            g.setClip(rBackup)
        }
    }

    Rectangle rBackup = new Rectangle()


    /**
     * @param mouseY (optional - else take this.mouseY as base for calc)
     * @return gridY - based on mouse pos
     */
    int getGridYFromMouseY(int mouseY = -1) {
        mouseY = mouseY < 0 ? this.mouseY : mouseY // take this.mouseY, if default-Param, else take param
        int gridY = ((mouseY - borderWidth) / grid) as int
        gridY
    }

    /**
     * @param mouseX (optional - else take this.mouseX as base for calc)
     * @return gridX - based on mouse pos
     */
    int getGridXFromMouseX(int mouseX = -1) {
        mouseX = mouseX < 0 ? this.mouseX : mouseX // take this.mouseY, if default-Param, else take param
        int gridX = ((mouseX - borderWidth) / grid) as int
        gridX
    }

}
