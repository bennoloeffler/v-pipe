package newview

import javax.swing.JScrollPane
import javax.swing.ToolTipManager
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent

trait PanelBasics {
    /**
     * ask for repaint
     */
    def invalidateAndRepaint() {
        revalidate()
        repaint()
    }

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


    JScrollPane getScrollPane() {
        try {
            getParent().getParent() as JScrollPane
        }catch(Exception e) {
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
}