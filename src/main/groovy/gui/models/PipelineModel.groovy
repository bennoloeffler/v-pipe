package gui.models

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovyx.gpars.GParsPool
import gui.models.GridElement
import gui.models.GridModel
import model.Model
import model.PipelineElement
import model.TaskInProject
import model.WeekOrMonth
import utils.RunTimer

import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

import static extensions.DateHelperFunctions._dToS
import static extensions.DateHelperFunctions._getStartOfWeek

@CompileStatic
class PipelineModel extends GridModel {

    @Bindable String selectedProject
    List<List<GridElement>> allProjectGridLines
    int nowXRowCache = -1
    Model model


    def tasksPropertyListener = { PropertyChangeEvent e ->
        updateGridElements()
        this.setUpdateToggle(!this.getUpdateToggle()) // just to fire an PropertyChange to the view
    }


    PipelineModel(Model model) {
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


    /**
     * create "the model" List<List<GridElement>> allProjectGridLines
     * from task-portfolio
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    private void updateGridElements() {
        allProjectGridLines = []
        RunTimer.getTimerAndStart('NewPipelineModel::updateGridElements').withCloseable {
            if (model.taskList) {
                Date startOfGrid = _getStartOfWeek(model.getStartOfProjects())
                Date endOfGrid = _getStartOfWeek(model.getEndOfProjects()) + 7
                GParsPool.withPool {
                    allProjectGridLines =
                            model.projectSequence.collectParallel { String projectName ->
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
    List<GridElement> fromProjectTasks( List<TaskInProject> projectTasks, Date startOfGrid, Date endOfGrid) {
        def gridElements = []
        try {
            assert projectTasks
            Date startOfTasks = _getStartOfWeek(projectTasks*.starting.min())
            Date endOfTasks = _getStartOfWeek(projectTasks*.ending.max()) + 7
            def deliveryDate = model.getDeliveryDate(projectTasks[0].project)
            def startOfProject = _getStartOfWeek(startOfTasks < deliveryDate ? startOfTasks : deliveryDate)
            def endOfProject = _getStartOfWeek(endOfTasks > deliveryDate ? endOfTasks : deliveryDate)
            def fromToDateString = "${_dToS(startOfTasks)} - ${_dToS(endOfTasks)}"
            Date now = new Date()
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
        model.fireUpdate()
    }

    @Override
    /*
    def toggleIntegrationPhase(int x, int y) {
        allProjectGridLines[y][x].integrationPhase = ! allProjectGridLines[y][x].integrationPhase
    }
     */

    @Override
    def swap(int y, int withY) {
        model.projectSequence.swap(y, withY)
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
    Map<String, String> getDetailsForTooltip(int x, int y) {
        Map<String, String> result = [:]
        result['line-row-idx'] = "${lineNames[y]} ${columnNames[x]}" as String
        def load = model.getProject(lineNames[y])
                .stream()
                .map({it.capacityNeeded})
                .reduce({a, b -> a + b }).get()
        result['capa'] = "$load" as String
        result
    }

    @Override
    def makeSmaller(int y) {
    }

    @Override
    def makeBigger(int y) {
    }

}
