package newview

import core.ProjectCapaNeedDetails

//import core.ProjectCapaNeedDetails // TODO: SÜNDE! ABHÄNGIGKEIT!
import groovy.beans.Bindable
import groovy.transform.TupleConstructor

/**
 * One Element in the grid
 */
@TupleConstructor
class GridLoadElement {

    String department // department
    String timeString
    double load // as absulute value
    double loadProject // as absulute value
    double yellow = -1 // as absulute value
    double red = -1 // as absulute value
    List<ProjectCapaNeedDetails> projectDetails

    // all floating averages
    double loadAvg
    double yellowAvg
    double redAvg

    static GridLoadElement nullElement = new GridLoadElement(
            department:'',
            timeString:'',
            load: 0.0,
            yellow: -1,
            red: -1,
            projectDetails: [],
    )

    boolean asBoolean() {
        ! nullElement.is(this)
    }

}


abstract class AbstractGridLoadModel {

    @Bindable
    boolean updateToggle


    /**
     * @param x grid coordinates
     * @param y grid coordinates
     * @return an element of the grid (GridElement nullElement - if there is no element in the grid)
     */
    abstract GridLoadElement getElement(int x, int y)

    /**
     * @return heigth or Y size of the grid
     */
    abstract int getSizeY()

    /**
     * @return width or X size of the grid
     */
    abstract int getSizeX()

    /**
     * @return the column which is to be marked as "now" in the view
     */
    abstract int getNowX()

    /**
     * @param y
     * @return the maximum value in percent or absolute of line y
     */
    abstract double getMaxValAndRed(int y)

    /**
     * @return as it says
     */
    abstract List<String> getYNames()


    // TODO:
    abstract List<String> getXNames()

}

