package newview

import application.MainGui
import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder
import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
import model.DataReader
import model.Model
import model.VpipeDataException

import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.UIManager
import java.awt.Color
import java.awt.Font
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class ResourceCapacityEditor {

    SwingBuilder swing
    Model model
    Color fg = UIManager.getLookAndFeelDefaults().get("TextField.foreground") as Color

    ResourceCapacityEditor(SwingBuilder swing, Model model) {
        this.swing = swing
        this.model = model
        model.addPropertyChangeListener {
            JList l = swing.resourcesList
            //println this.model
            //println this.model.getAllDepartments()
            //def dlm = new DefaultListModel()
            def allDeps = this.model.getAllDepartments() ?: []
            //def allDepsNonNull = allDeps?:[].toArray()
            //dlm.copyInto(allDepsNonNull)
            l.setListData(new Vector(allDeps))

            JTextPane t = this.swing.capaTextFile
            def yb = new YamlBuilder()
            yb(this.model.jsonSlurp)
            //t.setText(DataReader.capaTextCache?:"keine Kapazitätsinformation")
            t.setText(yb.toString()?:"keine Kapazitätsinformation")
        }
    }

    def renameResource = {
        println "rename Resource"
    }
    def createResource = {
        println "create Resource"
    }
    def checkDepartments(capaAvailable) {
        List<String> depsTasks = model.taskList*.department.unique()
        Set<String> depsCapa= capaAvailable.keySet()
        List<String> remain = (List<String>)(depsTasks.clone())
        remain.removeAll(depsCapa)
        if(remain) {
            "Abteilungen werden genutzt,\nfehlen aber hier:\n$remain"
        } else {null}
    }

    def checkYaml() {
        JTextPane ta = swing.capaTextFile
        JTextArea tf = swing.errorMessageCapaEdit

        try {
            def slurper = new YamlSlurper()
            def result = slurper.parseText(ta.text)
            def capaMap = model.calcCapa(result, false)
            model.check()
            def renameDepartmentsErr = checkDepartments(capaMap)
            if (renameDepartmentsErr) {
                tf.text = renameDepartmentsErr
                ta.setForeground(Color.RED)
            } else {
                tf.text = "keine Fehler... alles dufte."
                ta.setForeground(fg)
            }
        } catch (Throwable e) {
            ta.setForeground(Color.RED)
            tf.text = e.getMessage()
            //ta.invalidate()
            //tf.invalidate()
        }
        //ta.invalidate()
        //tf.invalidate()
    }

    def buildPanel() {
        swing.build {
            panel(name: 'Ressosurcen & Kapa') {

                migLayout(layoutConstraints: "", rowConstraints: "[][]", columnConstraints: "[]")
                panel(border: titledBorder('neue Ressource anlegen'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][][]", rowConstraints: "[][]")
                    label("Name", constraints: '')
                    textField(id: "resourceName", "hier_ein_eindeutiger_Name_ohne_Space", constraints: 'growx, wrap, span')
                    label("gelbe Grenze", constraints: '')
                    textField(id: "yellowLimit", "100", constraints: '')
                    label("Wochenkapazität h, Kg, m, ...", constraints: 'wrap')
                    label("rote Grenze", constraints: '')
                    textField(id: "redLimit", "200", constraints: '')
                    label("Wochenkapazität absolutes Maximum mit allen Flex-Möglichkeiten h, Kg, m, ...", constraints: 'wrap')
                    button("neu", actionPerformed: createResource, constraints: "span, growx, wrap")
                }
                //label(id: 'fileA', "noch nichts gewählt", constraints: 'wrap, growx')
                panel(border: titledBorder('Ressourcen bearbeiten'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][]", rowConstraints: "[]")
                    //button("kopieren", actionPerformed: createResource, constraints: "")
                    button("umbenennen", actionPerformed: renameResource, constraints: "")
                    label("Hier alles bis auf Ressourcen-Namen editieren", constraints: '')
                    label("Gibt's Fehler?", constraints: 'wrap')

                    //scrollPane(constraints: "span 2, grow") {
                        list(id: 'resourcesList', constraints: "grow")
                    //}

                    scrollPane(id: 'capaTextFileScrollPane', constraints: "h ${(int)(150 * MainGui.scaleY)}!, w ${(int)(300 * MainGui.scaleY)}!, growx, growy".toString()) {
                        textPane(id: 'capaTextFile', model.jsonSlurp, font: new Font(Font.MONOSPACED, Font.PLAIN, 12))//, constraints: "grow")
                    }
                    scrollPane(constraints: "w ${(int) (250 * MainGui.scaleY)}!, growy, growx, wrap".toString()) {
                        textArea(id: "errorMessageCapaEdit")
                    }
                }
            }
            //label(id: 'fileA', "noch nichts gewählt", constraints: 'wrap, growx')
            //button('Vergleichs-Datei B auswählen', actionPerformed: readFileB)
            //label(id: 'fileB', "noch nichts gewählt", constraints: "wrap, growx")
            //scrollPane(constraints: "span, grow") {
            //    textPane(id: 'diffTextArea', text: "ein Beispiel...", contentType: 'html', editorKit: new HTMLEditorKit())
            //}
            JTextPane ta = swing.capaTextFile
            JScrollPane sp = swing.capaTextFileScrollPane
            TextLineNumber ln = new TextLineNumber(ta)
            sp.setRowHeaderView(ln)

            ta.addKeyListener(new KeyListener() {
                void keyTyped(KeyEvent e) {checkYaml()}
                void keyPressed(KeyEvent e) {checkYaml()}
                void keyReleased(KeyEvent e) {checkYaml()}
            })
        }
        //swing.diffTextArea.setEditorKit(new HTMLEditorKit())
    }
}



