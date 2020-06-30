package utils

class SystemInfo {
    static String getSystemInfoTable() {

        long maxMemory = Runtime.getRuntime().maxMemory()
        def header = ["                   system ressource":'r', "   value": 'r']
        def data = [
                ['processors (cores)', Runtime.getRuntime().availableProcessors()],
                ['Free memory', formatSize(Runtime.getRuntime().freeMemory())],
                ['Maximum memory e.g. -Xmx1024m', (maxMemory == Long.MAX_VALUE ? "no limit" : formatSize(maxMemory))],
                ['Total memory e.g. -Xms64m', formatSize(Runtime.getRuntime().totalMemory())],
        ]
        ConsoleTable.tableToString(header, data)

//        /* Total number of processors or cores available to the JVM */
//        System.out.println("Available processors (cores): " +
//                Runtime.getRuntime().availableProcessors())
//
//        /* Total amount of free memory available to the JVM */
//        System.out.println("Free memory (bytes): " +
//                formatSize(Runtime.getRuntime().freeMemory()))
//
//        /* This will return Long.MAX_VALUE if there is no preset limit */
//        long maxMemory = Runtime.getRuntime().maxMemory()
//        /* Maximum amount of memory the JVM will attempt to use */
//        System.out.println("Maximum memory (bytes): " +
//                (maxMemory == Long.MAX_VALUE ? "no limit" : formatSize(maxMemory)))
//
//        /* Total memory currently in use by the JVM */
//        System.out.println("Total memory (bytes): " +
//                formatSize(Runtime.getRuntime().totalMemory()))
//
//        /* Get a list of all filesystem roots on this system */
//        File[] roots = File.listRoots();
//
//        /* For each filesystem root, print some info */
//        for (File root : roots) {
//            System.out.println("File system root: " + root.getAbsolutePath())
//            System.out.println("Total space (bytes): " + formatSize(root.getTotalSpace()))
//            System.out.println("Free space (bytes): " + formatSize(root.getFreeSpace()))
//            System.out.println("Usable space (bytes): " + formatSize(root.getUsableSpace()))
//        }
    }

    public static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}
