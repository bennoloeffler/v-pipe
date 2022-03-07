package application

import groovy.beans.Bindable
import groovyx.gpars.GParsPool
import model.Model
import model.PipelineElement
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

    @Bindable String selectedProject
    List<List<GridElement>> allProjectGridLines
    int nowXRowCache = -1
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
        updateGridElements()
    }

    void sortProjectNamesToEnd() {

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

    //def threadPool = Executors.newFixedThreadPool(4)

    /**
     * create "the model" List<List<GridElement>> allProjectGridLines
     * from task-portfolio
     */
    private void updateGridElements() {
        allProjectGridLines = []
        RunTimer.getTimerAndStart('NewPipelineModel::updateGridElements').withCloseable {
            if (model.taskList) {
                Date startOfGrid = _getStartOfWeek(model.getStartOfProjects())
                Date endOfGrid = _getStartOfWeek(model.getEndOfProjects()) + 7

                /*
                model.projectSequence.each {
                    List<TaskInProject> projectTasks = model.getProject(it)
                    def gridElements = fromProjectTasks(projectTasks, startOfGrid, endOfGrid)
                    allProjectGridLines << gridElements
                }*/

                GParsPool.withPool {
                    allProjectGridLines =
                            model.projectSequence.collectParallel { String projectName ->
                                //println "parallel for: " + projectName
                                List<TaskInProject> projectTasks = model.getProject(projectName)
                                fromProjectTasks(projectTasks, startOfGrid, endOfGrid)
                            } as List<List<GridElement>>
                }
            }
        }
    }


    /**
     * @param projectTasks tasks of one project
     * @return GridElements of one project
     */
    //@Memoized
    List<GridElement> fromProjectTasks( List<TaskInProject> projectTasks, startOfGrid, endOfGrid) {
        def gridElements = []
        try {
            assert projectTasks
            //Date startOfGrid = _getStartOfWeek(model.getStartOfTasks())
            //Date endOfGrid = _getStartOfWeek(model.getEndOfTasks()) + 7
            Date startOfTasks = _getStartOfWeek(projectTasks*.starting.min())
            Date endOfTasks = _getStartOfWeek(projectTasks*.ending.max()) + 7
            def deliveryDate = model.getDeliveryDate(projectTasks[0].project)
            def startOfProject = _getStartOfWeek(startOfTasks < deliveryDate ? startOfTasks : deliveryDate)
            def endOfProject = _getStartOfWeek(endOfTasks > deliveryDate ? endOfTasks : deliveryDate)

            def fromToDateString = "${_dToS(startOfTasks)} - ${_dToS(endOfTasks)}"

            Date now = new Date() // Date.newInstance()
            int row = 0
            for (Date w = startOfGrid; w < endOfGrid; w += 7) {
                if(w <= now && now < w + 7) {
                    nowXRowCache = row
                }
                row ++
                if (w >= startOfProject && w <= endOfProject) {
                    boolean integrationPhase = false
                    if(model.pipelineElements) {
                        PipelineElement element = model.getPipelineElement(projectTasks[0].project)
                        long overlap = element.getDaysOverlap(w, w+7)
                        if(overlap){ integrationPhase = true }
                    }
                    boolean isDeliveryDate = deliveryDate >= w && deliveryDate < w+7
                    if (w >= startOfTasks && w < endOfTasks) {
                        gridElements << new GridElement(
                                project: projectTasks[0].project,
                                department: '',
                                timeString: fromToDateString,
                                integrationPhase: integrationPhase,
                                deliveryDate: isDeliveryDate
                        )
                    } else {
                        if (isDeliveryDate || integrationPhase) {
                            gridElements << new GridElement(
                                    project: projectTasks[0].project,
                                    department: '',
                                    timeString: fromToDateString,
                                    integrationPhase: integrationPhase,
                                    deliveryDate: isDeliveryDate
                            )
                        } else {
                            gridElements << GridElement.nullElement
                        }
                    }
                } else {
                    gridElements << GridElement.nullElement
                }
            }
        } catch (Exception e) {
            println e
        }
        //String line = gridElements.collect {GridElement e -> (" - " + (e == GridElement.nullElement ? "X" : "P") +(e.isIntegrationPhase() ? "I" : "") + (e.isDeliveryDate() ? "D" : "") ) }.join("")
        //println line
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
        if(model.pipelineElements) {
            PipelineElement pe = model.getPipelineElement(projectName)
            pe.startDate += shift
            pe.endDate += shift
        }

        model.reCalcCapaAvailableIfNeeded()
        //model.setUpdateToggle(!model.getUpdateToggle())
        model.fireUpdate()
    }

    @Override
    def toggleIntegrationPhase(int x, int y) {
        allProjectGridLines[y][x].integrationPhase = ! allProjectGridLines[y][x].integrationPhase
    }

    @Override
    def swap(int y, int withY) {
        model.projectSequence.swap(y, withY)
        //updateGridElements()
        //this.setUpdateToggle(!this.getUpdateToggle()) // just to fire an PropertyChange to the view
        model.fireUpdate()
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

    @Override
    List<String> getDetailsForTooltip(int x, int y) {
        def result = []
        result << "${lineNames[y]} ${columnNames[x]}"
        def load = model.getProject(lineNames[y])
                .stream()
                .map({it.capacityNeeded})
                .reduce({a, b -> a + b }).get()
        result << "$load"
        result
    }

    @Override
    def makeSmaller(int y) {
    }

    @Override
    def makeBigger(int y) {
    }

}
