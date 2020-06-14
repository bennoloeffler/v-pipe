package newview

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import model.Model
import model.TaskInProject
import model.WeekOrMonth
import newview.GridElement
import newview.GridModel

import java.beans.PropertyChangeEvent

import static extensions.DateHelperFunctions.*

@CompileStatic
class NewProjectModel extends GridModel {

    List<List<GridElement>> allProjectGridLines

    @Bindable
    String projectName =""

    int nowXRowCache = -1
    List<TaskInProject> project = []

    //@Delegate
    Model model

    Closure projectNameCallback = {
        updateGridElements()
        setUpdateToggle(!getUpdateToggle())
    }

    NewProjectModel(Model model) {
        this.model = model
        this.addPropertyChangeListener('projectName', projectNameCallback)
        updateGridElements()
    }

/*
    def setProjectName(String projectName ) {
        def old = this.projectName
        this.projectName = projectName
        updateGridElements()
        firePropertyChange("projectName", old, this.projectName)
    }
*/

    /**
     * create "the model" List<List<GridElement>> allProjectGridLines
     * from task-portfolio
     */
    private void updateGridElements() {
        allProjectGridLines = []
        if (projectName) {
            //
            // just for sorting projects to ending-date
            //
            project = model.getProject(projectName)

            //project.sort {
            //    it.ending
            //}
            project.each {
                allProjectGridLines << fromTask(it, model.taskList)
            }
        }
    }


    /**
     * @param projectTasks tasks of one project
     * @return GridElements of one project
     */
    List<GridElement> fromTask(TaskInProject projectTask, List<TaskInProject> all) {
        nowXRowCache = - 1
        assert projectTask
        assert all
        def gridElements = []
        Date startOfTask = _getStartOfWeek(projectTask.starting)
        Date endOfTask = _getStartOfWeek(projectTask.ending) + 7
        Date startOfGrid = _getStartOfWeek(all*.starting.min())
        Date endOfGrid = _getStartOfWeek(all*.ending.max()) + 7
        def fromToDateString = "${_dToS(startOfTask)} - ${_dToS(endOfTask)}"

        Date now = new Date()
        int row = 0
        for (Date w = startOfGrid; w < endOfGrid; w += 7) {
            if(w <= now && now < w + 7) {
                nowXRowCache = row
            }
            row ++
            if (w >= startOfTask && w < endOfTask) {
                gridElements << new GridElement(projectTask.project, projectTask.department, "${_getWeekYearStr(w)}  ($fromToDateString)", false)
            } else {
                gridElements << GridElement.nullElement
            }
        }

        return gridElements
    }


    @Override
    GridElement getElement(int x, int y) { allProjectGridLines[y][x] }

    @Override
    int getSizeY() { allProjectGridLines.size() }

    @Override
    int getSizeX() { allProjectGridLines ? allProjectGridLines[0].size() : 0 }

    @Override
    def moveLeft(int y) {
        shiftProject(y, -7)
        updateModelRecalcAndFire()
    }

    @Override
    def moveRight(int y) {
        shiftProject(y, 7)
        updateModelRecalcAndFire()
    }

    def shiftProject(int y, int shift) {
        TaskInProject projectTask = project[y]
        projectTask.ending += shift
        projectTask.starting += shift
    }

    def updateModelRecalcAndFire() {
        model.reCalcCapaAvailableIfNeeded()
        model.setUpdateToggle(!model.getUpdateToggle())
        updateGridElements()
    }


    @Override
    def toggleIntegrationPhase(int x, int y) {
        // TODO: Write to other model first...
        // TODO: connect integration phase
        //allProjectGridLines[y][x].integrationPhase = ! allProjectGridLines[y][x].integrationPhase
    }

    @Override
    def swap(int y, int withY) {
        /*
        allProjectNames.swap(y, withY)
        updateGridElements()*/
    }

    @Override
    def setSelectedElement(int x, int y) {
        //throw new RuntimeException('not yet implemented')
    }

    @Override
    int getNowX() {
        nowXRowCache
    }

    @Override
    List<String> getLineNames() {
        def r =[]
        project.each {r << it.department}
        r
    }

    @Override
    List<String> getColumnNames() {
        return model.getFullSeriesOfTimeKeys(WeekOrMonth.WEEK)
    }

}
