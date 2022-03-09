package learn.swingbuilder

import com.formdev.flatlaf.FlatLightLaf
import groovy.beans.Bindable
import groovy.swing.SwingBuilder
import newview.GridDemoModel
import newview.GridModel
import newview.GridPanel
//import view.*

import javax.swing.AbstractListModel
import javax.swing.JFrame
import javax.swing.JSplitPane
import javax.swing.ListModel
import javax.swing.SwingUtilities
import java.awt.BorderLayout as BL
import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

// MVC & SwingBuilder
// See: Console.groovy, ConsoleActions.groovy, ConsoleView.groovy
// Old Doc: https://web.archive.org/web/20140702234351/http://groovy.codehaus.org/GUI+Programming+with+Groovy

// this is the "real model" - there may be a decoupling layer to the view - like a ListModel or a TableModel
class Model {

    @Bindable
    String projectName = '' // otherwise fails at binding to gui

    //@Bindable
    // see: Groovy Goodness: Observable Map and List
    final ObservableList history = []

    def init() {
        projectName = "initial value"
        (1..10).each {history << it.toString()+ "           "+it.toString()}
    }

}

// translate actions to the model
class Controller  {

    View view
    Model model

    def actionPerformedProjectNameChange = { ActionEvent e ->
        model.projectName = e.source.text

        /*
        def clone = model.history.clone() as List<String>
        clone.add(0, model.projectName)
        model.setHistory(clone) // just trigger firePropertyChange
        */

        model.history.add(0, model.projectName)
        //model.firePropertyChange('history', old, model.history)
        //model.firePropertyChange('history', null, null)
        println("actionPerformedProjectNameChange $model.history")
    }


    def actionPerformedProjectNameInit = {ActionEvent e ->
        model.projectName = "INIT"
        model.history.clear()
        model.history << model.projectName
        println('actionPerformedProjectNameInit')
    }


    def actionCommitTheShit = {ActionEvent e ->
        model.projectName = view.swing.txt.text
        model.history << model.projectName
        println('actionPerformedCommit')
    }
}


/**
 * This Panel may be reused
 */
class ReusablePanel {

    // TODO: Actions?

    static def getPanel(SwingBuilder swing) {
        //def swing = new SwingBuilder()

        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255


        swing.build {
            action ( id: 'commitAction',
                    name: "Commit that Shit",
                    mnemonic: 'C',
                    closure: {println "commitAction not connected to controller..."},
                    accelerator: shortcut('C'),
                    shortDescription: 'Eine lange Erklärung, wie ein Commit geht...')
            panel() {
                flowLayout()
                label(id: 'lbl', "project name: ")
                textField(id:'txt', columns:10)
                button('Button'){action:commitAction}
            }
        }
    }
}


// show info and is bound to model-properties therefore
// is decoupled by actionListeners - only actions that trigger direct view changes are done here
// ESPECIALLY:
// Does not know the controller (only actions - that are connected in glue code)
// Does not know the model (only
class View {

    SwingBuilder swing

    Model model

    //
    // adapter models (second layer...)
    //

    GridModel gridDemoModel = new GridDemoModel()

    ListModel getListModel() {
        def result = new AbstractListModel<String>() {
            @Override
            int getSize() {
                model.history.size()
            }

            @Override
            String getElementAt(int index) {
                return model.history[index]
            }

            void propertyChange(PropertyChangeEvent evt) {
                fireContentsChanged(this, 0, getSize()-1)
                //println("propertyChange")
            }
        }
        model.history.addPropertyChangeListener(result as PropertyChangeListener)
        result
    }

    View(Model model) {
        this.model = model
        FlatLightLaf.install()
        build()
    }

    // example!
    // very simple property change listener = SAM single abstract method interface
    def colorListener =  {PropertyChangeEvent evt ->
        println("propertyChange color: $evt")
    } as PropertyChangeListener


    void build() {
        swing = new SwingBuilder()
        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255

        swing.build {
            //lookAndFeel 'nimbus'

            action ( id: 'projectNameInitAction',
                    name: "init project name",
                    mnemonic: 'I',
                    closure: {println "projectNameInit not connected to controller..."},
                    accelerator: shortcut('I'),
                    //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                    shortDescription: 'Eine lange Erklärung, die vielleicht geändert werden muss...'
            )
            action ( id: 'projectNameChangedAction',
                    name: "change project name",
                    mnemonic: 'n',
                    closure: {println "projectNameChange not connected to controller..."},
                    accelerator: shortcut('N'),
                    shortDescription: 'Eine lange Erklärung, die vielleicht geändert werden muss...'
            )


            f = frame(id: 'frame', title: 'example', locationRelativeTo: null, show: true, defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
                menuBar(){
                    menu(text:'File', mnemonic:'F') {
                        menuItem(projectNameInitAction)
                    }
                }



                borderLayout()

                panel(constraints: BL.NORTH) {
                    flowLayout()
                    textLabel = label(text: bind(source: model, sourceProperty: "projectName", converter: { v -> v ? "Projekt-Name: $v" : 'Kein Projekt-Name...' }))
                    container(ReusablePanel.getPanel(swing))
                    button("setColor", actionPerformed: {theList.selectionBackground = cc.color})
                }

                b = button('Click me!', constraints: BL.EAST){action(projectNameInitAction)}

                splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout:true, constraints: BL.WEST) {
                    scrollPane() {
                        colorChooser(id: "cc")
                    }
                    scrollPane(){
                        l = list(id:'theList', model: getListModel())
                    }

                }

                scrollPane(constraints: BL.CENTER) {
                    widget(new GridPanel(20, gridDemoModel))
                }

                t = textField(text: bind(source: model, sourceProperty: "projectName", converter: { v -> v.toLowerCase()}), constraints: BL.SOUTH ) {
                    action(projectNameChangedAction)
                }
            }
            f.pack()

            //b.addActionListener(actionListenerProjectNameInit)
            //t.addActionListener(actionListenerProjectNameChange)
            //cc.addPropertyChangeListener('color', colorListener) // does not work. color does not fire events...

        }
    }

    def start() { swing.edt { swing.frame } }



}


static void main(String[] args) {

    Model model = new Model()
    View view = new View(model)
    Controller controller = new Controller(model: model, view: view)

    // glue code to connect view with controller
    view.swing.projectNameInitAction.closure = controller.&actionPerformedProjectNameInit
    view.swing.projectNameChangedAction.closure = controller.&actionPerformedProjectNameChange
    view.swing.commitAction.closure = controller.&actionCommitTheShit

    // config components
    view.swing.build() {
        bind(target: view.swing.lbl, targetProperty: 'text', source: model, sourceProperty: "projectName", converter: { v -> v.toUpperCase()})
    }



    SwingUtilities.invokeAndWait {
        model.init() // in EDT
    }
    view.start()

}
