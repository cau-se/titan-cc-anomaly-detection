FROM openjdk:11-slim

ADD build/distributions/titanccp-anomaly-detection.tar /

EXPOSE 80

CMD JAVA_OPTS="$JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=$LOG_LEVEL" \
    /titanccp-anomaly-detection/bin/titanccp-anomaly-detection
