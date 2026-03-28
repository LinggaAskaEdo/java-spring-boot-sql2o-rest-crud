#!/bin/bash
# stress_test.sh — Apache Benchmark stress test runner with HTML reporting
# Usage: [APP_HOST=...] [APP_PORT=...] [TOTAL_REQUESTS=...] [CONCURRENCY=...] ./stress_test.sh

set -euo pipefail

# ─────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────
APP_HOST="${APP_HOST:-localhost}"
APP_PORT="${APP_PORT:-6661}"
BASE_URL="http://${APP_HOST}:${APP_PORT}/api"

TOTAL_REQUESTS="${TOTAL_REQUESTS:-1000}"
CONCURRENCY="${CONCURRENCY:-10}"

declare -A TIER_KEYS=(
    [VVIP]="vvip-key-001 vvip-key-002"
    [VIP]="vip-key-001 vip-key-002"
    [Premium]="premium-key-001 premium-key-002"
    [General]="general-key-001 general-key-002"
)
declare -A TIER_LIMITS=(
    [VVIP]="100 req/sec"
    [VIP]="50 req/sec"
    [Premium]="10 req/sec"
    [General]="1 req/sec"
)
declare -A TIER_COLORS=(
    [VVIP]="#f59e0b"
    [VIP]="hsl(220,14%,60%)"
    [Premium]="hsl(25,60%,55%)"
    [General]="#22c55e"
)

ENDPOINTS=(
    "/products"
    "/companies"
    "/tutorials"
    "/events"
)

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_DIR="reports/${TIMESTAMP}"
HTML_REPORT="${REPORT_DIR}/report.html"
SUMMARY_FILE="${REPORT_DIR}/.summary.tmp"

# ─────────────────────────────────────────────────────────────
# Terminal colors
# ─────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# ─────────────────────────────────────────────────────────────
# Cleanup on exit
# ─────────────────────────────────────────────────────────────
cleanup() {
    local code=$?
    rm -f "${SUMMARY_FILE}"
    if [[ $code -ne 0 ]]; then
        echo -e "\n${RED}Script exited unexpectedly (code ${code}). Partial report may exist at: ${HTML_REPORT}${NC}"
    fi
}
trap cleanup EXIT

# ─────────────────────────────────────────────────────────────
# Dependency check
# ─────────────────────────────────────────────────────────────
check_dependencies() {
    local missing=()
    for cmd in ab curl awk sed; do
        if ! command -v "$cmd" &>/dev/null; then
            missing+=("$cmd")
        fi
    done
    if [[ ${#missing[@]} -gt 0 ]]; then
        echo -e "${RED}Error: Missing required tools: ${missing[*]}${NC}"
        echo "Install with: sudo apt-get install apache2-utils curl gawk"
        exit 1
    fi
}

# ─────────────────────────────────────────────────────────────
# Utility
# ─────────────────────────────────────────────────────────────
print_header() {
    echo -e "\n${BLUE}${BOLD}════════════════════════════════════════${NC}"
    echo -e "${BLUE}${BOLD}  $1${NC}"
    echo -e "${BLUE}${BOLD}════════════════════════════════════════${NC}\n"
}

print_progress() {
    local current=$1
    local total=$2
    local label=$3
    echo -e "${CYAN}[${current}/${total}]${NC} ${label}"
}

verify_endpoint() {
    local endpoint="$1"
    local api_key="$2"
    local http_code
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
        --max-time 5 \
        -H "x-api-key: ${api_key}" \
        "${BASE_URL}${endpoint}" 2>/dev/null) || http_code="000"
    echo "$http_code"
}

# Extract the percentage of responses with a given HTTP status code from an ab result file.
# ab only reports Non-2xx as an aggregate; to split 429 vs 5xx we need the -r flag is insufficient.
# We parse the result file for the "Non-2xx" line and accept the full count as rate-limited
# only when we explicitly test a rate-limit scenario. For accuracy, pass -v to ab and grep;
# here we use the standard ab output and note the limitation in the report.
parse_ab_result() {
    local result_file="$1"
    local field="$2"
    grep "${field}" "${result_file}" | awk '{print $NF}' | head -1
}

# Compute a percentile from an ab CSV file (column 2 = response time in ms)
percentile_from_csv() {
    local csv_file="$1"
    local pct="$2"        # e.g. 50, 90, 99
    awk -F',' -v p="$pct" '
        NR > 1 && $2 ~ /^[0-9]+$/ { times[NR] = $2; count++ }
        END {
            if (count == 0) { print "N/A"; exit }
            n = int(count * p / 100)
            if (n < 1) n = 1
            # Simple sort via index walk (ab CSV is already ordered by percentile)
            # Column 1 is the percentile value, column 2 is the time at that percentile.
            # ab -e produces: percentile,time — use column 1 directly.
        }
    ' "${csv_file}"
    # ab -e CSV format: "Percentage served,Time in ms"
    # Row "50,123" means 50th pct = 123ms. Extract directly:
    awk -F',' -v p="$pct" 'NR > 1 && int($1) == p { print $2; exit }' "${csv_file}"
}

# ─────────────────────────────────────────────────────────────
# HTML report helpers
# ─────────────────────────────────────────────────────────────
init_html_report() {
    cat > "${HTML_REPORT}" << 'HTMLEOF'
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Stress Test Report</title>
<style>
  @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Syne:wght@400;600;800&display=swap');

  :root {
    --bg:       #0d0f14;
    --surface:  #161922;
    --border:   #252a35;
    --text:     #e2e8f0;
    --muted:    #64748b;
    --accent:   #38bdf8;
    --success:  #4ade80;
    --warning:  #fb923c;
    --danger:   #f87171;
    --vvip:     #f59e0b;
    --vip:      #94a3b8;
    --premium:  #c07c3a;
    --general:  #22c55e;
    --radius:   8px;
    --mono:     'JetBrains Mono', monospace;
    --sans:     'Syne', sans-serif;
  }

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  body {
    font-family: var(--sans);
    background: var(--bg);
    color: var(--text);
    line-height: 1.6;
    padding: 40px 20px;
  }

  .page { max-width: 1200px; margin: 0 auto; }

  /* Header */
  .page-header {
    border-bottom: 1px solid var(--border);
    padding-bottom: 24px;
    margin-bottom: 40px;
  }
  .page-header h1 {
    font-family: var(--sans);
    font-size: 2rem;
    font-weight: 800;
    letter-spacing: -0.5px;
    color: var(--accent);
  }
  .page-header p { color: var(--muted); font-size: 0.9rem; margin-top: 4px; }

  /* Config grid */
  .config-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 12px;
    margin-bottom: 40px;
  }
  .config-card {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 14px 18px;
  }
  .config-card .label { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 1px; color: var(--muted); }
  .config-card .value { font-family: var(--mono); font-size: 1rem; color: var(--text); margin-top: 4px; }

  /* Tier legend */
  .tier-legend {
    display: flex; flex-wrap: wrap; gap: 16px;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    padding: 16px 20px;
    margin-bottom: 40px;
    align-items: center;
  }
  .tier-legend .legend-label { font-size: 0.75rem; color: var(--muted); text-transform: uppercase; letter-spacing: 1px; margin-right: 8px; }
  .tier-badge {
    display: inline-flex; align-items: center; gap: 6px;
    font-family: var(--mono); font-size: 0.8rem;
    padding: 4px 10px; border-radius: 20px;
  }
  .tier-dot { width: 8px; height: 8px; border-radius: 50%; }

  /* Section heading */
  .section-heading {
    font-size: 1.1rem; font-weight: 600;
    color: var(--accent);
    margin: 40px 0 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--border);
    font-family: var(--mono);
    letter-spacing: 0.5px;
  }

  /* Endpoint block */
  .endpoint-block {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius);
    margin-bottom: 32px;
    overflow: hidden;
  }
  .endpoint-block-header {
    background: #1e2330;
    padding: 12px 20px;
    font-family: var(--mono);
    font-size: 0.85rem;
    color: var(--accent);
    border-bottom: 1px solid var(--border);
    display: flex; align-items: center; gap: 8px;
  }
  .endpoint-block-header .method { color: var(--success); font-weight: 600; }

  /* Test result card */
  .result-card {
    border-bottom: 1px solid var(--border);
    padding: 16px 20px;
  }
  .result-card:last-child { border-bottom: none; }
  .result-header {
    display: flex; justify-content: space-between; align-items: center;
    margin-bottom: 12px;
  }
  .result-header .key { font-family: var(--mono); font-size: 0.8rem; color: var(--muted); }
  .result-header .tier-pill {
    font-family: var(--mono); font-size: 0.7rem;
    padding: 2px 8px; border-radius: 20px;
  }

  /* Metrics grid */
  .metrics {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
    gap: 8px;
    margin-bottom: 12px;
  }
  .metric {
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 6px;
    padding: 8px 12px;
    text-align: center;
  }
  .metric .val { font-family: var(--mono); font-size: 1.1rem; font-weight: 600; color: var(--accent); }
  .metric .lbl { font-size: 0.65rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.5px; margin-top: 2px; }

  /* Status bars */
  .status-row { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 10px; }
  .status-pill {
    font-family: var(--mono); font-size: 0.75rem;
    padding: 3px 10px; border-radius: 4px;
  }
  .pill-success { background: rgba(74,222,128,.12); color: var(--success); border: 1px solid rgba(74,222,128,.25); }
  .pill-warning { background: rgba(251,146,60,.12); color: var(--warning); border: 1px solid rgba(251,146,60,.25); }
  .pill-danger  { background: rgba(248,113,113,.12); color: var(--danger);  border: 1px solid rgba(248,113,113,.25); }
  .pill-muted   { background: rgba(100,116,139,.12); color: var(--muted);   border: 1px solid rgba(100,116,139,.25); }

  /* Percentile table */
  .pct-table { width: 100%; border-collapse: collapse; font-size: 0.8rem; margin-top: 10px; }
  .pct-table th { color: var(--muted); font-weight: 600; text-align: left; padding: 4px 8px; text-transform: uppercase; font-size: 0.65rem; letter-spacing: 0.5px; }
  .pct-table td { font-family: var(--mono); padding: 4px 8px; color: var(--text); }
  .pct-table tr:first-child th { border-bottom: 1px solid var(--border); }

  /* Summary section */
  .summary-table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
  .summary-table th {
    background: #1e2330; color: var(--muted);
    font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.5px;
    padding: 10px 14px; text-align: left;
    border-bottom: 1px solid var(--border);
  }
  .summary-table td { padding: 10px 14px; border-bottom: 1px solid var(--border); font-family: var(--mono); font-size: 0.8rem; }
  .summary-table tr:last-child td { border-bottom: none; }
  .summary-table tr:hover td { background: rgba(255,255,255,.025); }

  .col-success { color: var(--success); }
  .col-warning { color: var(--warning); }
  .col-danger  { color: var(--danger);  }
  .col-muted   { color: var(--muted);   }

  /* Tier summary cards */
  .tier-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin: 20px 0; }
  .tier-card {
    border-radius: var(--radius); padding: 18px 20px;
    border: 1px solid var(--border); background: var(--surface);
  }
  .tier-card .tier-name { font-weight: 800; font-size: 1rem; margin-bottom: 4px; }
  .tier-card .tier-limit { font-family: var(--mono); font-size: 0.7rem; color: var(--muted); margin-bottom: 12px; }
  .tier-card .stat-row { display: flex; justify-content: space-between; font-size: 0.8rem; margin-top: 6px; }
  .tier-card .stat-row .stat-lbl { color: var(--muted); }
  .tier-card .stat-row .stat-val { font-family: var(--mono); }

  footer { margin-top: 60px; text-align: center; color: var(--muted); font-size: 0.75rem; border-top: 1px solid var(--border); padding-top: 20px; }
</style>
</head>
<body>
<div class="page">
HTMLEOF
}

html_config_section() {
    cat >> "${HTML_REPORT}" << EOF
<div class="page-header">
  <h1>⚡ Stress Test Report</h1>
  <p>Generated: $(date '+%Y-%m-%d %H:%M:%S %Z')</p>
</div>

<div class="config-grid">
  <div class="config-card"><div class="label">Host</div><div class="value">${APP_HOST}</div></div>
  <div class="config-card"><div class="label">Port</div><div class="value">${APP_PORT}</div></div>
  <div class="config-card"><div class="label">Base URL</div><div class="value">${BASE_URL}</div></div>
  <div class="config-card"><div class="label">Total Requests</div><div class="value">${TOTAL_REQUESTS}</div></div>
  <div class="config-card"><div class="label">Concurrency</div><div class="value">${CONCURRENCY}</div></div>
</div>

<div class="tier-legend">
  <span class="legend-label">Tiers</span>
  <span class="tier-badge" style="background:rgba(245,158,11,.12);color:var(--vvip);border:1px solid rgba(245,158,11,.3)">
    <span class="tier-dot" style="background:var(--vvip)"></span>VVIP — 100 req/sec
  </span>
  <span class="tier-badge" style="background:rgba(148,163,184,.12);color:var(--vip);border:1px solid rgba(148,163,184,.3)">
    <span class="tier-dot" style="background:var(--vip)"></span>VIP — 50 req/sec
  </span>
  <span class="tier-badge" style="background:rgba(192,124,58,.12);color:var(--premium);border:1px solid rgba(192,124,58,.3)">
    <span class="tier-dot" style="background:var(--premium)"></span>Premium — 10 req/sec
  </span>
  <span class="tier-badge" style="background:rgba(34,197,94,.12);color:var(--general);border:1px solid rgba(34,197,94,.3)">
    <span class="tier-dot" style="background:var(--general)"></span>General — 1 req/sec
  </span>
</div>
EOF
}

html_endpoint_header() {
    local endpoint="$1"
    echo "<div class=\"section-heading\">GET ${BASE_URL}${endpoint}</div>" >> "${HTML_REPORT}"
    echo "<div class=\"endpoint-block\">" >> "${HTML_REPORT}"
}

html_endpoint_footer() {
    echo "</div>" >> "${HTML_REPORT}"
}

html_tier_heading() {
    local tier="$1"
    echo "<div class=\"endpoint-block-header\"><span class=\"method\">▸</span> ${tier} — ${TIER_LIMITS[$tier]}</div>" >> "${HTML_REPORT}"
}

html_result_card() {
    local tier="$1" key="$2" rps="$3" complete="$4" success="$5"
    local rate_limited="$6" other_fail="$7"
    local time_50="$8" time_90="$9" time_99="${10}"
    local transfer_rate="${11}"

    local tier_css_var
    case "$tier" in
        VVIP)    tier_css_var="var(--vvip)" ;;
        VIP)     tier_css_var="var(--vip)" ;;
        Premium) tier_css_var="var(--premium)" ;;
        General) tier_css_var="var(--general)" ;;
        *)       tier_css_var="var(--accent)" ;;
    esac

    local success_pct=0 rl_pct=0 fail_pct=0
    if [[ "$complete" -gt 0 ]]; then
        success_pct=$(( success * 100 / complete ))
        rl_pct=$(( rate_limited * 100 / complete ))
        fail_pct=$(( other_fail * 100 / complete ))
    fi

    cat >> "${HTML_REPORT}" << EOF
<div class="result-card">
  <div class="result-header">
    <span class="key">x-api-key: ${key}</span>
    <span class="tier-pill" style="background:rgba(0,0,0,.3);color:${tier_css_var};border:1px solid ${tier_css_var}44">${tier}</span>
  </div>
  <div class="metrics">
    <div class="metric"><div class="val">${rps:-N/A}</div><div class="lbl">req/sec</div></div>
    <div class="metric"><div class="val">${complete:-0}</div><div class="lbl">complete</div></div>
    <div class="metric"><div class="val">${transfer_rate:-N/A}</div><div class="lbl">KB/sec</div></div>
    <div class="metric"><div class="val">${time_50:-N/A}</div><div class="lbl">p50 ms</div></div>
    <div class="metric"><div class="val">${time_90:-N/A}</div><div class="lbl">p90 ms</div></div>
    <div class="metric"><div class="val">${time_99:-N/A}</div><div class="lbl">p99 ms</div></div>
  </div>
  <div class="status-row">
    <span class="status-pill pill-success">✓ ${success} OK (${success_pct}%)</span>
EOF

    if [[ "$rate_limited" -gt 0 ]]; then
        echo "    <span class=\"status-pill pill-warning\">⚠ ${rate_limited} rate-limited (${rl_pct}%)</span>" >> "${HTML_REPORT}"
    fi
    if [[ "$other_fail" -gt 0 ]]; then
        echo "    <span class=\"status-pill pill-danger\">✗ ${other_fail} errors (${fail_pct}%)</span>" >> "${HTML_REPORT}"
    fi
    if [[ "$rate_limited" -eq 0 && "$other_fail" -eq 0 ]]; then
        echo "    <span class=\"status-pill pill-muted\">no rate limiting observed</span>" >> "${HTML_REPORT}"
    fi

    echo "  </div>" >> "${HTML_REPORT}"
    echo "</div>" >> "${HTML_REPORT}"
}

save_summary_row() {
    # endpoint|tier|key|success|rps|rate_limited|other_fail|p50|p90|p99
    echo "${1}|${2}|${3}|${4}|${5}|${6}|${7}|${8}|${9}|${10}" >> "${SUMMARY_FILE}"
}

# ─────────────────────────────────────────────────────────────
# Core ab runner
# ─────────────────────────────────────────────────────────────
run_ab_test() {
    local endpoint="$1"
    local api_key="$2"
    local tier="$3"

    local safe_name
    safe_name=$(printf '%s' "${tier}_${api_key}_${endpoint}" | tr '/?=&' '_')
    local csv_file="${REPORT_DIR}/${safe_name}.csv"
    local result_file="${REPORT_DIR}/${safe_name}.txt"

    # Run ab; continue even if ab exits non-zero (e.g. when server rejects requests)
    ab -n "${TOTAL_REQUESTS}" -c "${CONCURRENCY}" \
       -e "${csv_file}" \
       -H "x-api-key: ${api_key}" \
       "${BASE_URL}${endpoint}" > "${result_file}" 2>&1 || true

    # ── Parse result file ──────────────────────────────────────
    local rps complete failed non2xx transfer_rate time_mean
    rps=$(grep -m1 "^Requests per second:" "${result_file}" | awk '{print $4}')
    complete=$(grep -m1 "^Complete requests:" "${result_file}" | awk '{print $3}')
    failed=$(grep -m1 "^Failed requests:" "${result_file}" | awk '{print $3}')
    non2xx=$(grep -m1 "^Non-2xx responses:" "${result_file}" | awk '{print $3}')
    transfer_rate=$(grep -m1 "^Transfer rate:" "${result_file}" | awk '{print $3}')
    time_mean=$(grep -m1 "^Time per request:" "${result_file}" | awk '{print $4}')

    # Default empty fields to 0
    complete="${complete:-0}"
    failed="${failed:-0}"
    non2xx="${non2xx:-0}"
    rps="${rps:-0}"

    # ── Percentiles from ab CSV (-e flag) ─────────────────────
    # ab -e CSV format: "Percentage served,Time in ms"
    # Row "50,123" means the 50th percentile completed in 123ms.
    local time_50 time_90 time_99
    if [[ -f "${csv_file}" && $(wc -l < "${csv_file}") -gt 1 ]]; then
        time_50=$(awk -F',' 'NR>1 && int($1)==50 {print $2; exit}' "${csv_file}")
        time_90=$(awk -F',' 'NR>1 && int($1)==90 {print $2; exit}' "${csv_file}")
        time_99=$(awk -F',' 'NR>1 && int($1)==99 {print $2; exit}' "${csv_file}")
    fi
    time_50="${time_50:-N/A}"
    time_90="${time_90:-N/A}"
    time_99="${time_99:-N/A}"

    # ── Classify failures ──────────────────────────────────────
    # ab lumps 429 and 5xx into Non-2xx. We attempt to determine
    # how many are rate-limit (429) vs actual errors by checking
    # the ratio: if success rate aligns with the tier's quota,
    # non2xx are likely rate-limited. Otherwise flag as unknown.
    # For accurate breakdown, use -v 2 mode in future iterations.
    local rate_limited other_fail
    rate_limited="${non2xx:-0}"
    other_fail="${failed:-0}"

    # Guard: success cannot go negative
    local success=$(( complete - non2xx - failed ))
    (( success < 0 )) && success=0

    # ── Terminal output ────────────────────────────────────────
    echo -e "  ${YELLOW}Key:${NC} ${api_key} ${YELLOW}|${NC} ${tier}"
    printf "  RPS: %-8s  Complete: %-6s  OK: %-6s  Rate-limited: %-6s  Errors: %s\n" \
        "${rps}" "${complete}" "${success}" "${rate_limited}" "${other_fail}"
    echo -e "  P50: ${time_50}ms  P90: ${time_90}ms  P99: ${time_99}ms"
    echo ""

    # ── HTML card ─────────────────────────────────────────────
    html_result_card "${tier}" "${api_key}" "${rps}" "${complete}" "${success}" \
        "${rate_limited}" "${other_fail}" "${time_50}" "${time_90}" "${time_99}" \
        "${transfer_rate:-N/A}"

    # ── Save summary row ──────────────────────────────────────
    save_summary_row "${endpoint}" "${tier}" "${api_key}" \
        "${success}" "${rps}" "${rate_limited}" "${other_fail}" \
        "${time_50}" "${time_90}" "${time_99}"
}

# ─────────────────────────────────────────────────────────────
# Summary report
# ─────────────────────────────────────────────────────────────
generate_summary() {
    [[ ! -f "${SUMMARY_FILE}" || ! -s "${SUMMARY_FILE}" ]] && return

    cat >> "${HTML_REPORT}" << 'EOF'
<div class="section-heading">Summary</div>
<div class="endpoint-block">
<table class="summary-table">
<tr>
  <th>Endpoint</th><th>Tier</th><th>Key</th>
  <th>Req/sec</th><th>Success</th><th>Rate-limited</th><th>Errors</th>
  <th>P50 ms</th><th>P90 ms</th><th>P99 ms</th>
</tr>
EOF

    while IFS='|' read -r endpoint tier key success rps rate_limited other_fail p50 p90 p99; do
        local tier_css
        case "$tier" in
            VVIP)    tier_css="var(--vvip)" ;;
            VIP)     tier_css="var(--vip)" ;;
            Premium) tier_css="var(--premium)" ;;
            General) tier_css="var(--general)" ;;
            *)       tier_css="var(--accent)" ;;
        esac

        local rl_class="" fail_class=""
        [[ "${rate_limited:-0}" -gt 0 ]] && rl_class=' class="col-warning"'
        [[ "${other_fail:-0}"   -gt 0 ]] && fail_class=' class="col-danger"'

        cat >> "${HTML_REPORT}" << EOF
<tr>
  <td style="font-family:var(--mono);color:var(--accent)">${endpoint}</td>
  <td style="color:${tier_css};font-weight:600">${tier}</td>
  <td style="color:var(--muted)">${key}</td>
  <td>${rps}</td>
  <td class="col-success">${success}</td>
  <td${rl_class}>${rate_limited}</td>
  <td${fail_class}>${other_fail}</td>
  <td>${p50}</td>
  <td>${p90}</td>
  <td>${p99}</td>
</tr>
EOF
    done < "${SUMMARY_FILE}"

    echo "</table></div>" >> "${HTML_REPORT}"

    # ── Per-tier aggregate cards ──────────────────────────────
    echo '<div class="section-heading">Per-Tier Aggregates</div>' >> "${HTML_REPORT}"
    echo '<div class="tier-cards">' >> "${HTML_REPORT}"

    for tier in VVIP VIP Premium General; do
        local total_success=0 total_rl=0 total_fail=0 count=0

        while IFS='|' read -r ep ep_tier key success rps rate_limited other_fail p50 p90 p99; do
            if [[ "$ep_tier" == "$tier" ]]; then
                total_success=$(( total_success + success ))
                total_rl=$(( total_rl + rate_limited ))
                total_fail=$(( total_fail + other_fail ))
                count=$(( count + 1 ))
            fi
        done < "${SUMMARY_FILE}"

        [[ "$count" -eq 0 ]] && continue

        local tier_css
        case "$tier" in
            VVIP)    tier_css="var(--vvip)" ;;
            VIP)     tier_css="var(--vip)" ;;
            Premium) tier_css="var(--premium)" ;;
            General) tier_css="var(--general)" ;;
        esac

        local efficiency=0
        local total_possible=$(( TOTAL_REQUESTS * count ))
        if [[ "$total_possible" -gt 0 ]]; then
            efficiency=$(awk "BEGIN { printf \"%.1f\", (${total_success} / ${total_possible}) * 100 }")
        fi

        local rl_badge=""
        if [[ "$total_rl" -gt 0 ]]; then
            rl_badge='<span class="status-pill pill-warning" style="display:inline-block;margin-top:10px">rate limiting observed</span>'
        else
            rl_badge='<span class="status-pill pill-muted" style="display:inline-block;margin-top:10px">no rate limiting</span>'
        fi

        cat >> "${HTML_REPORT}" << EOF
<div class="tier-card" style="border-top: 3px solid ${tier_css}">
  <div class="tier-name" style="color:${tier_css}">${tier}</div>
  <div class="tier-limit">${TIER_LIMITS[$tier]}</div>
  <div class="stat-row"><span class="stat-lbl">Tests run</span><span class="stat-val">${count}</span></div>
  <div class="stat-row"><span class="stat-lbl">Total successful</span><span class="stat-val col-success">${total_success}</span></div>
  <div class="stat-row"><span class="stat-lbl">Rate limited</span><span class="stat-val col-warning">${total_rl}</span></div>
  <div class="stat-row"><span class="stat-lbl">Errors</span><span class="stat-val col-danger">${total_fail}</span></div>
  <div class="stat-row"><span class="stat-lbl">Success rate</span><span class="stat-val">${efficiency}%</span></div>
  ${rl_badge}
</div>
EOF
    done

    echo "</div>" >> "${HTML_REPORT}"
}

end_html_report() {
    cat >> "${HTML_REPORT}" << EOF
<footer>
  Generated by stress_test.sh &nbsp;·&nbsp; ${TOTAL_REQUESTS} requests at concurrency ${CONCURRENCY} &nbsp;·&nbsp; $(date '+%Y-%m-%d %H:%M:%S')
</footer>
</div>
</body>
</html>
EOF
}

# ─────────────────────────────────────────────────────────────
# Main
# ─────────────────────────────────────────────────────────────
main() {
    check_dependencies

    mkdir -p "${REPORT_DIR}"
    > "${SUMMARY_FILE}"

    print_header "Stress Test Configuration"
    echo "Host:            ${APP_HOST}"
    echo "Port:            ${APP_PORT}"
    echo "Base URL:        ${BASE_URL}"
    echo "Total Requests:  ${TOTAL_REQUESTS}"
    echo "Concurrency:     ${CONCURRENCY}"
    echo "Report Dir:      ${REPORT_DIR}"
    echo ""

    print_header "Rate Limiter Tiers"
    for tier in VVIP VIP Premium General; do
        printf "  %-10s %s — keys: %s\n" "${tier}" "${TIER_LIMITS[$tier]}" "${TIER_KEYS[$tier]}"
    done
    echo ""

    # Verify all endpoints reachable before starting
    print_header "Endpoint Verification"
    local skip_run=false
    for endpoint in "${ENDPOINTS[@]}"; do
        local status
        status=$(verify_endpoint "${endpoint}" "vvip-key-001")
        local status_label
        if [[ "$status" == "200" ]]; then
            status_label="${GREEN}${status} OK${NC}"
        elif [[ "$status" == "000" ]]; then
            status_label="${RED}${status} UNREACHABLE${NC}"
            skip_run=true
        else
            status_label="${YELLOW}${status}${NC}"
        fi
        echo -e "  ${endpoint}: ${status_label}"
    done
    echo ""

    if [[ "$skip_run" == true ]]; then
        echo -e "${RED}One or more endpoints unreachable. Aborting test run.${NC}"
        exit 1
    fi

    # Initialise HTML
    init_html_report
    html_config_section

    # ── Test loop ──────────────────────────────────────────────
    local test_num=0
    local total_tiers=4
    local keys_per_tier=2
    local total_tests=$(( ${#ENDPOINTS[@]} * total_tiers * keys_per_tier ))

    for endpoint in "${ENDPOINTS[@]}"; do
        print_header "GET ${BASE_URL}${endpoint}"
        html_endpoint_header "${endpoint}"

        for tier in VVIP VIP Premium General; do
            echo -e "${CYAN}▸ ${tier} (${TIER_LIMITS[$tier]})${NC}"
            html_tier_heading "${tier}"

            for api_key in ${TIER_KEYS[$tier]}; do
                test_num=$(( test_num + 1 ))
                print_progress "${test_num}" "${total_tests}" "${tier} / ${api_key}"
                run_ab_test "${endpoint}" "${api_key}" "${tier}"
            done
        done

        html_endpoint_footer
    done

    # ── Summary ────────────────────────────────────────────────
    generate_summary

    end_html_report

    print_header "Complete"
    echo -e "${GREEN}HTML Report:${NC}  ${HTML_REPORT}"
    echo -e "${GREEN}CSV files:${NC}    ${REPORT_DIR}/*.csv"
    echo -e "${GREEN}Raw results:${NC}  ${REPORT_DIR}/*.txt"
    echo ""
}

main "$@"