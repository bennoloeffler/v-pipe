package gui

import groovy.transform.CompileStatic
import transform.YellowRedLimit
import utils.RunTimer

import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ToolTipManager
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener

import static extensions.DateHelperFunctions._getWeekYearStr

@CompileStatic
class LoadGridPanel extends JPanel implements MouseMotionListener{

    Color nowBarColor = new Color(255, 0, 0, 60)
    Color nowBarShadowColor = new Color(50, 50, 50, 30)


    int borderWidth
    int gridHeigth
    int gridWidth

    int nameWidth

    int nowXRowCache

    /**
     * THIS IS A PERCENTAGE - not the absolut load!
     */
    Map<String, Map<String, Double>> depTimeLoadMap // compared to maxRed, if capaAvailable
    List<String> allTimeKeys
    Map<String, Map<String, YellowRedLimit>> capaAvailable
    Map<String, Double> maxRed


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
        //mouseX = e.getX()
        //mouseY = e.getY()
        MouseEvent phantom = new MouseEvent(
                this,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                e.getX(),
                e.getY(),
                0,
                false)

        ToolTipManager.sharedInstance().mouseMoved(phantom)
    }

    @Override
    String getToolTipText(MouseEvent event) {
        String html = null

        def gridX = getGridXFromMouseX(event.x)
        def gridY = getGridYFromMouseY(event.y)
        if (gridX >= 0 && gridX < allTimeKeys.size() && gridY >= 0 && gridY < depTimeLoadMap.size()) {
            String timeKey = allTimeKeys[gridX]
            String dep = depTimeLoadMap.keySet()[gridY]
            Double load = depTimeLoadMap[dep][timeKey]
            Double red = capaAvailable[dep][timeKey].red
            Double yellow = capaAvailable[dep][timeKey].yellow


            // <br/>$element.fromToDateString
            html =
                    "<html><p style=\"background-color:white;\"><font color=\"#808080\" " +
                            "size=\"20\" face=\"Verdana\">$dep<br/>$timeKey<br/>Last: ${String.format('%.1f',load)}<br/>"+
                            "gelb bei: ${String.format('%.1f',yellow)}<br/>rot bei: ${String.format('%.1f',red)}" + // SIZE DOES NOT WORK!
                            "</font></p></html>"
        }
        return html
    }

    JScrollPane getScrollPane() {
        getParent()?.getParent() as JScrollPane
    }

    void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth
        gridHeigth = gridWidth * 3
        borderWidth = (int)(gridWidth / 3)
        scrollPane?.getVerticalScrollBar()?.setUnitIncrement((gridHeigth/6) as int)
        nameWidth = gridHeigth
        invalidate()
        repaint()
    }

    void setModelData(Map<String, Map<String, Double>> depTimeLoadMap, List<String> allTimeKeys, Map<String, Map<String, YellowRedLimit>> capaAvailable ) {
        this.depTimeLoadMap = depTimeLoadMap
        this.allTimeKeys = allTimeKeys
        this.capaAvailable = capaAvailable

        assert this.depTimeLoadMap != null
        assert this.allTimeKeys != null
        assert this.capaAvailable != null

        normalizeTo100Percent()
        setNow()
        invalidate()
        repaint()
    }

    LoadGridPanel(int gridWidth) {
        //super()
        //Map<String, Map<String, Double>> depTimeLoadMap=[:], List<String> allTimeKeys=[], Map<String, Map<String, YellowRedLimit>> capaAvailable = [:]
        setModelData([:], [], [:])
        setGridWidth(gridWidth)
        addMouseMotionListener(this)
    }

    def setNow() {
        Date now = Date.newInstance()
        int row = 0
        String nowStr = _getWeekYearStr(now)
        for (weekStr in allTimeKeys) {
            if(weekStr == nowStr) {
                nowXRowCache = row
                return
            }
            row++
        }
        // TODO: out of range = 0...
    }

    def normalizeTo100Percent() {
        maxRed = [:]
        if (capaAvailable) {
            depTimeLoadMap.each {
            String dep = it.key
            Map<String, Double> load = it.value
                Double maxVal = load.max {
                    it.value
                }.value
                maxRed[dep] = maxVal
                load.each { String timeStamp, Double capa ->
                    //Double avail = capaAvailable[dep][timeStamp].yellow
                    load[timeStamp] = (Double) (capa)
                }
            }
        } else {
            depTimeLoadMap.each {
                //String dep = it.key
                Map<String, Double> load = it.value
                Double maxVal = load.max {
                    it.value
                }.value
                load.each { String timeStamp, Double capa ->
                    load[timeStamp] = (Double) (capa / maxVal)
                }
            }
        }
        //println depTimeLoadMap
    }


    /**
     * @param g1d
     */
    @Override
    protected void paintComponent(Graphics g1d) {

        super.paintComponent(g1d)
        Graphics2D g = g1d as Graphics2D
        Rectangle r = g.getClipBounds()

        RunTimer t = new RunTimer(true)

        //
        // paint elements
        //
        int y = 0
        depTimeLoadMap.keySet().each { String department ->
            int x = 0
            allTimeKeys.each { String timeStr ->
                int gridX = nameWidth + borderWidth + x*gridWidth
                int gridY = borderWidth + y*gridHeigth
                Double val = depTimeLoadMap[department][timeStr]?:0
                if(capaAvailable) {
                    Double max = maxRed[department]
                    Double yellow = capaAvailable[department][timeStr].yellow
                    Double red = capaAvailable[department][timeStr].red
                    drawGridElementYelloRed(g, val, max, yellow, red, gridX, gridY)
                } else {
                    //println("b: $borderWidth w: $gridWidth h: $gridHeigth")
                    //println("x: $gridX y: $gridY")
                    drawGridElement(g, val, gridX, gridY)
                }
                x++
            }
            y++
        }

        //
        // paint the now-indicator row above everything else
        //

        int offset = (gridWidth/20) as int // shadow and space
        int size = (int)((gridWidth - offset)/3 ) // size of shadow box and element box
        if(gridWidth<15) {size=size*2}
        int round = (size / 2) as int // corner diameter
        int nowGraphX = borderWidth + nowXRowCache * gridWidth + (int)((gridWidth - size)/2) + nameWidth// position start (left up)
        int nowGraphY = borderWidth + depTimeLoadMap.size() * gridHeigth // position end (right down)

        // shadow
        g.setColor(nowBarShadowColor)
        g.fillRoundRect(nowGraphX+offset, +offset, size-4, nowGraphY+borderWidth-4, round, round)
        // element in project color, integration phase color (orange), empty color (white)
        g.setColor( nowBarColor )
        g.fillRoundRect(nowGraphX, 0, size-4 , nowGraphY+borderWidth-4, round, round)



        //
        // draw the department names
        //

        y = 0
        depTimeLoadMap.keySet().each { String department ->
            g.setColor(Color.LIGHT_GRAY)
            int gridY = borderWidth + y*gridHeigth
            g.fillRoundRect(borderWidth , gridY, nameWidth-4, gridHeigth - 4 , round*3, round*3)

            if(gridWidth>20) {
                // write (with shadow) some info
                // TODO: use Clip, Transform, Paint, Font and Composite
                float fontSize = gridWidth / 2
                g.getClipBounds(rBackup)
                g.setClip(borderWidth , gridY, nameWidth-6, gridHeigth - 6)
                g.setFont(g.getFont().deriveFont((float) fontSize))
                g.setColor(Color.WHITE)
                g.drawString(department, borderWidth + (int) (gridWidth * 0.2), gridY + (int) (gridWidth * 2/3))
                g.setColor(Color.BLACK)
                g.drawString(department, borderWidth + (int) (gridWidth * 0.2)-2, gridY + (int) (gridWidth * 2/3)-2)
                g.setClip(rBackup)
            }

            y++
        }

    }

    Rectangle rBackup = new Rectangle()



    /**
     * @return size as dimonsion based on model-size
     */
    @Override
    Dimension getPreferredSize() {
        int y = depTimeLoadMap.size()
        int x = allTimeKeys.size()
        //println("$x $y")
        return new Dimension(nameWidth + 2*borderWidth + gridWidth * x ,2 * borderWidth + gridHeigth * y) //model.sizeX*grid + 2*borderWidth, model.sizeY*grid + 2*borderWidth)
    }

    @CompileStatic
    def drawGridElementYelloRed(Graphics2D g,  Double val, Double max, Double yellow, Double red, int x, int y) {

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
        if (max < yellow) {max = yellow}

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

        g.fillRoundRect(x, y+percentShift, sizeX-4 , (int)(percent * (sizeY-4)), round, round)

        Double percentRed = (Double)(red / max)
        Double percentYellow = (Double)(yellow / max)

        percentShift = (int)((sizeY-4) - percentRed * (sizeY-4))
        g.setColor(Color.RED)
        //g.drawLine(x, y+percentShift, x - (int)(sizeX/2)+sizeX-4, y+percentShift)
        g.drawLine(x, y+percentShift, x +sizeX-4, y+percentShift)

        g.setColor(getBackground())
        //g.drawLine(x, y+percentShift, x - (int)(sizeX/2)+sizeX-4, y+percentShift)
        g.drawLine(x, y+percentShift+1, x +sizeX-4, y+percentShift+1)

        percentShift = (int)((sizeY-4) - percentYellow * (sizeY-4))
        g.setColor(Color.ORANGE)
        //g.drawLine(x+(int)(sizeX/2), y+percentShift, x+sizeX-4, y+percentShift)
        g.drawLine(x, y+percentShift, x+sizeX-4, y+percentShift)

        //
        // write percantage
        //
        String p = String.format('%.0f', val/yellow*100) +"%"

        // write (with shadow) some info
        float fontSize = 20.0 * gridWidth/60
        g.getClipBounds(rBackup)
        g.setClip(x, y, sizeX-8 , sizeY-8)
        g.setFont (g.getFont().deriveFont((float)fontSize) )
        g.setColor(Color.WHITE)
        g.drawString(p, x+4, y + gridHeigth - (int)fontSize)
        g.setColor(Color.BLACK)
        g.drawString(p, x+4-2, y + gridHeigth - (int)fontSize-2)
        g.setClip(rBackup)


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
    def drawGridElement(Graphics2D g, Double percent, int x, int y) {

        int offset = (gridWidth/20) as int // shadow and space
        int sizeX = gridWidth - offset // size of shadow box and element box
        int sizeY = gridHeigth - offset // size of shadow box and element box
        int round = (sizeX / 2) as int // corner diameter
        //int graphX = borderWidth + x * gridWidth // position
        //int graphY = borderWidth + y * gridHeigth // position
        //int gridMouseX = getGridXFromMouseX()
        //int gridMouseY = getGridYFromMouseY()
        //println("gridMouseX=$gridMouseX gridMouseY=$gridMouseY")

        //println(percent)
        int percentShift = (int)((sizeY-4) - percent * (sizeY-4))
        //int percentShift = sizeY - percentageGrid

        // shadow
        g.setColor(Color.LIGHT_GRAY)
        g.fillRoundRect(x+offset, y+percentShift+offset, sizeX-4, (int)(percent * (sizeY-4)), round, round)

        // element in project color, integration phase color (orange), empty color (white)
        g.setColor(Color.GRAY)

        // or current mouse location color (overwrites even more)
        //if(x==gridMouseX && y == gridMouseY) {
        //    g.setColor(makeTransparent(g.getColor(), 120))
        //} //g.setColor(Color.LIGHT_GRAY)}

        g.fillRoundRect(x, y+percentShift, sizeX-4 , (int)(percent * (sizeY-4)), round, round)

        // or cursor color (overwrites everything)

    }

    //Rectangle rBackup = new Rectangle()

}
