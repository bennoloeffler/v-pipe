package newview

import application.MainGui
import groovy.swing.SwingBuilder
import groovy.yaml.YamlBuilder
import groovy.yaml.YamlSlurper
import model.Model

import javax.swing.*
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.List

//TODO: Profil mit einem Feiertag und einem Kapa_Profil Eintrag - Kommentar: können gelöscht werden
//TODO: Profil kann ohne Feiertage und ohne Kapa-Profil gelesen werden
//TODO: Umbenennen.
//TODO: Button: wenn es keine Kapa gibt: createNewCapaProfile
//TODO: Button: applyCapa
//TODO: implement Rename Button
//TODO: Rename in tasks and other elements, see: emptyTheModel()
//TODO: fireUpdate
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
            checkYaml()
        }
    }

    def saveRessources = {

        println "saveResourcen"
    }

    String checkDuplicateRessourceName() {
        println "checkDuplicateRessourceName"
        return null
    }

    def renameResource ={
        println "rename Resource"
        String oldName = swing.resourcesList.selectedValue
        String errMessage = ""
        if(oldName) {
            String newName = swing.newRessourceName.text.replace(" ", "_")
            if(! model.allDepartments.contains(newName)) {
                if(newName?.size() > 0) {
                    model.renameDepartment(oldName, newName)
                    return
                } else {
                    errMessage = "Neuer Name fehlt. Bitte eintragen."
                }
            }  else {
                errMessage = "Neuer Name ($newName) existiert schon."
            }
        } else {
            errMessage = "Keine Ressource zum umbenennen ausgewählt."
        }
        JOptionPane.showConfirmDialog(
                swing.manageResourcesPanel as Component,
                errMessage,
                "Fehler",
                JOptionPane.OK_OPTION,
                JOptionPane.WARNING_MESSAGE
                )
    }

    def createResource = {
        println "create Resource"
        def yl = swing.yellowLimit.text
        try {
            yl = Double.parseDouble(yl)
        } catch (Exception e){
            yl = 0
        }
        def rl = swing.redLimit.text
        try {
            rl = Double.parseDouble(rl)
        } catch (Exception e){
            rl = 0
        }
        String rn = swing.resourceName.text.trim()
        rn.replace(" ", "_")
        if(rn.size() == 0) {
            rn = "Ressource_" + Math.abs(new Random().nextInt())
        }
        String result = "  $rn:\n    Kapa:\n      gelb: $yl\n      rot: $rl\n"
        swing.capaTextFile.text += result
        checkYaml()
        if(!checkDuplicateRessourceName()){
            saveRessources()
        }
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

    boolean checkYaml() {
        JTextPane ta = swing.capaTextFile
        JTextArea tf = swing.errorMessageCapaEdit

        try {
            def slurper = new YamlSlurper()
            def result = slurper.parseText(ta.text)
            def capaMap = model.calcCapa(result, false)
            //model.check()
            def renameDepartmentsErr = checkDepartments(capaMap)
            if (renameDepartmentsErr) {
                tf.text = renameDepartmentsErr
                ta.setForeground(Color.RED)
                return false
            } else {
                def doubleRessourceName = checkDuplicateRessourceName()
                if(!doubleRessourceName) {
                    tf.text = "keine Fehler... alles dufte."
                    ta.setForeground(fg)
                    return true
                }else{
                    tf.text = "doppelt vergebene Ressourcennamen: xyz"
                    ta.setForeground(Color.RED)
                }
            }
        } catch (Throwable e) {
            ta.setForeground(Color.RED)
            tf.text = e.getMessage()
            return false
            //ta.invalidate()
            //tf.invalidate()
        }
        //ta.invalidate()
        //tf.invalidate()
    }

    def buildPanel() {
        swing.build {
            panel(id: "manageResourcesPanel", name: 'Ressosurcen & Kapa') {

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
                    button("neu erzeugen und speichern", actionPerformed: createResource, constraints: "span, growx, wrap")
                }
                //label(id: 'fileA', "noch nichts gewählt", constraints: 'wrap, growx')
                panel(border: titledBorder('Ressource umbenennen'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][]", rowConstraints: "[]")
                    //button("kopieren", actionPerformed: createResource, constraints: "")
                    list(id: 'resourcesList', selectionMode: ListSelectionModel.SINGLE_SELECTION, constraints: "span 1 4, grow")
                    label("neuer Name für die selektierte Ressource:", constraints: 'wrap')
                    textField(id: "newRessourceName", "", constraints: 'grow, wrap')
                    button("umbenennen und speichern", actionPerformed: renameResource, constraints: "grow, wrap")


                }
                panel(border: titledBorder('Ressourcen bearbeiten'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][]", rowConstraints: "[]")
                    //button("kopieren", actionPerformed: createResource, constraints: "")
                    //button("umbenennen", actionPerformed: renameResource, constraints: "")
                    label("Hier alles bis auf Ressourcen-Namen editieren", constraints: '')
                    label("Gibt's Fehler?", constraints: 'wrap')

                    //scrollPane(constraints: "span 2, grow") {
                    //list(id: 'resourcesList', constraints: "grow")
                    //}

                    scrollPane(id: 'capaTextFileScrollPane', constraints: "h ${(int)(150 * MainGui.scaleY)}!, w ${(int)(300 * MainGui.scaleY)}!, growx, growy".toString()) {
                        textPane(id: 'capaTextFile', model.jsonSlurp, font: new Font(Font.MONOSPACED, Font.PLAIN, 12))//, constraints: "grow")
                    }
                    scrollPane(constraints: "w ${(int) (250 * MainGui.scaleY)}!, growy, growx, wrap".toString()) {
                        textArea(id: "errorMessageCapaEdit")
                    }
                    button("speichern", actionPerformed: saveRessources, constraints: "span, grow, wrap")

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



