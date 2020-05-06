package transform

import core.ProjectDataToLoadCalculator
import core.TaskInProject

/**
 * works on the list of TaskInProjects to change something
 */
abstract class Transformer {

    /**
     * has to be set before call to transform
     */
    ProjectDataToLoadCalculator plc

    String description

    /**
     * make shure to have a core.ProjectDataToLoadCalculator
     */
    Transformer(ProjectDataToLoadCalculator  plc) {
        this.plc = plc
    }

    /**
     * needs to do the work and needs to fill description
     * @return the work of transformer
     */
    abstract List<TaskInProject> transform ()

    /**
     * has to be filled during transform
     * @return string to describe the transformation done
     */
    String getDescription() {
        assert description
        return description
    }

    /**
     * hint, that config may have changed and to urgently re-read it before next operation
     * @return
     */
    abstract def updateConfiguration()
}
