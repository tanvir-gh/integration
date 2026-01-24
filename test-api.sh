#!/bin/bash

CATALOG_URL="http://localhost:8080/api/catalog"
WATCH_URL="http://localhost:8081/api/watch"
HISTORY_URL="http://localhost:8081/api/history"
ITERATIONS=${1:-10}
VISITOR_ID="visitor-$(date +%s)"

echo "Running $ITERATIONS iterations to generate traces..."
echo "Visitor ID: $VISITOR_ID"
echo

for i in $(seq 1 $ITERATIONS); do
    echo "=== Iteration $i ==="

    # Create content in catalog
    curl -s -X POST $CATALOG_URL -H "Content-Type: application/json" \
        -d "{\"title\":\"Movie $i\",\"type\":\"MOVIE\",\"durationMinutes\":120,\"genre\":\"ACTION\"}" > /dev/null

    curl -s -X POST $CATALOG_URL -H "Content-Type: application/json" \
        -d "{\"title\":\"Series $i\",\"type\":\"SERIES\",\"durationMinutes\":45,\"genre\":\"DRAMA\"}" > /dev/null

    # Get content by ID
    curl -s "$CATALOG_URL/$((i % 5 + 1))" > /dev/null

    # Record watch events (triggers Kafka producer)
    curl -s -X POST $WATCH_URL -H "Content-Type: application/json" \
        -d "{\"visitorId\":\"$VISITOR_ID\",\"contentId\":$((i * 2 - 1)),\"watchedSeconds\":$((RANDOM % 3600 + 600))}" > /dev/null

    curl -s -X POST $WATCH_URL -H "Content-Type: application/json" \
        -d "{\"visitorId\":\"$VISITOR_ID\",\"contentId\":$((i * 2)),\"watchedSeconds\":$((RANDOM % 3600 + 600))}" > /dev/null

    # Get watch history (triggers HTTP call to catalog-service)
    curl -s "$HISTORY_URL/$VISITOR_ID" > /dev/null

    # List all content
    curl -s $CATALOG_URL > /dev/null

    echo "  Created 2 items, recorded 2 watch events, fetched history"
    sleep 0.5
done

echo
echo "Done! Generated traces for $ITERATIONS iterations."
echo "Check New Relic for distributed traces across catalog-service and watch-history-service."
