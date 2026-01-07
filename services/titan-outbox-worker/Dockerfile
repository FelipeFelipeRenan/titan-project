# Estágio de Build
FROM golang:alpine AS builder

WORKDIR /app

# Copia os arquivos de dependência primeiro (cache layer)
COPY go.mod go.sum ./
RUN go mod download

# Copia o código fonte
COPY . .

# Compila explicitamente:
# -o app: define o nome de saída como "app"
# cmd/main.go: o arquivo principal
RUN go build -o app cmd/main.go

# Estágio Final
FROM alpine:latest

WORKDIR /app

# Instala certificados caso precise chamar HTTPS externo no futuro
RUN apk --no-cache add ca-certificates

# Copia APENAS o binário compilado do estágio anterior
COPY --from=builder /app/app .

# Comando de execução
CMD ["./app"]