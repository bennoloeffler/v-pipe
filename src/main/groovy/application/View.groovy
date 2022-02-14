
package application

import com.formdev.flatlaf.FlatLightLaf
import groovy.swing.SwingBuilder
import model.Model
import model.TaskInProject
import model.WeekOrMonth
import net.miginfocom.swing.MigLayout
import newview.FileDifferPanel
import newview.GridModel
import newview.GridPanel
import newview.NewLoadPanel

import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.UIManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Image
import java.awt.TextField
import java.awt.Toolkit

import static java.awt.Color.*

class View {

    Image frameIcon
    Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize()
    SwingBuilder swing
    Model model

    //
    // adapter models (second layer...)
    //
    NewPipelineModel gridPipelineModel = new NewPipelineModel(model)
    GridLoadModel gridLoadModel = new GridLoadModel(model)
    GridPipelineLoadModel gridPipelineLoadModel = new GridPipelineLoadModel(model)
    GridModel gridProjectModel = new NewProjectModel(model)

    def pipelineView = new GridPanel(20, gridPipelineModel)
    def projectView = new GridPanel(20, gridProjectModel)
    def loadView = new NewLoadPanel(20, gridLoadModel)
    def pipelineLoadView = new NewLoadPanel(20, gridPipelineLoadModel)
    def projectDetails
    FileDifferPanel fileDifferPanel
    ProjectTemplates projectTemplates

    View(Model model) {
        this.model = model
        frameIcon = new ImageIcon(getClass().getResource("/icons/vunds_icon_64x64_t.png")).getImage()
        swing = new SwingBuilder()
        fileDifferPanel = new FileDifferPanel(swing)
        projectTemplates = new ProjectTemplates(this)
        build()
        projectDetails = new ProjectDetails(this)


        pipelineView.name = "pipelineView" // in order to have a specific name in "paintComponent... getRuntimer(...)"
        projectView.name = "projectView"
        loadView.name = "departmentLoad"
        pipelineLoadView.name = "pipelineLoad"

    }


    void build() {

        Color highlightColor = new Color(80, 130, 220, 255)
        FlatLightLaf.install()
        UIManager.put( "Component.focusWidth", 3 )

        //MigLayout ml = new MigLayout()
        swing.registerBeanFactory('migLayout', MigLayout)

        // see: https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255
        swing.actions({

            // file

            action(id: 'openAction',
                    name: "oeffnen",
                    mnemonic: 'o',
                    closure: { println "openAction not connected to application..." },
                    accelerator: shortcut('O'),
                    focus: JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                    //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                    shortDescription: 'Verzeichnis mit Daten-Datein öffnen - alle Dateien darin'
            )

            action(id: 'saveAction',
                    name: "speichern",
                    mnemonic: 's',
                    closure: { println "saveAction not connected to application..." },
                    accelerator: shortcut('S'),
                    shortDescription: 'alle Daten ins aktuelle Daten-Verzeichnis sichern'
            )

            action(id: 'saveAsAction',
                    name: "speichern als...",
                    //mnemonic: 's',
                    closure: { println "saveAsAction not connected to application..." },
                    //accelerator: shortcut('S'),
                    shortDescription: 'anderes Daten-Verzeichnis wählen und Dateien speichern'
            )

            action(id: 'exitAction',
                    name: "beenden",
                    //mnemonic: 'p',
                    closure: { println "exitAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'v-pipe beenden - und vorher nochmal speichern ;-)'
            )

            // tools

            action(id: 'sortPipelineAction',
                    name: "Staffelung (P) sortieren",
                    mnemonic: 'p',
                    closure: { println "sortPipelineAction not connected to application..." },
                    accelerator: shortcut('P'),
                    shortDescription: 'Staffelung sortieren dem spätesten End-Termin des letzten Tasks des Projektes'
            )

            // view

            action(id: 'pipelineViewAction',
                    name: "Staffelungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: { println "pipelineViewAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'Staffelung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            action(id: 'loadViewAction',
                    name: "Abt-Belastungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: { println "loadViewAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'Abt-Belastung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            action(id: 'pipelineLoadViewAction',
                    name: "IP-Belastungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: { println "pipelineLoadViewAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'IP-Belastung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            action(id: 'projectViewAction',
                    name: "Projekt-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: { println "projectViewAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'Projekt-Ansicht in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            /*
            action(id: 'monthViewAction',
                    name: "Monats-Belastung, separat",
                    //mnemonic: 'p',
                    closure: { println "monthlyViewAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'Monats-Ansicht in gesondertem Fenster öffnen.'
            )*/

            // help

            action(id: 'helpAction',
                    name: "Hilfe...",
                    //mnemonic: 'p',
                    closure: { println "helpPerformanceAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'zeige die aktuelle Hilfe-Datei im Inter-Netz. Online :-(. Aktuelle Version...'
            )

            action(id: 'printPerformanceAction',
                    name: "Performance messen",
                    //mnemonic: 'p',
                    closure: { println "printPerformanceAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'gemessene Lauf-Zeiten auf Console printen'
            )

        })

        swing.build {

            f = frame(id: 'frame',
                    size:[(int)(screenDimension.width), (int)(screenDimension.height - 50)],
                    location: [0,0],
                    iconImage: frameIcon,
                    title: 'v-pipe    |  +/- = Zoom  |  Pfeile = Cursor bewegen  |  Shift+Pfeile = Projekt bewegen  |  d = Details an/aus  |  n = now  |  Strg+Pfeile = Tasks vergr./verkl.',
                    locationRelativeTo: null,
                    show: true,
                    defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE) {

                menuBar(id: 'menuBar') {

                    menu(text:'Dateien', mnemonic:'D') {
                        menuItem(openAction)
                        menuItem(saveAction)
                        menuItem(saveAsAction)
                        menuItem(exitAction)
                    }

                    menu(text:'Werkzeug', mnemonic:'W') {
                        menuItem(sortPipelineAction)
                    }

                    menu(text:'Ansicht', mnemonic:'A') {
                        menuItem(pipelineViewAction)
                        menuItem(loadViewAction)
                        menuItem(pipelineLoadViewAction)
                        menuItem(projectViewAction)
                    }

                    menu(text:'Hilfe', mnemonic:'H') {
                        menuItem(helpAction)
                        menuItem(printPerformanceAction)
                    }
                }

                migLayout(layoutConstraints:"fill", columnConstraints:"[][][][][][][][][][][grow]", rowConstraints:"[][grow]")

                label("Projekt suchen: ", foreground:GRAY)
                textField(id: 'searchTextField', toolTipText: 'Tutorial & Experimente: regex101.com', constraints: 'width 100')
                label("    Zeit: ", foreground:GRAY)
                label("", id: 'timeLabel', foreground:highlightColor)
                label("    Projekt: ", foreground:GRAY)
                label("", id: 'projectLabel', foreground:highlightColor)
                label("    Abt: ", foreground:GRAY)
                label("", id: 'depLabel', foreground:highlightColor)
                label("    Pfad: ", foreground:GRAY)
                label(id:'currentPath', constraints:  'wrap')

                splitPane(id: 'spH', orientation: JSplitPane.HORIZONTAL_SPLIT, continuousLayout:true, dividerLocation: 800, constraints: 'grow, span') {

                    splitPane(id: 'spV1', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 500) {

                        scrollPane (horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS) {
                            widget(pipelineView)
                        }

                        splitPane(id: 'spV3', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 100) {

                            scrollPane(id: 'pipelineLoadViewScrollPane', horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS) {
                                widget( pipelineLoadView)
                            }

                            scrollPane(id: 'spSwap', horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS) {
                                widget(loadView)
                            }
                        }
                    }

                    splitPane(id: 'spV2', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: 400) {

                        scrollPane {
                            widget(projectView)
                        }

                        tabbedPane(id: 'tabs', tabLayoutPolicy: JTabbedPane.SCROLL_TAB_LAYOUT) {

                            scrollPane (id: 'projectDetailsScrollPane',
                                    name: 'Projekt-Details',
                                    verticalScrollBarPolicy: JScrollPane.VERTICAL_SCROLLBAR_ALWAYS) {
                                //projectDetails.noDataPanel()
                            }

                            scrollPane(name: 'Projekt-Vorlagen') {
                                projectTemplates.buildDataPanel()
                            }
                            scrollPane(name: 'Info') {
                                textArea(id: 'textAreaLog', editable: false, focusable: false)
                            }

                            fileDifferPanel.buildPanel()

                        }
                    }
                }
            }
        }
        ((JScrollPane)(swing.projectDetailsScrollPane)).verticalScrollBar.setUnitIncrement(10)
    }


    def setFullSize(JFrame f) {
        //f.setSize((int)(screenDimension.width), (int)(screenDimension.height - 50))
        //f.setLocation(0, 0)
    }


    def start(Closure initModelAndNotifyView) {
        swing.edt {
            initModelAndNotifyView()
            setFullSize(swing.frame)
        }
    }

    int i = 0

    def openPipelineWindow() {
        def newPipelineView = new GridPanel(20, gridPipelineModel)
        swing.edt {

            frame(id: "framePipeline+${i++}", iconImage: frameIcon,
                    title: "v-pipe: Staffelung", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
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
            frame(id: "frameLoad+${i++}", iconImage: frameIcon,
                    title: "v-pipe: Abt.-Belastung", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
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
            frame(id: "framePipelineLoad+${i++}", iconImage: frameIcon,
                    title: "v-pipe: IP-Belastung", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane {
                    widget(newLoadView, name: "monthLoad$i++")
                }
            }
            bind(target: newLoadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newLoadView, sourceProperty: "cursorX")
        }
    }


    def openProjectWindow() {
        def newProjectView = new GridPanel(20, gridProjectModel)
        swing.edt {
            frame(id: "frameProjectLoad+${i++}", iconImage: frameIcon,
                    title: "v-pipe: Projekt", locationRelativeTo: null, show: true, pack:true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane {
                    widget(newProjectView, name: "monthLoad$i++")
                }
            }
            bind(target: newProjectView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newProjectView, sourceProperty: "cursorX")
        }
    }
}
