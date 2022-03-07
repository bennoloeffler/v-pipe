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

class GlobalController {

    Model model
    View view

    GlobalController(Model model, View view) {
        this.model = model
        this.view = view
        Timer timer = new Timer(10000, saveTimerAction);
        timer.setRepeats(true)
        timer.start();
    }

    def openActionPerformed = { ActionEvent e ->
        if (checkSave(false)) {
            JCheckBoxMenuItem i = view.swing.checkBoxMenuContSaving
            i.setSelected(false)
            toggleContinouosSaveAsActionPerformed(null)
            String dir = chooseDir('Datenverzeichnis öffnen', (JComponent) (e.source), "Verzeichnis auswählen & Daten-Dateien von dort lesen", true)
            if (dir) {
                openDir(dir)
            }
        }
    }

    def saveActionPerformed = { ActionEvent e ->
        //println("saveActionPerformed")
        if (model.isDirty()) {
            DataWriter dw = new DataWriter(model: model)
            dw.saveAll()
        }
    }


    boolean autoSave = false
    def saveTimerAction = {
        if (model.isDirty() && autoSave) {
            view.swing.doLater {    //doOutside crashes
                DataWriter dw = new DataWriter(model: model)
                dw.saveAll()
                println "gespeichert..."
            }
        }
    }

    def toggleContinouosSaveAsActionPerformed = { ActionEvent e ->
        JCheckBoxMenuItem i = view.swing.checkBoxMenuContSaving
        autoSave = i.isSelected()
        println "Auto-Speichern: " + (autoSave?"an":"aus")
    }

    def exitActionPerformed = { ActionEvent e ->
        //println("exitActionPerformed")
        //DataWriter dw = new DataWriter(model: model)
        checkSave(true)
    }

    def saveAsActionPerformed = { ActionEvent e ->
        //println("saveActionPerformed")
        String dir = chooseDir('Datenverzeichnis zum Speichern wählen', (JComponent) (e.source), "Verzeichnis auswählen & Daten-Dateien dort speichern", false)
        if (dir) {
            model.setCurrentDir(dir)
            DataWriter dw = new DataWriter(model: model)
            dw.saveAll()
        }
    }

    def sortPipelineActionPerformed = { ActionEvent e ->
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

    def helpActionPerformed = { ActionEvent e ->
        Main.openBrowserWithHelp()
    }

    def printPerformanceActionPerformed = { ActionEvent e ->
        println(SystemInfo.getSystemInfoTable())
        println(RunTimer.getResultTable())
        runMultiThreadedTest()
    }

    def compareActionPerformed = { ActionEvent e ->
        println(e.getSource())
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

            //fc.resetChoosableFileFilters()
            //fc.setAcceptAllFileFilterUsed(false)
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
            //File rootDir = fc.getCurrentDirectory()
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
            model.readAllData() // in EDT
            view.pipelineView.setCursorToNow()
            view.loadView.setCursorToNow()
            if (model.pipelineElements) {
                //view.swing.spV1.setDividerLocation((int)(300 * MainGui.scaleY))
                //view.swing.spV2.setDividerLocation((int)(100 * MainGui.scaleY))
                view.swing.spV3.setDividerLocation(((int) (100 * MainGui.scaleY)))
                view.swing.pipelineLoadViewScrollPane.setVisible(true)
            } else {
                view.swing.pipelineLoadViewScrollPane.setVisible(false)
            }
            println "Daten-Verzeichnis: " + dir
            //view.swing.frame.validate()
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
