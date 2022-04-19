package utils

class Commons {

    static def getOsName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            return ("linux");
        } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
            return ("macos");
        } else if (osName.contains("windows")) {
            return ("windows");
        } else if (osName.contains("sunos") || osName.contains("solaris")) {
            return ("solaris");
        } else if (osName.contains("freebsd")) {
            return ("freebsd");
        }
        return ("unix");
    }
}
