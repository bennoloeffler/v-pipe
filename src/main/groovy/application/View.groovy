package application

import com.formdev.flatlaf.FlatLightLaf
import groovy.beans.Bindable
import groovy.swing.SwingBuilder
import model.Model
import net.miginfocom.swing.MigLayout
import newview.FileDifferPanel
import newview.GridModel
import newview.GridPanel
import newview.NewLoadPanel
import newview.ResourceCapacityEditor

import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.UIManager
import java.awt.Color
import java.awt.Dimension
import java.awt.Image
import java.awt.Toolkit

import static application.ProjectDetails.*
import static java.awt.Color.*

/**
 * @see https://docs.oracle.com/javase/tutorial/uiswing/components/
 *
 * MVC with swing. BELs interpretation:
 * Model knows nothing - its observed and firing changes to an interface
 * View knows Model - but only to "bind". Holds swings actions. Holds the adaper-models (like JTableModel).
 * Controller knows Model and View. It connedts swings actions to real code that works on model.
 * @see GINA p.247 http://index-of.es/Java/Groovy%20in%20Action.pdf
 * @see OLD docu SwingBuilder https://web.archive.org/web/20140702234352/http://groovy.codehaus.org/Swing+Builder
 * @see SLIDES https://de.slideshare.net/aalmiray/javaone-ts5098-groovy-swingbuilder?qid=58967b6f-7007-4c0a-9dc2-148194f618c8&v=&b=&from_search=2
 * @see OLD article 1 https://uberconf.com/blog/andres_almiray/2009/11/building_rich_swing_applications_with_groovy__part_i
 * @see OLD article 2 https://uberconf.com/blog/andres_almiray/2009/11/building_rich_swing_applications_with_groovy__part_iI
 * @see OLD article 3 https://uberconf.com/blog/andres_almiray/2009/12/building_rich_swing_applications_with_groovy__part_iii
 * @see OLD article 4 https://uberconf.com/blog/andres_almiray/2009/12/building_rich_swing_applications_with_groovy__part_iv
 * @see Beispiel deutsch: https://docplayer.org/9822371-Programmieren-lernen-mit-groovy-graphische-oberflaechen-guis-graphical-user-interfaces.html
 */
class View {

    Image frameIcon
    Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize()
    SwingBuilder swing
    Model model

    @Bindable
    boolean showIntegrationPhase = true // if data is available

    //
    // adapter models (second layer...)
    //
    NewPipelineModel gridPipelineModel
    GridLoadModel gridLoadModel
    GridPipelineLoadModel gridPipelineLoadModel
    GridModel gridProjectModel

    //
    // views
    //
    def pipelineView
    def projectView
    def loadView
    def pipelineLoadView

    FileDifferPanel fileDifferPanel
    ProjectTemplates projectTemplates
    ResourceCapacityEditor resourceCapacityEditor
    PipelineEditor pipelineEditor

    def projectDetails

    View(Model model) {
        this.model = model
        frameIcon = new ImageIcon(getClass().getResource("/icons/vunds_icon_64x64_t.png")).getImage()
        swing = new SwingBuilder()

        gridPipelineModel = new NewPipelineModel(model)
        gridLoadModel = new GridLoadModel(model)
        gridPipelineLoadModel = new GridPipelineLoadModel(model)
        gridProjectModel = new NewProjectModel(model)

        pipelineView = new GridPanel(10 * MainGui.scaleX as int, gridPipelineModel)
        projectView = new GridPanel(10 * MainGui.scaleX as int, gridProjectModel)
        loadView = new NewLoadPanel(10 * MainGui.scaleX as int, gridLoadModel)
        pipelineLoadView = new NewLoadPanel(10 * MainGui.scaleX as int, gridPipelineLoadModel)

        fileDifferPanel = new FileDifferPanel(swing)
        projectTemplates = new ProjectTemplates(this)
        resourceCapacityEditor = new ResourceCapacityEditor(swing, model)
        pipelineEditor = new PipelineEditor(model: model, view: this, swing: swing)

        build()

        projectDetails = new ProjectDetails(this)

        pipelineView.name = "pipelineView" // in order to have a specific name in "paintComponent... getRuntimer(...)"
        projectView.name = "projectView"
        loadView.name = "departmentLoad"
        pipelineLoadView.name = "pipelineLoad"

    }


    void build() {

        // https://iconarchive.com/show/outline-icons-by-iconsmind.html
        // DONT DO '-' in ressource file names... wont work.
        //def url = new URL("https://icons.iconarchive.com/icons/iconsmind/outline/24/Bulleted-List-icon.png")
        //def url = getClass().getResource("/icons/open.png")
        //recentImageIcon =  scaleIcon(new ImageIcon(url), 0.5 * MainGui.scaleY)


        def saveAs = new URL("https://icons.iconarchive.com/icons/iconsmind/outline/24/Data-icon.png")
        saveAs = scaleIcon(new ImageIcon(saveAs), 0.5 * MainGui.scaleY)

        def saveCont = new URL("https://icons.iconarchive.com/icons/iconsmind/outline/24/Arrow-Refresh-icon.png")
        saveCont = scaleIcon(new ImageIcon(saveCont), 0.5 * MainGui.scaleY)

        def exit = new URL("https://icons.iconarchive.com/icons/icons8/ios7/24/Data-Export-icon.png")
        exit = scaleIcon(new ImageIcon(exit), 0.5 * MainGui.scaleY)


        def projectViewIcon = new URL("https://icons.iconarchive.com/icons/icons8/windows-8/24/Time-Gantt-Chart-icon.png")
        projectViewIcon = scaleIcon(new ImageIcon(projectViewIcon), 0.5 * MainGui.scaleY)

        def loadViewIcon = new URL("https://icons.iconarchive.com/icons/icons8/ios7/24/Data-Bar-Chart-icon.png")
        loadViewIcon = scaleIcon(new ImageIcon(loadViewIcon), 0.5 * MainGui.scaleY)

        def portfolioViewIcon = new URL("https://icons.iconarchive.com/icons/iconsmind/outline/24/Add-SpaceBeforeParagraph-icon.png")
        portfolioViewIcon = scaleIcon(new ImageIcon(portfolioViewIcon), 0.5 * MainGui.scaleY)


        Color highlightColor = new Color(80, 130, 220, 255)
        FlatLightLaf.install()
        UIManager.put("Component.focusWidth", 3)

        //MigLayout ml = new MigLayout()
        swing.registerBeanFactory('migLayout', MigLayout)

        swing.build {

            def i = { String iconPath, double scale = 0.5 ->
                scaleIcon(imageIcon(resource: iconPath), scale * MainGui.scaleY)
            }

            // https://stackoverflow.com/questions/42833424/java-key-bindings-using-groovy-swing-builder/42834255
            // https://stackoverflow.com/questions/9370326/default-action-button-icons-in-java


            action(id: 'newModelAction',
                    name: "neues Modell",
                    //mnemonic: 'o',
                    closure: { println "newModelAction not connected to application..." },
                    //accelerator: shortcut('O'),
                    smallIcon: i("/icons/new-model.png", 0.3),
                    shortDescription: 'neues Modell mit Beispiel-Projekt und Beispiel-Ressource erzeugen'
            )

            action(id: 'openAction',
                    name: "oeffnen",
                    mnemonic: 'o',
                    closure: { println "openAction not connected to application..." },
                    accelerator: shortcut('O'),
                    smallIcon: i("/icons/open.png"),
                    shortDescription: 'Verzeichnis mit Daten-Datein öffnen - alle Dateien darin'
            )


            action(id: 'saveAction',
                    name: "speichern",
                    mnemonic: 's',
                    closure: { println "saveAction not connected to application..." },
                    smallIcon: i("/icons/save.png"),
                    accelerator: shortcut('S'),
                    shortDescription: 'alle Daten ins aktuelle Daten-Verzeichnis sichern'
            )


            action(id: 'saveAsAction',
                    name: "speichern als...",
                    closure: { println "saveAsAction not connected to application..." },
                    smallIcon: i("/icons/save-as.png"),
                    //accelerator: shortcut('S'),
                    shortDescription: 'anderes Daten-Verzeichnis wählen und Dateien speichern'
            )

            action(id: 'toggleContinouosSaveAsAction',
                    name: "kontinuierlich speichern",
                    //mnemonic: 's',
                    closure: { println "toggleContinouosSaveAsAction not connected to application..." },
                    smallIcon: i("/icons/save-cont.png", 0.6),
                    shortDescription: 'alle 10 sec speichern'
            )

            action(id: 'exitAction',
                    name: "beenden",
                    //mnemonic: 'p',
                    closure: { println "exitAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    smallIcon: i("/icons/exit-it.png", 0.3),
                    shortDescription: 'v-pipe beenden - und vorher nochmal speichern ;-)'
            )

            // tools

            action(id: 'swapTemplatesAndProjectsAction',
                    name: "Vorlagen und Projekte tauschen",
                    //mnemonic: 'p',
                    closure: { println "swapTemplatesAndProjects not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'Projekte erscheinen als Vorlagen. Vorlagen werden bearbeitbar "als Projekte".'
            )

            action(id: 'sortPipelineAction',
                    name: "Staffelung (P) sortieren",
                    mnemonic: 'p',
                    closure: { println "sortPipelineAction not connected to application..." },
                    accelerator: shortcut('P'),
                    shortDescription: 'Staffelung sortieren dem spätesten End-Termin des letzten Tasks des Projektes'
            )

            action(id: 'insertInPhaseAction',
                    name: "IPs im Modell erzeugen",
                    //mnemonic: 'p',
                    closure: { println "insertInPhaseAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'falls nicht verfügbar, werden IPs im letzten Drittel des Projektes erzeugt'
            )
            action(id: 'removeInPhaseAction',
                    name: "IPs aus Model entfernen",
                    //mnemonic: 'p',
                    closure: { println "removeInPhaseAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'alle IPs aus dem Modell löschen'
            )
            // view

            action(id: 'toggleViewInPhaseAction',
                    name: "IPs sichtbar/unsichtbar",
                    //mnemonic: 'p',
                    closure: { println "toggleViewInPhaseAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'IPs bleiben im Modell. Sichtbar oder unsichtbar.'
            )

            action(id: 'pipelineViewAction',
                    name: "Staffelungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: { println "pipelineViewAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    //smallIcon: portfolioViewIcon,
                    shortDescription: 'Staffelung in gesondertem Fenster öffnen. Gerne mehrere. Multi-Monitor. Multi-View...'
            )

            action(id: 'loadViewAction',
                    name: "Abt-Belastungs-Ansicht, separat",
                    //mnemonic: 'p',
                    closure: { println "loadViewAction not connected to application..." },
                    //smallIcon: loadViewIcon,
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
                    //smallIcon: projectViewIcon,
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
                    shortDescription: 'zeige die aktuellste Hilfe-Datei im Internet.'
            )

            action(id: 'printPerformanceAction',
                    name: "Performance messen",
                    //mnemonic: 'p',
                    closure: { println "printPerformanceAction not connected to application..." },
                    //accelerator: shortcut('P'),
                    shortDescription: 'gemessene Lauf-Zeiten auf Console printen'
            )


            def f = frame(id: 'frame',
                    size: [(int) (screenDimension.width), (int) (screenDimension.height - 50)],
                    location: [0, 0],
                    iconImage: frameIcon,
                    title: 'v-pipe:  +/- = Zoom  |  Pfeile = Cursor bewegen  |  Shift+Pfeile = Projekt bewegen  |  d = Details an/aus  |  n = now  |  Strg+Pfeile = Tasks vergr./verkl.  |  m = Mittelwert',
                    locationRelativeTo: null,
                    show: true,
                    defaultCloseOperation: JFrame.DO_NOTHING_ON_CLOSE) {

                menuBar(id: 'menuBar') {

                    menu(text: 'Datei', mnemonic: 'D') {
                        menuItem(newModelAction)
                        menuItem(openAction)
                        menu(id: "recentMenuItem", "Letzte öffnen", icon: scaleIcon(imageIcon("/icons/recent.png"), 0.5))
                        menuItem(saveAction)
                        menuItem(saveAsAction)
                        checkBoxMenuItem(id: "checkBoxMenuContSaving", toggleContinouosSaveAsAction)
                        menuItem(exitAction)
                    }

                    menu(text: 'Werkzeug', mnemonic: 'W') {
                        menuItem(sortPipelineAction)
                        menuItem(insertInPhaseAction)
                        menuItem(removeInPhaseAction)
                        menuItem(swapTemplatesAndProjectsAction)
                    }

                    menu(text: 'Ansicht', mnemonic: 'A') {
                        menuItem(toggleViewInPhaseAction)
                        menuItem(pipelineViewAction)
                        menuItem(loadViewAction)
                        menuItem(pipelineLoadViewAction)
                        menuItem(projectViewAction)
                    }

                    menu(text: 'Hilfe', mnemonic: 'H') {
                        menuItem(helpAction)
                        menuItem(printPerformanceAction)
                    }
                }

                migLayout(layoutConstraints: "fill", columnConstraints: "[][][][][][][][][][][][grow]", rowConstraints: "[][grow]")

                label("Projekt suchen: ", foreground: GRAY)
                textField(id: 'searchTextField', toolTipText: 'Tutorial & Experimente: regex101.com', constraints: 'width 100')
                label(id: "swapped", foreground: RED, toolTipText: "speichern, laden, etc erst nach Beendigung des Vorlagen-Modus möglich.")
                label("    Zeit: ", foreground: GRAY)
                label("", id: 'timeLabel', foreground: highlightColor)
                label("    Projekt: ", foreground: GRAY)
                label("", id: 'projectLabel', foreground: highlightColor)
                label("    Abt: ", foreground: GRAY)
                label("", id: 'depLabel', foreground: highlightColor)
                label("    Pfad: ", foreground: GRAY)
                label(id: 'saveIndicator', "*", foreground: GRAY)
                label(id: 'currentPath', constraints: 'wrap')

                // left | right
                splitPane(id: 'spH', orientation: JSplitPane.HORIZONTAL_SPLIT, continuousLayout: true, dividerLocation: (int) (500 * MainGui.scaleX), constraints: 'grow, span') {

                    // pipeline
                    // --------
                    //  IP + Load
                    splitPane(id: 'spV1', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: (int) (300 * MainGui.scaleY)) {

                        scrollPane(horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS) {
                            widget(pipelineView)
                        }

                        // IP
                        // ----
                        // Load
                        splitPane(id: 'spV3', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: (int) (100 * MainGui.scaleY)) {

                            scrollPane(id: 'pipelineLoadViewScrollPane', horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS) {
                                widget(pipelineLoadView)
                            }

                            scrollPane(id: 'spSwap', horizontalScrollBarPolicy: JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS) {
                                widget(loadView)
                            }
                        }
                    }

                    // Project
                    // -------
                    // Details
                    splitPane(id: 'spV2', orientation: JSplitPane.VERTICAL_SPLIT, continuousLayout: true, dividerLocation: (int) (300 * MainGui.scaleY)) {

                        scrollPane() {
                            widget(projectView)
                        }

                        tabbedPane(id: 'tabs', tabLayoutPolicy: JTabbedPane.SCROLL_TAB_LAYOUT) {

                            scrollPane(id: 'projectDetailsScrollPane',
                                    name: 'Details',
                                    verticalScrollBarPolicy: JScrollPane.VERTICAL_SCROLLBAR_ALWAYS) {
                            }

                            scrollPane(name: 'Ressourcen') {
                                resourceCapacityEditor.buildPanel()
                            }

                            scrollPane(name: 'Vorlagen') {
                                projectTemplates.buildPanel()
                            }

                            scrollPane(name: 'Pipeline') {
                                pipelineEditor.buildPanel()
                            }

                            scrollPane(name: 'Log') {
                                textArea(id: 'textAreaLog', editable: false, focusable: false)
                            }

                            fileDifferPanel.buildPanel()

                        }
                    }
                }
            }
            bind(target: currentPath, targetProperty: 'text', source: model, sourceProperty: "currentDir", converter: { String v ->
                JLabel cp = currentPath
                cp.setToolTipText(v)
                def l = v.size()
                if (l > 80) {
                    def r = v[0..20] + "  [...]  " + v[l - 60..l - 1]
                    return r
                } else {
                    return v
                }
            })
            bind(target: saveIndicator, targetProperty: 'foreground', source: model, sourceProperty: "dirty", converter: { v ->
                v ? RED : GRAY
            })
            bind(target: swapped, targetProperty: 'text', source: model, sourceProperty: "projectsAndTemplatesSwapped", converter: { v ->
                f.validate() // to layout upmost panel and make "VORLAGEN-MOODUS" visible
                v ? "VORLAGEN-MODUS" : ""
            })


            // sync pipelineView with loadView and projectView (details of witch project?)
            bind(target: gridLoadModel, targetProperty: 'selectedProject', source: gridPipelineModel, sourceProperty: 'selectedProject')
            bind(target: gridProjectModel, targetProperty: 'projectName', source: gridPipelineModel, sourceProperty: 'selectedProject')

            // sync zoom factor of load views
            bind(target: pipelineLoadView, targetProperty: 'gridWidth', source: pipelineView, sourceProperty: 'gridWidth')
            bind(target: loadView, targetProperty: 'gridWidth', source: pipelineView, sourceProperty: 'gridWidth')

            // sync cursorX: central node ist the pipelineView
            bind(target: projectView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: 'cursorX')
            bind(target: loadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: 'cursorX')
            bind(target: pipelineLoadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: 'cursorX')

            bind(target: pipelineView, targetProperty: 'cursorX', source: loadView, sourceProperty: 'cursorX')
            bind(target: pipelineView, targetProperty: 'cursorX', source: projectView, sourceProperty: 'cursorX')
            bind(target: pipelineView, targetProperty: 'cursorX', source: pipelineLoadView, sourceProperty: 'cursorX')

            // sync indicators in tools and status line
            bind(target: pipelineView, targetProperty: 'hightlightLinePattern', source: searchTextField, sourceProperty: 'text')
            bind(target: timeLabel, targetProperty: 'text', source: pipelineView, sourceProperty: 'nowString')
            bind(target: projectLabel, targetProperty: 'text', source: gridPipelineModel, sourceProperty: 'selectedProject')
            bind(target: depLabel, targetProperty: 'text', source: gridProjectModel, sourceProperty: 'departmentName')

            // sync scroll-pane values hScrollBarValueZoomingSync
            bind(target: loadView, targetProperty: 'hScrollBarValueZoomingSync', source: pipelineView, sourceProperty: 'hScrollBarValueZoomingSync')
            bind(target: pipelineLoadView, targetProperty: 'hScrollBarValueZoomingSync', source: pipelineView, sourceProperty: 'hScrollBarValueZoomingSync')

            bind(target: pipelineView, targetProperty: 'showIntegrationPhase', source: this, sourceProperty: 'showIntegrationPhase')
            //bind(target: projectView, targetProperty: 'showIntegrationPhase', source: this, sourceProperty: 'showIntegrationPhase')
            bind(target: gridProjectModel, targetProperty: 'showIntegrationPhase', source: this, sourceProperty: 'showIntegrationPhase')

        }
        ((JScrollPane) (swing.projectDetailsScrollPane)).verticalScrollBar.setUnitIncrement(10)


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
        def newPipelineView = new GridPanel(10 * MainGui.scaleX as int, gridPipelineModel)
        swing.edt {

            frame(id: "framePipeline+${i++}", iconImage: frameIcon,
                    title: "v-pipe: Staffelung", locationRelativeTo: null, show: true, pack: true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane() {
                    widget(newPipelineView)
                }
            }
            bind(target: newPipelineView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newPipelineView, sourceProperty: "cursorX")
            bind(target: newPipelineView, targetProperty: 'showIntegrationPhase', source: this, sourceProperty: 'showIntegrationPhase')

        }
    }

    def openLoadWindow() {
        def newLoadView = new NewLoadPanel(10 * MainGui.scaleX as int, gridLoadModel)
        swing.edt {
            frame(id: "frameLoad+${i++}", iconImage: frameIcon,
                    title: "v-pipe: Abt.-Belastung", locationRelativeTo: null, show: true, pack: true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane() {
                    widget(newLoadView)
                }
            }
            bind(target: newLoadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newLoadView, sourceProperty: "cursorX")
        }
    }


    def openPipelineLoadWindow() {
        def newLoadView = new NewLoadPanel(10 * MainGui.scaleX as int, gridPipelineLoadModel)
        swing.edt {
            frame(id: "framePipelineLoad+${i++}", iconImage: frameIcon,
                    title: "v-pipe: IP-Belastung", locationRelativeTo: null, show: true, pack: true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane() {
                    widget(newLoadView, name: "monthLoad$i++")
                }
            }
            bind(target: newLoadView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newLoadView, sourceProperty: "cursorX")
        }
    }


    def openProjectWindow() {
        def newProjectView = new GridPanel(10 * MainGui.scaleX as int, gridProjectModel)
        swing.edt {
            frame(id: "frameProjectLoad+${i++}", iconImage: frameIcon,
                    title: "v-pipe: Projekt", locationRelativeTo: null, show: true, pack: true, defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE) {
                scrollPane() {
                    widget(newProjectView, name: "monthLoad$i++")
                }
            }
            bind(target: newProjectView, targetProperty: 'cursorX', source: pipelineView, sourceProperty: "cursorX")
            bind(target: pipelineView, targetProperty: 'cursorX', source: newProjectView, sourceProperty: "cursorX")
            bind(target: newProjectView, targetProperty: 'showIntegrationPhase', source: this, sourceProperty: 'showIntegrationPhase')
        }
    }

    def deselectProject() {
        gridPipelineModel.setSelectedProject(null)
    }

    String getSelectedProject() {
        gridPipelineModel.selectedProject
    }

    def setSelectedProject(String projectName) {
        gridPipelineModel.selectedProject = projectName
    }

    def showPipelineLoad() {
        if (model.pipelineElements && showIntegrationPhase) {
            swing.spV3.setDividerLocation(((int) (100 * MainGui.scaleY)))
            swing.pipelineLoadViewScrollPane.setVisible(true)
        } else {
            swing.pipelineLoadViewScrollPane.setVisible(false)
        }
        swing.frame.validate()
    }
}
