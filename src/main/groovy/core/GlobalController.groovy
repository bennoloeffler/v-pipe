package core

import application.Main
import gui.View
import model.DataReader
import model.DataWriter
import model.Model
import model.VpipeDataException
import utils.RunTimer
import utils.SystemInfo
import utils.UserSettingsStore

import javax.swing.*
import javax.swing.filechooser.FileFilter
import java.awt.event.ActionEvent

import static model.DataReader.TASK_FILE_NAME
import static model.DataReader.isValidModelFolder

class GlobalController {

    Model model
    View view

    GlobalController(Model model, View view) {
        this.model = model
        this.view = view
        addLastDirsToRecentMenu()
        Timer timer = new Timer(10000, saveTimerAction);
        timer.setRepeats(true)
        timer.start();


        //
        // glue view and controller together
        //

        // File
        view.swing.newModelAction.closure = newModelActionPerformed
        view.swing.openAction.closure = openActionPerformed
        view.swing.saveAction.closure = saveActionPerformed
        view.swing.saveAsAction.closure = saveAsActionPerformed
        view.swing.toggleContinouosSaveAsAction.closure = toggleContinouosSaveAsActionPerformed
        view.swing.exitAction.closure = exitActionPerformed

        // Tool
        view.swing.sortPipelineAction.closure = sortPipelineActionPerformed
        view.swing.swapTemplatesAndProjectsAction.closure = swapTemplatesAndProjectsActionPerformed
        view.swing.readProjectUpdatesAction.closure = readProjectUpdatesActionPerformed


        // View
        view.swing.toggleViewInPhaseAction.closure = toggleViewInPhaseActionPerformed
        view.swing.pipelineViewAction.closure = pipelineViewActionPerformed
        view.swing.loadViewAction.closure = loadViewActionPerformed
        view.swing.pipelineLoadViewAction.closure = pipelineLoadViewActionPerformed
        view.swing.projectViewAction.closure = projectViewActionPerformed

        // Help
        view.swing.helpAction.closure = helpActionPerformed
        view.swing.printPerformanceAction.closure = printPerformanceActionPerformed


    }

    def addLastDirsToRecentMenu() {
        def last = UserSettingsStore.instance
                .getRecentOpenedDataFolders().reverse().take(25)
        JMenu m = view.swing.recentMenuItem
        m.removeAll()
        def allRecentMenuItems = last.collect { String dir ->
            def nameOnly = dir.split("/").last()
            def mi = new JMenuItem(
                    view.swing.action(
                            name: nameOnly,
                            closure: {
                                if (checkSave(false)) {
                                    setSaveForValidModel()
                                    openDir(dir)
                                }
                            },
                            shortDescription: dir
                    ))
            if (!isValidModelFolder(dir)) {
                mi.getAction().setEnabled(false)
            }
            mi
        }
        allRecentMenuItems.each { m.add(it) }
    }

    def addToRecentMenu(dir) {
        UserSettingsStore.instance.addLastOpenedDataFolder(dir)
        addLastDirsToRecentMenu()
    }


    def newModelActionPerformed = {
        if (checkSave(false)) {
            view.deselectProject()
            model.newModel()
            setSaveForEmptyModel()
            Action a = view.swing.saveAsAction
            a.setEnabled(true) // BUT: use saveAS...
            switchAutoSave(false)
        }
    }


    def openActionPerformed = { ActionEvent e ->
        if (checkSave(false)) {
            //Action a = view.swing.saveAction
            //a.setEnabled(true)
            String dir = chooseDir('Datenverzeichnis öffnen', (JComponent) (e.source), "Verzeichnis auswählen & Daten-Dateien von dort lesen", true)
            if (dir) {
                switchAutoSave(false)
                setSaveForValidModel()
                openDir(dir)
            }
        }
    }

    def saveActionPerformed = { ActionEvent e ->
        if (model.isDirty() && model.taskList) {
            DataWriter dw = new DataWriter(model: model)
            dw.saveAll()
            model.setDirty(false)
        }
    }


    boolean autoSave = false
    def saveTimerAction = {
        Action a = view.swing.saveAction
        def saveAllowed = a.isEnabled() //saving not allowed in general, eg due to empty model or swapped
        if (model.isDirty() && autoSave && saveAllowed) {
            autosavingInProgress = true
            view.swing.doOutside {
                def start = System.currentTimeMillis()
                DataWriter dw = new DataWriter(model: model)
                dw.saveAll()
                def end = System.currentTimeMillis()
                println "gespeichert...${end - start} ms"
                view.swing.doLater {
                    model.setDirty(false)
                    autosavingInProgress = false
                }
            }
        }
    }

    boolean autosavingInProgress = false

    def checkAutoSave() {
        //just wait a little bit in
        while (autosavingInProgress) {
            Thread.sleep(100)
        }
    }


    def switchAutoSave(boolean on) {
        JCheckBoxMenuItem i = view.swing.checkBoxMenuContSaving
        i.setSelected(on)
        toggleContinouosSaveAsActionPerformed(null)
    }

    def toggleContinouosSaveAsActionPerformed = { ActionEvent e ->
        JCheckBoxMenuItem i = view.swing.checkBoxMenuContSaving
        autoSave = i.isSelected()
        println "Auto-Speichern: " + (autoSave ? "an" : "aus")
    }

    def exitActionPerformed = { ActionEvent e ->
        swapToNormalStateIfNeeded()
        checkAutoSave()
        checkSave(true)
    }

    def swapToNormalStateIfNeeded() {
        if (model.isProjectsAndTemplatesSwapped()) {
            swapTemplatesAndProjectsActionPerformed()
            println "swapped back projects and templates"
        }
    }

    def saveAsActionPerformed = { ActionEvent e ->
        String dir = chooseDir('Datenverzeichnis zum Speichern wählen', (JComponent) (e.source), "Verzeichnis auswählen & Daten-Dateien dort speichern", false)
        if (dir) {
            model.setCurrentDir(dir)
            DataWriter dw = new DataWriter(model: model)
            dw.saveAll()
            model.setDirty(false)
            addToRecentMenu(dir)
            setSaveForValidModel()
        }
    }


    def toggleViewInPhaseActionPerformed = {
        view.setShowIntegrationPhase(!view.showIntegrationPhase)
        showPipelineLoad()
        println "anzeigen IPs: " + (view.showIntegrationPhase ? "an" : "aus")
        if (!model.pipelineElements) {
            println "Es gibt keine Integrations-Phasen... kein Effekt."
        }
    }

    def sortPipelineActionPerformed = { ActionEvent e ->
        view.gridPipelineModel.sortProjectNamesToEnd()
    }

    def setNewLoadAndSave(boolean how) {
        Action a = view.swing.saveAction
        a.setEnabled(how)
        a = view.swing.saveAsAction
        a.setEnabled(how)
        a = view.swing.newModelAction
        a.setEnabled(how)
        a = view.swing.openAction
        a.setEnabled(how)
        a = view.swing.toggleContinouosSaveAsAction
        a.setEnabled(how)
        JMenu m = view.swing.recentMenuItem
        m.setEnabled(how)
    }

    def swapTemplatesAndProjectsActionPerformed = {
        view.deselectProject()
        model.swapTemplatesAndProjects()
        if (model.projectsAndTemplatesSwapped) {
            setNewLoadAndSave(false)
        } else {
            setNewLoadAndSave(true)
        }
        model.fireUpdate()
    }

    def readProjectUpdatesActionPerformed = {
        view.deselectProject()

        // Start plugin to transform data to input format
        GroovyShell shell = new GroovyShell()
        shell.evaluate(new File(DataReader.get_UPDATE_PLUGIN_FILE_NAME()))

        if (DataReader.isDataInUpdateFolder()) {
            def updates = model.readUpdatesFromUpdateFolder()
            def updatedStr = "\nneue Projekte:\n" +
                    (updates.new ? updates.new.join("\n") : "keine") +
                    "\n\naktualisierte Projekte:\n" +
                    (updates.updated ? updates.updated.join("\n") : "keine")
            if (updates.err) {
                // this is a stupid workaround: the error message contains the wrong filename
                if(updates.err.contains("enthält keine Daten")) updates.err = "Datei enthält keine Daten.\n"
                JOptionPane.showMessageDialog(null, "Datei:\n" + DataReader.get_UPDATE_TASK_FILE_NAME() +"\n" + updates.err, "Fehler beim lesen des Update!", JOptionPane.ERROR_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(null, updatedStr, "Update!", JOptionPane.INFORMATION_MESSAGE)
            }
        } else {
            JOptionPane.showMessageDialog(null, "Im Verzeichnis zum Update der Projektdaten liegen keine Daten:\n" + DataReader.updateDir(), "Keine Daten!", JOptionPane.INFORMATION_MESSAGE)
        }
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

    def helpActionPerformed = { ActionEvent e ->
        Main.openBrowserWithHelp()
    }

    def printPerformanceActionPerformed = { ActionEvent e ->
        println(SystemInfo.getSystemInfoTable())
        println(RunTimer.getResultTable())
    }

    def compareActionPerformed = { ActionEvent e ->
        println(e.getSource())
    }

    JFileChooser fc = null

    boolean chooseDirWhileOpen

    private String chooseDir(String dialogTitle, JComponent root, String applyButtonText, Boolean open) {
        String result = null
        if (!fc) {
            fc = new JFileChooser(new File(model.currentDir)) {

                def acceptVPipeDir() {
                    if (chooseDirWhileOpen) {
                        def checkPath1 = getSelectedFile().getAbsolutePath() + "/" + TASK_FILE_NAME
                        def rightDirectorySelected = new File(checkPath1).exists()
                        def rightFileSelected = false
                        if (getSelectedFile().isFile()) {
                            def checkPath2 = getSelectedFile().parentFile.getAbsolutePath() + "/" + TASK_FILE_NAME
                            rightFileSelected = new File(checkPath2).exists()
                        }
                        return rightDirectorySelected || rightFileSelected
                    } else {
                        return true
                    }
                }

                void approveSelection() {
                    if (acceptVPipeDir()) {
                        super.approveSelection()
                    }
                }
            }

            fc.setFileFilter(new FileFilter() {
                @Override
                boolean accept(File f) {

                    def checkPath = f.getAbsolutePath() + "/" + TASK_FILE_NAME
                    def checkNeigbour = f.parentFile.getAbsolutePath() + "/" + TASK_FILE_NAME
                    def pathExists = new File(checkPath).exists()
                    def neighbourExists = new File(checkNeigbour).exists()
                    return pathExists || neighbourExists || f.isDirectory()
                    /*
                    // DOES NOT WORK???
                    DataWriter.ALL_DATA_FILES.each {
                        if(f.isFile()){
                            String dataFileName = it
                            String checkFileName = f.getName()
                            //Thread.start{println "${dataFileName == checkFileName} --> $dataFileName ==? $checkFileName"}
                            if(dataFileName == checkFileName) {
                                //Thread.start{println f.getAbsolutePath()}
                                return true
                            }
                        }
                    }
                    return f.isDirectory()
                     */
                }

                @Override
                String getDescription() {
                    return "$TASK_FILE_NAME available?"
                }
            })
        }
        fc.setDialogTitle(dialogTitle)
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES)
        fc.setApproveButtonText(applyButtonText)
        fc.setApproveButtonToolTipText("Im gewählten Verzeichnis müssen gültige Daten liegen.")

        int returnVal = fc.showOpenDialog(root)

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File dir = fc.getSelectedFile()
            if (!dir.isDirectory()) {
                dir = dir.parentFile
            }
            result = dir.absolutePath
            println("Verzeichnis: $result")
        } else {
            println("Verzeichnis-Wechsel abgebrochen.")
        }
        result
    }


    private boolean checkSave(doExitThen) {
        if (model.isDirty()) {
            def dialogButton = JOptionPane.showConfirmDialog(null, "Speichern?", "HIRN EINSCHALTEN!", JOptionPane.YES_NO_CANCEL_OPTION)
            if (dialogButton == JOptionPane.YES_OPTION) {
                saveActionPerformed(null)
            }
            if (dialogButton == JOptionPane.CANCEL_OPTION) {
                return false
            }
        }

        if (doExitThen) {
            view.swing.frame.dispose()
            System.exit(0)
        }
        return true
    }

    def showPipelineLoad() {
        view.showPipelineLoad()
    }

    def openDir(String dir) {
        try {
            model.setCurrentDir(dir)
            view.deselectProject()
            model.readAllData()
            view.pipelineView.setCursorToNow()
            view.loadView.setCursorToNow()
            showPipelineLoad()
            addToRecentMenu(dir)
            model.setDirty(false)
            println "Daten-Verzeichnis: " + dir
        } catch (VpipeDataException vde) {
            JOptionPane.showMessageDialog(null,
                    vde.message,
                    "DATEN-FEHLER beim Start",
                    JOptionPane.WARNING_MESSAGE)
            setEmptyModel()
            setSaveForEmptyModel()
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Stacktrace speichern. Bitte.\nNochmal in Console starten.\nDann speichern.\nFehler: $e.message",
                    "F I E S E R   FEHLER beim Start  :-(",
                    JOptionPane.ERROR_MESSAGE)
            setEmptyModel()
            setSaveForEmptyModel()

            throw e // to produce stacktrace on console...
        }
    }

    def setSaveForEmptyModel() {
        Action a = view.swing.saveAction
        a.setEnabled(false)
        a = view.swing.saveAsAction
        a.setEnabled(false)
        a = view.swing.toggleContinouosSaveAsAction
        a.setEnabled(false)
    }

    def setEmptyModel() {
        model.emptyTheModel()
        model.setVPipeHomeDir()
        model.setUpdateToggle(!model.updateToggle)
    }

    def setSaveForValidModel() {
        Action a = view.swing.saveAction
        a.setEnabled(true)
        a = view.swing.saveAsAction
        a.setEnabled(true)
        a = view.swing.toggleContinouosSaveAsAction
        a.setEnabled(true)
    }
}
