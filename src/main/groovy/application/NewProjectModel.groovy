package application

import groovy.beans.Bindable
import groovy.transform.CompileStatic
import model.Model
import model.PipelineElement
import model.TaskInProject
import model.WeekOrMonth
import newview.GridElement
import newview.GridModel
import utils.RunTimer

import static extensions.DateHelperFunctions.*

@CompileStatic
class NewProjectModel extends GridModel {

    final Model model

    List<List<GridElement>> allProjectGridLines

    @Bindable
    String projectName =""

    @Bindable
    String departmentName =""

    int nowXRowCache = -1

    List<TaskInProject> project = []

    Closure updateCallback = {
        updateGridElementsFromDomainModel()
        setUpdateToggle(!getUpdateToggle())
    }

    NewProjectModel(Model model) {
        this.model = model
        this.addPropertyChangeListener('projectName', updateCallback)
        model.addPropertyChangeListener('updateToggle', updateCallback)
        updateGridElementsFromDomainModel()
    }


    private void updateGridElementsFromDomainModel() {
        RunTimer.getTimerAndStart('NewProjectModel::updateGridElementsFromDomainModel').withCloseable {

            allProjectGridLines = []
            if (projectName) {
                Date startOfGrid = _getStartOfWeek(model.getStartOfProjects())
                Date endOfGrid = _getStartOfWeek(model.getEndOfProjects()) + 7
                if (model.pipelineElements) {
                    allProjectGridLines << fromPipelineElement(model.getPipelineElement(projectName), startOfGrid, endOfGrid)
                }
                project = model.getProject(projectName)
                project.each {
                    allProjectGridLines << fromTask(it, startOfGrid, endOfGrid)
                }
            }
            allProjectGridLines
        }
    }


    /**
     * @param projectTasks tasks of one project
     * @return GridElements of one project
     */
    List<GridElement> fromTask(TaskInProject projectTask, Date startOfGrid, Date endOfGrid) {
        nowXRowCache = - 1
        assert projectTask
        def gridElements = []
        def deliveryDate = model.getDeliveryDate(projectTask.project)
        Date startOfTask = _getStartOfWeek(projectTask.starting)
        Date endOfTask = _getStartOfWeek(projectTask.ending) + 7
        Date startOfProject = _getStartOfWeek(deliveryDate < projectTask.starting  ? deliveryDate : projectTask.starting)
        Date endOfProject = _getStartOfWeek(deliveryDate > projectTask.ending ? deliveryDate : projectTask.ending ) + 7


        def fromToDateString = "${_dToS(startOfTask)} - ${_dToS(endOfTask)}"

        Date now = new Date()
        int row = 0
        for (Date w = startOfGrid; w < endOfGrid; w += 7) {
            if(w <= now && now < w + 7) {
                nowXRowCache = row
            }
            row ++
            boolean isDeliveryDate = deliveryDate >= w && deliveryDate < w+7

            if (w >= startOfTask && w < endOfTask) {
                gridElements << new GridElement(projectTask.project, projectTask.department, fromToDateString, false, isDeliveryDate)
            } else {
                if (isDeliveryDate) {
                    gridElements << new GridElement(projectTask.project, projectTask.department, fromToDateString, false, isDeliveryDate)
                } else {
                    gridElements << GridElement.nullElement
                }
            }
        }


        return gridElements
    }

    List<GridElement> fromPipelineElement(PipelineElement element, Date startOfGrid, Date endOfGrid) {
        assert element
        def gridElements = []

        Date startOfTask = _getStartOfWeek(element.startDate)
        Date endOfTask = _getStartOfWeek(element.endDate) + 7

        def fromToDateString = "${_dToS(startOfTask)} - ${_dToS(endOfTask)}"

        for (Date w = startOfGrid; w < endOfGrid; w += 7) {
            if (w >= startOfTask && w < endOfTask) {
                gridElements << new GridElement(element.project, 'IP', fromToDateString, true)
            } else {
                gridElements << GridElement.nullElement
            }
        }

        return gridElements
    }


    @Override
    GridElement getElement(int x, int y) {
        def r = allProjectGridLines[y][x]
        r
    }

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
        if(model.pipelineElements) {
            if (y == 0) {
                //Date startP = _getStartOfWeek(project*.starting.min())
                //Date endProject = _getStartOfWeek(project*.ending.max() + 7)
                Date potPipStartDate = model.getPipelineElement(projectName).startDate + shift
                Date potPipEndDate = model.getPipelineElement(projectName).endDate + shift
                //if (potPipStartDate >= startP && potPipEndDate <= endProject) {
                model.getPipelineElement(projectName).startDate = potPipStartDate
                model.getPipelineElement(projectName).endDate = potPipEndDate
                //}
            } else {
                TaskInProject projectTask = project[y - 1]
                projectTask.ending += shift
                projectTask.starting += shift
            }
        } else {
            TaskInProject projectTask = project[y]
            projectTask.ending += shift
            projectTask.starting += shift
        }
    }

    def updateModelRecalcAndFire() {
        model.reCalcCapaAvailableIfNeeded()
        //model.setUpdateToggle(!model.getUpdateToggle())
        model.fireUpdate()
        updateGridElementsFromDomainModel()
    }


    @Override
    def toggleIntegrationPhase(int x, int y) {
    }

    @Override
    def swap(int y, int withY) {
        /*
        def shift = 0
        println("$y $withY")
        if(model.pipelineElements) shift = 1
        if(y-shift >= 0 && withY-shift >= 0) {
            project.swap(y - shift, withY - shift)
        }
        updateGridElementsFromDomainModel()
        */
    }

    @Override
    def setSelectedElement(int x, int y) {
        setDepartmentName(getLineNames()[y])

    }

    @Override
    int getNowX() {
        nowXRowCache
    }

    @Override
    List<String> getLineNames() {
        def r =[]
        if(model.pipelineElements) {r << 'IP'}
        project.each {r << it.department}
        r
    }

    @Override
    List<String> getColumnNames() {
        return model.getFullSeriesOfTimeKeys(WeekOrMonth.WEEK)
    }

    @Override
    List<String> getDetailsForTooltip(int x, int y) {
        List<String> result = []
        result.add("${lineNames[y]} ${columnNames[x]}" as String)
        def shift = 0
        if(model.pipelineElements) shift = 1
        if(y-shift == -1) {
            result.add("${model.getPipelineElement(projectName).pipelineSlotsNeeded}" as String)
        } else {
            def d = project[y - shift].description
            d = (d == null || d == "")?"keine Task-Info":d
            result.add("${project[y - shift].capacityNeeded}" as String)
            result.add("$d" as String)
        }
        result
    }

    @Override
    def makeSmaller(int y) {
        shiftSize(y, -7)
        updateModelRecalcAndFire()
    }

    @Override
    def makeBigger(int y) {
        shiftSize(y, 7)
        updateModelRecalcAndFire()
    }

    def shiftSize(int y, int shift) {
        if(model.pipelineElements) {
            if (y == 0) {
                //Date startP = _getStartOfWeek(project*.starting.min())
                //Date endProject = _getStartOfWeek(project*.ending.max() + 7)
                Date potPipStartDate = model.getPipelineElement(projectName).startDate + shift
                Date potPipEndDate = model.getPipelineElement(projectName).endDate - shift
                if(potPipStartDate >= potPipEndDate) {
                    potPipStartDate = model.getPipelineElement(projectName).startDate
                    potPipEndDate = model.getPipelineElement(projectName).endDate
                }
                //if (potPipStartDate >= startP && potPipEndDate <= endProject) {
                model.getPipelineElement(projectName).startDate = potPipStartDate
                model.getPipelineElement(projectName).endDate = potPipEndDate
                //}
            } else {
                TaskInProject projectTask = project[y - 1]
                if(projectTask.ending + shift > projectTask.starting - shift) {
                    projectTask.ending += shift
                    projectTask.starting -= shift
                }
            }
        } else {
            TaskInProject projectTask = project[y]
            if(projectTask.ending + shift > projectTask.starting - shift) {
                projectTask.ending += shift
                projectTask.starting -= shift
            }
        }
    }

}
