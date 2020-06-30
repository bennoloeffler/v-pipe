package application

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import model.Model
import model.PipelineOriginalElement
import model.TaskInProject
import model.WeekOrMonth
import newview.GridElement
import newview.GridModel
import utils.RunTimer

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

import static extensions.DateHelperFunctions.*

//@CompileStatic
class NewPipelineModel extends GridModel {

    @Bindable
    String selectedProject

    List<List<GridElement>> allProjectGridLines

    // TODO: write this to model and save it
    //List<String> allProjectNames =[]

    int nowXRowCache = -1

    //@Delegate
    Model model

    def tasksPropertyListener = { PropertyChangeEvent e ->
        if(e.propertyName=='updateToggle') {
            //sortProjectNamesWhenChanged() // new task list - probably after opening a new Data-Set
            //updateProjectNames()
        }
        if(e.propertyName=='reloadToggle') {
            //updateProjectNames()
        }
        updateGridElements()
        this.setUpdateToggle(!this.getUpdateToggle()) // just to fire an PropertyChange to the view
    }

    NewPipelineModel(Model model) {
        this.model = model
        model.addPropertyChangeListener(tasksPropertyListener as PropertyChangeListener)

        //updateProjectNames()

        updateGridElements()
    }

    //@CompileStatic(TypeCheckingMode.SKIP)
    /*void updateProjectNames() {
        List<String> current = getAllProjects()
        List<String> intersect = current.intersect(allProjectNames)
        List<String> toRemove = allProjectNames - intersect
        List<String> toAdd = current - intersect
        allProjectNames.removeAll(toRemove)
        allProjectNames.addAll(toAdd)
    }*/

    void sortProjectNamesToEnd() {

        //List<String> allProjects = getAllProjects()
        Map<String, List<TaskInProject>> projectsMap = [:]
        model.projectSequence.each {
            projectsMap[it] = model.getProject(it)
        }
        List<String> allProjectNames = model.projectSequence.sort { String p1, String p2 ->
            projectsMap[p2]*.ending.max() <=> projectsMap[p1]*.ending.max()
        }



        model.projectSequence = allProjectNames


        updateGridElements()
        this.setUpdateToggle(!this.getUpdateToggle()) // just to fire an PropertyChange to the view

    }

    /**
     * create "the model" List<List<GridElement>> allProjectGridLines
     * from task-portfolio
     */
    private void updateGridElements() {
        def t = RunTimer.getTimerAndStart('NewPipelineModel::updateGridElements')
        allProjectGridLines = []
        if(model.taskList) {
            model.projectSequence.each {
                List<TaskInProject> projectTasks = model.getProject(it)
                def gridElements = fromProjectTasks(projectTasks)
                allProjectGridLines << gridElements
            }
        }
        t.stop()
    }


    /**
     * @param projectTasks tasks of one project
     * @return GridElements of one project
     */
    List<GridElement> fromProjectTasks( List<TaskInProject> projectTasks) {
        assert projectTasks
        def gridElements = []
        Date startOfGrid = _getStartOfWeek(model.getStartOfTasks())
        Date endOfGrid = _getStartOfWeek(model.getEndOfTasks()) + 7
        Date startOfProject = _getStartOfWeek(projectTasks*.starting.min())
        Date endOfProject = _getStartOfWeek(projectTasks*.ending.max()) + 7
        def fromToDateString = "${_dToS(startOfProject)} - ${_dToS(endOfProject)}"

        Date now = new Date() // Date.newInstance()
        int row = 0
        for (Date w = startOfGrid; w < endOfGrid; w += 7) {
            if(w <= now && now < w + 7) {
                nowXRowCache = row

            }
            row ++
            if (w >= startOfProject && w < endOfProject) {
                boolean integrationPhase = false
                if(model.pipelineElements) {
                    PipelineOriginalElement element = model.getPipelineElement(projectTasks[0].project)
                    long overlap = element.getDaysOverlap(w, w+7)
                    if(overlap){ integrationPhase = true }
                }
                gridElements << new GridElement(
                        project: projectTasks[0].project,
                        department: '',
                        timeString: "${_getWeekYearStr(w)}  ($fromToDateString)",
                        integrationPhase: integrationPhase)
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
        updateGridElements()

    }

    @Override
    def moveRight(int y) {
        shiftProject(y, 7)
        updateGridElements()
    }

    def shiftProject(int y, int shift) {
        def projectName = model.projectSequence[y]
        List<TaskInProject> project = model.getProject(projectName)
        project.each {
            it.ending += shift
            it.starting += shift
        }
        PipelineOriginalElement pe = model.getPipelineElement(projectName)
        pe.startDate += shift
        pe.endDate += shift

        model.reCalcCapaAvailableIfNeeded()
        model.setUpdateToggle(!model.getUpdateToggle())
    }

    @Override
    def toggleIntegrationPhase(int x, int y) {
        // TODO: Write to other model first...
        // TODO: connect integration phase
        allProjectGridLines[y][x].integrationPhase = ! allProjectGridLines[y][x].integrationPhase
    }

    @Override
    def swap(int y, int withY) {
        model.projectSequence.swap(y, withY)
        updateGridElements()
        this.setUpdateToggle(!this.getUpdateToggle()) // just to fire an PropertyChange to the view
    }

    @Override
    def setSelectedElement(int x, int y) {
        setSelectedProject(model.projectSequence[y])
    }

    @Override
    int getNowX() {
        nowXRowCache
    }

    @Override
    List<String> getLineNames() {
        return model.projectSequence
    }

    @Override
    List<String> getColumnNames() {
        return model.getFullSeriesOfTimeKeys(WeekOrMonth.WEEK)
    }

}
