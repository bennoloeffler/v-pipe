package gui.models

import core.ProjectCapaNeedDetails

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import groovy.transform.ToString
import groovy.transform.TupleConstructor

/**
 * One Element in the grid
 */
@TupleConstructor
@CompileStatic
@ToString(includeNames = true)
class GridLoadElement {

    String department // department
    String timeString
    Double load // as absolute value
    double loadProject // as absolute value
    double yellow = -1 // as absolute value
    double red = -1 // as absolute value
    List<ProjectCapaNeedDetails> projectDetails

    // all moving averages
    double loadMovingAvg

    static GridLoadElement nullElement = new GridLoadElement(
            department:'',
            timeString:'',
            load: 0.0d,
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
    String selectedProject = ""


    @Bindable
    boolean updateToggle


    /**
     * @param x grid coordinates
     * @param y grid coordinates
     * @return an element of the grid (GridElement nullElement - if there is no element in the grid)
     */
    abstract GridLoadElement getElement(int x, int y)

    /**
     * @return height or Y size of the grid
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


    abstract List<String> getXNames()



}

