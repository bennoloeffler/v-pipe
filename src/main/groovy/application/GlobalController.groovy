package application

import model.DataWriter
import model.Model
import model.VpipeDataException
import utils.RunTimer
import utils.SystemInfo

import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.JSplitPane
import java.awt.event.ActionEvent

class GlobalController {

    Model model
    View view

    GlobalController(Model model, View view) {
        this.model = model
        this.view = view
    }

    def openActionPerformed = { ActionEvent e ->
        String dir = chooseDir('Datenverzeichnis öffnen', (JComponent)(e.source), "Verzeichnis auswählen & Daten-Dateien von dort lesen")
        if (dir) {
            openDir(dir)
        }

    }

    def saveActionPerformed = { ActionEvent e ->
        //println("saveActionPerformed")
        DataWriter dw = new DataWriter(model: model)
        dw.saveAll()
    }

    def exitActionPerformed = { ActionEvent e ->
        //println("exitActionPerformed")
        //DataWriter dw = new DataWriter(model: model)
        checkSaveBeforeExit()
    }

    def saveAsActionPerformed = { ActionEvent e ->
        //println("saveActionPerformed")
        String dir = chooseDir('Datenverzeichnis zum Speichern wählen', (JComponent)(e.source), "Verzeichnis auswählen & Daten-Dateien dort speichern")
        if (dir) {
            model.setCurrentDir(dir)
            DataWriter dw = new DataWriter(model: model)
            dw.saveAll()
        }
    }

    def sortPipelineActionPerformed = {ActionEvent e ->
        //println("sortPipelineActionPerformed")
        view.gridPipelineModel.sortProjectNamesToEnd()
    }

    def pipelineViewActionPerformed = { ActionEvent e ->
        view.openPipelineWindow()
    }

    def loadViewActionPerformed = { ActionEvent e ->
        view.openLoadWindow()
    }

    def pipelineLoadViewActionPerformed = { ActionEvent e ->
        view.openPipelineLoadWindow()
    }

    def projectViewActionPerformed = { ActionEvent e ->
        view.openProjectWindow()
    }

    def helpActionPerformed = {ActionEvent e ->
        Main.openBrowserWithHelp()
    }

    def printPerformanceActionPerformed = {ActionEvent e ->
        println(SystemInfo.getSystemInfoTable())
        println(RunTimer.getResultTable())
    }

    def compareActionPerformed = {ActionEvent e ->
        println(e.getSource())
    }




    private String chooseDir(String dialogTitle, JComponent root, String applyButtonText) {
        String result = null
        JFileChooser fc = new JFileChooser(new File(model.currentDir))
        fc.setDialogTitle(dialogTitle)
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY, )
        fc.setApproveButtonText(applyButtonText)
        int returnVal = fc.showOpenDialog(root)

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile()
            File rootDir = fc.getCurrentDirectory()
            result = "$rootDir.absolutePath/$dir.name"
            println("Verzeichnis: $result")
        } else {
            println("Verzeichnis-Wechsel abgebrochen.")
        }
        result
    }


    private void checkSaveBeforeExit() {
        int dialogButton = JOptionPane.YES_NO_CANCEL_OPTION
        dialogButton = JOptionPane.showConfirmDialog(null, "Speichern?", "HIRN EINSCHALTEN!", dialogButton)
        if (dialogButton == JOptionPane.YES_OPTION) {
            saveActionPerformed(null)
            view.swing.frame.dispose()
            System.exit(0)
        }
        if (dialogButton == JOptionPane.NO_OPTION) {
            view.swing.frame.dispose()
            System.exit(0)
        }
    }

    def openDir(String dir) {
        try {
            model.setCurrentDir(dir)
            view.gridPipelineModel.setSelectedProject(null)
            model.readAllData() // in EDT
            view.pipelineView.setCursorToNow()
            view.loadView.setCursorToNow()
            if (model.pipelineElements) {
                view.swing.pipelineLoadViewScrollPane.setVisible(true)
                view.swing.spV1.setDividerLocation(500)
                view.swing.spV2.setDividerLocation(500)
                view.swing.spV3.setDividerLocation(120)
            } else {
                view.swing.pipelineLoadViewScrollPane.setVisible(false)
            }
        } catch (VpipeDataException vde) {
            JOptionPane.showMessageDialog(null,
                    vde.message,
                    "DATEN-FEHLER beim Start",
                    JOptionPane.WARNING_MESSAGE)
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Stacktrace speichern. Bitte.\nNochmal in Console starten.\nDann speichern.\nFehler: $e.message",
                    "F I E S E R   FEHLER beim Start  :-(",
                    JOptionPane.ERROR_MESSAGE)
            throw e // to produce stacktrace on console...
        }
    }


}
