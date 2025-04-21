FROM node@sha256:dcb9e35d8afca163a231cdfad9657d2360947f212faf0fab647f8d11cb1e63a9

RUN apt-get update && apt-get -y upgrade
COPY ./app /app
RUN cd /app && npm i
WORKDIR /app
EXPOSE 6060
CMD ["npm", "start"]