package newview

import core.AbsoluteLoadCalculator
import core.CapaNeedDetails
import groovy.beans.Bindable
import model.Model
import model.WeekOrMonth

import java.beans.PropertyChangeEvent

class GridLoadModel extends AbstractGridLoadModel  {

    Model model

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
                Double maxAbsolute = absoluteLoadCalculator.getMax(department)
                if(model.capaAvailable.size()) {
                    yellowAbs = model.capaAvailable[department][timeStr].yellow
                    redAbs = model.capaAvailable[department][timeStr].red
                }
                Double projectLoad = 0
                capaNeedDetailsAbsolut.projects.each {
                    if(it.originalTask.project == selectedProject) {
                        projectLoad += it.projectCapaNeed
                    }
                }
                gridElements[department][timeStr] = new GridLoadElement(department, timeStr, capaNeedDetailsAbsolut.totalCapaNeed, projectLoad, yellowAbs, redAbs)
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
        gridElements.values()?gridElements.values()[0].size():0
    }

    @Override
    int getNowX() {
        return 0
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
