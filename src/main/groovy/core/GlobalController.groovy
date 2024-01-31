package core

import application.MainGui
import extensions.DateHelperFunctions
import extensions.StringExtension
import gui.View
import gui.panels.ModelReaderMessagePanel
import model.DataReader
import model.DataWriter
import model.Model
import model.VpipeDataException
import utils.RunTimer
import utils.SystemInfo
import utils.UserSettingsStore

import javax.swing.*
import javax.swing.filechooser.FileFilter
import java.awt.*
import java.awt.event.*
import java.text.SimpleDateFormat

import static model.DataReader.TASK_FILE_NAME
import static model.DataReader.isValidModelFolder

class GlobalController {

    Model model
    View view

    GlobalController(Model model, View view) {
        this.model = model
        this.view = view
        addLastDirsToRecentMenu()
        Timer timer = new Timer(10000, saveTimerAction)
        timer.setRepeats(true)
        timer.start()

        //
        // glue view and controller together
        //

        // File
        view.swing.newModelAction.closure = newModelActionPerformed
        view.swing.openAction.closure = openActionPerformed
        view.swing.saveAction.closure = saveActionPerformed
        view.swing.saveAsAction.closure = saveAsActionPerformed
        view.swing.toggleContinuosSaveAsAction.closure = toggleContinuosSaveAsActionPerformed
        view.swing.exitAction.closure = exitActionPerformed

        // Tool
        view.swing.sortPipelineAction.closure = sortPipelineActionPerformed
        view.swing.swapTemplatesAndProjectsAction.closure = swapTemplatesAndProjectsActionPerformed
        view.swing.readProjectUpdatesAction.closure = readProjectUpdatesActionPerformed
        view.swing.correctProjectFilesAction.closure = correctProjectFilesActionPerformed
        view.swing.startEndFilterAction.closure = startEndFilterActionPerformed

        // View
        view.swing.toggleViewInPhaseAction.closure = toggleViewInPhaseActionPerformed
        view.swing.pipelineViewAction.closure = pipelineViewActionPerformed
        view.swing.loadViewAction.closure = loadViewActionPerformed
        view.swing.pipelineLoadViewAction.closure = pipelineLoadViewActionPerformed
        view.swing.projectViewAction.closure = projectViewActionPerformed

        // Help
        view.swing.helpAction.closure = helpActionPerformed
        view.swing.aboutAction.closure = aboutActionPerformed

        view.swing.printPerformanceAction.closure = printPerformanceActionPerformed
        view.swing.deleteFilterAction.closure = deleteFilterActionPerformed
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
            autoSavingInProgress = true
            view.swing.doOutside {
                def start = System.currentTimeMillis()
                DataWriter dw = new DataWriter(model: model)
                dw.saveAll()
                def end = System.currentTimeMillis()
                println "gespeichert...${end - start} ms"
                view.swing.doLater {
                    model.setDirty(false)
                    autoSavingInProgress = false
                }
            }
        }
    }

    boolean autoSavingInProgress = false

    def checkAutoSave() {
        //just wait a little bit in
        while (autoSavingInProgress) {
            Thread.sleep(100)
        }
    }


    def switchAutoSave(boolean on) {
        JCheckBoxMenuItem i = view.swing.checkBoxMenuContSaving
        i.setSelected(on)
        toggleContinuosSaveAsActionPerformed(null)
    }

    def toggleContinuosSaveAsActionPerformed = { ActionEvent e ->
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
        a = view.swing.toggleContinuosSaveAsAction
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
        ClassLoader mcl = MainGui.instance.class.classLoader
        def binding = new Binding()
        binding.pathToUpdateDir = DataReader.updateDir()
        //binding.pathToUpdateDoneDir = DataReader.updateDoneDir()

        GroovyShell shell = new GroovyShell(mcl, binding)

        File plugin = new File(DataReader.get_UPDATE_PLUGIN_FILE_NAME())
        try {
            if (plugin.exists()) {
                shell.evaluate(plugin)
            } else {
                println "kein plugin/import.groovy file für Transformation beim Daten-Import gefunden. Wird übersprungen...\n$plugin"
            }
        } catch (Exception e) {
            println "**** ERROR IN PLUGIN **** \n $e.message"
            JOptionPane.showMessageDialog(null, "Datei:\n" + plugin + "\n" + e.message, "Fehler im Import-Plugin!", JOptionPane.ERROR_MESSAGE)
            return // stop import process
        }
        if (DataReader.isDataInUpdateFolder()) {
            def updates = model.readUpdatesFromUpdateFolder()
            def updatedStr = "\nneue Projekte:\n" +
                    (updates.new ? updates.new.join("\n") : "keine") +
                    "\n\naktualisierte Projekte:\n" +
                    (updates.updated ? updates.updated.join("\n") : "keine") +
                    "\n\ngelöschte Projekte:\n" +
                    (updates.deleted ? updates.deleted.join("\n") : "keine")
            if (updates.err) {
                // this is a stupid workaround: the error message contains the wrong filename
                if (updates.err.contains("enthält keine Daten")) updates.err = "Datei enthält keine Daten.\n"
                JOptionPane.showMessageDialog(null, "Datei:\n" + DataReader.get_UPDATE_TASK_FILE_NAME() + "\n" + updates.err, "Fehler beim lesen des Update!", JOptionPane.ERROR_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(null, updatedStr, "Update!", JOptionPane.INFORMATION_MESSAGE)
            }
        } else {
            JOptionPane.showMessageDialog(null, "Im Verzeichnis zum Update der Projektdaten liegen keine Daten:\n" + DataReader.updateDir(), "Keine Daten!", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    static JDialog d
    ModelReaderMessagePanel modelReaderMessagePanel
    boolean reactiveAutoSave

    SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy");

    private Date[] showDateRangeDialog() {
        def filterFrom = model.getFilterFrom()
        if (filterFrom) {
            filterFrom = DateHelperFunctions._getWeekYearStr(filterFrom)
        }
        def filterTo = model.getFilterTo()
        if (filterTo) {
            filterTo = DateHelperFunctions._getWeekYearStr(filterTo)
        }

        def checkDateActionListener = { KeyEvent e ->
            JTextField tf = (JTextField) (e.getSource())
            String text = tf.getText()
            if (text) {
                if (StringExtension.isYearWeek(text)) {
                    try {
                        Date d = StringExtension.toDateFromYearWeek(text)
                        tf.setForeground(Color.BLACK)
                    } catch (Exception ex) {
                        tf.setForeground(Color.RED)
                    }
                } else {
                    try {
                        Date d = formatter.parse(text)
                        tf.setForeground(Color.BLACK)
                    } catch (Exception ex) {
                        tf.setForeground(Color.RED)
                    }
                }
            }
        }
        JTextField startField = new JTextField(filterFrom, 10);
        startField.addKeyListener(checkDateActionListener as KeyListener)
        JTextField endField = new JTextField(filterTo, 10);
        endField.addKeyListener(checkDateActionListener as KeyListener)

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Beispiel-Formate:"));
        panel.add(new JLabel(""));
        panel.add(new JLabel("Datum:"));
        panel.add(new JLabel("03.12.2020"));
        panel.add(new JLabel("KW:"));
        panel.add(new JLabel("2022-w03"));

        panel.add(new JLabel("Start:"));
        panel.add(startField);
        panel.add(new JLabel("Ende:"));
        panel.add(endField);

        int result = JOptionPane.showConfirmDialog(null, panel,
                "Datum eingrenzen", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                Date startDate = null
                if (startField.getText()) {
                    if (StringExtension.isYearWeek(startField.getText())) {
                        startDate = StringExtension.toDateFromYearWeek(startField.getText())
                    } else {
                        startDate formatter.parse(startField.getText());
                    }
                }
                Date endDate = null
                if(endField.getText()) {
                    if (StringExtension.isYearWeek(endField.getText())) {
                        endDate = StringExtension.toDateFromYearWeek(endField.getText())
                    } else {
                        endDate formatter.parse(endField.getText());
                    }
                }
                return new Date[]{startDate, endDate};
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Ungültiges Datum.");
                return null;
            }
        } else {
            return null;
        }
    }

    def startEndFilterActionPerformed = {
        Date[] dates = showDateRangeDialog();
        if(dates) {
            model.setFilter(null, null)
            model.forceReCalc()
            model.setFilter(dates[0], dates[1])
            model.fireUpdate()
        }
    }

    def deleteFilterActionPerformed = {
        model.setFilter(null, null)
        model.fireUpdate()
    }


    def correctProjectFilesActionPerformed = {
        if (model.isDirty()) {
            JOptionPane.showMessageDialog(null, "Speichern sie ihre Änderungen\nvor der Aktivierung des 'Daten-Datei-Lese-Modus'.", "Fehler: Änderungen sind nicht gespeichert!", JOptionPane.ERROR_MESSAGE)
            return
        }

        reactiveAutoSave = false
        if (autoSave) {
            reactiveAutoSave = true
            switchAutoSave(false)
        }
        if (!d) {
            modelReaderMessagePanel = new ModelReaderMessagePanel(swing: view.swing)
            JFrame f = view.swing.frame
            d = new JDialog(f, "Datendateien kontinuierlich einlesen...", true)
            d.setSize((int) (f.getSize().width / 2), (int) (f.getSize().height / 2))
            JPanel p = modelReaderMessagePanel.buildPanel()
            d.add(p)
            d.addWindowListener(new WindowAdapter() {
                void windowClosing(WindowEvent e) {
                    modelReaderMessagePanel.stopReading()
                    if (reactiveAutoSave) {
                        switchAutoSave(true)
                    }
                }
            })
        }
        modelReaderMessagePanel.startReading()
        d.setVisible(true)
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
        MainGui.openBrowserWithHelp()
    }

    def aboutActionPerformed = { ActionEvent e ->
        openAboutPanel()
    }

    def printPerformanceActionPerformed = { ActionEvent e ->
        println(SystemInfo.getSystemInfoTable())
        println(RunTimer.getResultTable())
        view.showLog()
    }

    /*
    def compareActionPerformed = { ActionEvent e ->
        println(e.getSource())
    }*/

    JFileChooser fc = null

    boolean chooseDirWhileOpen

    private String chooseDir(String dialogTitle, JComponent root, String applyButtonText, Boolean open) {
        chooseDirWhileOpen = open // to avoid the open get bound in the closure
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
                    def checkNeighbour = f.parentFile.getAbsolutePath() + "/" + TASK_FILE_NAME
                    def pathExists = new File(checkPath).exists()
                    def neighbourExists = new File(checkNeighbour).exists()
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
                    return "Datei $TASK_FILE_NAME muss im Verzeichnis liegen..."
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
            correctProjectFilesActionPerformed()
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
        a = view.swing.toggleContinuosSaveAsAction
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
        a = view.swing.toggleContinuosSaveAsAction
        a.setEnabled(true)
    }

    static void openAboutPanel() {
        JOptionPane.showMessageDialog(null,
                "\n\nRelease:  v-pipe-" + MainGui.VERSION_STRING + "       \n\n" +
                        "Author:    Benno Löffler       \n\nE-Mail:    benno.loeffler@gmx.de       \n\n",
                "about v-pipe",
                JOptionPane.INFORMATION_MESSAGE)
    }
}
