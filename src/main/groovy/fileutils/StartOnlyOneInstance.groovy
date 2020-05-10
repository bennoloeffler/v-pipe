package fileutils

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class StartOnlyOneInstance {
    static void checkStart()
    {

        FileSupport.checkBackupDir()

        String fileLock = "$FileSupport.BACKUP_DIR/v-pipe.lck"
        FileChannel channel=FileChannel.open(Paths.get(fileLock), StandardOpenOption.WRITE,StandardOpenOption.CREATE);
        FileLock fl=channel.tryLock();
        if(fl==null) {
            println("F E H L E R:   Es kann nur einen geben!\nVermutlich war das ein Versehen oder ein technisches Problem.\n"+
                    "Gehen Sie zu Task-Manager Zweiter V-Pipe-Prozess startet nur mit Start-Option -m\n")
            sleep(10*1000)
            System.exit(0)
        }
    }
}
