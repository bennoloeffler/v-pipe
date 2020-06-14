package newview

import core.AbsoluteLoadCalculator
import core.CapaNeedDetails
import groovy.beans.Bindable
import model.Model
import model.TaskInProject
import model.WeekOrMonth

import java.beans.PropertyChangeEvent

import static extensions.DateHelperFunctions._dToS
import static extensions.DateHelperFunctions._dToS
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getStartOfWeek
import static extensions.DateHelperFunctions._getWeekYearStr

class GridLoadModel extends AbstractGridLoadModel  {

    Model model

    int nowXRowCache = -1

    @Bindable
    String selectedProject = ""

    WeekOrMonth weekOrMonth = WeekOrMonth.WEEK

    Map<String, Map<String, GridLoadElement>> gridElements =[:]

    AbsoluteLoadCalculator absoluteLoadCalculator

    def updateCallback = {
        updateAllFromModelData()
    }

    def updateCallbackCurrentProject = { PropertyChangeEvent e ->
        //println("updateCallbackCurrentProject: $e.oldValue -> $e.newValue")
        updateAllFromModelData()
    }

    GridLoadModel(Model model, WeekOrMonth weekOrMonth = WeekOrMonth.WEEK) {
        this.model = model
        this.weekOrMonth = weekOrMonth
        model.addPropertyChangeListener('updateToggle', updateCallback)
        this.addPropertyChangeListener('selectedProject', updateCallbackCurrentProject)
        updateAllFromModelData()
    }

    void updateAllFromModelData() {
        absoluteLoadCalculator = new AbsoluteLoadCalculator(model.taskList)
        calcGridElements()
        calcRowX()
        setUpdateToggle(!getUpdateToggle())
    }


    void calcGridElements() {

        gridElements = [:]
        model.getAllDepartments().each { String department ->
            gridElements[department] = [:]
            model.getFullSeriesOfTimeKeys(weekOrMonth).each { String timeStr ->
                Double yellowAbs = -1
                Double redAbs = -1
                CapaNeedDetails capaNeedDetailsAbsolut = absoluteLoadCalculator.getCapaNeeded(department, timeStr)

                //Double maxAbsolute = absoluteLoadCalculator.getMax(department)

                Double projectLoad = 0
                capaNeedDetailsAbsolut.projects.each {
                    if(it.originalTask.project == selectedProject) {
                        projectLoad += it.projectCapaNeed
                    }
                }



                if(model.capaAvailable.size()) {
                    yellowAbs = model.capaAvailable[department][timeStr].yellow
                    redAbs = model.capaAvailable[department][timeStr].red
                } else {
                    //projectLoad /= maxAbsolute
                    //capaNeedDetailsAbsolut.totalCapaNeed /= maxAbsolute
                }

                gridElements[department][timeStr] = new GridLoadElement(department, timeStr, capaNeedDetailsAbsolut.totalCapaNeed, projectLoad, yellowAbs, redAbs, capaNeedDetailsAbsolut.projects)
            }
        }
    }

    private void calcRowX() {
        nowXRowCache = -1
        Date startOfGrid = _getStartOfWeek(model.getStartOfTasks())
        Date endOfGrid = _getStartOfWeek(model.getEndOfTasks()) + 7

        Date now = new Date() // Date.newInstance()
        int row = 0
        for (Date w = startOfGrid; w < endOfGrid; w += 7) {
            if (w <= now && now < w + 7) {
                nowXRowCache = row
            }
            row++
        }
    }

    @Override
    GridLoadElement getElement(int x, int y) {
        gridElements.values()[y].values()[x]
    }

    @Override
    int getSizeY() {
        gridElements.size()
    }

    @Override
    int getSizeX() {
        gridElements.values()?gridElements.values()[0].size():0
    }

    @Override
    int getNowX() {
        return nowXRowCache
    }

    @Override
    double getMax(int y) {
        return absoluteLoadCalculator.getMax(model.getAllDepartments()[y])
    }

    @Override
    List<String> getYNames() {
        return model.getAllDepartments()

    }

    @Override
    List<String> getXNames() {
        return model.getFullSeriesOfTimeKeys(weekOrMonth)
    }
}
