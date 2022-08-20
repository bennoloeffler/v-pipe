import java.util.concurrent.TimeUnit

while (true) {
    def gitText = "git pull".execute().text.trim()
    println "Pulled last time at: " + new Date()
    if (gitText == "Already up to date.") {
        println gitText
        Thread.sleep(1 * 60 * 1000)
    } else {
        println gitText
        def proc = "gradle __deployToDropbox".execute()
        proc.waitFor(5, TimeUnit.MINUTES)
        def gradleText = proc.text.trim()
        println gradleText

    }
}