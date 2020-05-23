package gui

import groovy.transform.CompileStatic
import utils.RunTimer

import javax.swing.JPanel
import javax.swing.JScrollPane
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle

import static extensions.DateHelperFunctions._getWeekYearStr

@CompileStatic
class LoadGridPanel extends JPanel{

    Color nowBarColor = new Color(255, 0, 0, 60)
    Color nowBarShadowColor = new Color(50, 50, 50, 30)


    int borderWidth
    int gridHeigth
    int gridWidth

    int nowXRowCache

    /**
     * THIS IS A PERCENTAGE - not the absolut load!
     */
    Map<String, Map<String, Double>> depTimeLoadMap
    List<String> allTimeKeys

    JScrollPane getScrollPane() {
        getParent()?.getParent() as JScrollPane
    }

    void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth
        gridHeigth = gridWidth * 3
        borderWidth = (int)(gridWidth / 3)
        scrollPane?.getVerticalScrollBar()?.setUnitIncrement((gridHeigth/6) as int)

        invalidate()
        repaint()
    }

    void setModelData(Map<String, Map<String, Double>> depTimeLoadMap, List<String> allTimeKeys) {
        this.depTimeLoadMap = depTimeLoadMap
        this.allTimeKeys = allTimeKeys
        normalizeTo100Percent()
        setNow()
        invalidate()
        repaint()
    }

    LoadGridPanel(int gridWidth, Map<String, Map<String, Double>> depTimeLoadMap=[:], List<String> allTimeKeys=[]) {
        //super()
        setModelData(depTimeLoadMap, allTimeKeys)
        setGridWidth(gridWidth)
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
    }

    def normalizeTo100Percent() {

        depTimeLoadMap.each {
            //String dep = it.key
            Map<String, Double> load = it.value
            Double maxVal = load.max {
                it.value
            }.value
            load.each {String timeStamp, Double capa ->
                load[timeStamp] = (Double) (capa / maxVal)
            }
        }
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
                int gridX = borderWidth + x*gridWidth
                int gridY = borderWidth + y*gridHeigth
                Double val = depTimeLoadMap[department][timeStr]?:0
                //println("b: $borderWidth w: $gridWidth h: $gridHeigth")
                //println("x: $gridX y: $gridY")
                drawGridElement(g, val, gridX, gridY)
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
        int nowGraphX = borderWidth + nowXRowCache * gridWidth + (int)((gridWidth - size)/2)// position start (left up)
        int nowGraphY = borderWidth + depTimeLoadMap.size() * gridHeigth // position end (right down)

        // shadow
        g.setColor(nowBarShadowColor)
        g.fillRoundRect(nowGraphX+offset, +offset, size-4, nowGraphY+borderWidth-4, round, round)
        // element in project color, integration phase color (orange), empty color (white)
        g.setColor( nowBarColor )
        g.fillRoundRect(nowGraphX, 0, size-4 , nowGraphY+borderWidth-4, round, round)


        //
        // paint only elements inside clipBounds
        //
        /*
        int y = 0
        depTimeLoadMap.entrySet().each { def mapEntry  ->
            Map<String, Double> loads = mapEntry.value // example: Map of key: 2020-W05 value: 2.0
            int x = 0
            loads.each {def timeLoadEntry ->
                int gridX = borderWidth + x*gridWidth
                int gridY = borderWidth + y*gridHeigth
                drawGridElement(g, timeLoadEntry.value, gridX, gridY)
                x++
            }
            y++
        }
        t.stop("drawing all")
        */
    }


    /**
     * @return size as dimonsion based on model-size
     */
    @Override
    Dimension getPreferredSize() {
        int y = depTimeLoadMap.size()
        int x = allTimeKeys.size()
        //println("$x $y")
        return new Dimension(2*borderWidth + gridWidth * x ,2 * borderWidth + gridHeigth * y) //model.sizeX*grid + 2*borderWidth, model.sizeY*grid + 2*borderWidth)
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

/*
        // write (with shadow) some info
        // TODO: use Clip, Transform, Paint, Font and Composite
        float fontSize = 20.0 * grid/60
        g.getClipBounds(rBackup)
        g.setClip(graphX, graphY, size-6, size-6)
        g.setFont (g.getFont().deriveFont((float)fontSize) )
        g.setColor(Color.WHITE)
        g.drawString(e.project, graphX + (int)(grid*0.2),     graphY + (int)(grid/2))
        g.setColor(Color.BLACK)
        g.drawString(e.project, graphX + (int)(grid*0.2 - 2), graphY + (int)(grid/2 - 2))
        g.setClip(rBackup)
*/
    }

    //Rectangle rBackup = new Rectangle()

}
