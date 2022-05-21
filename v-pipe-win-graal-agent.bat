@echo off
java -version
java -agentlib:native-image-agent=config-output-dir=conf/ -cp "c:/projects/v-pipe/build/dist/v-pipe/lib/*" application.MainGui
