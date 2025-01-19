FROM python:3.12-alpine

WORKDIR /app

RUN mkdir /opt; cd /opt; \
    wget https://aka.ms/download-jdk/microsoft-jdk-17.0.9-alpine-x64.tar.gz \
    && tar zxf microsoft-jdk-17.0.9-alpine-x64.tar.gz \
    && ln -s jdk-17.0.9+8 java \
    && rm -f microsoft-jdk-17.0.9-alpine-x64.tar.gz

ENV JAVA_HOME=/opt/java
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN python3 -m pip install -i https://pypi.tuna.tsinghua.edu.cn/simple \
    flask demjson3 pillow

COPY src ./

ENV FLASK_APP=app.py
ENV FLASK_RUN_HOST=0.0.0.0
ENV FLASK_RUN_PORT=8080

EXPOSE 8080

CMD ["flask", "run"]
