#!/bin/bash
set -e

BASE="http://localhost:8080/api/v1"

fail() {
  echo "FAIL: $1"
  curl -sf -X POST "$BASE/generator/stop" >/dev/null 2>&1 || true
  exit 1
}

echo "==> [1/6] Checking actuator health..."
STATUS=$(curl -sf "http://localhost:8080/actuator/health" | jq -r '.status')
[ "$STATUS" = "UP" ] || fail "Actuator health is not UP (got: $STATUS)"
echo "    Health: $STATUS"

echo "==> [2/6] Stopping any running generator..."
curl -sf -X POST "$BASE/generator/stop" >/dev/null 2>&1 || true
sleep 1

echo "==> [3/6] Creating E2E test intent..."
INTENT=$(curl -sf -X POST "$BASE/intents" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "E2E BGP Adjacency Test",
    "intentType": "BGP_ADJACENCY",
    "targetEntity": "router-edge-01",
    "targetRegion": "EU-WEST-1",
    "thresholdValue": 0,
    "thresholdUnit": "count",
    "severity": "CRITICAL"
  }')
INTENT_ID=$(echo "$INTENT" | jq -r '.intentId')
[ "$INTENT_ID" != "null" ] && [ -n "$INTENT_ID" ] || fail "Intent creation failed"
echo "    Intent ID: $INTENT_ID"

echo "==> [4/6] Starting CASCADE_FAILURE scenario..."
curl -sf -X POST "$BASE/generator/start" \
  -H "Content-Type: application/json" \
  -d '{"scenarioType":"CASCADE_FAILURE"}' >/dev/null
echo "    Generator started"

echo "==> Waiting 35s for correlation window..."
sleep 35

echo "==> [5/6] Checking correlated incidents..."
INCIDENTS=$(curl -sf "$BASE/incidents/active")
INCIDENT_COUNT=$(echo "$INCIDENTS" | jq 'length')
[ "$INCIDENT_COUNT" -gt 0 ] || fail "No active incidents found"
echo "    Active incidents: $INCIDENT_COUNT"

echo "==> [6/6] Checking intent violations..."
VIOLATIONS=$(curl -sf "$BASE/violations/active")
VIOLATION_COUNT=$(echo "$VIOLATIONS" | jq 'length')
[ "$VIOLATION_COUNT" -gt 0 ] || fail "No active violations found"
echo "    Active violations: $VIOLATION_COUNT"

echo "==> Stopping generator..."
curl -sf -X POST "$BASE/generator/stop" >/dev/null
echo ""
echo "==> PASS: Full pipeline verified"
echo "    Incidents: $INCIDENT_COUNT | Violations: $VIOLATION_COUNT | Intent: $INTENT_ID"