#!/usr/bin/env bash

set -euo pipefail

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required."
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required."
  exit 1
fi

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_BASE_URL="${BASE_URL}/api/auth"
TEST_NAME="${TEST_NAME:-Dung Nguyen}"
TEST_EMAIL="${TEST_EMAIL:-dung.$(date +%s)@example.com}"
TEST_PASSWORD="${TEST_PASSWORD:-Password123}"

echo "BASE_URL=${BASE_URL}"
echo "TEST_EMAIL=${TEST_EMAIL}"

echo
echo "1. Register"
REGISTER_RESPONSE="$(curl --silent --show-error --location \
  --request POST "${AUTH_BASE_URL}/register" \
  --header 'Content-Type: application/json' \
  --data-raw "{
    \"name\": \"${TEST_NAME}\",
    \"email\": \"${TEST_EMAIL}\",
    \"password\": \"${TEST_PASSWORD}\"
  }")"
echo "${REGISTER_RESPONSE}" | jq .

REGISTER_ACCESS_TOKEN="$(echo "${REGISTER_RESPONSE}" | jq -r '.data.tokens.accessToken')"
REGISTER_REFRESH_TOKEN="$(echo "${REGISTER_RESPONSE}" | jq -r '.data.tokens.refreshToken')"

echo
echo "2. Login"
LOGIN_RESPONSE="$(curl --silent --show-error --location \
  --request POST "${AUTH_BASE_URL}/login" \
  --header 'Content-Type: application/json' \
  --data-raw "{
    \"email\": \"${TEST_EMAIL}\",
    \"password\": \"${TEST_PASSWORD}\"
  }")"
echo "${LOGIN_RESPONSE}" | jq .

ACCESS_TOKEN="$(echo "${LOGIN_RESPONSE}" | jq -r '.data.tokens.accessToken')"
REFRESH_TOKEN="$(echo "${LOGIN_RESPONSE}" | jq -r '.data.tokens.refreshToken')"

echo
echo "3. Get current user profile"
ME_RESPONSE="$(curl --silent --show-error --location \
  --request GET "${AUTH_BASE_URL}/me" \
  --header "Authorization: Bearer ${ACCESS_TOKEN}")"
echo "${ME_RESPONSE}" | jq .

echo
echo "4. Refresh token"
REFRESH_RESPONSE="$(curl --silent --show-error --location \
  --request POST "${AUTH_BASE_URL}/refresh" \
  --header 'Content-Type: application/json' \
  --data-raw "{
    \"refreshToken\": \"${REFRESH_TOKEN}\"
  }")"
echo "${REFRESH_RESPONSE}" | jq .

ROTATED_ACCESS_TOKEN="$(echo "${REFRESH_RESPONSE}" | jq -r '.data.tokens.accessToken')"
ROTATED_REFRESH_TOKEN="$(echo "${REFRESH_RESPONSE}" | jq -r '.data.tokens.refreshToken')"

echo
echo "5. Logout"
LOGOUT_RESPONSE="$(curl --silent --show-error --location \
  --request POST "${AUTH_BASE_URL}/logout" \
  --header "Authorization: Bearer ${ROTATED_ACCESS_TOKEN}")"
echo "${LOGOUT_RESPONSE}" | jq .

echo
echo "6. Verify rotated refresh token is revoked after logout"
POST_LOGOUT_REFRESH_RESPONSE="$(curl --silent --show-error --location \
  --request POST "${AUTH_BASE_URL}/refresh" \
  --header 'Content-Type: application/json' \
  --data-raw "{
    \"refreshToken\": \"${ROTATED_REFRESH_TOKEN}\"
  }")"
echo "${POST_LOGOUT_REFRESH_RESPONSE}" | jq .

echo
echo "Tokens captured during test:"
echo "REGISTER_ACCESS_TOKEN=${REGISTER_ACCESS_TOKEN}"
echo "REGISTER_REFRESH_TOKEN=${REGISTER_REFRESH_TOKEN}"
echo "ACCESS_TOKEN=${ACCESS_TOKEN}"
echo "REFRESH_TOKEN=${REFRESH_TOKEN}"
echo "ROTATED_ACCESS_TOKEN=${ROTATED_ACCESS_TOKEN}"
echo "ROTATED_REFRESH_TOKEN=${ROTATED_REFRESH_TOKEN}"
