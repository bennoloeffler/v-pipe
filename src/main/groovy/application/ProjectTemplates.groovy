package application

import groovy.swing.SwingBuilder
import model.Model
import model.PipelineOriginalElement
import model.TaskInProject

import javax.swing.DefaultListSelectionModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class ProjectTemplates {
    View view
    Model model
    SwingBuilder swing


    ProjectTemplates(View view) {
        this.view = view
        model = view.model // shortcut
        swing = view.swing // shortcut
        model.addPropertyChangeListener("updateToggle", update)
    }

    def update = {
        //println "UPDATE template list"
        def t = model.getAllTemplates()
        JList l = swing.templateList
        l.setListData(new Vector<String>(t))
        if(t) {
            l.setSelectedIndex(0)
        }
        checkData()
    }

    def enableCloning = {
        checkData()
    }


    def err(String str) {
        swing.templateProjectError.text = "--> " + str
    }

    /**
     * @param dateStr
     * @return an error message or null - if no error
     */
    def checkDate(String dateStr) {
        try {
            Date d = dateStr.toDate()
            if(Math.abs (model.getEndOfTasks() - d) > 20*365) {
                return "Bitte Datum ändern. Das Datum $dateStr hat mehr als 20 Jahre Differenz zum Portfolio"
            }
        } catch (Exception e) {
            return "Bitte Datum korrigieren. $dateStr wird nicht als Datum erkannt"
        }
        null
    }

    def checkData() {
        swing.copyTemplate.enabled = false

        if(! model.getAllTemplates()) {
            err ("KEINE VORLAGEN vorhanden! (Vorlagen-Projekt-Start-End-Abt-Kapa.txt)")
            return false
        }


        if(! swing.templateList.selectedValue) {
            err ("Bitte Template auswählen")
            return false
        }
        if(swing.templateName.text == "") {
            err ("Bitte Namen für neues Projekt eingeben... ")
            return false
        }
        if(model.getProject(swing.templateName.text)) {
            err ("Bitte Projektname ändern - Projektname " + swing.templateName.text + " existiert schon im Portfolio")
            return false
        }
        String dateErr = checkDate(swing.templateFinish.text)
        if(dateErr) {
            err (dateErr)
            return false
        }
        swing.templateProjectError.text=""
        swing.copyTemplate.enabled = true
        true
    }

    def cloneTemplate = { evt ->
        if(checkData()) {
            String templateName = swing.templateList.selectedValue
            String projectName = swing.templateName.text
            Date projectDate = swing.templateFinish.text.toDate()
            def theCopy = model.copyFromTemplate(templateName, projectName, projectDate)

            // copy pipeline...
            PipelineOriginalElement  thePipelineCopy = null
            if(model.pipelineElements) {
                thePipelineCopy = model.copyPipelineFromTemplate(templateName, projectName, projectDate)
                assert thePipelineCopy
            }

            SwingUtilities.invokeLater {
                if(model.pipelineElements) {
                    model.pipelineElements.add(thePipelineCopy)
                }
                model.addProject( theCopy )
            }
            swing.searchTextField.text = projectName
        }
    }

    def buildDataPanel() {
        def t = model.allTemplates
        //if(t) {
            swing.actions({
                action(id: 'cloneTemplate',
                        name: "neues Projekt aus Vorlage erzeugen",
                        //mnemonic: 'r',
                        closure: cloneTemplate,
                        //accelerator: shortcut('C'), //, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK),
                        focus: JComponent.WHEN_IN_FOCUSED_WINDOW,
                        //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                        shortDescription: 'Vorlage mit neuem Projekt-Namen und End-Datum ins Portfolio kopieren'
                )
            })

            swing.build {
                panel {
                    migLayout(layoutConstraints: "", columnConstraints: "[]", rowConstraints: "[][]")

                    panel(border: titledBorder('Projekt aus Vorlage erstellen'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[][]", rowConstraints: "[][]1[]")
                        label('Name des neuen Projektes:', constraints: "")
                        textField(id: 'templateName', enabled: true, text: "", constraints: 'w 600!, span 4, wrap')
                        label('End-Termin des neuen Projektes:')
                        textField(id: 'templateFinish', text: (new Date().plus(365)).toString(), enabled: true, constraints: 'w 200!')
                        label('z.B. 17.2.2023', constraints: 'wrap')
                        button(id: 'copyTemplate', action: swing.cloneTemplate, enabled: false, constraints: 'span, growx, wrap') // actionPerformed: saveProjectDetails
                        label(id: 'templateProjectError', foreground: Color.RED, constraints: 'span, growx, wrap')
                    }
                    panel(border: titledBorder('Bitte eine Vorlage auswählen'), constraints: 'grow, span') {
                        borderLayout()
                        scrollPane(constraints: BorderLayout.CENTER) {
                            list(id: 'templateList', selectionMode: ListSelectionModel.SINGLE_SELECTION,  ) // bind(enableCloning)
                        }
                        def ka = new KeyAdapter() {
                            @Override
                            void keyTyped(KeyEvent e) {
                                SwingUtilities.invokeLater {
                                    enableCloning()
                                }
                            }
                        }
                        swing.templateList.selectionModel.addListSelectionListener {enableCloning()}
                        swing.templateName.addKeyListener(ka)
                        swing.templateFinish.addKeyListener(ka)
                    }
                }
            }
        //} else {
        //    noDataPanel()
        //}
        JList list;

    }

    def noDataPanel() {
        swing.build {
            panel {
                migLayout(layoutConstraints: "fill", columnConstraints: "", rowConstraints: "")
                label ("keine Projektvorlagen in Datei Vorlagen.txt", constraints: 'north')
            }
        }
    }

}
