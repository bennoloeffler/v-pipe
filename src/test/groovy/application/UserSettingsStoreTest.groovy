package application

import spock.lang.Specification
import utils.UserSettingsStore

class UserSettingsStoreTest extends Specification {

    def "emptyOpenedDataFolder"() {
        given:
        def u = UserSettingsStore.instance

        when:
        u.deleteAllContent()

        then:
        u.getRecentOpenedDataFolders().size() == 0
    }

    def "RemoveOpenedDataFolder"() {
        given:
        def u = UserSettingsStore.instance
        u.deleteAllContent()
        u.addLastOpenedDataFolder("something/wild.txt")
        u.addLastOpenedDataFolder("something/even/wilder.txt")
        u.addLastOpenedDataFolder("something/even/wilder.txt")

        when:
        u.removeOpenedDataFolder("something/even/wilder.txt")

        then:
        u.getRecentOpenedDataFolders().size() == 1
        u.getRecentOpenedDataFolders()[0] == "something/wild.txt"
    }


    def "AddLastOpenedDataFolder"() {
        given:
        def u = UserSettingsStore.instance
        u.deleteAllContent()

        when:
        u.addLastOpenedDataFolder("something/wild.txt")
        u.addLastOpenedDataFolder("something/even/wilder.txt")
        u.addLastOpenedDataFolder("something/even/wilder.txt") // will be written but removed while reading

        then:
        u.getRecentOpenedDataFolders().size() == 2
    }


    def "GetLastOpenedDataFolders"() {
        given:
        def u = UserSettingsStore.instance
        u.deleteAllContent()

        when:
        u.addLastOpenedDataFolder("something/wild.txt")
        u.addLastOpenedDataFolder("something/even/wilder.txt")
        u.addLastOpenedDataFolder("boring") // will be written but removed while reading
        u.addLastOpenedDataFolder("something/even/wilder.txt") // will be written but removed while reading
        u.addLastOpenedDataFolder("something/wild.txt")

        then:
        def df = u.getRecentOpenedDataFolders()
        df.size() == 3
        df[0] == "boring"
        df[1] == "something/even/wilder.txt"
        df[2] == "something/wild.txt"
    }
}
