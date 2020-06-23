package newview

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import utils.RunTimer

import javax.swing.JPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import java.beans.PropertyChangeEvent

@CompileStatic
class NewLoadPanel  extends JPanel implements MouseMotionListener, PanelBasics {

    //Color nowBarShadowColor = new Color(50, 50, 50, 30)

    @Bindable int gridWidth

    //int gridHeigth
    //int nameWidth

    AbstractGridLoadModel model

    Closure updateFromModelCallback = {
        invalidateAndRepaint(this)
    }

    Closure updateFromGridWidthCallback = { PropertyChangeEvent e ->
        updateOthersFromGridWidth(gridWidth, this)
        invalidateAndRepaint(this)
    }

    NewLoadPanel(int gridWidth, AbstractGridLoadModel model) {
        this.model = model
        setGridWidth(gridWidth)
        updateOthersFromGridWidth(gridWidth, this)

        this.model.addPropertyChangeListener('updateToggle', updateFromModelCallback) // when updateToggle is changed
        this.addPropertyChangeListener('gridWidth', updateFromGridWidthCallback) // when gridWidth is changed
        addMouseMotionListener(this)
    }







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


    /**
     * @param g1d
     */
    @Override
    protected void paintComponent(Graphics g1d) {

        super.paintComponent(g1d)
        def t = RunTimer.getTimerAndStart('NewLoadPanel::paintComponent')

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
        // draw the department names
        //

        int y = 0
        model.getYNames().each { String yNames ->
            g.setColor(Color.LIGHT_GRAY)
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

        g.setColor(Color.BLUE)
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
            // write percantage
            //
            String p = String.format('%.0f', val / yellow * 100) + "%"

            // write (with shadow) some info
            float fontSize = 20.0 * gridWidth / 60
            //g.getClipBounds(rBackup)
            //g.setClip(x, y, sizeX-8 , sizeY-8)
            g.setFont(g.getFont().deriveFont((float) fontSize))
            g.setColor(Color.WHITE)
            g.drawString(p, x + 4, y + gridHeigth - (int) fontSize)
            g.setColor(Color.BLACK)
            g.drawString(p, x + 4 - 1, y + gridHeigth - (int) fontSize - 1)
            //g.setClip(rBackup)
        }

    }




    @Override
    String getToolTipText(MouseEvent event) {
        String html = null

        def gridX = getGridXFromMouseX(event.x)
        def gridY = getGridYFromMouseY(event.y)
        if (gridX >= 0 && gridX < model.sizeX && gridY >= 0 && gridY < model.sizeY) {
            def element = model.getElement(gridX, gridY)
            if (element.load == 0) {
                return null
            }

            def yellowRed = element.yellow >=0 ? "($element.yellow, $element.red)":""
            def yellow = element.yellow >= 0 ? "$element.yellow" : "keine Daten"
            def red = element.red >= 0 ? "$element.red" : "keine Daten"
            // <br/>$element.fromToDateString
            //html =  "Gesamtbelastung: $element.load\nGelb: $element.percentageYellow, Rot: $element.percentageYellow)\nGewähltes Projekt:$element.loadProject"
            html  =      """<html><head><style>
                                h1 { color: #808080; font-family: verdana; font-size: 120%; }
                                p  { color: black; font-family: courier; font-size: 120%; } </style> </head>
                                
                                <body>
                                    <h1>${element.load.round(1)} $yellowRed</h1>
                                    <p>
                                        Gesamtbelastung: ${element.load.round(1)}<br/>
                                        Gelb: $yellow, Rot: $red<br/>
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
    void mouseMoved(MouseEvent e) {
        mouseMoved(e, this)
    }
}
