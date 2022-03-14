package application

import groovy.swing.SwingBuilder
import model.Model

import javax.swing.*
import java.awt.Color
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.beans.PropertyChangeListener

class PipelineEditor {

    Model model
    View view
    SwingBuilder swing


    def adaptPanel() {
        JLabel pipelineVisibleLabel = swing.pipelineVisible
        JLabel pipelineAvailableLabel = swing.pipelineAvailable
        JButton remove = swing.removePipelineButton
        JButton create = swing.createPipelineButton
        JTextField slots = swing.slotsPipeline
        JButton save = swing.saveSlotsButton

        if (model.pipelineElements) {
            pipelineAvailableLabel.text = "Pipeline und IPs im Model:  Ja"
            remove.setEnabled(true)
            create.setEnabled(false)
            slots.setEnabled(true)
            save.setEnabled(true)
            slots.text = model.maxPipelineSlots.toString()
        } else {
            pipelineAvailableLabel.text = "Pipeline und IPs im Modell:  Nein"
            remove.setEnabled(false)
            create.setEnabled(true)
            slots.setEnabled(false)
            save.setEnabled(false)
            slots.text = "kein Wert"
        }
        if (view.showIntegrationPhase) {
            pipelineVisibleLabel.text = "Pipeline sichtbar:  Ja"
        } else {
            pipelineVisibleLabel.text = "Pipeline sichtbar:  Nein"
        }
    }

    PropertyChangeListener l = {
        adaptPanel()
    }


    def removePipeline = {
        model.removePipeline()
        view.showPipelineLoad()
    }

    def createPipeline = {
        model.createPipeline()
        view.showPipelineLoad()
    }

    def saveSlots = {
        if(checkSlots()) {
            model.maxPipelineSlots = Integer.parseInt(swing.slotsPipeline.text)
        }
        model.fireUpdate()
    }

    def setError(err) {
        swing.pipelineError.text = err
        swing.manageResourcesPanel.doLayout()
    }

    boolean checkSlots() {
        JLabel l = swing.pipelineError
        String slotsText = swing.slotsPipeline.text
        try {
            int slotsInt = Integer.parseInt(slotsText)
            int biggestInModel = model.pipelineElements*.pipelineSlotsNeeded.max()
            if (slotsInt < biggestInModel) {
                setError("ist kleiner als größter Pipeline-Einzel-Bedarf im Modell: $biggestInModel")
                return false
            }
        } catch (Exception e) {
            setError("keine Ganz-Zahl: $slotsText")
            return false
        }
        setError("")
        return true
    }

    def buildPanel() {
        swing.build {
            panel(id: "manageResourcesPanel", name: 'Pipeline') {
                migLayout(layoutConstraints: "", rowConstraints: "[][]", columnConstraints: "[]")
                panel(border: titledBorder('Pipeline bearbeiten'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][]", rowConstraints: "[]")
                    textField(id: "slotsPipeline", constraints: '')
                    button(id: "saveSlotsButton", "Slots speichern", actionPerformed: saveSlots, constraints: "wrap")
                    label(id: "pipelineError", foreground: Color.RED, constraints: 'span, wrap')
                    button(id: "removePipelineButton", "Pipeline aus Modell entfernen", actionPerformed: removePipeline, constraints: "span, wrap")
                    button(id: "createPipelineButton", "Neue Pipeline in Modell erzeugen", actionPerformed: createPipeline, constraints: "span, wrap")
                    label(id: "pipelineVisible", constraints: 'span, wrap')
                    label(id: "pipelineAvailable", constraints: 'span, wrap')

                }

            }

            JTextField tf = swing.slotsPipeline
            tf.addKeyListener(new KeyListener() {
                void keyTyped(KeyEvent e) { checkSlots() }

                void keyPressed(KeyEvent e) { checkSlots() }

                void keyReleased(KeyEvent e) { checkSlots() }
            })
        }

        model.addPropertyChangeListener(l)
        view.addPropertyChangeListener(l)
    }

}
