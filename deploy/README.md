# VPS deployment (PostgreSQL + API + SPA)

Repositories stay separate: clone **both** next to each other on the server (sibling folders), then build from this directory.

## 1. Layout

```text
/opt/tourisme/
  tour-timeless-backend/    # this repo
  tour-timeless/            # frontend repo
```

## 2. Configure

```bash
cd /opt/tourisme/tour-timeless-backend/deploy
cp env.example .env
nano .env   # DB_PASSWORD, JWT_SECRET, SITE_PUBLIC_URL, CORS_ORIGINS, optional GOOGLE_CLIENT_ID
```

- Production domain: **morocco-mosaic.com**. Point DNS A/AAAA (and `www` if used) at your VPS; then set `SITE_PUBLIC_URL` (e.g. `https://morocco-mosaic.com`) and `CORS_ORIGINS` to match every origin the browser may use (often apex + `www`, comma-separated).
- If the frontend repo is **not** a sibling of the backend folder (e.g. monorepo), set `FRONTEND_CONTEXT` in `.env` (see `env.example`).

## 3. Firewall

Allow HTTP/HTTPS (and SSH only):

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

## 4. Start

```bash
cd /opt/tourisme/tour-timeless-backend/deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

- **Postgres** is not published to the internet; only the **frontend** container exposes `HTTP_PORT` (default 80).
- Uploaded images live in the Docker volume `uploads_data`; back it up with your DB.

## 5. HTTPS (optional)

Put **Caddy** or **nginx** in front (see `Caddyfile.example`) or use a reverse proxy at your cloud provider. Point TLS to the host port that maps to the frontend container (or to Caddy if it reverse-proxies to `tourisme-frontend:80` on the Docker network).

## 6. Database restore

Restore a dump into Postgres **before** or **after** first boot; if the schema lags the app, apply SQL migrations as documented in the main repo (`scripts/apply-pending-migrations.ps1` pattern on Linux: pipe SQL files with `docker exec -i tourisme-postgres psql ...`).
