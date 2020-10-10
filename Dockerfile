FROM openjdk:8-slim

RUN apt-get update

RUN mkdir /src
COPY . /src
WORKDIR /src

RUN ./gradlew shadowJar

# TODO: build.gradleでのバージョン設定が変わるとここのファイル名も変わるのであぶない、どうにかしたい
ENTRYPOINT ["java", "-jar", "/src/build/libs/genkai-dictionary-1.0-SNAPSHOT-all.jar"]
