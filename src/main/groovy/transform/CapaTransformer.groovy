package transform

import groovy.transform.InheritConstructors
import model.Model



/**
 * TODO: Can move all the starting and ending of model.TaskInProject.
 * Based on capacity of the departments.
 */
@InheritConstructors
class CapaTransformer extends Transformer {

    /**
     * @return the model.TaskInProject List that is transformed
     */
    @Override
    Model transform() {

        description="Dates transformed:\n"

        return model
    }



}

