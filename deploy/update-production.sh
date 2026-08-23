#!/usr/bin/env bash
# Run on the VPS after git pull in both repos (see deploy/README.md).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ ! -f .env ]]; then
  echo "Missing .env — copy env.example to .env and configure secrets first."
  exit 1
fi

echo "Pulling latest backend..."
git -C .. pull --ff-only

FRONTEND_DIR="${FRONTEND_CONTEXT:-../../tour-timeless}"
if [[ -d "$FRONTEND_DIR/.git" ]]; then
  echo "Pulling latest frontend..."
  git -C "$FRONTEND_DIR" pull --ff-only
fi

echo "Rebuilding and restarting stack..."
docker compose -f docker-compose.prod.yml --env-file .env up -d --build

for SEED_DIR in "$SCRIPT_DIR/seed-images/12-days-in-morocco" "$SCRIPT_DIR/seed-images/destinations"; do
  if [[ -d "$SEED_DIR" ]]; then
    echo "Syncing seed images from $(basename "$SEED_DIR") to backend uploads volume..."
    for f in "$SEED_DIR"/*; do
      [[ -f "$f" ]] || continue
      docker cp "$f" tourisme-backend:/app/uploads/$(basename "$f")
    done
  fi
done

echo "Done. Flyway runs on backend start; verify:"
echo "  curl -s https://morocco-mosaic.com/api/settings?lang=en | grep contactPhone"
