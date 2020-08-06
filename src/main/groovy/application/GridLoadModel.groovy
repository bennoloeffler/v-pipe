package application

import core.AbsoluteLoadCalculator
import core.CapaNeedDetails
import groovy.beans.Bindable
import model.Model
import model.WeekOrMonth
import newview.AbstractGridLoadModel
import newview.GridLoadElement
import utils.RunTimer

import java.beans.PropertyChangeEvent

import static extensions.DateHelperFunctions._getStartOfWeek

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
        RunTimer.getTimerAndStart('GridLoadModel::updateAllFromModelData').withCloseable {
            absoluteLoadCalculator = new AbsoluteLoadCalculator(model.taskList)
            calcGridElements()
            calcRowX()
        }
        setUpdateToggle(!getUpdateToggle())
    }


    void calcGridElements() {
        def oldElements = gridElements
        def t = RunTimer.getTimerAndStart('GridLoadModel::calcGridElements')
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
                def avail = oldElements[department]?.get(timeStr)
                if(avail) {
                    avail.department = department
                    avail.timeString = timeStr
                    avail.load = capaNeedDetailsAbsolut.totalCapaNeed
                    avail.loadProject = projectLoad
                    avail.yellow = yellowAbs
                    avail.red = redAbs
                    avail.projectDetails = capaNeedDetailsAbsolut.projects
                    gridElements[department][timeStr] = avail
                } else {
                    gridElements[department][timeStr] = new GridLoadElement(department, timeStr, capaNeedDetailsAbsolut.totalCapaNeed, projectLoad, yellowAbs, redAbs, capaNeedDetailsAbsolut.projects)
                }

            }
        }
        t.stop()
    }

    private void calcRowX() {
        def t = RunTimer.getTimerAndStart('GridLoadModel::calcRowX')

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
        t.stop()
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
    double getMaxValAndRed(int y) {
        def maxLoad = absoluteLoadCalculator.getMax(model.getAllDepartments()[y])
        def maxRed = gridElements[model.getAllDepartments()[y]].entrySet()*.value.max {it.red}
        Math.max(maxLoad, maxRed.red)
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
