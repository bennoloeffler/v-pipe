package newview

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Log
import groovyx.gpars.GParsPool
import groovyx.gpars.TransparentParallel
import utils.RunTimer

import javax.swing.*
import java.awt.*
import java.awt.event.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * custom JPanel with a grid and a red cursor in that grid,
 * that is moved with arrow keys: UP, RIGHT, DOWN, LEFT.
 * It has a GridModel to
 */
@CompileStatic
class GridPanel extends JPanel implements MouseWheelListener, MouseMotionListener, MouseListener, KeyListener, FocusListener, PanelBasics {


    //Color cursorColor = new Color(255, 10, 50, 160)
    //Color nowBarColor = new Color(255, 0, 0, 60)
    //Color nowBarShadowColor = new Color(50, 50, 50, 30)

    GridModel model

    @Bindable String hightlightLinePattern

    @Bindable String nowString

    @Bindable int gridWidth

    @Bindable detailsToolTip = true

    //int borderWidth
    //int nameWidth

    /**
     * the cursor - modeled as position in grid
     */
    @Bindable
    int cursorX = 0
    int cursorY = 0

    /**
     * the last mouse position
     */
    int mouseX = 0
    int mouseY = 0

    @Bindable
    int hScrollBarValueZoomingSync

    /**
     * start and end of the last mouse-drag operation
     */
    //int startDragX = -1, startDragY = 1, endDragX = 1, endDragY = -1




    /**
     * the Mouse- and KeyListener, that gets all the commands and acts like a application
     */


    //
    // Focus Listener
    //

    @Override
    void focusGained(FocusEvent e) {

    }

    @Override
    void focusLost(FocusEvent e) {
        mouseX = -1
        invalidateAndRepaint(this)
    }

    //
    // MouseWheelListener
    //
    @Override
    @CompileStatic(TypeCheckingMode.SKIP) // because of that: scrollPane.processMouseWheelEvent(e)
    void mouseWheelMoved(MouseWheelEvent e) {
        if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
            //   negative values if the mouse wheel was rotated up/away from the user,
            //   and positive values if the mouse wheel was rotated down/ towards the user
            double rot = e.getPreciseWheelRotation()
            //println "rot: $rot"
            int inc = (int) (gridWidth * rot / 10)
            //println "inc: $inc"
            def oldPreferredSize = getPreferredSize()
            setGridWidth(gridWidth + (inc != 0 ? inc : 1)) // min one tic bigger
            minMaxGridCheck()
            updateOthersFromGridWidth(gridWidth, this)
            def newPreferredSize = getPreferredSize()
            def ratio = newPreferredSize.width / oldPreferredSize.width
            //println("ratio: $ratio")

            adjustLocationAndMouseAfterZooming(mouseX, mouseY, oldPreferredSize.width, newPreferredSize.width)
            //registerScrollBarListener()

            //SwingUtilities.invokeLater {
                invalidateAndRepaint(this)
            //}
        }else {
            getScrollPane(this)?.getVerticalScrollBar()?.setUnitIncrement((gridWidth/3) as int)
            getScrollPane(this)?.processMouseWheelEvent(e)
        }
    }


    //
    // MouseMotionListener
    //


    @Override
    void mouseMoved(MouseEvent e) {
        mouseX = e.getX()
        mouseY = e.getY()
        //println "mouse move: $mouseX $mouseY"
        mouseMoved(e, this)
        invalidateAndRepaint(this)
    }



    @Override
    String getToolTipText(MouseEvent event) {
        if(!detailsToolTip) {return null}

        String html = null

        def gridX = getGridXFromMouseX(event.x)
        def gridY = getGridYFromMouseY(event.y)
        if(gridX < 0 || gridX > model.sizeX-1) {
            return model.getLineNames()[gridY]
        }
        if(gridY > model.getSizeY()-1) {
            return model.getColumnNames()[gridX]
        }
        if (gridX >= 0 && gridX < model.sizeX && gridY >= 0 && gridY < model.sizeY) {
            def element = model.getElement(gridX, gridY)
            if (element == GridElement.nullElement) {
                return model.getLineNames()[gridY] + '  ' + model.getColumnNames()[gridX]
            }

            // <br/>$element.fromToDateString
            def details = model.getDetailsForTooltip(gridX, gridY)

            html  =      """<html><head><style>
                                h1 { color: #808080; font-family: verdana; font-size: 120%; }
                                p  { color: black; font-family: courier; font-size: 120%; } </style> </head>
                                
                                <body>
                                    <h1>${details[0]}</h1>
                                    <p>
                                        Kapa-Bedarf: ${details[1]}<br/>
                                        ${(details.size()>2)?(details[2]+"<br/>"):"INTEGRATIONS-PHASE...<br/>"}
                                        ${element ? element.timeString : ""} <br/>
                                    </p>
                                </body>
                             </html>                                
                         """
        }
        return html
    }

    @Override
    Point getToolTipLocation(MouseEvent event) {
        if(!detailsToolTip){return null}
        def gridX = (int)((event.x-borderWidth-nameWidth)/gridWidth)
        def locX = gridX*gridWidth + borderWidth + nameWidth + 2*gridWidth
        def gridY = (int)((event.y-borderWidth)/gridWidth)
        def locY = gridY*gridWidth + borderWidth

        return new Point(locX, locY)
    }


    //
    // MouseListener
    //

    @Override
    void mouseClicked(MouseEvent e) {
        requestFocusInWindow()

        //def redraw = [new Point(cursorX, cursorY)]
        mouseX = e.getX()
        mouseY = e.getY()
        //cursorX = getGridXFromMouseX()
        setCursorX(getGridXFromMouseX()) // @bindable...
        cursorY = getGridYFromMouseY()
        model.setSelectedElement(cursorX, cursorY)

        invalidateAndRepaint(this)
    }

    @Override
    void mousePressed(MouseEvent e) {
        //startDragX = e.getX()
        //startDragY = e.getY()
    }

    @Override
    void mouseReleased(MouseEvent e) {
        //endDragX = e.getX()
        //endDragY = e.getY()
        //startDragX = startDragY = endDragX = endDragY = -1
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void mouseEntered(MouseEvent e) {
        requestFocusInWindow()
        ToolTipManager.sharedInstance().hideTipWindow()
    }

    @Override
    void mouseExited(MouseEvent e) {}

    //
    // KeyListener
    //

    @Override
    void keyTyped(KeyEvent e) {}

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void keyPressed(KeyEvent e){

        int keyCode = e.getExtendedKeyCode()
        //println e.toString()

        if(KeyEvent.VK_I == keyCode) {
            //model.toggleIntegrationPhase(cursorX, cursorY)
        }

        if(KeyEvent.VK_Z == keyCode) {
            printZoomInfo()
        }

        if(KeyEvent.VK_N == keyCode) {
            setCursorToNow()
        }

        if(KeyEvent.VK_D == keyCode) {
            setDetailsToolTip(!detailsToolTip)
            if(detailsToolTip) {
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
                ToolTipManager.sharedInstance().showTipWindow()
            }else{
                ToolTipManager.sharedInstance().hideTipWindow()
            }
        }

        //println "VK_PLUS: " + KeyEvent.VK_PLUS
        //println "ext k c: " + e.getExtendedKeyCode()

        if(KeyEvent.VK_PLUS == e.getExtendedKeyCode()) {
            doZooming {gridWidth * 1.1}
            //registerScrollBarListener()
        }

        if(KeyEvent.VK_MINUS == e.getExtendedKeyCode()) {
            doZooming {gridWidth / 1.1}
            //registerScrollBarListener()
        }



        //def redraw = [new Point(cursorX, cursorY)]
        if(KeyEvent.VK_UP == keyCode)    {cursorY > 0              ? --cursorY :0; scrollToCursorXY()}
        if(KeyEvent.VK_DOWN == keyCode)  {cursorY < model.sizeY-1  ? ++cursorY :0; scrollToCursorXY()}
        if(KeyEvent.VK_LEFT == keyCode)  {cursorX > 0              ? setCursorX(cursorX-1) :0; scrollToCursorXY()}
        if(KeyEvent.VK_RIGHT == keyCode) {cursorX < model.sizeX-1  ? setCursorX(cursorX+1) :0; scrollToCursorXY()}

        if(keyAndShiftPressed(e, KeyEvent.VK_UP)) {
            //println("SHIFT UP x: $cursorX y: $cursorY")
            if(cursorY >= 0) {
                model.swap(cursorY, cursorY + 1)
                colors.swap(cursorY % colors.size(), (cursorY + 1) % colors.size())
                scrollToCursorXY()
            }
        }

        if(keyAndShiftPressed(e, KeyEvent.VK_DOWN)) {
            //println("SHIFT DOWN x: $cursorX y: $cursorY")
            if(cursorY <= model.sizeY-1) {
                model.swap(cursorY - 1, cursorY)
                colors.swap((cursorY - 1) % colors.size(), cursorY % colors.size())
                scrollToCursorXY()
            }
        }

        if(keyAndShiftPressed(e, KeyEvent.VK_LEFT)) {
            model.moveLeft(cursorY)
            scrollToCursorXY()
        }

        if(keyAndShiftPressed(e, KeyEvent.VK_RIGHT)) {
            model.moveRight(cursorY)
            scrollToCursorXY()
        }

        if(keyAndCtrlPressed(e, KeyEvent.VK_LEFT)) {
            model.makeSmaller(cursorY)
            scrollToCursorXY()
        }

        if(keyAndCtrlPressed(e, KeyEvent.VK_RIGHT)) {
            model.makeBigger(cursorY)
            scrollToCursorXY()
        }

        //scrollToCursorXY()
        invalidateAndRepaint(this)

        model.setSelectedElement(cursorX, cursorY)

    }

    void scrollToCursorXY() {
        scrollRectToVisible(new Rectangle(nameWidth + cursorX * gridWidth - 2*gridWidth, cursorY * gridWidth - 2*gridWidth, 4 * gridWidth, 4 * gridWidth))
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

    private boolean keyAndCtrlPressed(KeyEvent e, keyCode) {
        (keyCode == e.getKeyCode()) && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0
    }

    def minMaxGridCheck() {
        if(gridWidth > 100){setGridWidth(100)}
        if(gridWidth < 10){setGridWidth(10)}
    }


    def cursorXChanged = { PropertyChangeEvent e ->
        if(cursorX>-1) {
            invalidateAndRepaint(this)
            def nowStr = model.getColumnNames()[cursorX]
            setNowString(nowStr)
            SwingUtilities.invokeLater {
                scrollToCursorXY()
            }
        }
    }


    def doZooming(Closure howToZoom) {
        def oldPreferredSize = getPreferredSize()
        setGridWidth(howToZoom() as int)
        minMaxGridCheck()
        updateOthersFromGridWidth(gridWidth, this)
        def vr = getVisibleRect()
        adjustLocationAndMouseAfterZooming(
                vr.centerX,
                vr.centerY,
                oldPreferredSize.width,
                getPreferredSize().width)
    }

    /**
     * @param x point to keep where it is
     * @param y point to keep where it is
     * @param oldSize of the panel
     * @param newSize of the panel
     */
    def adjustLocationAndMouseAfterZooming(double x, double y, double oldSize, double newSize ) {
        def r = getVisibleRect()
        //println "\n\nvr: $r"
        double ratio = newSize / oldSize

        double deltaX = x * (1 - ratio)
        double tmpX = r.x - deltaX

        double deltaY = y * (1 - ratio)
        double tmpY = r.y - deltaY

        //println "dX dY $deltaX $deltaY"
        //println "tmpX tmpY $tmpX $tmpY"

        //tmpX >= 0?:println("--> tmpX = $tmpX")
        //tmpY >= 0?:println("--> tmpY = $tmpY")
        def locX = (int) (tmpX >= 0 ? tmpX : 0)
        def locY = (int) (tmpY >= 0 ? tmpY : 0)
        //println "locX locY $locX $locY"

        setLocation(-locX, -locY)

        mouseY = (int)(mouseY * ratio)
        mouseX = (int)(mouseX * ratio)
        //println "mouse: $mouseX $mouseY"
        //println "cursor: ${getGridXFromMouseX(-1)} ${getGridYFromMouseY(-1)}"

        SwingUtilities.invokeLater notifyScrollBars as Runnable // after invalidate to get the new value of scrollBar

    }

    AdjustmentListener scrollBarAdjListener = new AdjustmentListener() {
        @Override
        void adjustmentValueChanged(AdjustmentEvent e) {
            notifyScrollBars.call()
        }
    }

    def registerScrollBarListener() {
        def hsb = getScrollPane(this)?.getHorizontalScrollBar()
        if (hsb) {
            if(! hsb.adjustmentListeners.contains(scrollBarAdjListener)) {
                hsb.addAdjustmentListener(scrollBarAdjListener as AdjustmentListener)
            }
        }
    }

    Closure notifyScrollBars = {
        def hsb = getScrollPane(this)?.getHorizontalScrollBar()
        def spv = -1
        if (hsb) {
            spv = hsb.value
        }
        sethScrollBarValueZoomingSync(spv)
    }

    def printZoomInfo() {
        def location = getLocation()
        println("location: $location")
        def los = getLocationOnScreen()
        println("loc on screen: $los")
        def rect = getBounds()
        println("bounds: $rect")
        def valueSB = getScrollPane(this).getHorizontalScrollBar().value
        println("H-Scrollbar val: $valueSB")
        def visRect = getVisibleRect()
        println("visible rect: $visRect")
        def prefSize = getPreferredSize()
        println("pref size: $prefSize")
    }


    /**
     * create with custom grid and custom model
     * @param grid
     * @param borderWidth
     * @param model
     */
    GridPanel(int grid, GridModel model) {
        //setDoubleBuffered(false)

        setFocusable(true)
        this.gridWidth = grid
        minMaxGridCheck()
        updateOthersFromGridWidth(gridWidth, this)

        addKeyListener(this)
        addMouseMotionListener(this)
        addMouseListener(this)
        addMouseWheelListener(this)
        addFocusListener(this)
        this.model = model
        PropertyChangeListener l = {
            SwingUtilities.invokeLater {
                invalidateAndRepaint(this)
            }
        } as PropertyChangeListener
        PropertyChangeListener sbs = { PropertyChangeEvent e ->
            //println "hScrollBarValueZoomingSync changed: $e.oldValue to $e.newValue"
        } as PropertyChangeListener
        model.addPropertyChangeListener('updateToggle', l)
        addPropertyChangeListener('cursorX', cursorXChanged as PropertyChangeListener)
        addPropertyChangeListener('hightlightLinePattern', l)
        addPropertyChangeListener('hScrollBarValueZoomingSync', sbs)

    }

    /*
    void setModel(GridModel model) {
        assert false
        this.model = model
        setCursorToNow()
    }*/


    def setCursorToNow() {
        if(model.nowX >= 0) {
            setCursorX(model.nowX)
            scrollToCursorXY()
        }
        SwingUtilities.invokeLater{
            registerScrollBarListener()
        }
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    static ArrayList getList(int x) {
        new ArrayList<>(0..x).makeConcurrent() as ArrayList<Integer>
    }

    Rectangle r = new Rectangle()
    /**
     * heart of painting
     * @param g1d
     */
    @Override
    //@CompileStatic
    //@TypeChecked

    //@CompileDynamic

    protected void paintComponent(Graphics g1d) {
        super.paintComponent(g1d)
        //println(this.name)
        RunTimer.getTimerAndStart("${this.name} GridPanel::paintComponent ").withCloseable {

            Graphics2D g = g1d as Graphics2D
            hints(g)
            g.getClipBounds(r)


            //
            // paint only elements inside clipBounds
            //

            //for (int x in 0..model.sizeX - 1) {
            GParsPool.withPool {
                ArrayList<Integer> l = getList(model.sizeX)
                l.each { int x ->
                    for (int y in 0..model.sizeY - 1) {
                        int graphX = borderWidth + x * gridWidth + nameWidth
                        int graphY = borderWidth + y * gridWidth
                        if (graphX >= r.x - 2 * gridWidth && graphX <= r.x + 2 * gridWidth + r.width && graphY >= r.y - 2 * gridWidth && graphY <= r.y + 2 * gridWidth + r.height) {
                            // TODO: println "x: $x (sizeX: $model.sizeX) y: $y (sizeY: $model.sizeY)"
                            if (model.sizeX == 0 || model.sizeY == 0) {
                                //t.stop()
                                return
                            }
                            GridElement e = model.getElement(x, y)

                            if (e != null && (e != GridElement.nullElement || cursorX == x && cursorY == y)) {
                                Color c = getColor(y)
                                g.setColor(c)
                                drawGridElement(c, g, e, x, y)
                            }
                        }
                    }
                }
            }


            int offset = (gridWidth / 20) as int // shadow and space
            int size = (int) ((gridWidth - offset) / 3) // size of shadow box and element box
            if (gridWidth < 15) {
                size = size * 2
            }
            int round = (size / 2) as int // corner diameter

            if (mouseX > 0) {
                int gridX = (int) ((mouseX - nameWidth - borderWidth) / gridWidth)
                //int gridY = (int)((mouseY-borderWidth)/gridHeigth)
                //println ("$gridX ")
                // shadow
                //g.setColor(nowBarShadowColor)
                //g.fillRoundRect(nowGraphX + offset, +offset, size - 4, nowGraphY + borderWidth - 4, round, round)
                // element in project color, integration phase color (orange), empty color (white)
                g.setColor(mouseColor)
                g.fillRoundRect((int) (borderWidth + nameWidth + gridX * gridWidth + gridWidth / 4), borderWidth, (int) (gridWidth / 2), gridWidth * model.sizeY, round, round)
            }


            //
            // paint the cursor-indicator line above everything else
            //

            if (cursorX >= 0) {
                int nowGraphX = borderWidth + cursorX * gridWidth + (int) ((gridWidth - size) / 2) + nameWidth
                int nowGraphY = borderWidth + model.sizeY * gridWidth // position end (right down)
                g.setColor(cursorColor)
                g.fillRoundRect(nowGraphX, 0, size - 4, nowGraphY + borderWidth - 4, round, round)

                nowGraphX = borderWidth + model.sizeX * gridWidth - 4
                nowGraphY = borderWidth + cursorY * gridWidth + (int) ((gridWidth - size) / 2)
                g.setColor(cursorColor)
                g.fillRoundRect(nameWidth, nowGraphY, nowGraphX, size - 4, round, round)
            }

            //
            // paint the now-indicator row above everything else
            //

            if (model.nowX >= 0 && model.nowX < model.sizeX) {
                int nowGraphX = borderWidth + model.nowX * gridWidth + (int) ((gridWidth - 4) / 4) + nameWidth
                // position start (left up)
                int nowGraphY = borderWidth + model.sizeY * gridWidth // position end (right down)

                // shadow
                //g.setColor(nowBarShadowColor)
                //g.fillRoundRect(nowGraphX + offset, +offset, size - 4, nowGraphY + borderWidth - 4, round, round)
                // element in project color, integration phase color (orange), empty color (white)
                g.setColor(nowBarColor)
                g.fillRoundRect(nowGraphX, 0, (int) ((gridWidth - 4) / 2), nowGraphY + borderWidth - 4, round, round)
            }

            //
            // draw the line names
            //
            if (gridWidth > 0) {
                int y = 0
                model.getLineNames()
                model.getLineNames().each { String projectName ->
                    int gridY = borderWidth + y * gridWidth
                    try {
                        if (hightlightLinePattern && projectName =~ hightlightLinePattern) {

                            g.setColor(cursorColor) //new Color(150,255,255,100))
                            g.fillRoundRect(borderWidth, (int) (gridY + gridWidth / 2) - 2, nameWidth + borderWidth + gridWidth * model.sizeX, 4, round, round)
                        } else {
                            g.setColor(Color.WHITE)
                        }
                    } catch (Exception e) {
                        //println "problem with regex: " + hightlightLinePattern
                    }
                    g.fillRoundRect(borderWidth, gridY, nameWidth - 4, gridWidth - 4, round, round)

                    if (gridWidth > 0) {
                        // write (with shadow) some info
                        float fontSize = gridWidth / 2
                        g.getClipBounds(rBackup)
                        Rectangle newClip = new Rectangle(borderWidth, gridY, nameWidth - 6, gridWidth - 6)
                        g.setClip(newClip.intersection(rBackup))
                        g.setFont(g.getFont().deriveFont((float) fontSize))
                        g.setColor(Color.WHITE)
                        g.drawString(projectName, borderWidth + (int) (gridWidth * 0.2), gridY + (int) (gridWidth * 2 / 3))
                        g.setColor(Color.BLACK)
                        g.drawString(projectName, borderWidth + (int) (gridWidth * 0.2) - 1, gridY + (int) (gridWidth * 2 / 3) - 1)
                        g.setClip(rBackup)
                    }

                    y++
                }

                g.drawImage(frameIcon, (int) borderWidth, (int) (borderWidth + y * gridWidth), nameWidth - 4, nameWidth - 4, null)

                //
                // draw the row names
                //

                int x = 0
                model.getColumnNames().each { String rowName ->
                    g.setColor(Color.WHITE)
                    int gridX = borderWidth + x * gridWidth + nameWidth
                    int gridY = borderWidth + (model.sizeY) * gridWidth
                    g.fillRoundRect(gridX, gridY, gridWidth - 4, nameWidth - 4, round, round)

                    if (gridWidth > 0) {
                        // write (with shadow) some info
                        float fontSize = gridWidth / 2
                        g.setFont(g.getFont().deriveFont((float) fontSize))
                        atBackup = g.getTransform()
                        //Resets transform to rotation
                        rotationTransform.setToRotation((double) Math.PI / 2)
                        translateTransform.setToTranslation(gridX, gridY)
                        //Chain the transforms (Note order matters)
                        totalTransform.setToIdentity()
                        totalTransform.concatenate(atBackup)
                        totalTransform.concatenate(translateTransform)
                        totalTransform.concatenate(rotationTransform)
                        //at.rotate((double)(Math.PI / 2))
                        g.setTransform(totalTransform)
                        g.setColor(Color.WHITE)

                        g.getClipBounds(rBackup)
                        Rectangle newClip = new Rectangle(0, -gridWidth, nameWidth - 6, gridWidth)
                        g.setClip(newClip.intersection(rBackup))

                        g.drawString(rowName, (int) (gridWidth * 0.2), 0 - (int) (gridWidth * 0.2))
                        g.setColor(Color.BLACK)
                        g.drawString(rowName, (int) (gridWidth * 0.2) - 1, 0 - (int) (gridWidth * 0.2) - 1)
                        g.setClip(rBackup)
                        g.setTransform(atBackup)
                    }
                    x++
                }
            }
        }
    }


    /**
     * @return size as dimonsion based on model-size
     */
    @Override
    Dimension getPreferredSize() {
        return new Dimension(nameWidth + model.sizeX*gridWidth + 2*borderWidth, nameWidth + model.sizeY*gridWidth + 2*borderWidth)
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

        int offset = (gridWidth/20) as int // shadow and space
        int size = gridWidth - offset // size of shadow box and element box
        int round = (size / 2) as int // corner diameter
        int graphX = borderWidth + x * gridWidth + nameWidth// position
        int graphY = borderWidth + y * gridWidth // position
        int gridMouseX = getGridXFromMouseX()
        int gridMouseY = getGridYFromMouseY()
        //println("gridMouseX=$gridMouseX gridMouseY=$gridMouseY")

        // shadow
        g.setColor(Color.LIGHT_GRAY)
        //g.fillRoundRect(graphX+offset, graphY+offset, size-4, size-4, round, round)

        // element in project color, integration phase color (orange), empty color (white)
        if( e != null) {
            g.setColor(e.integrationPhase ? Color.orange : (e == e.nullElement ? Color.WHITE : c))

            // or current mouse location color (overwrites even more)
            if (x == gridMouseX && y == gridMouseY) {
                //println "x $x, y $y (gmx $gridMouseX, gmy $gridMouseY)"
                g.setColor(new Color(255, 0, 0, 150))
            } //g.setColor(Color.LIGHT_GRAY)}
            g.fillRoundRect(graphX, graphY, size - 4, size - 4, round, round)
            if (e.deliveryDate) {
                g.setColor(Color.RED)
                g.fillRect(graphX + 4, graphY + 4, size - 8, size - 8,)
            }

            //g.fillRoundRect(graphX, graphY, size-4 , size-4, round, round)


            // or cursor color (overwrites everything)
            if (x == cursorX && y == cursorY) {
                g.setColor(cursorColor)
                g.fillRoundRect(graphX, graphY, size - 4, size - 4, round, round)
            }


            if (gridWidth > 1000) {
                // write (with shadow) some info
                // TODO: use Clip, Transform, Paint, Font and Composite
                float fontSize = 20.0 * gridWidth / 60

                g.getClipBounds(rBackup)
                Rectangle newClip = new Rectangle(graphX, graphY, size - 6, size - 6)
                g.setClip(newClip.intersection(rBackup))
                g.setFont(g.getFont().deriveFont((float) fontSize))
                g.setColor(Color.WHITE)
                g.drawString(e.project, graphX + (int) (gridWidth * 0.2), graphY + (int) (gridWidth / 2))
                g.setColor(Color.BLACK)
                g.drawString(e.project, graphX + (int) (gridWidth * 0.2 - 1), graphY + (int) (gridWidth / 2 - 1))
                g.setClip(rBackup)
            }
        }
    }

    Rectangle rBackup = new Rectangle()


    /**
     * @param mouseY (optional - else take this.mouseY as base for calc)
     * @return gridY - based on mouse pos
     */
    int getGridYFromMouseY(int mouseY = -1) {
        mouseY = mouseY < 0 ? this.mouseY : mouseY // take this.mouseY, if default-Param, else take param
        int gridY = ((mouseY - borderWidth) / gridWidth) as int
        gridY
    }

    /**
     * @param mouseX (optional - else take this.mouseX as base for calc)
     * @return gridX - based on mouse pos
     */
    int getGridXFromMouseX(int mouseX = -1) {
        mouseX = mouseX < 0 ? this.mouseX : mouseX // take this.mouseY, if default-Param, else take param
        int gridX = ((mouseX - borderWidth- nameWidth) / gridWidth) as int
        gridX
    }

}

