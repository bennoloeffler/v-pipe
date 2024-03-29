package gui.panels


import groovy.transform.CompileStatic

import javax.swing.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.util.List

@CompileStatic
trait PanelBasics {

    Image frameIcon = new ImageIcon(getClass().getResource("/icons/vunds_icon_ 400x400.png")).getImage()


    Color nowBarColor = new Color(0, 0, 255, 90)
    Color cursorColor = new Color(0, 90, 255, 90)
    //Color cursorColor = new Color(133, 200, 240, 190)
    Color mouseColor  = new Color(200, 200, 200, 130)

    AffineTransform totalTransform = new AffineTransform()
    AffineTransform rotationTransform = new AffineTransform()
    AffineTransform translateTransform = new AffineTransform()
    AffineTransform atBackup

    //@Bindable int gridWidth

    int borderWidth
    int nameWidth
    int gridHeigth

    void updateOthersFromGridWidth(int gridWidth, JComponent c) {
        gridHeigth = gridWidth * 4
        nameWidth = (int)(gridHeigth *1.0) // strange compile bug without???
        borderWidth = (int)5 //(gridWidth / 3)
        getScrollPane(c)?.getVerticalScrollBar()?.setUnitIncrement((gridHeigth/6) as int)
    }


    /*
    int scrollMouseToMouse(JComponent c, Point mouse) {
        getScrollPane(c)?.getVerticalScrollBar()?.getValue()
    }*/


    /**
     * ask for repaint
     */
    def invalidateAndRepaint(JComponent c) {
        c.revalidate()
        c.repaint()
    }


    //
    // MouseMotionListener
    //

    void mouseMoved(MouseEvent e, JComponent c) {
        //mouseX = e.getX()
        //mouseY = e.getY()
        //println "$e.x $e.y in $c.name"
        /*
        MouseEvent phantom = new MouseEvent(
                c,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                e.getX(),
                e.getY(),
                0,
                false)
         */
        ToolTipManager.sharedInstance().mouseMoved(e)//phantom)
    }


    void mouseDragged(MouseEvent e) {
        //int x = e.getY()
        //int y = e.getX()
        /*
        println("drag: x=$x y=$y")
        int gX = getGridXFromMouseX(x)
        int gY = getGridYFromMouseY(y)
        int swapX = getGridXFromMouseX(startDragX)
        int swapY = getGridYFromMouseY(startDragY)
        */
    }



    JScrollPane getScrollPane(JComponent c) {
        try {
            c.getParent().getParent() as JScrollPane
        }catch(Exception ignore) {
            null // no scrollPane...
        }
    }

    def hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON)

        // Set anti-alias for text
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    }

    /*
    JToolTip createToolTip() {
        //balloonTip = new BalloonTip()
        JToolTip tip = new JToolTip()
        tip.setComponent(this)
        return tip
    }*/

    /**
     * colors for lines...
     */
    List<Color> colors = createColorList()

    static List<Color> createColorList() {
        List<Color> cl = []
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
     * next color of an index mapped to the color list
     * @param idx
     * @return color
     */
    Color getColor(int idx) {
        idx = idx % colors.size()
        return colors[ idx ]
    }



}