import java.util.concurrent.TimeUnit

println "INIT: doing initial pull"
println pull()
println "INIT: now build and deploy initially"
println deploy()

while (true) {
    def gitText = pull()
    println "Pulled last time at: " + new Date()
    println gitText
    if (gitText == "Already up to date.") {
        Thread.sleep(30 * 1000)
    } else {
        println deploy()
    }
}

String pull() {
    "git pull".execute().text.trim()
}

String deploy() {
    def proc = "gradle __deployToDropbox".execute()
    proc.waitFor(5, TimeUnit.MINUTES)
    def gradleText = proc.text.trim()
    gradleText
}