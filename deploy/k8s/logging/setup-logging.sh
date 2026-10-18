#!/usr/bin/env bash
set -euo pipefail

elasticsearch_url="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"
kibana_url="${KIBANA_URL:-http://kibana:5601}"
config_root="${LOGGING_CONFIG_ROOT:-/usr/share/blog-platform/logging}"

wait_for_endpoint() {
  local name="$1"
  local url="$2"
  local attempts="${3:-60}"

  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl --fail --silent --show-error "$url" >/dev/null; then
      return 0
    fi
    echo "Waiting for ${name} (${attempt}/${attempts})..."
    sleep 5
  done

  echo "Timed out waiting for ${name} at ${url}" >&2
  return 1
}

wait_for_endpoint "Elasticsearch" "${elasticsearch_url}/_cluster/health?wait_for_status=yellow&timeout=5s"

curl --fail --silent --show-error \
  --request PUT \
  --header 'Content-Type: application/json' \
  --data-binary "@${config_root}/ilm-policy.json" \
  "${elasticsearch_url}/_ilm/policy/blog-platform-logs-14-days" >/dev/null

curl --fail --silent --show-error \
  --request PUT \
  --header 'Content-Type: application/json' \
  --data-binary "@${config_root}/index-template.json" \
  "${elasticsearch_url}/_index_template/blog-platform-logs" >/dev/null

wait_for_endpoint "Kibana" "${kibana_url}/api/status"

if ! curl --fail --silent \
  --header 'kbn-xsrf: blog-platform-logging-setup' \
  "${kibana_url}/api/data_views/data_view/blog-platform-logs" >/dev/null; then
  curl --fail --silent --show-error \
    --request POST \
    --header 'Content-Type: application/json' \
    --header 'kbn-xsrf: blog-platform-logging-setup' \
    --data '{"data_view":{"id":"blog-platform-logs","name":"Blog Platform Logs","title":"blog-platform-logs-*","timeFieldName":"@timestamp","allowNoIndex":true}}' \
    "${kibana_url}/api/data_views/data_view" >/dev/null
fi

echo "Blog Platform logging lifecycle, index template, and Kibana data view are ready."
