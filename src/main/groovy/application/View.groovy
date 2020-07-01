
package application

import com.formdev.flatlaf.FlatLightLaf
import groovy.swing.SwingBuilder
import model.Model
import model.WeekOrMonth
import net.miginfocom.swing.MigLayout
import newview.GridModel
import newview.GridPanel
import newview.NewLoadPanel

import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JSplitPane
import javax.swing.JTextArea
import java.awt.Dimension
import java.awt.Font
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
    //GridLoadModel gridMonthLoadModel = new GridLoadModel(model, WeekOrMonth.MONTH)
    GridPipelineLoadModel gridPipelineLoadModel = new GridPipelineLoadModel(model)
    GridModel gridProjectModel = new NewProjectModel(model)

    def pipelineView = new GridPanel(20, gridPipelineModel)
    def projectView = new GridPanel(20, gridProjectModel)
    def loadView = new NewLoadPanel(20, gridLoadModel)
    def pipelineLoadView = new NewLoadPanel(20, gridPipelineLoadModel)


    View(Model model) {
        this.model = model
        build()

        // TODO: move to Bilder?
        pipelineView.name = "pipelineView" // in order to have a specific name in "paintComponent... getRuntimer(...)"
        projectView.name = "projectView"
        loadView.name = "departmentLoad"
        pipelineLoadView.name = "pipelineLoad"
    }

    /*
    void log(String logMessage) {
        def jta = (JTextArea)(swing.textAreaLog)
        //jta.setFont(new Font("Monospaced", jta.getFont().getStyle(), 14));
        jta.append('\n'+logMessage+'\n')
    }*/



    void build() {

        FlatLightLaf.install()


        //MigLayout ml = new MigLayout()
        swing = new SwingBuilder()
        swing.registerBeanFactory('migLayout', MigLayout)



        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255
        swing.actions {


            // file

            action ( id: 'openAction',
                    name: "oeffnen",
                    mnemonic: 'o',
                    closure: {println "openAction not connected to application..."},
                    accelerator: shortcut('O'),
                    focus: JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                    //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                    shortDescription: 'Verzeichnis mit Daten-Datein öffnen - alle Dateien darin'
            )
            action ( id: 'saveAction',
                    name: "speichern",
                    mnemonic: 's',
                    closure: {println "saveAction not connected to application..."},
                    accelerator: shortcut('S'),
                    shortDescription: 'alle Daten ins aktuelle Daten-Verzeichnis sichern'
            )
            action ( id: 'saveAsAction',
                    name: "speichern als...",
                    //mnemonic: 's',
                    closure: {println "saveAsAction not connected to application..."},
                    //accelerator: shortcut('S'),
                    shortDescription: 'anderes Daten-Verzeichnis wählen und Dateien speichern'
            )

            action ( id: 'exitAction',
                    name: "beenden",
                    //mnemonic: 'p',
                    closure: {println "exitAction not connected to application..."},
                    //accelerator: shortcut('P'),
                    shortDescription: 'v-pipe beenden - und vorher nochmal speichern ;-)'
            )

            // tools

            action ( id: 'sortPipelineAction',
                    name: "Staffelung (P) sortieren",
                    mnemonic: 'p',
                    closure: {println "sortPipelineAction not connected to application..."},
                    accelerator: shortcut('P'),
                    shortDescription: 'Staffelung sortieren dem spätesten End-Termin des letzten Tasks des Projektes'
            )


            // view

            action ( id: 'pipelineViewAction',
                    name: "Staffelungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: {println "pipelineViewAction not connected to application..."},
                    //accelerator: shortcut('P'),
                    shortDescription: 'Staffelung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            action ( id: 'loadViewAction',
                    name: "Abt-Belastungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: {println "loadViewAction not connected to application..."},
                    //accelerator: shortcut('P'),
                    shortDescription: 'Abt-Belastung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )


            action ( id: 'pipelineLoadViewAction',
                    name: "IP-Belastungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: {println "pipelineLoadViewAction not connected to application..."},
                    //accelerator: shortcut('P'),
                    shortDescription: 'IP-Belastung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            // help

            action ( id: 'helpAction',
                    name: "Hilfe...",
                    //mnemonic: 'p',
                    closure: {println "helpPerformanceAction not connected to application..."},
                    //accelerator: shortcut('P'),
                    shortDescription: 'zeige die aktuelle Hilfe-Datei im Inter-Netz. Online :-(. Aktuelle Version...'
            )


            action ( id: 'printPerformanceAction',
                    name: "Performance messen",
                    //mnemonic: 'p',
                    closure: {println "printPerformanceAction not connected to application..."},
                    //accelerator: shortcut('P'),
                    shortDescription: 'gemessene Lauf-Zeiten auf Console printen'
            )



        }

        swing.build {

            //lookAndFeel 'nimbus'

            f = frame(id: 'frame', title: 'v-pipe    |  +/- = Zoom  |  Pfeile = Cursor bewegen  |  Shift+Pfeile = Projekt bewegen  | CTRL-o = öffnen  ', locationRelativeTo: null, show: false, defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE) {

                menuBar(id: 'menuBar') {
                    menu(text:'Dateien', mnemonic:'D') {
                        menuItem(openAction)
                        menuItem(saveAction)
                        menuItem(saveAsAction)
                        menuItem(exitAction)
                    }
                    menu(text:'Werkzeug', mnemonic:'W') {
                        menuItem(sortPipelineAction)
                        //menuItem(viewPipelineLoadAction)
                    }
                    menu(text:'Ansicht', mnemonic:'A') {
                        menuItem(pipelineViewAction)
                        //menuItem(projectViewAction)
                        menuItem(loadViewAction)
                        menuItem(pipelineLoadViewAction)
                    }
                    menu(text:'Hilfe', mnemonic:'H') {
                        menuItem(helpAction)
                        menuItem(printPerformanceAction)
                    }
                }

                //migLayout(layoutConstraints:"fill, debug", columnConstraints:"", rowConstraints:"[][grow]")
                migLayout(layoutConstraints:"fill", columnConstraints:"[][][][][][grow]", rowConstraints:"[][grow]")
                label("Projekt markieren: ")
                textField(id: 'searchTextField', columns: 20, constraints: 'width 100')
                label("    Zeit-Markierung: ")
                label("-----W--", id: 'timeLabel')
                label("    Pfad: ")
                label(id:'currentPath', constraints:  'wrap')

                splitPane(id: 'spH', orientation: JSplitPane.HORIZONTAL_SPLIT, continuousLayout:true, dividerLocation: 0.5, constraints: 'grow, span') {
                    splitPane(id: 'spV1', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 0.5) {
                        scrollPane {
                            widget(pipelineView)
                        }

                        splitPane(id: 'spV3', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 0.5) {
                            scrollPane(id: 'pipelineLoadViewScrollPane') {
                                widget( pipelineLoadView)
                            }
                            scrollPane(id: 'spSwap') {
                                widget(loadView)
                            }
                        }
                        // toggeling view...
                        // spV1.setSecond(view.swing.sp-swap) // only load, without pipeline
                        // spV1.setSecond(view.swing.spV3) // both

                    }
                    splitPane(id: 'spV2', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 0.5) {
                        scrollPane {
                            widget(projectView)
                        }
                        scrollPane {
                            textArea(id: 'textAreaLog', editable: false)
                        }

                    }
                }

            }
        }
    }


    def setFullSize(JFrame f) {
        f.setSize((int)(screenDimension.width), (int)(screenDimension.height - 30))
        f.setLocation(0, 0)
    }


    def start(Closure initModelAndNotifyView) {
        swing.edt {
            //swing.frame
            initModelAndNotifyView()
            swing.frame.setVisible(true)
            setFullSize(swing.frame)
            // TODO: move to Builder?
            swing.spH.setDividerLocation(900)
            swing.spV1.setDividerLocation(500)
            swing.spV2.setDividerLocation(500)
            swing.spV3.setDividerLocation(120)
        }
    }

    int i = 0

    def openPipelineWindow() {
        def newPipelineView = new GridPanel(20, gridPipelineModel)
        swing.edt {

            frame(id: "framePipeline+${i++}", title: "v-pipe: Staffelung", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane {
                    widget(newPipelineView)
                }
            }
            bind(target: newPipelineView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newPipelineView, sourceProperty: "cursorX")
        }
    }

    def openLoadWindow() {
        def newLoadView = new NewLoadPanel(20, gridLoadModel)
        swing.edt {
            frame(id: "frameLoad+${i++}", title: "v-pipe: Abt.-Belastung", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane {
                    widget(newLoadView)
                }
            }
            bind(target: newLoadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newLoadView, sourceProperty: "cursorX")
        }
    }


    def openPipelineLoadWindow() {
        def newLoadView = new NewLoadPanel(20, gridPipelineLoadModel)
        swing.edt {
            frame(id: "framePipelineLoad+${i++}", title: "v-pipe: IP-Belastung", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane {
                    widget(newLoadView, name: "monthLoad$i++")
                }
            }
            bind(target: newLoadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newLoadView, sourceProperty: "cursorX")
        }
    }
}
