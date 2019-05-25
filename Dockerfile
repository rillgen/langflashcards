FROM adoptopenjdk/openjdk8:slim

ADD fcardbot/target/fcardbot.jar /app/fcardbot.jar

CMD ["java", "-jar", "/app/fcardbot.jar"]