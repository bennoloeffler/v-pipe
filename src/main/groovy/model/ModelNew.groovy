package model

import groovy.beans.Bindable
import groovy.transform.AutoClone
import groovy.transform.AutoCloneStyle
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.pcollections.*

@Canonical
@AutoClone(style=AutoCloneStyle.COPY_CONSTRUCTOR)
class TaskOfProject implements StartEndInterval {
    double capacityNeed
    String description
}

@Canonical
@AutoClone(style=AutoCloneStyle.COPY_CONSTRUCTOR)
class Project {
    String name
    Date deliveryDate
    PVector<TaskOfProject> allTasks
    //PVector<TaskOfProject> getTasks(){allTasks}
    TaskOfProject integrationPhase
    /*def setTasks(List<TaskOfProject> tasks) {
        allTasks = TreePVector.from(tasks)
    }
     */
}

@Canonical
class CapacityOfferPerWeek {
    double greenUpperLimit
    double yellowUpperLimit
}

@Canonical
class Department {
    String name
}

@CompileStatic
class ModelNew {

    @Delegate
    DayIntervalCalculator dayIntervallCalculator = new DayIntervalCalculator()

    @Bindable
    boolean updateToggle

    def fireUpdate(){
        setUpdateToggle(!updateToggle)
    }

    private PMap data = HashTreePMap.empty()
    private PVector<PMap> undoBuffer = TreePVector.empty()
    private PVector<PMap> redoBuffer = TreePVector.empty()

    ModelNew() {
        data = data.plus("projects", HashTreePMap.empty())
        data = data.plus("departments", HashTreePMap.empty())
        data = data.plus("pipelineMaxElements", 0) //indicates: no pipeline
        createHistoryEntryForUndo()
    }

    def createHistoryEntryForUndo() {
        undoBuffer = undoBuffer + data
        redoBuffer = TreePVector.empty()
    }

    def undo(){
        if (undoBuffer.size() > 0) {
            int prevDataIdx = undoBuffer.size()-1
            redoBuffer = redoBuffer + data
            data = undoBuffer[prevDataIdx]
            undoBuffer = undoBuffer.minus(prevDataIdx)
            fireUpdate()
        }
    }

    def redo() {
        if (redoBuffer.size() > 0) {
            int nextDataIdx = redoBuffer.size()-1
            undoBuffer = undoBuffer + data
            data = redoBuffer[nextDataIdx]
            redoBuffer = redoBuffer.minus(nextDataIdx)
            fireUpdate()
        }
    }

    PMap<String, Project> getProjects() {
        data["projects"]
    }

    PMap<String, Department> getDepartments() {
        data["departments"]
    }

    /**
     * Create or change project - based on stable project name.
     * To change the project name, first delete it, then call setProject.
     */
    def setProject(Project p) {
        data = data.plus("projects", projects.plus(p.name, p))
        fireUpdate()
    }

    def removeProject(Project p) {
        createHistoryEntryForUndo()
        data = data.plus("projects", projects - p.name)
        fireUpdate()
    }

    def setAllTasks(Project p, List<TaskOfProject> ts) {
        createHistoryEntryForUndo()
        p.allTasks = TreePVector.from(ts)
        expandMinMaxDate(ts as List<StartEndInterval>)
        data = data.plus("projects", projects.plus(p.name, p))
        fireUpdate()
    }

}
