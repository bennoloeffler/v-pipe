package learn.swingbuilder


import com.formdev.flatlaf.FlatLightLaf
import groovy.beans.Bindable
import groovy.swing.SwingBuilder

import javax.swing.*
import javax.swing.colorchooser.ColorSelectionModel
import javax.swing.event.ChangeEvent
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

    //@Bindable (NOT NEEDED! in the model. Visualisation things stay there. Even if they include state...)
    //Color highlight = Color.BLUE

    //@Bindable
    // see: Groovy Goodness: Observable Map and List
    final ObservableList history = []

    def init() {
        projectName = "initial value pn"
        (1..5).each { history << it.toString() }
    }

}

class HistoryListModel extends AbstractListModel<String> {

    Model model

    HistoryListModel(Model model) {
        this.model = model
        model.history.addPropertyChangeListener(this as PropertyChangeListener)
    }

    @Override
    int getSize() {
        model.history.size()
    }

    @Override
    String getElementAt(int index) {
        return model.history[index]
    }

    void propertyChange(PropertyChangeEvent evt) {
        fireContentsChanged(this, 0, getSize() - 1)
    }
}


// translate actions to the model
class Controller {

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


    def historyResetActionPerformed = { ActionEvent e ->
        model.projectName = "firstNewEntry"
        model.history.clear()
        model.history << model.projectName
        println('historyResetActionPerformed')
    }


    def actionCommitTheShit = { ActionEvent e ->
        model.projectName = view.swing.txt.text
        model.history << model.projectName
        println('actionPerformedCommit')
    }

}


/**
 * This Panel may be reused
 */
class ReusablePanel {

    static def getPanel(SwingBuilder swing) {

        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255
        swing.build {
            action(id: 'commitAction',
                    name: "Commit that Shit",
                    mnemonic: 'C',
                    closure: { println "commitAction not connected to controller..." },
                    accelerator: shortcut('C'),
                    shortDescription: 'Eine lange Erklärung, wie ein Commit geht...')
            panel(id: "abc") {
                flowLayout(id: "fl")
                //label(id: 'lbl', "project name: ")
                textField(id: 'txt', columns: 10)
                button(action: commitAction)
            }
        }
    }
}


class View {

    SwingBuilder swing

    // just for binding!
    Model model

    //
    // adapter models (second layer...)
    //

    ListModel historyListModel

    View(Model model) {
        this.model = model
        historyListModel = new HistoryListModel(model)
        FlatLightLaf.install()
        build()
    }

    // example!
    // very simple property change listener = SAM single abstract method interface
    def colorListener = { ChangeEvent evt ->
        println("propertyChange color: $evt")
        swing.theList.selectionBackground = swing.cc.color
    }


    void build() {
        swing = new SwingBuilder()
        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255

        swing.build {
            //lookAndFeel 'nimbus'

            action(id: 'resetHistoryAction',
                    name: "reset history",
                    mnemonic: 'R',
                    closure: { println "projectNameInit not connected to controller..." },
                    accelerator: shortcut('R'),
                    //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                    shortDescription: 'Eine lange Erklärung, die vielleicht geändert werden muss...'
            )
            action(id: 'projectNameChangedAction',
                    name: "change project name",
                    mnemonic: 'n',
                    closure: { println "projectNameChange not connected to controller..." },
                    accelerator: shortcut('N'),
                    shortDescription: 'Eine lange Erklärung, die vielleicht geändert werden muss...'
            )


            f = frame(id: 'frame', title: 'example', locationRelativeTo: null, show: true, defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
                menuBar() {
                    menu(text: 'File', mnemonic: 'F') {
                        menuItem(resetHistoryAction)
                    }
                }


                borderLayout()

                panel(constraints: BL.NORTH) {
                    flowLayout()
                    textLabel = label(text: bind(source: model, sourceProperty: "projectName", converter: { v -> v ? "Projekt-Name: $v" : 'Kein Projekt-Name...' }))
                    hstrut(width: 50)
                    container(ReusablePanel.getPanel(swing))
                    button("setColor", actionPerformed: { theList.selectionBackground = cc.color }) // WE DONT NEED THAT IN MODEL
                }

                b = button('Click me!', constraints: BL.EAST, action: resetHistoryAction)

                //splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, constraints: BL.WEST) {
                    scrollPane(constraints: BL.CENTER) {
                        colorChooser(id: "cc")
                    }
                    scrollPane(constraints: BL.WEST) {
                        l = list(id: 'theList', model: historyListModel)
                    }

                //}



                t = textField(text: bind(source: model, sourceProperty: "projectName", converter: { v -> v.toUpperCase() }), constraints: BL.SOUTH) {
                    action(projectNameChangedAction)
                }
            }
            //bind(target: theList, targetProperty: 'selectionBackground', source: cc.getSelectionModel(), sourceProperty: "color")
            f.pack()

            //b.addActionListener(actionListenerProjectNameInit)
            //t.addActionListener(actionListenerProjectNameChange)
            JColorChooser c = cc
            ColorSelectionModel csm = c.getSelectionModel()
            csm.addChangeListener(colorListener)
            JList ll = l
            ll.addPropertyChangeListener({ll.setSelectedIndex(ll.getModel().size - 1)})

        }
    }

    def start() { swing.edt { swing.frame } }

}


static void main(String[] args) {

    Model model = new Model()
    View view = new View(model)
    Controller controller = new Controller(model: model, view: view)

    // glue code to connect view with controller
    view.swing.resetHistoryAction.closure = controller.&historyResetActionPerformed
    view.swing.projectNameChangedAction.closure = controller.&actionPerformedProjectNameChange
    view.swing.commitAction.closure = controller.&actionCommitTheShit

    // config components
    view.swing.build() {
    }


    SwingUtilities.invokeAndWait {
        model.init() // in EDT
    }
    view.start()

}
