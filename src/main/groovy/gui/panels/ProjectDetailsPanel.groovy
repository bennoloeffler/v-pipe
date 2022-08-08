package gui.panels


import gui.View
import groovy.swing.SwingBuilder
import model.Model
import model.PipelineElement
import model.TaskInProject

import javax.swing.*
import java.awt.*
import java.util.List

import static java.awt.Color.RED

class ProjectDetailsPanel {

    View view
    Model model
    SwingBuilder swing
    Color fg = UIManager.getLookAndFeelDefaults().get("TextField.foreground") as Color

    ImageIcon cut = null
    ImageIcon copy = null

    static def scaleIcon(icon, scale) {
        Image img = icon.getImage()
        Image newImg = img.getScaledInstance((int) (img.getHeight(null) * scale), (int) (img.getWidth(null) * scale), Image.SCALE_SMOOTH)
        new ImageIcon(newImg)
    }

    ProjectDetailsPanel(View view) {
        cut = scaleIcon(new ImageIcon(getClass().getResource("/icons/cut.png")), 0.03 * View.scaleY)
        copy = scaleIcon(new ImageIcon(getClass().getResource("/icons/copy.png")), 0.03 * View.scaleY)
        this.view = view
        model = view.model // shortcut
        swing = view.swing // shortcut
        view.gridPipelineModel.addPropertyChangeListener("selectedProject", updateProjectDetails)
        view.addPropertyChangeListener("showIntegrationPhase", updateProjectDetails)
        model.addPropertyChangeListener("updateToggle", updateProjectDetails)
    }


    def updateProjectDetails = {
        def p = buildDataPanel()
        swing.projectDetailsScrollPane.setViewportView(p)
    }


    def buildDataPanel() {
        def p = view.selectedProject
        if (p) {
            List<TaskInProject> project = model.getProject(p)

            swing.build {

                action(id: 'applyProjectDetails',
                        name: "Änderungen übernehmen",
                        mnemonic: 'r',
                        closure: saveProjectDetails,
                        //accelerator: shortcut('R'), //, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK),
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
                action(id: 'copyProject',
                        name: "Projekt kopieren",
                        //mnemonic: 'r',
                        closure: copySelectedProject,
                        //accelerator: shortcut('R'), //, KeyEvent.ALT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK),
                        focus: JComponent.WHEN_IN_FOCUSED_WINDOW,
                        //smallIcon: imageIcon(resource: "icons/folder_r1.png"),
                        shortDescription: 'dieses Projekt kopieren'
                )
                panel() {
                    migLayout(layoutConstraints: "", columnConstraints: "[][]", rowConstraints: "[][]")

                    panel(border: titledBorder('Projekt'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[right]rel[fill, grow]", rowConstraints: "[][]")
                        label('Projekt-Name:', constraints: "")
                        textField(id: 'projectName', enabled: true, text: p, constraints: 'w 10:300:1000, growx, wrap',
                                actionPerformed: checkProjectDetails,
                                toolTipText: "alter Wert:  " + p)
                        label('Liefer-Termin (ex-works):')
                        textField(id: 'planFinishProject', text: model.getDeliveryDate(p).toString(),
                                toolTipText: "alter Wert:  " + model.getDeliveryDate(p).toString(),
                                enabled: true, constraints: 'w 10:300:1000, growx, wrap')
                        //label('Wahrscheinlichkeit:')
                        //textField(id: 'probability', text: 100, enabled: false, constraints: 'w 100!')
                        //label('[%]', constraints: '')
                    }
                    panel(border: titledBorder('Tasks'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[][][][][][][]", rowConstraints: "")
                        button(id: 'applyDetails', 'Änderungen übernehmen', action: applyProjectDetails, constraints: 'span, growx, wrap')
                        // actionPerformed: saveProjectDetails

                        label('  Abteilung')
                        label('  Start')
                        label('  Ende')
                        label('  Last')
                        label('  Info/ID:')
                        label("  löschen    ") // for button cut
                        label("  duplizieren  ", constraints: 'wrap') // for button copy
                        def idx = 0
                        for (task in project) {
                            buildProjectLine(idx++, task.department, task.starting, task.ending, task.capacityNeeded, task.description)
                        }
                        if(view.showIntegrationPhase && model.pipelineElements) {
                            label(" Integrations-Phase", constraints: 'growx, span, wrap')
                            PipelineElement pe = model.getPipelineElement(view.selectedProject)
                            buildIPLine(pe.startDate, pe.endDate, pe.pipelineSlotsNeeded, "")
                        }
                    }
                    panel(border: titledBorder('Projektverwaltung'), constraints: 'wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[][]", rowConstraints: "[][]")
                        button("Projekt löschen", enabled: true, action: deleteProject)
                        button("Projekt duplizieren", enabled: true, action: copyProject)
                        //button("Projekt aus Vorlage", enabled: true, constraints: '')
                        //checkBox("Vorlage (wird nicht als Last gerechnet)", enabled: false, constraints: 'wrap')

                    }
                    panel(border: titledBorder('Anmerkungen'), constraints: 'span, growx, wrap') {
                        migLayout(layoutConstraints: "fill", columnConstraints: "[]", rowConstraints: "[][]")
                        scrollPane(name: 'Log', constraints: 'span, growx') {
                            textPane(text:  model.projectComments[p], id: 'projectComment', editable: true, focusable: true, font: new Font("Monospaced", Font.PLAIN, (int) (View.scaleX * 8)))
                        }
                    }

                }
            }
        } else {
            noDataPanel()
        }
    }

    def buildIPLine(def starting, def ending, def capacityNeeded, def description) {
        swing.build {
            comboBox(id: "department-IP", items: ["IP"], selectedItem: "IP", enabled: false, constraints: 'growx')
            //label("IP", toolTipText: "die Integrations-Phase des Projektes")
            textField(id: "planStart-IP", text: starting.toString(), toolTipText: "alter Wert:  " + starting.toString(), actionPerformed: checkProjectDetails)
            textField(id: "planFinish-IP", text: ending.toString(), toolTipText: "alter Wert:  " + ending.toString(), actionPerformed: checkProjectDetails)
            textField(id: "capaNeeded-IP", text: capacityNeeded as String, toolTipText: "alter Wert:  " + (capacityNeeded as String),
                    actionPerformed: checkProjectDetails,  constraints: 'wrap')
            //textField(id: "description-$idx", text: description, toolTipText: "alter Wert:  $description", constraints: 'growx', actionPerformed: checkProjectDetails)
            //button("", icon: cut, constraints: 'growy, growx', actionPerformed: deleteLine.curry(idx))
            //button("", icon: copy, constraints: "growy, growx, wrap", actionPerformed: copyLine.curry(idx))
        }
    }

    def buildProjectLine(def idx, def department, def starting, def ending, def capacityNeeded, def description) {
        swing.build {
            comboBox(id: "department-$idx", items: model.allDepartments, selectedItem: department, toolTipText: "alter Wert:  $department")
            textField(id: "planStart-$idx", text: starting.toString(), toolTipText: "alter Wert:  " + starting.toString(), actionPerformed: checkProjectDetails)
            textField(id: "planFinish-$idx", text: ending.toString(), toolTipText: "alter Wert:  " + ending.toString(), actionPerformed: checkProjectDetails)
            textField(id: "capaNeeded-$idx", text: capacityNeeded as String, toolTipText: "alter Wert:  " + (capacityNeeded as String), actionPerformed: checkProjectDetails)
            textField(id: "description-$idx", text: description, toolTipText: "alter Wert:  $description", constraints: 'growx', actionPerformed: checkProjectDetails)
            button("", icon: cut, constraints: 'growy, growx', actionPerformed: deleteLine.curry(idx))
            button("", icon: copy, constraints: "growy, growx, wrap", actionPerformed: copyLine.curry(idx))
        }
    }

    def deleteLine = { int idx, evt ->
        def p = view.selectedProject
        if (p) {
            model.deleteProjectTask(idx, p)
            model.fireUpdate()
        }
    }

    def copyLine = { idx, evt ->
        def p = view.selectedProject
        if (p) {
            model.copyProjectTask(idx, p)
            model.fireUpdate()
        }
    }


    def noDataPanel() {
        swing.build {
            panel() {
                migLayout(layoutConstraints: "fill", columnConstraints: "", rowConstraints: "")
                label("es ist kein Projekt ausgewählt - daher: hier keine Daten", constraints: 'north')
            }
        }
    }

    def confirmDelete(project) {
        int dialogButton = JOptionPane.YES_NO_OPTION
        dialogButton = JOptionPane.showConfirmDialog(null, "\n\n$project \n\nwirklich  L Ö S C H E N ?", "HIRN EINSCHALTEN!", dialogButton)
        if (dialogButton == JOptionPane.YES_OPTION) {
            return true
        }
        if (dialogButton == JOptionPane.NO_OPTION) {
            return false
        }
    }

    def deleteSelectedProject = {
        def p = view.selectedProject
        if (p) {
            if (confirmDelete(p)) {
                view.deselectProject()
                model.deleteProject(p)
            }
        }
    }

    def copySelectedProject = {
        def p = view.selectedProject
        if (p) {
            //if (confirmDelete(p)) {
                String newProjectName = model.copyProject(p)
                view.selectedProject = newProjectName
            //}
        }
    }

    def saveProjectDetails = {
        if (checkProjectDetails()) {
            def p = view.selectedProject
            List<TaskInProject> project = model.getProject(p)
            def idx = 0
            for (task in project) {
                task.department = swing."department-$idx".selectedItem as String
                task.starting = swing."planStart-$idx".text.toDate()
                task.ending = swing."planFinish-$idx".text.toDate()
                task.capacityNeeded = swing."capaNeeded-$idx".text as Double
                task.description = swing."description-$idx".text
                idx++
            }
            model.deliveryDates.put(p, swing.planFinishProject.text.toDate())

            model.saveComment(p, swing.projectComment.text)

            if(view.showIntegrationPhase && model.pipelineElements) {
                PipelineElement pe = model.getPipelineElement(view.selectedProject)
                pe.startDate = swing."planStart-IP".text.toDate()
                pe.endDate = swing."planFinish-IP".text.toDate()
                pe.pipelineSlotsNeeded = swing."capaNeeded-IP".text as Integer
            }

            if (view.selectedProject != swing.projectName.text) {
                model.renameProject(view.selectedProject, swing.projectName.text)
                view.selectedProject = swing.projectName.text
            }

            model.reCalcCapaAvailableIfNeeded()
            model.fireUpdate()
        } else {
            println "NOT SAVING project details - found errors..."
        }
    }


    def checkProjectDetails = {
        def result = true
        if (!checkTextField(swing.planFinishProject, Date.class)) {
            result = false
        }
        def idx = 0
        def p = view.selectedProject
        List<TaskInProject> project = model.getProject(p)
        for (task in project) {
            def startValid = true
            def endValid = true
            if (!checkTextField(swing."planStart-$idx", Date.class)) {
                result = false; startValid = false
            }
            if (!checkTextField(swing."planFinish-$idx", Date.class)) {
                result = false; endValid = false
            }
            if (!checkTextField(swing."capaNeeded-$idx", Double.class)) result = false
            if (!checkTextField(swing."description-$idx", String.class)) result = false
            if (startValid && endValid) {
                if (!checkStartBeforeEnd(swing."planStart-$idx", swing."planFinish-$idx")) {
                    result = false
                }
            }
            idx++
        }
        if(view.showIntegrationPhase && model.pipelineElements) {
            def startValid = true
            def endValid = true
            if (!checkTextField(swing."planStart-IP", Date.class)) {
                result = false; startValid = false
            }
            if (!checkTextField(swing."planFinish-IP", Date.class)) {
                result = false; endValid = false
            }
            if (startValid && endValid && !checkStartBeforeEnd(swing."planStart-IP", swing."planFinish-IP")) {
                result = false
            }
            if (!checkTextField(swing."capaNeeded-IP", Integer.class)) {
                result = false
            } else {
                int capaNeeded = Integer.parseInt(swing."capaNeeded-IP".text)
                if(capaNeeded > model.maxPipelineSlots) {
                    markError(swing."capaNeeded-IP" as JTextField, "Bedarf ist größer als Maximum der Slots: $model.maxPipelineSlots")
                    result = false
                }
            }
        }
        if (result) {
            swing.applyDetails.text = "Änderungen übernehmen"
        } else {
            swing.applyDetails.text = "bitte erst Fehler korrigieren... Dann: Änderungen übernehmen"
        }
        if (view.selectedProject != swing.projectName.text) {
            if (!checkProjectName()) {
                result = false
            }
        }
        result
    }


    static def markError(JTextField textField, String error) {
        textField.setForeground(RED)
        textField.setToolTipText(error + "  ($textField.text)\n" + textField.getToolTipText())

    }


    def markValid(JTextField textField) {
        textField.setForeground(fg)
        String[] split = textField.getToolTipText().split("\n")
        if (split.size() > 1) {
            textField.setToolTipText(split[split.size() - 1])
        }
    }


    def checkStartBeforeEnd(JTextField textFieldStart, JTextField textFieldEnd) {
        def result = true
        Date start = textFieldStart.text.toDate()
        Date end = textFieldEnd.text.toDate()
        if (start >= end) {
            markError(textFieldStart, "Start >= Ende...")
            markError(textFieldEnd, "Start >= Ende...")
            result = false
        } else if (end - start > 20 * 365) {
            markError(textFieldStart, "Start bis Ende mehr als 20 Jahre...")
            markError(textFieldEnd, "Start bis Ende mehr als 20 Jahre...")
            result = false
        } else {
            markValid(textFieldStart)
            markValid(textFieldEnd)
        }
        result
    }

    def checkProjectName() {
        JTextField pn = swing.projectName
        String val = pn.text
        if (val.contains(" ") || val.contains("\t")) {
            markError(pn, "enthält Leerzeichen...")
            return false
        }
        if (model.getProject(val)) {
            markError(pn, "es gibt schon ein Projekt mit den Namen $val")
            return false
        }
        markValid(pn)
        return true
    }

    def checkTextField = { JTextField textField, Class type ->
        def result = true

        if (type == Integer) {
            try {
                textField.text.toInteger()
            } catch (Exception e) {
                markError(textField, "keine Ganzzahl...")
                result = false
            }
        } else if (type == Double) {
            try {
                textField.text.toDouble()
            } catch (Exception e) {
                markError(textField, "keine Gleitkommazahl...")
                result = false
            }
        } else if (type == String) {
            def val = textField.text
            if (val.contains(" ") || val.contains("\t")) {
                markError(textField, "enthält Leerzeichen...")
                result = false
            }
        } else if (type == Date) {
            try {
                Date then = textField.text.toDate()
                Date now = new Date()
                if (Math.abs(now - then) > 10 * 365) {
                    markError(textField, "Datum ist mehr als 10 Jahre entfernt")
                    result = false
                }
            } catch (Exception e) {
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
