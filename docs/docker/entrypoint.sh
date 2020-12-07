#!/bin/bash
echo "CONFIG_ENV:{} $CONFIG_ENV"
echo "JAVA_OPTS:{} $JAVA_OPTS"

echo " environment"
java -jar $JAVA_OPTS -Denv=$CONFIG_ENV app.jar

