package gui.panels

import application.FileErrorChecker
import application.MainGui
import groovy.swing.SwingBuilder

import javax.swing.*
import java.awt.*

class ModelReaderMessagePanel {
    //Model model
    //View view
    SwingBuilder swing
    //JButton startButton
    //JButton stopButton
    JTextPane tp
    def m = new FileErrorChecker()


    PrintStream readerOutStream = new PrintStream(System.out) {
        @Override
        void println(String s) {
            out.println(s)
            def doc = tp.getStyledDocument()
            doc.insertString(0, s + '\n', null)
            tp.setCaretPosition(0)
        }

        void print(String s) {
            out.print(s)
            def doc = tp.getStyledDocument()
            doc.insertString(0, s, null)
            tp.setCaretPosition(0)
        }
    }

    def openFolder = {
        Desktop.getDesktop().open(new File(MainGui.instance.controller.model.currentDir))
    }

    def startReading = {
        // if model is dirty - ask to save first.
        assert (!MainGui.instance.controller.model.isDirty())
        // if auto-save-mode ask to switch off first.
        assert (!MainGui.instance.controller.autoSave)
        //startButton.enabled = false
        //stopButton.enabled = true
        System.setOut(readerOutStream)
        System.setErr(readerOutStream)
        //println "startReading"
        swing.doOutside {
            //println "Verzeichnis: " + MainGui.instance.controller.model.currentDir
            def err = m.runIt(MainGui.instance.controller.model.currentDir, swing)
            if (err) {
                MainGui.instance.controller.setEmptyModel()
                MainGui.instance.controller.setSaveForEmptyModel()
            } else {
                MainGui.instance.controller.model.setDirty(false)
            }
        }
    }

    def stopReading = {
        m.stopIt()
        //println "stopReading"
        //startButton.enabled = true
        //stopButton.enabled = false
        System.setOut(MainGui.instance.outStream)
        System.setErr(MainGui.instance.outStream)

        // after having failed or successfully read and user stops, the model is NOT DIRTY.
    }


    JPanel buildPanel() {
        swing.build {
            panel(id: 'modelReaderMessagePanel', name: 'Einlese-Fehler', focusable: true) {
                migLayout(layoutConstraints: "fill", columnConstraints: "[][]", rowConstraints: "[][grow]")
                //startButton = button("lesen starten", id: "startReading", actionPerformed: startReading, constraints: 'split 3, ')
                //label(id: 'fileA', "noch nichts gewählt", constraints: 'wrap, growx')
                //stopButton = button('lesen beenden', id: "stopReading", actionPerformed: stopReading, constraints: '', enabled: false)
                //label(id: 'fileB', "noch nichts gewählt", constraints: "wrap, growx")
                button('Verzeichnis öffnen', actionPerformed: openFolder, constraints: 'wrap')
                scrollPane(constraints: "span, growx, growy", focusable: true) {
                    tp = textPane(id: 'messagePanel', text: "...", focusable: true)
                }
            }
        } as JPanel
    }
}
