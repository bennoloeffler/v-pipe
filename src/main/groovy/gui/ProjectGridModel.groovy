package gui

import core.TaskInProject
import core.TaskListPortfolioAccessor
import groovy.transform.CompileStatic

import static extensions.DateHelperFunctions.*

@CompileStatic
class ProjectGridModel implements GridModel, TaskListPortfolioAccessor {

    List<List<GridElement>> allProjectGridLines
    List<String> allProjectNames
    int nowXRowCache = 0


    ProjectGridModel(List<TaskInProject> taskList) {
        this.taskList = taskList

        //
        // just for sorting projects to ending-date
        //
        List<String> allProjects = getAllProjects()
        Map<String, List<TaskInProject>> projectsMap = [:]
        allProjects.each {
            projectsMap[it] = getProject(it)
        }
        allProjectNames = allProjects.sort { String p1, String p2 ->
            projectsMap[p2]*.ending.max() <=> projectsMap[p1]*.ending.max()
        }

        updateGridElements()
    }

    /**
     * create "the model" List<List<GridElement>> allProjectGridLines
     * from task-portfolio
     */
    private void updateGridElements() {
        allProjectGridLines = []
        allProjectNames.each {
            List<TaskInProject> projectTasks = getProject(it)
            def gridElements = fromProjectTasks(projectTasks)
            allProjectGridLines << gridElements
        }
    }


    /**
     * @param projectTasks tasks of one project
     * @return GridElements of one project
     */
    List<GridElement> fromProjectTasks( List<TaskInProject> projectTasks) {
        assert projectTasks
        def gridElements = []
        Date startOfGrid = _getStartOfWeek(getStartOfTasks())
        Date endOfGrid = _getStartOfWeek(getEndOfTasks()) + 7
        Date startOfProject = _getStartOfWeek(projectTasks*.starting.min())
        Date endOfProject = _getStartOfWeek(projectTasks*.ending.max()) + 7
        def fromToDateString = "${_dToS(startOfProject)} - ${_dToS(endOfProject)}"

        Date now = Date.newInstance()
        int row = 0
        for (Date w = startOfGrid; w < endOfGrid; w += 7) {
            if(w <= now && now < w + 7) {
                nowXRowCache = row

            }
            row ++
            if (w >= startOfProject && w < endOfProject) {
                gridElements << new GridElement(projectTasks[0].project, '', "${_getWeekYearStr(w)}  ($fromToDateString)", false)
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
        def projectName = allProjectNames[y]
        List<TaskInProject> project = getProject(projectName)
        project.each {
            it.ending += shift
            it.starting += shift
        }
    }

    @Override
    def toggleIntegrationPhase(int x, int y) {
        // TODO: Write to other model first...
        // TODO: connect integration phase
        allProjectGridLines[y][x].integrationPhase = ! allProjectGridLines[y][x].integrationPhase
    }

    @Override
    def swap(int y, int withY) {
        allProjectNames.swap(y, withY)
        updateGridElements()
    }

    @Override
    int getNowX() {
        nowXRowCache
    }

    @Override
    List<String> getProjectNames() {
        return allProjectNames
    }
}
