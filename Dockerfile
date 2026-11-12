FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:eddc3f7fdaa3fdc75ce43c988c9aec57f8e073d4e2f9e2116f149bb3bdac4205

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]