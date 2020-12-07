FROM openjdk:8-alpine

WORKDIR /data/application

ADD skycloud-base-auth/target/skycloud-base-authorization.jar app.jar

COPY  docs/docker/entrypoint.sh .

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime

RUN echo 'Asia/Shanghai' >/etc/timezone

RUN ["chmod","+x","entrypoint.sh"]

ENTRYPOINT ["sh","entrypoint.sh"]
