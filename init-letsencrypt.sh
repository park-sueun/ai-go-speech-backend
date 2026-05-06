#!/bin/bash
# Usage: ./init-letsencrypt.sh <domain> <email> <dev|prod>
# Example: ./init-letsencrypt.sh api.example.com admin@example.com prod

set -e

DOMAIN=$1
EMAIL=$2
ENV=${3:-prod}

if [ -z "$DOMAIN" ] || [ -z "$EMAIL" ]; then
  echo "Usage: $0 <domain> <email> [dev|prod]"
  exit 1
fi

NGINX_CONTAINER=$([ "$ENV" = "dev" ] && echo "nginx-dev" || echo "nginx")
COMPOSE="-f docker-compose.yml -f docker-compose.${ENV}.yml"
ENV_FILE="--env-file .env.${ENV}"

echo "==> [1/5] Creating certbot directories..."
mkdir -p ./certbot/conf ./certbot/www

echo "==> [2/5] Downloading recommended TLS parameters..."
if [ ! -f "./certbot/conf/options-ssl-nginx.conf" ]; then
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/_internal/tls_configs/options-ssl-nginx.conf \
    > ./certbot/conf/options-ssl-nginx.conf
fi
if [ ! -f "./certbot/conf/ssl-dhparams.pem" ]; then
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/certbot/ssl-dhparams.pem \
    > ./certbot/conf/ssl-dhparams.pem
fi

echo "==> [3/5] Starting nginx with HTTP-only config for ACME challenge..."
cat > ./nginx/conf.d/default.conf << NGINXEOF
include /etc/nginx/conf.d/service-url.inc;

server {
    listen 80;
    server_name ${DOMAIN};

    resolver 127.0.0.11 valid=30s;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        proxy_pass \$service_url;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
NGINXEOF

docker compose $COMPOSE $ENV_FILE up -d nginx
sleep 3

echo "==> [4/5] Requesting Let's Encrypt certificate for ${DOMAIN}..."
docker compose $COMPOSE $ENV_FILE run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email \
  -d "$DOMAIN"

echo "==> [5/5] Activating HTTPS nginx config..."
sed "s/DOMAIN_PLACEHOLDER/${DOMAIN}/g" ./nginx/conf.d/default.conf.template \
  > ./nginx/conf.d/default.conf
docker exec "$NGINX_CONTAINER" nginx -s reload

echo ""
echo "Success! HTTPS is now active for ${DOMAIN}"
echo ""
echo "Run the full stack:"
echo "  docker compose $COMPOSE $ENV_FILE up -d"
echo ""
echo "Add this cron job on the host to reload nginx after cert renewal:"
echo "  0 3 * * * docker exec ${NGINX_CONTAINER} nginx -s reload 2>/dev/null"
