
package newview

import groovy.swing.SwingBuilder
import model.Model
import net.miginfocom.swing.MigLayout

import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JSplitPane
import java.awt.Dimension
import java.awt.Toolkit

class View {

    Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize()

    SwingBuilder swing

    Model model

    //
    // adapter models (second layer...)
    //

    GridModel gridPipelineModel = new NewPipelineModel(model)
    GridLoadModel gridLoadModel = new GridLoadModel(model)
    GridModel gridProjectModel = new NewProjectModel(model)

    def pipelineView = new GridPanel(20, gridPipelineModel)
    def projectView = new GridPanel(20, gridProjectModel)
    def loadView = new NewLoadPanel(20, gridLoadModel)


    View(Model model) {
        this.model = model
        build()
    }


    void build() {

        MigLayout ml = new MigLayout()
        swing = new SwingBuilder()
        swing.registerBeanFactory('migLayout', MigLayout)



        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255
        swing.actions {

            action ( id: 'openAction',
                    name: "Open data folder",
                    mnemonic: 'o',
                    closure: {println "openAction not connected to controller..."},
                    accelerator: shortcut('O'),
                    focus: JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                    //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                    shortDescription: 'Verzeichnis wechseln, in dem die Daten-Dateien liegen'
            )
            action ( id: 'saveAction',
                    name: "Save all data files",
                    mnemonic: 's',
                    closure: {println "saveAction not connected to controller..."},
                    accelerator: shortcut('S'),
                    shortDescription: 'alle Daten ins akuelle Verzeichnis sichern'
            )

        }

        swing.build {

            lookAndFeel 'nimbus'

            f = frame(id: 'frame', title: 'Pipeline', locationRelativeTo: null, show: false, defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {

                menuBar{
                    menu(text:'File', mnemonic:'F') {
                        menuItem(openAction)
                        menuItem(saveAction)
                    }
                }

                //migLayout(layoutConstraints:"fill, debug", columnConstraints:"", rowConstraints:"[][grow]")
                migLayout(layoutConstraints:"fill", columnConstraints:"", rowConstraints:"[][grow]")

                label(id:'currentPath', constraints:  'wrap')

                splitPane(orientation: JSplitPane.HORIZONTAL_SPLIT, continuousLayout:true, dividerLocation: 0.5, constraints: 'grow') {
                    splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 0.5) {
                        scrollPane {
                            widget(pipelineView)
                        }
                        scrollPane {
                            widget(loadView)
                        }

                    }
                    splitPane(orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 0.5) {
                        scrollPane {
                            widget(projectView)
                        }
                        scrollPane {
                            textArea(id: 'textAreaLog', "Log text goes here")
                        }

                    }
                }

            }
            //f.pack()

            //b.addActionListener(actionListenerProjectNameInit)
            //t.addActionListener(actionListenerProjectNameChange)
            //cc.addPropertyChangeListener('color', colorListener) // does not work. color does not fire events...

        }
    }


    def setFullSize(JFrame f) {
        f.setSize((int)(screenDimension.width), (int)(screenDimension.height - 30))
        f.setLocation(0, 0)
    }


    def start(Closure initModel) {
        swing.edt {
            //swing.frame
            setFullSize(swing.frame)
            initModel()
            swing.frame.setVisible(true)
        }
    }



}
