package gui.models

import groovy.beans.Bindable
import groovy.transform.TupleConstructor

/**
 * One Element in the grid
 */
@TupleConstructor
class GridElement {
    String project // project
    String department // department
    String timeString
    //String fromToDateString
    boolean integrationPhase // integration?
    boolean deliveryDate = false

    //static GridElement nullElement = new GridElement(project:'', department:'', timeString:'', fromToDateString:'', integrationPhase: false)
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
     * @return heigth or Y size of the grid
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
     * switch the state of an GridElement: i on or off
     * @param x
     * @param y
     */
    abstract def toggleIntegrationPhase(int x, int y)

    /**
     * swap two lines
     * @param x
     * @param y
     */
    abstract def swap(int y, int withY)

    abstract def setSelectedElement(int x, int y)

    /**
     * @return the row which is to be marked as "now" in the view
     */
    abstract int getNowX()

    /**
     * @return as it says
     */
    abstract List<String> getLineNames()

    abstract List<String> getColumnNames()

    /**
     * @param x
     * @param y
     * @return headline, load - line by line
     */
    abstract List<String> getDetailsForTooltip(int x, int y)

    abstract makeSmaller(int i)
    abstract makeBigger(int i)
}

