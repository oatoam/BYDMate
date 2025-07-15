FROM golang:1.24-alpine

WORKDIR /app

COPY go.mod ./
COPY go.sum ./
RUN go mod download

COPY . .

RUN go build -o /data_analyzer

EXPOSE 8081

CMD ["/data_analyzer"]