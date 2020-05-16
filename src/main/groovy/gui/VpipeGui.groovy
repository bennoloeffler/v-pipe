package gui


import groovy.transform.Immutable

import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JViewport
import javax.swing.SwingUtilities
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

/**
 * One Element in the grid
 */
@Immutable
class GridElement {
    String project // project
    String department // department
    boolean integrationPhase // integration?
    static GridElement nullElement = new GridElement(project:'', department:'', integrationPhase: false)
}


/**
 * base model for grid - this one is for manual testing
 */
class GridModel {

    List <List<String>> data =
            [
                ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.model',     'p1.model.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         '',         'p2.k',     'p2.model',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         'p3.k',     'p3.t',     't3.model.i',       'p3.ibn.i',     'p3.ibn',           '',             '',         '',         '',         '',         ''      ],
                ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.model',     'p4.model',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.model',     'p1.model.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         '',         'p2.k',     'p2.model',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         'p3.k',     'p3.t',     't3.model.i',       'p3.ibn.i',     'p3.ibn',           '',             '',         '',         '',         '',         ''      ],
                ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.model',     'p4.model',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.model',     'p1.model.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         '',         'p2.k',     'p2.model',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         'p3.k',     'p3.t',     't3.model.i',       'p3.ibn.i',     'p3.ibn',           '',             '',         '',         '',         '',         ''      ],
                ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.model',     'p4.model',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                ['p1.k',    'p1.k',     'p1.t',     'p1.v',     'p1.model',     'p1.model.i',   'p1.ibn.i',     'p1.ibn',       '',                 '',             '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         '',         'p2.k',     'p2.model',         'p2.ibn.i',     'p2.ibn.i',         'p2.ibn',       '',         '',         '',         '',         ''      ],
                ['p4.k',    'p4.k',     'p4.t',     'p4.v',     'p4.model',     'p4.model',     'p4.ibn',       'p4.ibn.i',     'p4.ibn.i',         'p4.ibn',       '',         '',         '',         '',         ''      ],
                ['',        '',         '',         '',         '',         '',         '',             'p4.k',         'p4.model',             'p4.model',         'p4.ibn',   'p4.ibn.i', 'p4.p',     'p4.p',     'p4.p'  ]
            ]

    /**
     * 
     * @param x grid coordinates
     * @param y grid coordinates
     * @return an element of the grid (GridElement nullElement - if there is no element in the grid)
     */
    GridElement getElement(int x, int y) {
        assert y < data.size()
        assert x < data[y].size()
        def d = data[y][x]
        List<String> split = d.split(/\./)
        split.removeAll('')
        def size = split.size()
        if (size) {
            assert   1 < split.size() && split.size() <= 3 // 2 or 3
            return new GridElement(project: split[0], department: split[1], integrationPhase: split.size() == 3)
        } else {
            GridElement.nullElement
        }
    }

    
    /**
     * @return heigth or Y size of the grid
     */
    int getSizeY() {data.size()}

    
    /**
     * @return width or X size of the grid
     */
    int getSizeX() {data*.size().max()}


    /**
     * move the complete line in line y to the left
     * @param y line
     */
    def moveLeft(int y) {
        if( ! data[y][0]) {
            data[y].remove(0)
            data[y].add('')
        } else {
            for (line in 0..data.size()-1) {
                if(y==line) {
                    data[line].add('')
                } else {
                    data[line].add(0, '')
                }
            }
        }
    }


    /**
     * move the complete line in line y to the right
     * @param y line
     */
    def moveRight(int y) {

        if( ! data[y][data[y].size()-1]) {
            data[y].remove(data[y].size()-1)
            data[y].add(0, '')
        } else {
            for (line in 0..data.size()-1) {
                if(y==line) {
                    data[line].add(0, '')
                } else {
                    data[line].add('')
                }
            }
        }
    }
}



/**
 * Model
 */
class GridFileModel {

    List <List<String>> data = []

    /**
     *
     * @param x grid coordinates
     * @param y grid coordinates
     * @return an element of the grid (GridElement nullElement - if there is no element in the grid)
     */
    GridElement getElement(int x, int y) {
        assert y < data.size()
        assert x < data[y].size()
        def d = data[y][x]
        List<String> split = d.split(/\./)
        split.removeAll('')
        def size = split.size()
        if (size) {
            assert   1 < split.size() && split.size() <= 3 // 2 or 3
            return new GridElement(project: split[0], department: split[1], integrationPhase: split.size() == 3)
        } else {
            GridElement.nullElement
        }
    }


    /**
     * @return heigth or Y size of the grid
     */
    int getSizeY() {data.size()}


    /**
     * @return width or X size of the grid
     */
    int getSizeX() {data*.size().max()}


    /**
     * move the complete line in line y to the left
     * @param y line
     */
    def moveLeft(int y) {
        if( ! data[y][0]) {
            data[y].remove(0)
            data[y].add('')
        } else {
            for (line in 0..data.size()-1) {
                if(y==line) {
                    data[line].add('')
                } else {
                    data[line].add(0, '')
                }
            }
        }
    }


    /**
     * move the complete line in line y to the right
     * @param y line
     */
    def moveRight(int y) {

        if( ! data[y][data[y].size()-1]) {
            data[y].remove(data[y].size()-1)
            data[y].add(0, '')
        } else {
            for (line in 0..data.size()-1) {
                if(y==line) {
                    data[line].add(0, '')
                } else {
                    data[line].add('')
                }
            }
        }
    }

}




/**
 * custom JPanel with a grid and a red cursor in that grid,
 * that is moved with arrow keys: UP, RIGHT, DOWN, LEFT.
 * It has a GridModel.
 */
class GridPanel extends JPanel {

    MouseWheelListener gridMouseWheelListener = new MouseWheelListener() {

        @Override
        void mouseWheelMoved(MouseWheelEvent e) {
            //super?
            if((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) > 0) {
                //   negative values if the mouse wheel was rotated up/away from the user,
                //   and positive values if the mouse wheel was rotated down/ towards the user
                int rot = e.getWheelRotation()
                int inc = grid * rot / 20
                grid += inc!=0?inc:1 // min one tic bigger
                if(grid < 15){grid=15}
                if(grid > 400){grid=400}
            } else {
                scrollPane.getVerticalScrollBar().setUnitIncrement((grid/3) as int)
                scrollPane.processMouseWheelEvent(e)
            }
        }

    }


    MouseMotionListener gridMouseMotionListener = new MouseMotionListener() {

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
            //println("mouse: X = $mouseX y = $mouseY")
            revalidate()
            repaint()
        }

    }


    MouseListener gridMouseListener = new MouseListener() {

        @Override
        void mouseClicked(MouseEvent e) {
            mouseX = e.getX()
            mouseY = e.getY()
            cursorX = getGridXFromMouseX()
            cursorY = getGridYFromMouseY()
            revalidate()
            repaint()
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
            startDragX = StartDragY = endDragX = endDragY = -1
        }

        @Override
        void mouseEntered(MouseEvent e) {

        }

        @Override
        void mouseExited(MouseEvent e) {

        }
    }

    /**
     * the KeyListener, that gets all the commands and acts like a controller
     */
    KeyListener gridKeyListener = new KeyAdapter() {

        @Override
        void keyPressed(KeyEvent e){

            if(KeyEvent.VK_PLUS == e.getKeyCode()) {
                grid = (grid * 1.1) as int
                if(grid > 400){grid=400}
            }
            if(KeyEvent.VK_MINUS == e.getKeyCode()) {
                grid = (grid / 1.1) as int
                if(grid < 15){grid=15}
            }

            if(KeyEvent.VK_UP == e.getKeyCode())    {cursorY > 0              ? --cursorY :0}
            if(KeyEvent.VK_DOWN == e.getKeyCode())  {cursorY < model.sizeY-1  ? ++cursorY :0}
            if(KeyEvent.VK_LEFT == e.getKeyCode())  {cursorX > 0              ? --cursorX :0}
            if(KeyEvent.VK_RIGHT == e.getKeyCode()) {cursorX < model.sizeX-1  ? ++cursorX :0}

            if(keyAndShiftPressed(e, KeyEvent.VK_UP)) {
                //println("SHIFT UP x: $cursorX y: $cursorY")
                if(cursorY >= 0) {
                    model.data.swap(cursorY, cursorY + 1)
                    colors.swap(cursorY % colors.size(), (cursorY + 1) % colors.size())
                }
            }

            if(keyAndShiftPressed(e, KeyEvent.VK_DOWN)) {
                //println("SHIFT DOWN x: $cursorX y: $cursorY")
                if(cursorY <= model.sizeY-1) {
                    model.data.swap(cursorY - 1, cursorY)
                    colors.swap((cursorY - 1) % colors.size(), cursorY % colors.size())
                }
            }

            if(keyAndShiftPressed(e, KeyEvent.VK_LEFT)) {
                //println("SHIFT LEFT x: $cursorX y: $cursorY")
                model.moveLeft(cursorY)
            }

            if(keyAndShiftPressed(e, KeyEvent.VK_RIGHT)) {
                //println("SHIFT RIGHT x: $cursorX y: $cursorY")
                model.moveRight(cursorY)
            }

            SwingUtilities.invokeLater() {
                scrollRectToVisible(new Rectangle(cursorX*grid-grid , cursorY*grid -grid, 3*grid, 3*grid))
            }

            revalidate()
            repaint()
        }

        /**
         * @param e KeyEvent
         * @param keyCode e.g. KeyEvent.VK_RIGHT
         * @return pressed the keyCode together with SHIFT
         */
        private boolean keyAndShiftPressed(KeyEvent e, keyCode) {
            (keyCode == e.getKeyCode()) && (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) > 0
        }

    }


    GridModel model = new GridModel()
    int grid
    int borderWidth

    /**
     * the cursor - modeled as position
     */
    int cursorX = 0
    int cursorY = 0

    int mouseX = 0
    int mouseY = 0

    int startDragX = -1, StartDragY = 1, endDragX = 1, endDragY = -1

    List<Color> colors =[Color.getHSBColor(0.0, 0.2, 0.9),
                         Color.getHSBColor(0.5, 0.2, 0.9),
                         Color.getHSBColor(0.1, 0.2, 0.9),
                         Color.getHSBColor(0.6, 0.2, 0.9),
                         Color.getHSBColor(0.2, 0.2, 0.9),
                         Color.getHSBColor(0.7, 0.2, 0.9),
                         Color.getHSBColor(0.3, 0.2, 0.9),
                         Color.getHSBColor(0.8, 0.2, 0.9),
                         Color.getHSBColor(0.4, 0.2, 0.8),
                         Color.getHSBColor(0.9, 0.2, 0.8),
                         Color.getHSBColor(0.05, 0.25, 0.95),
                         Color.getHSBColor(0.55, 0.25, 0.95),
                         Color.getHSBColor(0.15, 0.25, 0.95),
                         Color.getHSBColor(0.65, 0.25, 0.95),
                         Color.getHSBColor(0.25, 0.25, 0.95),
                         Color.getHSBColor(0.75, 0.25, 0.95),
                         Color.getHSBColor(0.35, 0.25, 0.95),
                         Color.getHSBColor(0.85, 0.25, 0.95),
                         Color.getHSBColor(0.45, 0.25, 0.85),
                         Color.getHSBColor(0.95, 0.25, 0.85),
                         Color.getHSBColor(0.05, 0.25, 0.95),
                         Color.getHSBColor(0.55, 0.25, 0.95),
                         Color.getHSBColor(0.15, 0.25, 0.95),
                         Color.getHSBColor(0.65, 0.25, 0.95),
                         Color.getHSBColor(0.25, 0.25, 0.95),
                         Color.getHSBColor(0.75, 0.25, 0.95),
                         Color.getHSBColor(0.35, 0.25, 0.95),
                         Color.getHSBColor(0.85, 0.25, 0.95),
                         Color.getHSBColor(0.45, 0.25, 0.85),
                         Color.getHSBColor(0.95, 0.25, 0.85)

                         ]


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
    GridPanel(int grid = 60) {
        //setPreferredSize(new Dimension(700, 600))
        this.grid = grid
        this.borderWidth = grid / 3 as int
        addKeyListener(gridKeyListener)
        addMouseMotionListener(gridMouseMotionListener)
        addMouseListener(gridMouseListener)
        addMouseWheelListener(gridMouseWheelListener)
    }

    /**
     * create with custom grid and custom model
     * @param grid
     * @param borderWidth
     * @param model
     */
    GridPanel(int grid, GridModel model) {
        this(grid)
        this.model = model
    }


    /**
     * heart of painting
     * @param g1d
     */
    @Override
    protected void paintComponent(Graphics g1d) {
        super.paintComponent(g1d)
        //g1d.getClipBounds()
        Graphics2D g = g1d as Graphics2D

        for(int x in 0..model.sizeX - 1) {
            for(int y in 0..model.sizeY - 1) {
                Color c = getColor(y)
                g.setColor(c)
                drawGridElement(c, g, model.getElement(x,y), x, y)
            }
        }
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
    def drawGridElement(Color c, Graphics2D g, GridElement e, int x, int y) {

        def offset = (grid/20) as int // shadow and space
        def size = grid - offset // size of shadow box and element box
        def round = (size / 2) as int // corner diameter
        def graphX = borderWidth + x * grid // position
        def graphY = borderWidth + y * grid // position
        def gridMouseX = getGridXFromMouseX()
        def gridMouseY = getGridYFromMouseY()
        //println("gridMouseX=$gridMouseX gridMouseY=$gridMouseY")

        // shadow
        g.setColor(Color.LIGHT_GRAY)
        g.fillRoundRect(graphX+offset, graphY+offset, size-4, size-4, round, round)

        // element in project color, integration phase color (orange), empty color (white)
        g.setColor( e.integrationPhase ? Color.orange : (e == e.nullElement ? Color.WHITE : c) )
        // or cursor color (overwrites everything)
        if(x==cursorX && y == cursorY) { g.setColor(Color.RED) }
        // or current mouse location color (overwrites even more)
        if(x==gridMouseX && y == gridMouseY) {
            g.setColor(makeTransparent(g.getColor(), 120))
        } //g.setColor(Color.LIGHT_GRAY)}

        g.fillRoundRect(graphX, graphY, size-4 , size-4, round, round)


        // write (with shadow) some info
        // TODO: use Clip, Transform, Paint, Font and Composite
        def fontSize = 20.0 * grid/60
        g.setFont (g.getFont().deriveFont (fontSize))
        g.setColor(Color.WHITE)
        g.drawString(e.project, graphX + grid*0.2,     graphY + grid/2)
        g.setColor(Color.BLACK)
        g.drawString(e.project, graphX + grid*0.2 - 2, graphY + grid/2 - 2)
    }

    /**
     * @param mouseY (optional - else take this.mouseY as base for calc)
     * @return gridY - based on mouse pos
     */
    int getGridYFromMouseY(mouseY = -1) {
        mouseY = mouseY < 0 ? this.mouseY : mouseY // take this.mouseY, if default-Param, else take param
        ((mouseY - borderWidth) / grid) as int
    }

    /**
     * @param mouseX (optional - else take this.mouseX as base for calc)
     * @return gridX - based on mouse pos
     */
    int getGridXFromMouseX(mouseX = -1) {
        mouseX = mouseX < 0 ? this.mouseX : mouseX // take this.mouseY, if default-Param, else take param
        def gridX = ((mouseX - borderWidth) / grid) as int
        gridX
    }

    Color makeTransparent(Color source, int alpha)
    {
        return new Color(source.getRed(), source.getGreen(), source.getBlue(), alpha);
    }
}


/**
 *
 */
class VpipeGui {

    static void createFrame(GridPanel p) {
        JFrame frame = new JFrame("v-pipe    |  +/- = Zoom  |  Pfeile = Cursor bewegen  |  Shift+Pfeile = Projekt bewegen  |  CTRL-s = speichern  | CTRL-o = öffnen  ")
        JScrollPane sp = new JScrollPane(p)
        //sp.getViewport().putClientProperty("EnableWindowBlit", Boolean.TRUE);
        sp.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        //sp.getVerticalScrollBar().setUnitIncrement(500);
        frame.getContentPane().add(sp)
        //frame.addMouseWheelListener(p.gridMouseWheelListener)
        frame.addKeyListener(p.gridKeyListener) // needs to be here... not in p
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
        frame.pack()
        frame.setVisible(true)
    }

    /**
     * DEMO-Mode
     * @param args
     */
    static void main(String[] args) {
        GridPanel p = new GridPanel()
        createFrame(p)
    }
}

//main(null)
