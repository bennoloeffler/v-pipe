package transform

import core.TaskInProject
import core.VpipeException
import transform.Transformer

/**
 * Can move all the starting and ending of core.TaskInProject.
 * Based on data that is a map of project names and time shifts in days.
 */
@groovy.transform.InheritConstructors
class DateShiftTransformer extends Transformer {


    /**
     * key: project name
     * value: integer, representing the shift of the project:
     * +7 = delay one week to the future.
     * 0 = no shift.
     * -7 = due date one week earlier.
     */
    Map<String, Integer> projectDayShift


    /**
     * @return the core.TaskInProject List that is transformed
     */
    @Override
    List<TaskInProject> transform() {

        assert projectDayShift != null
        assert plc != null

        description="Dates transformed:\n"
        List<TaskInProject> result = []
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
        if (description == "Dates transformed:\n") {description+"none..."}
        result
    }

}
