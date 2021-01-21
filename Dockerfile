FROM openjdk:11-slim

ADD build/distributions/titan-ccp-anomaly-detection.tar /

EXPOSE 80

CMD JAVA_OPTS="$JAVA_OPTS -Dorg.slf4j.simpleLogger.defaultLogLevel=$LOG_LEVEL" \
    /titan-ccp-anomaly-detection/bin/titan-ccp-anomaly-detection
