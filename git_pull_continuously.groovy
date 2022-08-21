/**
 * just start in intellij or on command line by:
 * groovy git_pull_continuously.groovy
 */

println "INIT: doing initial git pull"
println pull()
println "INIT: now build and deploy to dropbox initially"
deploy()

def untilCTRL_C=true
while (untilCTRL_C) {
    def gitText = pull()
    print "git pulled: " + new Date().format("yyyy-MM-dd HH:mm:ss") + ", said: " + gitText + "\n"
    if (gitText == "Already up to date.") {
        Thread.sleep(30 * 1000)
    } else {
        deploy()
    }
}

static String pull() {
    def proc = "git pull".execute()
    proc.waitFor()
    proc.text.trim()
}

static void deploy() {
    def proc = "gradle __deployToDropbox".execute()
    proc.waitForProcessOutput(System.out, System.err)
}