package main

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/segmentio/kafka-go"
)

type Outbox struct {
	ID            string
	AggregateType string
	AggregateID   string
	Type          string
	Payload       string
	Created       time.Time
	processed     bool
}

var Topic string = "transfer-events"

func main() {

	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")
	dbUser := os.Getenv("DB_USER")
	dbPassword := os.Getenv("DB_PASSWORD")
	dbName := os.Getenv("DB_NAME")

	if dbHost == "" || dbUser == "" {
		fmt.Printf("configurações de banco de dados insuficientes")
		os.Exit(1)
	}

	connStr := fmt.Sprintf("postgres://%s:%s@%s:%s/%s?sslmode=disable", dbUser, dbPassword, dbHost, dbPort, dbName)
	conn, err := pgx.Connect(context.Background(), connStr)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Unable to connect to database: %v\n", err)
		os.Exit(1)
	}
	defer conn.Close(context.Background())

	writer := &kafka.Writer{
		Addr:                   kafka.TCP("kafka:29092"),
		Topic:                  Topic,
		Balancer:               &kafka.Hash{},
		AllowAutoTopicCreation: true,
	}

	defer writer.Close()

	ticker := time.NewTicker(500 * time.Millisecond)
	for range ticker.C {

		err := processBatch(conn, writer)
		if err != nil {
			fmt.Printf("❌ ERRO FATAL: %v\n", err)
		}

	}

}

func processBatch(conn *pgx.Conn, writer *kafka.Writer) error {
	ctx := context.Background()

	tx, err := conn.Begin(ctx)
	if err != nil {
		return err
	}

	defer tx.Rollback(ctx)

query := "SELECT id, aggregate_id, payload FROM outbox_events WHERE processed = false ORDER BY created_at ASC LIMIT 10 FOR UPDATE SKIP LOCKED"
	rows, err := tx.Query(ctx, query)
	if err != nil {
		return err
	}

	defer rows.Close()

	var events []Outbox

	for rows.Next() {
		var evt Outbox

		if err := rows.Scan(&evt.ID, &evt.AggregateID ,  &evt.Payload); err != nil {
			rows.Close()
			return fmt.Errorf("scan error: %w", err)
		}
		events = append(events, evt)
	}

	rows.Close()

	if rows.Err() != nil {
		return rows.Err()
	}

	if len(events) == 0 {
		return nil
	}

	fmt.Printf("🔄 Processando %d eventos...\n", len(events))

	for _, e := range events {
		fmt.Printf("   >> Evento ID: %s | Payload: %s\n", e.ID, e.Payload)

		err := writer.WriteMessages(context.Background(),
			kafka.Message{Key: []byte(e.AggregateID), Value: []byte(e.Payload)},
		)
		if err != nil {
			return fmt.Errorf("failed to write message: %w", err)
		}
		_, err = tx.Exec(ctx, "UPDATE outbox_events SET processed = true WHERE id = $1", e.ID)
		if err != nil {
			return fmt.Errorf("erro ao atualizar ID %s: %w", e.ID, err)
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("erro no commit: %w", err)
	}

	fmt.Println("✅ Lote comitado com sucesso!")
	return nil
}
