package application

import groovy.swing.SwingBuilder
import model.Model
import model.TaskInProject

import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JTextField
import javax.swing.UIManager
import java.awt.Color
import java.awt.Image
import java.awt.event.KeyEvent

import static java.awt.Color.RED

class ProjectDetails {
    ImageIcon cut = null
    ImageIcon copy = null


    View view
    Model model
    SwingBuilder swing
    Color fg = UIManager.getLookAndFeelDefaults().get("TextField.foreground") as Color

    static def scaleIcon(icon, scale) {
        Image img = icon.getImage()
        Image newImg = img.getScaledInstance( (int)(img.getHeight(null) * scale), (int)(img.getWidth(null)*scale),  Image.SCALE_SMOOTH )
        new ImageIcon( newImg )
    }

    ProjectDetails(View view) {
        cut =  scaleIcon(new ImageIcon(getClass().getResource("/icons/cut.png")), 0.08)
        copy = scaleIcon (new ImageIcon(getClass().getResource("/icons/copy.png")), 0.08)
        this.view = view
        model = view.model // shortcut
        swing = view.swing // shortcut
        view.gridPipelineModel.addPropertyChangeListener("selectedProject", updateProjectDetails)
        model.addPropertyChangeListener("updateToggle", updateProjectDetails)
    }


    def updateProjectDetails = {
        def p = buildDataPanel()
        swing.projectDetailsScrollPane.setViewportView(p)
    }


    def buildDataPanel() {
        def p = view.gridPipelineModel.selectedProject
        if(p) {
            List<TaskInProject> project = model.getProject(p)

            swing.actions({
                action(id: 'applyProjectDetails',
                        name: "Änderungen übernehmen",
                        mnemonic: 'r',
                        closure: saveProjectDetails,
                        accelerator: shortcut('R'), //, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK),
                        focus: JComponent.WHEN_IN_FOCUSED_WINDOW,
                        //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                        shortDescription: 'geänderte Details in die Projektdaten übernehmen'
                )
                action(id: 'deleteProject',
                        name: "Projekt löschen",
                        //mnemonic: 'r',
                        closure: deleteSelectedProject,
                        //accelerator: shortcut('R'), //, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK),
                        focus: JComponent.WHEN_IN_FOCUSED_WINDOW,
                        //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                        shortDescription: 'dieses Projekt löschen'
                )
            })

            swing.build {
                panel {
                    migLayout(layoutConstraints: "", columnConstraints: "[]", rowConstraints: "[][]")

                    panel(border: titledBorder('Projekt'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[][]", rowConstraints: "[][]1[]")
                        label('Projekt-Name:', constraints: "")
                        textField(id: 'projectName', enabled: false, text: p, constraints: 'w 600!, span 4, wrap')
                        label('Liefer-Termin:')
                        textField(id: 'planFinish', enabled: false, constraints: 'w 200!, span 3, wrap')
                        label('Wahrscheinlichkeit:')
                        textField(id: 'probability', text: 100, enabled: false, constraints: 'w 100!')
                        label('[%]', constraints: '')
                    }
                    panel(border: titledBorder('Tasks'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[][200!][200!][150!][400!][][]", rowConstraints: "")
                        button(id: 'applyDetails', 'Änderungen übernehmen', action: applyProjectDetails, constraints: 'span, growx, wrap') // actionPerformed: saveProjectDetails

                        label('Abteilung')
                        label('Start')
                        label('Ende')
                        label('Last')
                        label('Bezeichnung', constraints: 'growx')
                        label("löschen    ") // for button cut
                        label("duplizieren  ", constraints: 'wrap') // for button copy
                        def idx = 0
                        for(task in project) {
                            buildProjectLine(idx++, task.department, task.starting, task.ending, task.capacityNeeded, task.description)
                        }
                    }
                    panel(border: titledBorder('Projektverwaltung'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[][]", rowConstraints: "[][]")
                        button("Projekt löschen", enabled: true, action: deleteProject)
                        //button("Projekt duplizieren", enabled: true)
                        //button("Projekt aus Vorlage", enabled: true, constraints: '')
                        //checkBox("Vorlage (wird nicht als Last gerechnet)", enabled: false, constraints: 'wrap')

                    }

                }
            }
        } else {
            noDataPanel()
        }
    }


    def buildProjectLine(def idx, def department, def starting, def ending, def capacityNeeded, def description){
        swing.build {
            comboBox(id: "department-$idx", items: model.allDepartments, selectedItem: department, toolTipText: "alter Wert:  $department")
            textField(id: "planStart-$idx", text: starting.toString(), toolTipText: "alter Wert:  " + starting.toString(), actionPerformed: checkProjectDetails)
            textField(id: "planFinish-$idx", text: ending.toString(), toolTipText: "alter Wert:  " + ending.toString(), actionPerformed: checkProjectDetails)
            textField(id: "capaNeeded-$idx", text: capacityNeeded as String, toolTipText: "alter Wert:  " + (capacityNeeded as String), actionPerformed: checkProjectDetails)
            textField(id: "description-$idx", text: description, toolTipText: "alter Wert:  $description", constraints: 'growx', actionPerformed: checkProjectDetails)
            button("", icon: cut, constraints: 'growx', actionPerformed: deleteLine.curry(idx))
            button("", icon: copy, constraints: "growx, wrap", actionPerformed: copyLine.curry(idx))
        }
    }

    def deleteLine = {int idx, evt ->
        def p = view.gridPipelineModel.selectedProject
        if(p) {
            model.deleteProjectTask(idx, p)
            model.fireUpdate()
        }
    }

    def copyLine = {idx, evt ->
        def p = view.gridPipelineModel.selectedProject
        if(p) {
            model.copyProjectTask(idx, p)
            model.fireUpdate()
        }
    }


    def noDataPanel() {
        swing.build {
            panel {
                migLayout(layoutConstraints: "fill", columnConstraints: "", rowConstraints: "")
                label ("es ist kein Projekt ausgewählt - daher: hier keine Daten", constraints: 'north')
            }
        }
    }

    def confirmDelete(project) {
        int dialogButton = JOptionPane.YES_NO_OPTION
        dialogButton = JOptionPane.showConfirmDialog(null, "$project \nwirlich  L Ö S C H E N ?", "HIRN EINSCHALTEN!", dialogButton)
        if (dialogButton == JOptionPane.YES_OPTION) {
            return true
        }
        if (dialogButton == JOptionPane.NO_OPTION) {
            return false
        }
    }

    def deleteSelectedProject = {
        def p = view.gridPipelineModel.selectedProject
        if(p) {
            if(confirmDelete(p)) {
                view.gridPipelineModel.setSelectedProject(null)
                model.deleteProject(p)
            }
        }

    }

    def saveProjectDetails = {
        if(checkProjectDetails()) {
            println "SAVE project details"
            def p = view.gridPipelineModel.selectedProject
            List<TaskInProject> project = model.getProject(p)
            def idx = 0
            for(task in project) {
                task.department = swing."department-$idx".selectedItem as String
                task.starting = swing."planStart-$idx".text.toDate()
                task.ending = swing."planFinish-$idx".text.toDate()
                task.capacityNeeded = swing."capaNeeded-$idx".text as Double
                task.description = swing."description-$idx".text
                idx++
            }
            model.reCalcCapaAvailableIfNeeded()
            model.fireUpdate()
            //model.setUpdateToggle(!model.updateToggle)
        } else {
            println "NOT SAVING project details - found errors..."
        }
    }


    def checkProjectDetails = {
        println "CEHCK project details"
        def result = true
        def p = view.gridPipelineModel.selectedProject
        List<TaskInProject> project = model.getProject(p)
        def idx = 0
        for(task in project) {
            def startValid = true
            def endValid = true
            if( ! checkTextField(swing."planStart-$idx", Date.class)) {result = false; startValid = false}
            if( ! checkTextField(swing."planFinish-$idx", Date.class)) {result = false; endValid = false}
            if( ! checkTextField(swing."capaNeeded-$idx", Double.class)) result = false
            if( ! checkTextField(swing."description-$idx", String.class)) result = false
            if(startValid && endValid) {
                if( ! checkStartBeforeEnd(swing."planStart-$idx", swing."planFinish-$idx")) {
                    result = false
                }
            }
            idx++
        }
        if(result) {
            swing.applyDetails.text = "Änderungen übernehmen"
        } else {
            swing.applyDetails.text = "bitte erst Fehler korrigieren... Dann: Änderungen übernehmen"
        }
        result
    }


    static def markError(JTextField textField, String error) {
        textField.setForeground(RED)
        textField.setToolTipText(error + "  ($textField.text)\n"+ textField.getToolTipText())

    }


    def markValid(JTextField textField) {
        textField.setForeground(fg)
        String[] split = textField.getToolTipText().split("\n")
        if(split.size() > 1) {
            textField.setToolTipText(split[split.size()-1])
        }
    }


    def checkStartBeforeEnd(JTextField textFieldStart, JTextField textFieldEnd){
        def result = true
        Date start = textFieldStart.text.toDate()
        Date end = textFieldEnd.text.toDate()
        if(start >= end) {
            markError(textFieldStart, "Start >= Ende...")
            markError(textFieldEnd, "Start >= Ende...")
            result = false
        } else if(end - start > 20*365 ) {
            markError(textFieldStart, "Start bis Ende mehr als 20 Jahre...")
            markError(textFieldEnd, "Start bis Ende mehr als 20 Jahre...")
            result = false
        } else {
            markValid(textFieldStart)
            markValid(textFieldEnd)
        }
        result
    }


    def checkTextField = {JTextField textField, Class type ->
        def result = true
        if(type == Double) {
            try {
                textField.text.toDouble()
            } catch(Exception e) {
                markError(textField, "keine Gleitkommazahl...")
                result = false
            }
        }else if(type == String){
            def val = textField.text
            if(val.contains(" ") || val.contains("\t")) {
                markError(textField, "enthält Leerzeichen...")
                result = false
            }
        }else if (type == Date){
            try {
                textField.text.toDate()
            } catch(Exception e) {
                markError(textField, "kein gültiges Datum tt.mm.jjjj...")
                result = false
            }
        } else {
            throw new RuntimeException("type to check unknown... $type")
        }
        if (result) markValid(textField)
        result
    }


}
