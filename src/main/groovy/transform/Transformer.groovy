package transform

import model.Model

/**
 * works on the model to change something
 */
abstract class Transformer {

    /**
     * has to be set before call to transform
     */
    @Delegate
    Model model

    String description

    /**
     * make shure to have a core.ProjectDataToLoadCalculator
     */
    Transformer(Model model) {
        this.model = model
    }

    /**
     * needs to do the work and needs to fill description
     * @return the work of transformer
     */
    abstract void transform ()

    /**
     * has to be filled during transform
     * @return string to describe the transformation done
     */
    String getDescription() {
        assert description
        return description
    }

}
