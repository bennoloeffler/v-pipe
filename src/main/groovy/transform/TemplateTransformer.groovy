package transform


import groovy.transform.InheritConstructors
import model.DataReader
import model.TaskInProject
import model.VpipeDataException

@InheritConstructors
class TemplateTransformer extends Transformer {

    @Override
    void transform() {
        model.templateProjects.each {

            //tasks
            String templatedProject = it[0]
            String originalProject = it[1]
            Integer dayShift = it[2]

            List<TaskInProject> p = getProject(originalProject)
            if(p) {
                List<TaskInProject> newProject = p.collect { it.cloneFromTemplate(templatedProject, dayShift) }
                taskList.addAll(newProject)
            } else {
                throw new VpipeDataException("Das Template-Projekt: $templatedProject\nin der Datei ${DataReader.get_SCENARIO_FILE_NAME()} \nbezieht sich auf ein nicht existierendes\nOriginal-Projekt: $originalProject")
            }

            // pipelining
            if(pipelineElements) {
                def e = getPipelineElement(originalProject)
                pipelineElements << e.cloneFromTemplate(templatedProject, dayShift)
            }
        }
    }
}
