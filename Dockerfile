FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26@sha256:a4aa830802741123c1ed3ad3c5b1417d1297aaedc1e5c8f1784fc4df7568bf97

COPY --chown=1069:1069 build/libs/app.jar /app.jar

EXPOSE 8080

CMD ["-jar", "/app.jar"]