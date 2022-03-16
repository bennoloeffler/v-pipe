package gui.panels

import groovy.swing.SwingBuilder

import javax.swing.*
import java.awt.*

class FileDifferPanel {

    String currentDir = "."
    String fileA
    String fileB

    SwingBuilder swing
    Color fg = UIManager.getLookAndFeelDefaults().get("TextField.foreground") as Color

    FileDifferPanel(SwingBuilder swing) {
        this.swing = swing // shortcut
    }

    private String chooseFile() {
        String result = null
        JFileChooser fc = new JFileChooser(currentDir)
        fc.setDialogTitle("Datei auswählen")
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
        int returnVal = fc.showOpenDialog(swing.differPanel)

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile()
            result = dir.absolutePath
            println("Datei: $result")
        } else {
            println("Datei Auswahl abgebrochen.")
        }
        result
    }

    def readFileA = {
        fileA = chooseFile()
        if(fileA) {
            swing.fileA.text = fileA
        }
    }

    def readFileB = {
        fileB = chooseFile()
        if(fileB) {
            swing.fileB.text = fileB
        }
        diffFiles()
    }

    def diffFiles(){
        if(fileA && fileB && new File(fileA).exists() && new File(fileB).exists()) {
            String aText = new File(fileA).text
            String bText = new File(fileB).text
            String html = get().diff_prettyHtml(get().diff_main(aText, bText))
            def fHtml = new File("diff.html")
            fHtml.text = html
            Desktop.getDesktop().open(fHtml)
        }
    }


    def buildPanel() {
        swing.build {
            panel(id: 'differPanel', name: 'Differenz Dateien') {
                migLayout(layoutConstraints:"fill", columnConstraints:"[][][grow]", rowConstraints:"[][][grow]")
                button("Datei A auswählen", actionPerformed: readFileA)
                label(id: 'fileA', "noch nichts gewählt", constraints: 'wrap, growx')
                button('Vergleichs-Datei B auswählen', actionPerformed: readFileB)
                label(id: 'fileB', "noch nichts gewählt", constraints: "wrap, growx")
                //scrollPane(constraints: "span, grow") {
                //    textPane(id: 'diffTextArea', text: "ein Beispiel...", contentType: 'html', editorKit: new HTMLEditorKit())
                //}
            }
            //swing.diffTextArea.setEditorKit(new HTMLEditorKit())
        }
    }

}

