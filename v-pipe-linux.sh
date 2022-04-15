#!/bin/bash
ABSPATH=$(cd "$(dirname "$0")"; pwd -P)
echo "starting v-pipe here:"
echo $ABSPATH
cd "$ABSPATH"
#read -p "Press enter to continue"
export JAVA_HOME=$ABSPATH/jre
"$JAVA_HOME/bin/java" -cp "$ABSPATH/lib/*": application.MainGui
