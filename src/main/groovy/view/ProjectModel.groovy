package view

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import model.Model
import model.TaskInProject

import javax.swing.SwingUtilities

import static extensions.DateHelperFunctions.*

@CompileStatic
class ProjectModel implements GridModel {

    List<List<GridElement>> allProjectGridLines

    @Bindable
    String projectName

    int nowXRowCache = 0
    List<TaskInProject> project

    @Delegate
    Model model


    ProjectModel(Model model) {
        this.model = model
        //this.taskList = taskList
        updateGridElements()
    }


    def setProjectName(String projectName ) {
        def old = this.projectName
        this.projectName = projectName
        updateGridElements()
        firePropertyChange("projectName", old, this.projectName)
    }

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
            project = getProject(projectName)

            project.sort {
                it.ending
            }
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
        assert projectTask
        assert all
        def gridElements = []
        Date startOfTask = _getStartOfWeek(projectTask.starting)
        Date endOfTask = _getStartOfWeek(projectTask.ending) + 7
        Date startOfGrid = _getStartOfWeek(all*.starting.min())
        Date endOfGrid = _getStartOfWeek(all*.ending.max()) + 7
        def fromToDateString = "${_dToS(startOfTask)} - ${_dToS(endOfTask)}"

        Date now = Date.newInstance()
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
        //shiftProject(y, -7)
        //updateGridElements()

    }

    @Override
    def moveRight(int y) {
        //shiftProject(y, 7)
        //updateGridElements()
    }

    def shiftProject(int y, int shift) {
        /*
        def projectName = allProjectNames[y]
        List<TaskInProject> project = getProject(projectName)
        project.each {
            it.ending += shift
            it.starting += shift
        }
        */
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
    int getNowX() {
        nowXRowCache
    }

    @Override
    List<String> getLineNames() {
        def r =[]
        project.each {r << it.department}
        r
    }
}
