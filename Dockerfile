FROM gcr.io/distroless/java21-debian12:nonroot
ENV TZ="Europe/Oslo"

EXPOSE 8080
COPY build/libs/*.jar ./

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

CMD ["-jar", "app.jar"]