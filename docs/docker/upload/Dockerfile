FROM openjdk:8-alpine

WORKDIR /data/application

ADD skycloud-base-upload/target/skycloud-base-upload.jar app.jar

COPY  docs/docker/entrypoint.sh .

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN echo 'Asia/Shanghai' >/etc/timezone

RUN ["chmod","+x","entrypoint.sh"]

ENTRYPOINT ["sh","entrypoint.sh"]
