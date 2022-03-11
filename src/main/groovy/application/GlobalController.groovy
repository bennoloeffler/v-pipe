package application

import model.DataReader
import model.DataWriter
import model.Model
import model.VpipeDataException
import utils.RunTimer
import utils.SystemInfo

import javax.swing.*
import javax.swing.filechooser.FileFilter
import java.awt.event.ActionEvent

// TODO: keep users from saving, when model is emtpy or new and you are in a valid directory (e.g. force safe as...)
// TODO: after startup, disable "save", when dirty: enable (disable/enable the action)
// TODO: hypothesis: exit kills the current automatic save operation FIX: wait for save to have happened)
//
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


        // View
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
            new JMenuItem(
                    view.swing.action(
                            name: nameOnly,
                            closure: {
                                if (checkSave(false)) {
                                    openDir(dir)
                                }
                            },
                            shortDescription: dir
                    ))
        }
        allRecentMenuItems.each { m.add(it) }
    }

    def addToRecentMenu(dir) {
        UserSettingsStore.instance.addLastOpenedDataFolder(dir)
        addLastDirsToRecentMenu()
    }


    def openActionPerformed = { ActionEvent e ->
        if (checkSave(false)) {
            switchAutoSave(false)
            String dir = chooseDir('Datenverzeichnis öffnen', (JComponent) (e.source), "Verzeichnis auswählen & Daten-Dateien von dort lesen", true)
            if (dir) {
                openDir(dir)
            }
        }
    }

    def saveActionPerformed = { ActionEvent e ->
        if (model.isDirty() && model.taskList) {
            swapToNormalStateIfNeeded() // TODO check that!
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
            view.swing.doOutside {
                def start = System.currentTimeMillis()
                DataWriter dw = new DataWriter(model: model)
                dw.saveAll()
                def end = System.currentTimeMillis()
                println "gespeichert... ${end - start}ms"
                view.swing.doLater {
                    model.setDirty(false)
                }
            }
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
        checkSave(true)
    }

    def swapToNormalStateIfNeeded() {
        if(model.isProjectsAndTemplatesSwapped()) {
            swapTemplatesAndProjectsActionPerformed()
            println "swapped back projects and templates"
        }
    }

    def saveAsActionPerformed = { ActionEvent e ->
        String dir = chooseDir('Datenverzeichnis zum Speichern wählen', (JComponent) (e.source), "Verzeichnis auswählen & Daten-Dateien dort speichern", false)
        if (dir) {
            swapToNormalStateIfNeeded() // TODO check that!
            model.setCurrentDir(dir)
            DataWriter dw = new DataWriter(model: model)
            dw.saveAll()
            model.setDirty(false)
            addToRecentMenu(dir)
            Action a = view.swing.saveAction
            a.setEnabled(true)
        }
    }

    def sortPipelineActionPerformed = { ActionEvent e ->
        view.gridPipelineModel.sortProjectNamesToEnd()
    }

    def swapTemplatesAndProjectsActionPerformed = {
        view.gridPipelineModel.setSelectedProject(null)
        model.swapTemplatesAndProjects()
        if ( model.projectsAndTemplatesSwapped) {
            Action a = view.swing.saveAction
            a.setEnabled(false)
            a = view.swing.saveAsAction
            a.setEnabled(false)
        } else {
            Action a = view.swing.saveAction
            a.setEnabled(true)
            a = view.swing.saveAsAction
            a.setEnabled(true)
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

    def newModelActionPerformed = {
        if (checkSave(false)) {
            model.newModel()
            Action a = view.swing.saveAction
            a.setEnabled(false) // use saveAS...
            switchAutoSave(false)
        }
    }


    JFileChooser fc = null

    private String chooseDir(String dialogTitle, JComponent root, String applyButtonText, boolean open) {
        String result = null
        def currentOpen = { open }
        if (!fc) {
            fc = new JFileChooser(new File(model.currentDir)) {

                def acceptVPipeDir() {
                    if (currentOpen()) {
                        def checkPath1 = getSelectedFile().getAbsolutePath() + "/" + DataReader.TASK_FILE_NAME
                        def rightDirectorySelected = new File(checkPath1).exists()
                        def rightFileSelected = false
                        if (getSelectedFile().isFile()) {
                            def checkPath2 = getSelectedFile().parentFile.getAbsolutePath() + "/" + DataReader.TASK_FILE_NAME
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

                    def checkPath = f.getAbsolutePath() + "/" + DataReader.TASK_FILE_NAME
                    def checkNeigbour = f.parentFile.getAbsolutePath() + "/" + DataReader.TASK_FILE_NAME
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
                    return "$DataReader.TASK_FILE_NAME available?"
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

    def openDir(String dir) {
        try {
            model.setCurrentDir(dir)
            view.gridPipelineModel.setSelectedProject(null)
            model.readAllData()
            view.pipelineView.setCursorToNow()
            view.loadView.setCursorToNow()
            if (model.pipelineElements) {
                view.swing.spV3.setDividerLocation(((int) (100 * MainGui.scaleY)))
                view.swing.pipelineLoadViewScrollPane.setVisible(true)
            } else {
                view.swing.pipelineLoadViewScrollPane.setVisible(false)
            }
            addToRecentMenu(dir)
            model.setDirty(false)
            println "Daten-Verzeichnis: " + dir
        } catch (VpipeDataException vde) {
            JOptionPane.showMessageDialog(null,
                    vde.message,
                    "DATEN-FEHLER beim Start",
                    JOptionPane.WARNING_MESSAGE)
            makeGuiEmpty()
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Stacktrace speichern. Bitte.\nNochmal in Console starten.\nDann speichern.\nFehler: $e.message",
                    "F I E S E R   FEHLER beim Start  :-(",
                    JOptionPane.ERROR_MESSAGE)
            makeGuiEmpty()

            throw e // to produce stacktrace on console...
        }
    }

    def makeGuiEmpty() {
        model.emptyTheModel()
        Action a = view.swing.saveAction
        a.setEnabled(false)
        model.setEmptyDir()
        model.setUpdateToggle(!model.updateToggle)
    }
}
