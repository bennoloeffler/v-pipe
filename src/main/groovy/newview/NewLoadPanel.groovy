package newview

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import utils.RunTimer

import javax.swing.JPanel
import javax.swing.JToolTip
import javax.swing.ToolTipManager
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.beans.PropertyChangeEvent

@CompileStatic
class NewLoadPanel  extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener, FocusListener, PanelBasics {

    //Color nowBarShadowColor = new Color(50, 50, 50, 30)

    @Bindable int gridWidth

    @Bindable int cursorX = -1
    //int gridHeigth
    //int nameWidth

    @Bindable boolean detailsToolTip = true

    int mouseX
    int mouseY

    AbstractGridLoadModel model

    Closure updateCallback = {
        invalidateAndRepaint(this)
        scrollToCursorX()
    }

    Closure updateFromGridWidthCallback = { PropertyChangeEvent e ->
        updateOthersFromGridWidth(gridWidth, this)
        invalidateAndRepaint(this)
    }

    NewLoadPanel(int gridWidth, AbstractGridLoadModel model) {
        setFocusable(true)
        this.model = model
        setGridWidth(gridWidth)
        updateOthersFromGridWidth(gridWidth, this)

        this.model.addPropertyChangeListener('updateToggle', updateCallback) // when updateToggle is changed
        this.addPropertyChangeListener('gridWidth', updateFromGridWidthCallback) // when gridWidth is changed
        this.addPropertyChangeListener('cursorX', updateCallback)
        addMouseMotionListener(this)
        addMouseWheelListener(this)
        addMouseListener(this)
        addKeyListener(this)
        addFocusListener(this)
    }

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
            int rot = e.getWheelRotation()
            int inc = (int)(gridWidth * rot / 10)
            setGridWidth(gridWidth + (inc!=0?inc:1)) // min one tic bigger
            minMaxGridCheck()
            updateOthersFromGridWidth(gridWidth, this)

            invalidateAndRepaint(this)
        } else {
            getScrollPane(this)?.getVerticalScrollBar()?.setUnitIncrement((gridWidth/3) as int)
            getScrollPane(this)?.processMouseWheelEvent(e)
        }
    }


    def minMaxGridCheck() {
        if(gridWidth > 100){setGridWidth(100)}
        if(gridWidth < 10){setGridWidth(10)}
    }

    //
    // MouseListener
    //

    @Override
    void mouseClicked(MouseEvent e) {
        requestFocusInWindow()
        setCursorX(getGridXFromMouseX(e.getX())) // @bindable...
    }

    @Override
    void mousePressed(MouseEvent e) {}

    @Override
    void mouseReleased(MouseEvent e) {}

    @Override @CompileStatic(TypeCheckingMode.SKIP)
    void mouseEntered(MouseEvent e) {
        requestFocusInWindow()
        ToolTipManager.sharedInstance().hideTipWindow()
    }

    @Override
    void mouseExited(MouseEvent e) {}


    @Override
    void mouseMoved(MouseEvent e) {
        mouseMoved(e, this)
        mouseX = e.x
        mouseY = e.y
        invalidateAndRepaint(this)
    }


    //
    // KeyListener
    //

    @Override
    void keyTyped(KeyEvent e) {}

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    void keyPressed(KeyEvent e){

        if(KeyEvent.VK_D == e.getKeyCode()) {
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

        if(KeyEvent.VK_O == e.getKeyCode()) {
            //openFile()
        }

        if(KeyEvent.VK_L == e.getKeyCode()) {
            //VpipeGui.openLoad()
        }

        if(KeyEvent.VK_PLUS == e.getKeyCode()) {
            setGridWidth((gridWidth * 1.1) as int)
            minMaxGridCheck()
            updateOthersFromGridWidth(gridWidth, this)

        }

        if(KeyEvent.VK_MINUS == e.getKeyCode()) {
            setGridWidth((gridWidth / 1.1) as int)
            minMaxGridCheck()
            updateOthersFromGridWidth(gridWidth, this)
        }

        if(KeyEvent.VK_LEFT == e.getKeyCode())  {cursorX > 0              ? setCursorX(cursorX-1) :0}
        if(KeyEvent.VK_RIGHT == e.getKeyCode()) {cursorX < model.sizeX-1  ? setCursorX(cursorX+1) :0}


        scrollToCursorX()
        invalidateAndRepaint(this)

    }



    @Override
    void keyReleased(KeyEvent e) {}


    /**
     * @return gridY - based on mouse pos
     */
    int getGridYFromMouseY(int mouseY) {
        int gridY = ((mouseY - borderWidth) / gridHeigth) as int
        gridY
    }

    /**
     * @return gridX - based on mouse pos
     */
    int getGridXFromMouseX(int mouseX) {
        int gridX = ((mouseX - borderWidth- nameWidth) / gridWidth) as int
        gridX
    }

    void scrollToCursorX() {
        scrollRectToVisible(new Rectangle(nameWidth + cursorX * gridWidth - gridWidth, (int)(getVisibleRect().getY()), 3 * gridWidth, 3 * gridWidth))
    }

    /**
     * @param g1d
     */
    @Override
    @CompileStatic
    protected void paintComponent(Graphics g1d) {

        super.paintComponent(g1d)
        def t = RunTimer.getTimerAndStart("${this.name}NewLoadPanel::paintComponent")

        Graphics2D g = g1d as Graphics2D
        Rectangle r = g.getClipBounds()
        hints(g)


        //
        // paint elements
        //

        //println("m.sizeX: $model.sizeX, m.sizeY: $model.sizeY")
        for(int x = 0; x < model.sizeX; x++ ) {
            for (int y = 0; y < model.sizeY; y++ ) {
                int gridX = nameWidth + borderWidth + x*gridWidth
                int gridY = borderWidth + y*gridHeigth
                GridLoadElement element = model.getElement(x, y)
                //if( element.red == -1 ) {
                //    drawGridElement(g, element.load, gridX, gridY)
                //} else {
                    drawGridElementYelloRed(g,
                            element.load,
                            element.loadProject,
                            model.getMax(y),
                            element.yellow,
                            element.red,
                            gridX,
                            gridY)
                //}
            }
        }


        int offset = (gridWidth / 20) as int // shadow and space
        int size = (int) ((gridWidth - offset) / 3) // size of shadow box and element box
        if (gridWidth < 15) {
            size = size * 2
        }
        int round = (size / 2) as int // corner diameter

        //
        // paint the now-indicator row above everything else
        //

        if(model.nowX >= 0) {

            int nowGraphX = borderWidth + model.getNowX() * gridWidth + (int) ((gridWidth - size) / 2) + nameWidth
            // position start (left up)
            int nowGraphY = borderWidth + model.getSizeY() * gridHeigth // position end (right down)

            // shadow
            //g.setColor(nowBarShadowColor)
            //g.fillRoundRect(nowGraphX + offset, +offset, size - 4, nowGraphY + borderWidth - 4, round, round)
            // element in project color, integration phase color (orange), empty color (white)
            g.setColor(nowBarColor)
            g.fillRoundRect(nowGraphX, 0, size - 4, nowGraphY + borderWidth - 4, round, round)
        }

        //
        // paint the cursor-indicator line above everything else
        //

        if(cursorX >=0 ) {
            int nowGraphX = borderWidth + cursorX * gridWidth + (int) ((gridWidth - size) / 2) + nameWidth
            // position start (left up)
            int nowGraphY = borderWidth + model.getSizeY() * gridHeigth // position end (right down)

            // shadow
            //g.setColor(nowBarShadowColor)
            //g.fillRoundRect(nowGraphX + offset, +offset, size - 4, nowGraphY + borderWidth - 4, round, round)
            // element in project color, integration phase color (orange), empty color (white)
            g.setColor(cursorColor)
            g.fillRoundRect(nowGraphX, 0, size - 4, nowGraphY + borderWidth - 4, round, round)
        }

        if(mouseX>0) {
            int gridX = (int)((mouseX-nameWidth-borderWidth)/gridWidth)
            int gridY = (int)((mouseY-borderWidth)/gridHeigth)
            //println ("$gridX $gridY")
                    // shadow
            //g.setColor(nowBarShadowColor)
            //g.fillRoundRect(nowGraphX + offset, +offset, size - 4, nowGraphY + borderWidth - 4, round, round)
            // element in project color, integration phase color (orange), empty color (white)
            g.setColor(mouseColor)
            g.fillRoundRect((int)(borderWidth+nameWidth+gridX*gridWidth+gridWidth/4), borderWidth+gridY*gridHeigth,  (int)(gridWidth/2), gridHeigth,  round, round)
        }

        //
        // draw the department names
        //

        int y = 0
        model.getYNames().each { String yNames ->
            g.setColor(Color.WHITE)
            int gridY = borderWidth + y * gridHeigth
            g.fillRoundRect(borderWidth , gridY, nameWidth-4, gridHeigth - 4 , round*3, round*3)

            //if(gridWidth>20) {
                float fontSize = gridWidth / 2
                g.getClipBounds(rBackup)
                g.setClip(borderWidth , gridY, nameWidth-6, gridHeigth - 6)
                // TODO: intersect
                g.setFont(g.getFont().deriveFont((float) fontSize))
                g.setColor(Color.WHITE)
                g.drawString(yNames, borderWidth + (int) (gridWidth * 0.2), gridY + (int) (gridWidth * 2/3))
                g.setColor(Color.BLACK)
                g.drawString(yNames, borderWidth + (int) (gridWidth * 0.2)-2, gridY + (int) (gridWidth * 2/3)-2)
                g.setClip(rBackup)
            //}
            y++
        }
        t.stop()

    }

    Rectangle rBackup = new Rectangle()



    /**
     * @return size as dimonsion based on model-size
     */
    @Override
    Dimension getPreferredSize() {
        int y = model.sizeY
        int x = model.sizeX
        //println("$x $y")
        return new Dimension(nameWidth + 2*borderWidth + gridWidth * x ,2 * borderWidth + gridHeigth * y) //model.sizeX*grid + 2*borderWidth, model.sizeY*grid + 2*borderWidth)
    }

    @CompileStatic
    def drawGridElementYelloRed(Graphics2D g,  Double val, Double valProject, Double max, Double yellow, Double red, int x, int y) {

        int offset = (gridWidth/20) as int // shadow and space
        int sizeX = gridWidth - offset // size of shadow box and element box
        int sizeY = gridHeigth - offset // size of shadow box and element box
        int round = (sizeX / 2) as int // corner diameter
        //int graphX = borderWidth + x * gridWidth // position
        //int graphY = borderWidth + y * gridHeigth // position
        //int gridMouseX = getGridXFromMouseX()
        //int gridMouseY = getGridYFromMouseY()
        //println("gridMouseX=$gridMouseX gridMouseY=$gridMouseY")


        if (max < red) {max = red}
        //if (max < yellow) {max = yellow}

        Double percent = (Double)(val / max)

        //println(percent)
        int percentShift = (int)((sizeY-4) - percent * (sizeY-4))
        //int percentShift = sizeY - percentageGrid

        // shadow
        g.setColor(Color.LIGHT_GRAY)
        g.fillRoundRect(x+offset, y+percentShift+offset, sizeX-4, (int)(percent * (sizeY-4)), round, round)

        //
        // color and bar accordig to load...
        //
        //
        g.setColor(Color.GRAY)
        if(val <= yellow) {g.setColor(Color.GREEN)}
        if(val > yellow && val <= red) {g.setColor(Color.YELLOW)}
        if(val > red) {g.setColor(Color.RED)}
        if(red == -1) {g.setColor(Color.GRAY)}

        g.fillRoundRect(x, y+percentShift, sizeX-4 , (int)(percent * (sizeY-4)), round, round)



        //
        // project caused load
        //
        Double percentProject = (Double)(valProject / max)
        int percentShiftProject = (int)((sizeY-4) - percentProject * (sizeY-4))

        g.setColor(cursorColor)
        g.fillRoundRect(x+(int)(sizeX*0.2), y+percentShiftProject, sizeX-4 - (int)(sizeX*0.4) , (int)(percentProject * (sizeY-4)), round, round)



        //
        // draw max-line of yellow and read
        //

        if(red >= 0) {
            Double percentRed = (Double) (red / max)
            Double percentYellow = (Double) (yellow / max)

            percentShift = (int) ((sizeY - 4) - percentRed * (sizeY - 4))
            g.setColor(Color.RED)
            //g.drawLine(x, y+percentShift, x - (int)(sizeX/2)+sizeX-4, y+percentShift)
            g.drawLine(x, y + percentShift, x + sizeX - 4, y + percentShift)

            g.setColor(getBackground())
            //g.drawLine(x, y+percentShift, x - (int)(sizeX/2)+sizeX-4, y+percentShift)
            g.drawLine(x, y + percentShift + 1, x + sizeX - 4, y + percentShift + 1)

            percentShift = (int) ((sizeY - 4) - percentYellow * (sizeY - 4))
            g.setColor(Color.ORANGE)
            //g.drawLine(x+(int)(sizeX/2), y+percentShift, x+sizeX-4, y+percentShift)
            g.drawLine(x, y + percentShift, x + sizeX - 4, y + percentShift)

            //
            // write percentage
            //
            String p = String.format('%.0f', val / yellow * 100) + "%"

            // write (with shadow) some info
            float fontSize = 20.0 * gridWidth / 60
            //g.getClipBounds(rBackup)
            //g.setClip(x, y, sizeX-8 , sizeY-8)
            g.setFont(g.getFont().deriveFont((float) fontSize))
            if(val > red) {g.setColor(Color.BLACK)}else{g.setColor(Color.WHITE)}
            g.drawString(p, x + 4, y + gridHeigth - (int) fontSize)
            if(val > red) {g.setColor(Color.WHITE)}else{g.setColor(Color.BLACK)}
            //g.setColor(Color.BLACK)
            g.drawString(p, x + 4 - 1, y + gridHeigth - (int) fontSize - 1)
            //g.setClip(rBackup)
        }

    }




    @Override
    String getToolTipText(MouseEvent event) {
        if(!detailsToolTip) return null

        String html = null

        def gridX = getGridXFromMouseX(event.x)
        def gridY = getGridYFromMouseY(event.y)

        if(event.x <= borderWidth + nameWidth) {
            return model.getYNames()[gridY]
        }
        if(event.y > borderWidth + gridHeigth*(model.sizeY)) {
            return model.getXNames()[gridX]
        }
        if(event.x > borderWidth + nameWidth + gridWidth*(model.sizeX)) {
            return model.getYNames()[gridY]
        }

        if (gridX >= 0 && gridX < model.sizeX && gridY >= 0 && gridY < model.sizeY) {
            def element = model.getElement(gridX, gridY)
            if (element.load == 0) {
                return "0% ${model.getYNames()[gridY]}"
            }
            def department = model.getYNames()[gridY]
            def timeStr = model.getXNames()[gridX]
            def yellowRed = element.yellow >=0 ? "($element.yellow, $element.red)":""
            def yellow = element.yellow >= 0 ? "$element.yellow" : "keine Daten"
            def red = element.red >= 0 ? "$element.red" : "keine Daten"
            def percentTotal = element.yellow > 0 ? ((double)(element.load / element.yellow)*100).round(1) : -1
            def percentRed = element.yellow > 0 ? ((double)(element.red / element.yellow)*100).round(1) : -1
            def percentStr = percentTotal >=0 ? "$percentTotal% Rot bei: $percentRed%<br/>": ""
            // <br/>$element.fromToDateString
            //html =  "Gesamtbelastung: $element.load\nGelb: $element.percentageYellow, Rot: $element.percentageYellow)\nGewähltes Projekt:$element.loadProject"
            html  =      """<html><head><style>
                                h1 { color: #808080; font-family: verdana; font-size: 120%; }
                                p  { color: black; font-family: courier; font-size: 120%; } </style> </head>
                                
                                <body>
                                    <h1>${percentTotal>=0?percentTotal.round(1)+ '% ':''}${element.load.round(1)} $yellowRed $department $timeStr</h1>
                                    <p>
                                        Gesamtbelastung: ${element.load.round(1)}<br/>
                                        Gelb: $yellow, Rot: $red<br/>
                                        $percentStr
                                        Gewähltes Projekt: ${element.loadProject.round(1)}<br/>
                                        Details: <br/> ${(element.projectDetails.collect { it.projectCapaNeed.round(1) + " : " + it.originalTask.toString() + "<br/>" } as List<String>).join('')}
                                    </p>
                                </body>
                             </html>                                
                         """
        }
        return html
    }

    @Override
    Point getToolTipLocation(MouseEvent event) {
        if(!detailsToolTip) return null
        def gridX = (int)((event.x-borderWidth-nameWidth)/gridWidth)
        def locX = gridX*gridWidth + borderWidth + nameWidth + 2*gridWidth
        def gridY = (int)((event.y-borderWidth)/gridHeigth)
        def locY = gridY*gridHeigth + borderWidth
        if(gridY > borderWidth + (gridHeigth*(model.sizeY-1))) {
            return null
        }
        if(gridX > borderWidth + nameWidth+ (gridWidth*(model.sizeX-1))) {
            return null
        }

        return new Point(locX, locY)
    }

}
