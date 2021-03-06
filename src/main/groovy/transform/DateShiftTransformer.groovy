package transform

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import model.DataReader
import model.TaskInProject
import model.VpipeDataException

/**
 * Can move all the starting and ending of model.TaskInProject.
 * Based on data that is a map of project names and time shifts in days.
 */
//@CompileStatic
@InheritConstructors
class DateShiftTransformer extends Transformer {

    /**
     * @return the model.TaskInProject List that is transformed
     */
    @Override
    void transform() {

        assert projectDayShift != null
        assert model != null

        description="Dates transformed:\n"
        List<TaskInProject> result = []

        def allProjects = _getAllProjects()
        allProjects.each() { projectName ->

            // tasks
            def projectList = getProject(projectName)
            int shift = projectDayShift[projectName]?:0
            if(shift){description += "$projectName: $shift\n"}
            projectList.each() {
                result << new TaskInProject(it.project, it.starting + shift, it.ending + shift, it.department, it.capacityNeeded, it.description)
            }
            // pipelining
            if(pipelineElements) {
                def e = getPipelineElement(projectName)
                e.startDate = e.startDate + shift
                e.endDate = e.endDate + shift
            }

        }
/*
        projectDayShift.forEach() { project, shift ->
            def projectList = plc.getProject(project)
            if(projectList) {
                description += "$project: $shift\n"
                projectList.each {
                    result << new TaskInProject(it.project, it.starting + shift, it.ending + shift, it.department, it.capacityNeeded)
                }
            } else {
                throw new VpipeException("ERROR: did not find project elements to shift for $shift days for project: $project")
            }
        }
        */
        projectDayShift.keySet().each {
            if(! getProject(it)) {
                throw new VpipeDataException("Es gibt keine Projektdaten,\n"+
                        "um das Projekt $it zu verschieben.\n"+
                        "Ursache in ${DataReader.get_DATESHIFT_FILE_NAME()}")
            }
        }
        if (description == "Dates transformed:\n") {description+"none..."}

        model.taskList = result

    }


}
