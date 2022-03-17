package gui.models

import core.AbsoluteLoadCalculator
import core.CapaNeedDetails
import core.MovingAverage
import groovy.beans.Bindable
import groovy.time.TimeCategory
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import model.Model
import model.WeekOrMonth
import utils.RunTimer

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

import static extensions.DateHelperFunctions._getStartOfMonth
import static extensions.DateHelperFunctions._getStartOfWeek

// Performance IDEA
// 1 GridLoadModelSparce with x and y and shiftX (korrekt x and y when saving (and drawing), set ShiftX to 0 when loading)
// 2 GridLoadModelSparce --> iterate (and draw) only those, that are there
// 3 make creating model simple by calculating x and y ONCE while start and when changing a project or task
@CompileStatic
class GridLoadProjectsModel extends AbstractGridLoadModel {

    Model model

    int nowXRowCache = -1

    @Bindable
    String selectedProject = ""

    WeekOrMonth weekOrMonth = WeekOrMonth.WEEK

    Map<String, Map<String, GridLoadElement>> gridElements = [:] //Map( department : Map( timeStr : GridLoadElement))

    AbsoluteLoadCalculator absoluteLoadCalculator

    PropertyChangeListener updateCallback = {
        updateAllFromModelData()
    }

    PropertyChangeListener updateCallbackCurrentProject = { PropertyChangeEvent e ->
        //println("updateCallbackCurrentProject: $e.oldValue -> $e.newValue")
        updateAllFromModelData()
    }

    GridLoadProjectsModel(Model model, WeekOrMonth weekOrMonth = WeekOrMonth.WEEK) {
        this.model = model
        this.weekOrMonth = weekOrMonth
        model.addPropertyChangeListener('updateToggle', updateCallback)
        this.addPropertyChangeListener('selectedProject', updateCallbackCurrentProject)
        updateAllFromModelData()
    }

    void updateAllFromModelData() {
        RunTimer.getTimerAndStart('GridLoadModel::updateAllFromModelData').withCloseable {
            absoluteLoadCalculator = new AbsoluteLoadCalculator(model.taskList, weekOrMonth)
            calcGridElements()
            calcAverageValues()
            calcRowX()
        }
        setUpdateToggle(!getUpdateToggle())
    }


    void calcGridElements() {
        def oldElements = gridElements
        RunTimer.getTimerAndStart('GridLoadModel::calcGridElements').withCloseable {
            gridElements = [:]
            model.getAllDepartments().each { String department ->
                gridElements[department] = [:] as Map<String, GridLoadElement>
                model.getFullSeriesOfTimeKeys(weekOrMonth).each { String timeStr ->
                    Double yellowAbs = -1
                    Double redAbs = -1
                    CapaNeedDetails capaNeedDetailsAbsolut = absoluteLoadCalculator.getCapaNeeded(department, timeStr)

                    //Double maxAbsolute = absoluteLoadCalculator.getMax(department)

                    Double projectLoad = 0
                    capaNeedDetailsAbsolut.projects.each {
                        if (it.originalTask.project == selectedProject) {
                            projectLoad += it.projectCapaNeed
                        }
                    }


                    if (model.capaAvailable.size()) {
                        //println department + "   " + timeStr
                        assert model.capaAvailable[department][timeStr]
                        yellowAbs = model.capaAvailable[department][timeStr].yellow
                        redAbs = model.capaAvailable[department][timeStr].red
                    } else {
                        //projectLoad /= maxAbsolute
                        //capaNeedDetailsAbsolut.totalCapaNeed /= maxAbsolute
                    }
                    def avail = oldElements[department]?.get(timeStr)
                    if (avail) {
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
        }
    }

    @CompileDynamic
    private void calcRowX() {
        RunTimer.getTimerAndStart('GridLoadModel::calcRowX').withCloseable {

            nowXRowCache = -1
            use(TimeCategory) {
                def add = 0.day
                Date startOfGrid
                Date endOfGrid
                if (weekOrMonth == WeekOrMonth.WEEK) {
                    startOfGrid = _getStartOfWeek(model.getStartOfProjects())
                    endOfGrid = _getStartOfWeek(model.getEndOfProjects()) + 7
                    add = 7.day
                } else {
                    startOfGrid = _getStartOfMonth(model.getStartOfProjects())
                    endOfGrid = _getStartOfMonth(model.getEndOfProjects()) + 1.month
                    add = 1.month
                }

                Date now = new Date() // Date.newInstance()
                int row = 0
                for (Date w = startOfGrid; w < endOfGrid; w += add) {
                    if (w <= now && now < w + add) {
                        nowXRowCache = row
                    }
                    row++
                }
                //if (weekOrMonth == WeekOrMonth.MONTH) {
                //    nowXRowCache = -1
                //}
            }
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
        gridElements.values() ? gridElements.values()[0].size() : 0
    }

    @Override
    int getNowX() {
        return nowXRowCache
    }

    @Override
    double getMaxValAndRed(int y) {

        def d = model.getAllDepartments()[y]
        //println "$y: $d"
        //println gridElements[d].entrySet()
        def maxLoad = absoluteLoadCalculator.getMax(d)
        def maxRed = gridElements[d].entrySet().collect { it.value }.max { it.red }
        if (maxRed) {
            Math.max(maxLoad, maxRed.red)
        } else {
            1.0
        } // if there is no load at all...
    }

    @Override
    List<String> getYNames() {
        return model.getAllDepartments()

    }

    @Override
    List<String> getXNames() {
        return model.getFullSeriesOfTimeKeys(weekOrMonth)
    }

    void calcAverageValues() {
        model.getAllDepartments().each { String department ->
            def ma = { gridElements[department].values()*.load } as MovingAverage
            ma.howMany = 5
            def maList = ma.getAverageValues()
            def i = 0
            gridElements[department].each {
                it.value.loadMovingAvg = maList[i++]
            }
        }
    }

}
