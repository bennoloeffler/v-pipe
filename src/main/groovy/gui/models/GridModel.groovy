package gui.models

import groovy.beans.Bindable
import groovy.transform.TupleConstructor

/**
 * one element in the grid
 */
@TupleConstructor
class GridElement {
    String project
    String department
    String timeString
    boolean integrationPhase
    boolean deliveryDate = false

    static GridElement nullElement = new GridElement(project:'', department:'', timeString:'', integrationPhase: false)

    boolean asBoolean() {
        this != nullElement
    }
}



abstract class GridModel {

    @Bindable
    boolean updateToggle

    /**
     * @param x grid coordinates
     * @param y grid coordinates
     * @return an element of the grid (GridElement nullElement - if there is no element in the grid)
     */
    abstract GridElement getElement(int x, int y)

    /**
     * @return height or Y size of the grid
     */
    abstract int getSizeY()

    /**
     * @return width or X size of the grid
     */
    abstract int getSizeX()

    /**
     * move the complete line in line y to the left
     * @param y line
     */
    abstract def moveLeft(int y)

    /**
     * move the complete line in line y to the right
     * @param y line
     */
    abstract def moveRight(int y)

    /**
     * swap two lines
     */
    abstract def swap(int y, int withY)

    abstract def setSelectedElement(int x, int y)

    /**
     * @return the row which is to be marked as "now" in the view
     */
    abstract int getNowX()

    abstract List<String> getLineNames()

    abstract List<String> getColumnNames()

    abstract Map<String, String> getDetailsForTooltip(int x, int y)

    abstract makeSmaller(int i)

    abstract makeBigger(int i)
}

