FROM gcr.io/distroless/java21:nonroot

USER nonroot

ENV TZ="Europe/Oslo"

EXPOSE 8080
COPY /build/libs/app.jar /app/app.jar

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"

CMD ["-jar", "/app/app.jar"]