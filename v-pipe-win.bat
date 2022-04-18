set ABSPATH = %CD%
echo "starting v-pipe here:"
echo %ABSPATH%
cd "$ABSPATH"
set JAVA_HOME=$ABSPATH/jre
"$JAVA_HOME/bin/java" -cp "$ABSPATH/lib/*": application.MainGui

java -Dfile.encoding=UTF-8 -Dlog4j.configurationFile=log4j2.xml -cp "./cream2.jar;./libs/*"   bel.cream2.deamon.CreamDeamonStarter
