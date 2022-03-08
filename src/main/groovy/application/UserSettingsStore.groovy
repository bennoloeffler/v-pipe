package application

@Singleton
class UserSettingsStore {

    @Lazy
    String home = System.getProperty("user.home");

    @Lazy
    File settingsDir = getSettingsDir()

    File getSettingsDir(){
        def f = new File(home+"/.v-pipe")
        if (! f.exists()) {
            f.mkdir()
        }
        f
    }


    File getLastOpenedDataFoldersFile(){
        def f = new File(settingsDir, "lastOpendedDataFolders.txt")
        if (! f.exists()) f.createNewFile()
        f
    }

    def deleteAllContent() {
        getLastOpenedDataFoldersFile().delete()
    }

    def removeOpenedDataFolder(String dir) {
        File f = getLastOpenedDataFoldersFile()
        List l = f.text.trim().split("\n").toList()
        l.removeAll {it == dir}
        def all = l.join("\n")
        f.text = all
    }

    def addLastOpenedDataFolder(String dir) {
        removeOpenedDataFolder(dir)
        getLastOpenedDataFoldersFile() << "\n" + dir
    }

    List<String> getLastOpenedDataFolders(){
        List l = getLastOpenedDataFoldersFile().text.trim().split("\n").toList()
    }
}
