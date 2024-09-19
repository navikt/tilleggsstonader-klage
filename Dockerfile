FROM gcr.io/distroless/java21:nonroot

USER nonroot

ENV TZ="Europe/Oslo"

EXPOSE 8080
COPY /build/libs/app.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

CMD ["-jar", "/app/app.jar"]