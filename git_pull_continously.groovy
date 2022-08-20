import java.util.concurrent.TimeUnit

while (true) {
    def gitText = "git pull".execute().text.trim()
    println "Pulled last time at: " + new Date()
    println deploy()
    if (gitText == "Already up to date.") {
        println gitText
        Thread.sleep(30 * 1000)
    } else {
        println gitText
        println deploy()
    }
}

String deploy() {
    def proc = "gradle __deployToDropbox".execute()
    proc.waitFor(5, TimeUnit.MINUTES)
    def gradleText = proc.text.trim()
    gradleText
}