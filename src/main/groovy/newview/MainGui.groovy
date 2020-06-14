package newview

import controller.PipelineController
import model.Model

import javax.swing.JFrame
import java.awt.Dimension
import java.awt.Toolkit
import java.beans.PropertyChangeEvent

class MainGui {

    static void main(String[] args) {
        new MainGui().glueAndStart()
    }



    def glueAndStart() {

        Model model = new Model()
        View view = new View(model)
        PipelineController controller = new PipelineController(model, view)

        // glue view and controller together
        view.swing.openAction.closure = controller.&openActionPerformed
        view.swing.saveAction.closure = controller.&saveActionPerformed

        // config components
        view.swing.build() {
            bind(target: view.swing.currentPath, targetProperty: 'text', source: model, sourceProperty: "currentDir", converter: { v -> v.toUpperCase()})
            bind(target: view.gridLoadModel, targetProperty: 'selectedProject', source: view.gridPipelineModel, sourceProperty: 'selectedProject')
            bind(target: view.loadView, targetProperty: 'gridWidth', source: view.pipelineView, sourceProperty: 'grid')
            bind(target: view.gridProjectModel, targetProperty: 'projectName', source: view.gridPipelineModel, sourceProperty: 'selectedProject')
        }


        def selectedProjectChanged = { PropertyChangeEvent e ->
            println(e)
            //view.gridLoadModel.setSelectedProject((String)(e.newValue))
        }
        view.pipelineView.addPropertyChangeListener('grid', selectedProjectChanged)


        view.start() {
            model.readAllData() // in EDT
        }



    }
}
