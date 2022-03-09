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
            String capaYamlText = ""
            if (this.model.jsonSlurp) {
                yb(this.model.jsonSlurp)
                capaYamlText = yb.toString()
                capaYamlText = capaYamlText.replace("---\n", "")
            }
            //t.setText(DataReader.capaTextCache?:"keine Kapazitätsinformation")
            t.setText(capaYamlText)
            checkYaml()
        }
    }

    def createNewCapaModel = {
        def allDeps = this.model.getAllDepartments() ?: []
        String capaStart =
                '''Kapa_Gesamt:

# hier keine Einträge?
# dann: Feiertage: []
# oder: Kapa_Profil: {}

  Feiertage:
  - "1.1.2020"
  - "10.04.2020"
  
  Kapa_Profil:
    "2020-W21": 50
    "2020-W22": 50
    
Kapa_Abteilungen:

'''
        def kapaDep =
                '''    Kapa:
      gelb: 340
      rot: 460
      
'''
        String allDepsJoined = allDeps.collect { "\n  " + it + ":\n$kapaDep" }.join("")

        String capaEnd =
                '''\n  Beispiel_Ress_1:
    Kapa:
      gelb: 200
      rot: 400
    Kapa_Profil:
      #ueberschreibt die Firmen-Werte
      "2020-W01": 100 # nur in KW
      "2020-W02": 100 # nur in KW
      #ueberschreibt den Ressourcen-Standard
      "2020-W34": # ab dann
        gelb: 120
        rot: 150
      "2020-W38": # ab dann
        gelb: 200
        rot: 400

  Beispiel_Ress_2:
    Kapa:
      gelb: 160
      rot: 200
    Kapa_Profil: {} # Profil leer
'''
        swing.capaTextFile.text = capaStart + allDepsJoined + capaEnd
        checkYaml()
    }

    def saveRessources = {
        JTextPane ta = swing.capaTextFile
        model.saveRessources("---\n" + ta.text)
        //println "saveResourcen"
    }

    String checkDuplicateRessourceName() {
        //println "checkDuplicateRessourceName"
        return null
    }

    def renameResource = {
        //println "rename Resource"
        String oldName = swing.resourcesList.selectedValue
        String errMessage = ""
        if (oldName) {
            String newName = swing.newRessourceName.text.replace(" ", "_")
            if (!model.allDepartments.contains(newName)) {
                if (newName?.size() > 0) {
                    model.renameDepartment(oldName, newName)
                    return
                } else {
                    errMessage = "Neuer Name fehlt. Bitte eintragen."
                }
            } else {
                errMessage = "Neuer Name ($newName) existiert schon."
            }
        } else {
            errMessage = "Keine Ressource zum umbenennen ausgewählt."
        }
        JOptionPane.showConfirmDialog(
                swing.manageResourcesPanel as Component,
                errMessage,
                "Fehler",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
        )
    }

    def createResource = {
        //println "create Resource"
        def yl = swing.yellowLimit.text
        try {
            yl = Double.parseDouble(yl)
        } catch (Exception e) {
            yl = 0
        }
        def rl = swing.redLimit.text
        try {
            rl = Double.parseDouble(rl)
        } catch (Exception e) {
            rl = 0
        }
        String rn = swing.resourceName.text.trim()
        rn = rn.replace(" ", "_")
        if (rn.size() == 0) {
            rn = "Ressource_" + Math.abs(new Random().nextInt())
        }
        String result = "  $rn:\n    Kapa:\n      gelb: $yl\n      rot: $rl\n"
        swing.capaTextFile.text += result
        checkYaml()
        if (!checkDuplicateRessourceName()) {
            saveRessources()
        }
    }

    def checkDepartments(capaAvailable) {
        List<String> depsTasks = model.taskList*.department.unique()
        Set<String> depsCapa = capaAvailable.keySet()
        List<String> remain = (List<String>) (depsTasks.clone())
        remain.removeAll(depsCapa)
        if (remain) {
            "Abteilungen werden genutzt,\nfehlen aber hier:\n$remain"
        } else {
            null
        }
    }

    boolean checkYaml() {
        JTextPane ta = swing.capaTextFile
        JTextArea tf = swing.errorMessageCapaEdit
        JButton b = swing.yamlSaveButton
        b.setEnabled(false)
        JButton byc = swing.yamlCreateButton
        byc.setEnabled(true)

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
                if (!doubleRessourceName) {
                    tf.text = "keine Fehler... alles dufte."
                    ta.setForeground(fg)
                    byc.setEnabled(false)
                    b.setEnabled(true)
                    return true
                } else {
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
                panel(border: titledBorder('Ressourcen bearbeiten'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][]", rowConstraints: "[]")
                    //button("kopieren", actionPerformed: createResource, constraints: "")
                    //button("umbenennen", actionPerformed: renameResource, constraints: "")
                    label("Hier alles bis auf Ressourcen-Namen editieren", constraints: '')
                    label("Gibt's Fehler?", constraints: 'wrap')

                    //scrollPane(constraints: "span 2, grow") {
                    //list(id: 'resourcesList', constraints: "grow")
                    //}

                    scrollPane(id: 'capaTextFileScrollPane', constraints: "h ${(int) (150 * MainGui.scaleY)}!, w ${(int) (300 * MainGui.scaleY)}!, growx, growy".toString()) {
                        textPane(id: 'capaTextFile', model.jsonSlurp, font: new Font(Font.MONOSPACED, Font.PLAIN, 12))
                    }
                    scrollPane(constraints: "w ${(int) (250 * MainGui.scaleY)}!, growy, growx, wrap".toString()) {
                        textArea(id: "errorMessageCapaEdit")
                    }
                    button(id: "yamlSaveButton", "speichern", actionPerformed: saveRessources, constraints: "span, grow, wrap")
                    button(id: "yamlCreateButton", "Kapa-Profil neu erstellen", actionPerformed: createNewCapaModel, constraints: "span, grow, wrap")

                }
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
                panel(border: titledBorder('Ressource umbenennen'), constraints: 'wrap') {
                    migLayout(layoutConstraints: "fill", columnConstraints: "[][][][]", rowConstraints: "[]")
                    //button("kopieren", actionPerformed: createResource, constraints: "")
                    list(id: 'resourcesList', selectionMode: ListSelectionModel.SINGLE_SELECTION, constraints: "span 1 4, grow")
                    label("neuer Name für die selektierte Ressource:", constraints: 'wrap')
                    textField(id: "newRessourceName", "", constraints: 'grow, wrap')
                    button("umbenennen und speichern", actionPerformed: renameResource, constraints: "grow, wrap")
                }
            }

            //
            // line numbers in JTextPane
            //
            JTextPane ta = swing.capaTextFile
            JScrollPane sp = swing.capaTextFileScrollPane
            TextLineNumber ln = new TextLineNumber(ta)
            sp.setRowHeaderView(ln)

            ta.addKeyListener(new KeyListener() {
                void keyTyped(KeyEvent e) { checkYaml() }

                void keyPressed(KeyEvent e) { checkYaml() }

                void keyReleased(KeyEvent e) { checkYaml() }
            })
        }
    }
}



