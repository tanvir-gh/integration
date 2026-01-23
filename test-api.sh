#!/bin/bash

CONTENT_URL="http://localhost:8080/api/content"
RECO_URL="http://localhost:8081/api/recommendations"
ITERATIONS=${1:-10}

echo "Running $ITERATIONS iterations to generate traces..."
echo

for i in $(seq 1 $ITERATIONS); do
    echo "=== Iteration $i ==="

    # Create content (triggers Kafka producer)
    curl -s -X POST $CONTENT_URL -H "Content-Type: application/json" \
        -d "{\"title\":\"Movie $i\",\"type\":\"MOVIE\"}" > /dev/null

    curl -s -X POST $CONTENT_URL -H "Content-Type: application/json" \
        -d "{\"title\":\"Show $i\",\"type\":\"SHOW\"}" > /dev/null

    # Get content by ID
    curl -s "$CONTENT_URL/$((i % 5 + 1))" > /dev/null

    # Get recommendations (triggers HTTP call to content-service)
    curl -s $RECO_URL > /dev/null

    # List all content
    curl -s $CONTENT_URL > /dev/null

    echo "  Created 2 items, fetched recommendations"
    sleep 0.5
done

echo
echo "Done! Generated traces for $ITERATIONS iterations."
echo "Check New Relic for distributed traces across content-service and recommendation-service."
