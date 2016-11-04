FROM java:8

MAINTAINER delivery-engineering@netflix.com

ENV FRONT50_HOST front50
ENV FRONT50_PORT 8080
ENV IGOR_HOST igor
ENV IGOR_PORT 8088
ENV REDIS_URL redis://redis:6379

COPY . workdir/

WORKDIR workdir

RUN GRADLE_USER_HOME=cache ./gradlew buildDeb -x test && dpkg -i ./orca-web/build/distributions/*.deb

CMD ["/opt/orca/bin/orca"]
