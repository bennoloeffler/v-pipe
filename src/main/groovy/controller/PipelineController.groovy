package controller

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import model.Model
import view.GridElement
import view.PipelinePanel
import view.VpipeGui

import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

class PipelineController {

    Model model
    PipelinePanel panel

    PipelineController(Model model, PipelinePanel panel) {
        this.model = model
        this. panel = panel
    }

    /**
     * the Mouse- and KeyListener, that gets all the commands and acts like a controller
     */

//    //
//    // MouseWheelListener
//    //
//    @Override
//    void mouseWheelMoved(MouseWheelEvent e) {
//        //super?
//        if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
//            //   negative values if the mouse wheel was rotated up/away from the user,
//            //   and positive values if the mouse wheel was rotated down/ towards the user
//            int rot = e.getWheelRotation()
//            int inc = (int)(grid * rot / 10)
//            grid += inc!=0?inc:1 // min one tic bigger
//            if(grid < 10){grid=10}
//            if(grid > 400){grid=400}
//            nameWidth = grid *3
//            lp?.setGridWidth(grid)
//            invalidateAndRepaint()
//        } else {
//            scrollPane.getVerticalScrollBar().setUnitIncrement((grid/3) as int)
//            scrollPane.processMouseWheelEvent(e)
//        }
//    }
//
//
//    //
//    // MouseMotionListener
//    //
//
//    @Override
//    void mouseDragged(MouseEvent e) {
//        int x = e.getY()
//        int y = e.getX()
//        /*
//        println("drag: x=$x y=$y")
//        int gX = getGridXFromMouseX(x)
//        int gY = getGridYFromMouseY(y)
//        int swapX = getGridXFromMouseX(startDragX)
//        int swapY = getGridYFromMouseY(startDragY)
//        */
//    }
//
//    @Override
//    void mouseMoved(MouseEvent e) {
//        int mouseX = e.getX()
//        int mouseY = e.getY()
//        MouseEvent phantom = new MouseEvent(
//                this,
//                MouseEvent.MOUSE_MOVED,
//                System.currentTimeMillis(),
//                0,
//                mouseX,
//                mouseY,
//                0,
//                false)
//
//        ToolTipManager.sharedInstance().mouseMoved(phantom)
//    }
//
//    @Override
//    String getToolTipText(MouseEvent event) {
//        String html = null
//
//        def gridX = getGridXFromMouseX(event.x)
//        def gridY = getGridYFromMouseY(event.y)
//        if (gridX >= 0 && gridX < model.sizeX && gridY >= 0 && gridY < model.sizeY) {
//            def element = model.getElement(gridX, gridY)
//            if (element == GridElement.nullElement) {
//                return null
//            }
//
//            // <br/>$element.fromToDateString
//            html =
//                    "<html><p style=\"background-color:white;\"><font color=\"#808080\" " +
//                            "size=\"20\" face=\"Verdana\">${element.timeString?:''}<br/>$element.project<br/>$element.department" + // SIZE DOES NOT WORK!
//                            "</font></p></html>"
//        }
//        return html
//    }
//
//
//    //
//    // MouseListener
//    //
//
//    @Override
//    void mouseClicked(MouseEvent e) {
//        //def redraw = [new Point(cursorX, cursorY)]
//        mouseX = e.getX()
//        mouseY = e.getY()
//        cursorX = getGridXFromMouseX()
//        cursorY = getGridYFromMouseY()
//        SwingUtilities.invokeLater {
//            updateLoadData()
//        }
//        //redraw << new Point(cursorX, cursorY)
//        //revalidate()
//        //model.firePointListeners(redraw)
//        invalidateAndRepaint()
//    }
//
//    @Override
//    void mousePressed(MouseEvent e) {
//        startDragX = e.getX()
//        startDragY = e.getY()
//    }
//
//    @Override
//    void mouseReleased(MouseEvent e) {
//        endDragX = e.getX()
//        endDragY = e.getY()
//        startDragX = startDragY = endDragX = endDragY = -1
//    }
//
//    @Override
//    void mouseEntered(MouseEvent e) {}
//
//    @Override
//    void mouseExited(MouseEvent e) {}
//
//    //
//    // KeyListener
//    //
//
//    @Override
//    void keyTyped(KeyEvent e) {}
//
//    @Override
//    void keyPressed(KeyEvent e){
//
//        if(KeyEvent.VK_I == e.getKeyCode()) {
//            model.toggleIntegrationPhase(cursorX, cursorY)
//        }
//
//        if(KeyEvent.VK_O == e.getKeyCode()) {
//            openFile()
//        }
//
//        if(KeyEvent.VK_L == e.getKeyCode()) {
//            VpipeGui.openLoad()
//        }
//
//        if(KeyEvent.VK_PLUS == e.getKeyCode()) {
//            grid = (grid * 1.1) as int
//            if(grid > 400){grid=400}
//            nameWidth = grid *3
//            lp?.setGridWidth(grid)
//
//        }
//        if(KeyEvent.VK_MINUS == e.getKeyCode()) {
//            grid = (grid / 1.1) as int
//            if(grid < 10){grid=10}
//            nameWidth = grid *3
//            lp?.setGridWidth(grid)
//        }
//
//        //def redraw = [new Point(cursorX, cursorY)]
//        if(KeyEvent.VK_UP == e.getKeyCode())    {cursorY > 0              ? --cursorY :0}
//        if(KeyEvent.VK_DOWN == e.getKeyCode())  {cursorY < model.sizeY-1  ? ++cursorY :0}
//        if(KeyEvent.VK_LEFT == e.getKeyCode())  {cursorX > 0              ? --cursorX :0}
//        if(KeyEvent.VK_RIGHT == e.getKeyCode()) {cursorX < model.sizeX-1  ? ++cursorX :0}
//
//        if(keyAndShiftPressed(e, KeyEvent.VK_UP)) {
//            //println("SHIFT UP x: $cursorX y: $cursorY")
//            if(cursorY >= 0) {
//                model.swap(cursorY, cursorY + 1)
//                colors.swap(cursorY % colors.size(), (cursorY + 1) % colors.size())
//            }
//        }
//
//        if(keyAndShiftPressed(e, KeyEvent.VK_DOWN)) {
//            //println("SHIFT DOWN x: $cursorX y: $cursorY")
//            if(cursorY <= model.sizeY-1) {
//                model.swap(cursorY - 1, cursorY)
//                colors.swap((cursorY - 1) % colors.size(), cursorY % colors.size())
//            }
//        }
//
//        if(keyAndShiftPressed(e, KeyEvent.VK_LEFT)) {
//            //println("SHIFT LEFT x: $cursorX y: $cursorY")
//            model.moveLeft(cursorY)
//            updateLoadData()
//        }
//
//        if(keyAndShiftPressed(e, KeyEvent.VK_RIGHT)) {
//            //println("SHIFT RIGHT x: $cursorX y: $cursorY")
//            model.moveRight(cursorY)
//            updateLoadData()
//        }
//
//        SwingUtilities.invokeLater() {
//            scrollToCursorXY()
//            updateLoadData()
//
//        }
//
//        invalidateAndRepaint()
//    }
//
//    void scrollToCursorXY() {
//        scrollRectToVisible(new Rectangle(nameWidth + cursorX * grid - grid, cursorY * grid - grid, 3 * grid, 3 * grid))
//    }
//
//    @Override
//    void keyReleased(KeyEvent e) {}
//
//
//    /**
//     * @param e KeyEvent
//     * @param keyCode e.g. KeyEvent.VK_RIGHT
//     * @return pressed the keyCode together with SHIFT
//     */
//    private boolean keyAndShiftPressed(KeyEvent e, keyCode) {
//        (keyCode == e.getKeyCode()) && (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) > 0
//    }


}
