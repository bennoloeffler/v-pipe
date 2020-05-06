package transform

import core.TaskInProject
import core.VpipeException
import fileutils.FileDataReaderSupport

/**
 * Can move all the starting and ending of core.TaskInProject.
 * Based on data that is a map of project names and time shifts in days.
 */
@groovy.transform.InheritConstructors
class DateShiftTransformer extends Transformer {

    static String FILE_NAME = "Projekt-Verschiebung.txt"

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

        def allProjects = plc.getAllProjects()
        allProjects.each() { projectName ->
            def projectList = plc.getProject(projectName)
            int shift = projectDayShift[projectName]?:0
            if(shift){description += "$projectName: $shift\n"}
            projectList.each() {
                result << new TaskInProject(it.project, it.starting + shift, it.ending + shift, it.department, it.capacityNeeded)
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
            if(! plc.getProject(it)) {
                throw new VpipeException("ERROR: did not find project elements to shift for project: $it")
            }
        }
        if (description == "Dates transformed:\n") {description+"none..."}
        result
    }

    @Override
    def updateConfiguration() {
        projectDayShift = [:]
        List<String[]> lines = FileDataReaderSupport.getDataLinesSplitTrimmed(FILE_NAME)
        lines.each { line ->
            def errMsg = {"Lesen von Datei $FILE_NAME fehlgeschlagen. Datensatz: ${line}"}
            if(line?.length != 2) {
                throw new VpipeException(errMsg())
            }
            try {
                def project = line[0]
                def days = line[1] as Integer
                projectDayShift[project] = days
            } catch (Exception e) {
                throw new VpipeException(errMsg() + '  Vermutung... Der Tagesversatz ist keine Ganz-Zahl.', e)
            }
        }
    }
}
