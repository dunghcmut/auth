#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

cat <<EOF
Register:
curl --request POST '${BASE_URL}/api/auth/register' \\
  --header 'Content-Type: application/json' \\
  --data-raw '{
    "name": "Dung Nguyen",
    "email": "dung@example.com",
    "password": "Password123"
  }'

Login:
curl --request POST '${BASE_URL}/api/auth/login' \\
  --header 'Content-Type: application/json' \\
  --data-raw '{
    "email": "dung@example.com",
    "password": "Password123"
  }'

Refresh:
curl --request POST '${BASE_URL}/api/auth/refresh' \\
  --header 'Content-Type: application/json' \\
  --data-raw '{
    "refreshToken": "<refresh-token>"
  }'

Me:
curl --request GET '${BASE_URL}/api/auth/me' \\
  --header 'Authorization: Bearer <access-token>'

Logout:
curl --request POST '${BASE_URL}/api/auth/logout' \\
  --header 'Authorization: Bearer <access-token>'
EOF
