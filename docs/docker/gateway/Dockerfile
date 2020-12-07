FROM openjdk:8-alpine

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN echo 'Asia/Shanghai' >/etc/timezone

ENV APP_FILE app.jar
ENV APP_HOME /usr/apps

COPY skycloud-base-gateway/target/skycloud-base-gateway.jar $APP_HOME/$APP_FILE

WORKDIR $APP_HOME

ENTRYPOINT ["sh", "-c"]

CMD ["exec java -jar $APP_FILE"]


