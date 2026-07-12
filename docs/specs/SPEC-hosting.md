# SPEC - Hosting (W30 deploy) · ADR-H1

_NIL-55 output, Opus, 2026-07-07 (idle-window run). Budget-first hosting recommendation for the **W30 deploy** target. Feeds NIL-46 (domain) and NIL-47 (provision + deploy), and answers the **NIL-29** topology question (web container vs static frontend). **No provisioning here** - this is decision support; Nils ratifies the host pick and does the buy. Every price was verified by web search on 2026-07-07 and is cited in §9. Prices are shown **net (ex-VAT)** and **incl. 25 % Swedish VAT** - Nils is a private customer in southern Sweden; §2 covers the reverse-charge path if a business VAT-ID is used._

**Status:** Proposed (awaiting Nils's ratification of the host pick)
**Deciders:** Nils - host + plan (NIL-47), domain (NIL-46)
**Deploy blocker of record:** the `gorilla-tidy-*.csv` participant-ID privacy gate must be closed before the app or repos go public - see §7.

---

## 0. Recommendation (the one-liner)

**Primary - Hetzner Cloud CX32** (4 vCPU / 8 GB / 80 GB NVMe; Nuremberg · Falkenstein · Helsinki), running the **existing `docker compose` stack behind Caddy** for automatic Let's Encrypt HTTPS. **€6.80/mo net → €8.50/mo incl. 25 % VAT.** Drop to the 4 GB **CX22** (€3.79 net → **€4.74/mo incl. VAT**) for the floor; the extra headroom of the CX32 is cheap insurance for the JVM + MySQL sharing one box and for later XL-audio growth.

**Runner-up - Fly.io** (managed, git-push deploys, EU regions FRA/AMS): **~€12–18/mo**, and you self-run MySQL (no first-class managed MySQL). Pick it only if *not owning a server* is worth ~2× the money.

**Static frontend (answers NIL-29):** build to static assets and serve them from **Caddy on the same box** - single origin, no prod CORS, one compose file. **Cloudflare Pages (free)** is the optional CDN-offload variant. **Do not** add a web/Vite container.

**Cost contrast:** the same always-on stack on **AWS** (EC2 t4g.small + RDS MySQL db.t4g.micro, Stockholm) is **~€31–44/mo net before storage/egress/VAT** - roughly **4–9×** the Hetzner box.

---

## 1. Context

The artifact to host is fixed and small (api `docker-compose.yml` + `Dockerfile`, read this session):

- **MySQL 8.4** - schema + seed load from `/docker-entrypoint-initdb.d` on first init; `ddl-auto=validate` fails fast if the schema is absent.
- **Spring Boot jar** - built in a Maven stage, run on a Temurin-21 JRE as non-root; listens on 8081 (published to host 18081 in dev).
- **Frontend** - React/Vite/TS, built **separately** to static assets; talks to the api via `VITE_API_BASE_URL` or a proxy. No web container today (deliberate - W26 shipped "backend + MySQL only").
- **Stimuli** - `/stimuli/**` served from a read-only bind mount.

Forces at play: **budget is the deciding factor** (NIL-55, explicit); traffic is **hobby / portfolio** scale; the deploy is **non-commercial** (CC BY data licences); the data is **participant-adjacent** research data, so **EU residency + GDPR** carry weight (Nils is in Sweden; NIL-72 demographics still pending); Nils is **Linux/Docker-comfortable** (WSL/Arch), so a self-managed VPS is not a skills barrier and is arguably more portfolio-credible (it shows infra competence). Target week **W30**.

The stack is a plain long-running x86/ARM Linux workload with a stateful DB. That shape rewards a **boring VPS**, not serverless - which is the spine of the analysis below.

---

## 2. Cost basis - VAT and data residency

**Swedish standard VAT is 25 % in 2026** (Skatteverket; unchanged) [S8]. Under the EU **One-Stop-Shop (OSS)** B2C rule, an EU provider bills a Swedish *private* customer at the **Swedish 25 %** rate, not the provider's home rate. Hetzner's own billing docs confirm both halves: EU customers are charged **their country's VAT rate**, and a customer with a **valid VAT-ID is invoiced under reverse-charge with no VAT added** [S3].

Practical consequence:

- **Apples-to-apples comparison must be done on the _net_ price** (below), because the 25 % is added identically whoever the EU vendor is.
- If Nils registers/uses an **enskild firma** VAT-ID, every EU-vendor figure here drops to its **net** column (reverse-charge; he self-accounts the VAT). Worth doing before the buy - it's a ~20 % standing saving.
- Providers that *display* VAT-inclusive prices do so at **German 19 %**; a Swedish customer is re-rated to 25 % under OSS, so those headline figures are converted to net before comparison and flagged in the table.

**Residency:** Hetzner, Netcup and Contabo are **German companies** with EU datacentres and offer a **GDPR DPA** - the cleanest story for participant-adjacent data. Contabo also has non-EU regions (must actively pick an EU one). Fly.io, Railway and AWS are **US-owned**; they have EU regions (FRA/AMS/Stockholm) but the processor entity is US, so SCCs/DPA scrutiny matters more - relevant only if NIL-72 turns the app into a real recruitment surface. For a portfolio deploy this is a tie-breaker, not a gate.

USD figures converted at **€1 = $1.14** (ECB reference range, 1–6 Jul 2026) [S9]; i.e. $1 ≈ €0.88.

---

## 3. Options considered

All rows sized to a **comfortable ~8 GB box** (see §3.1 for why), except where a provider's model differs. "Net" = ex-VAT; "incl. VAT" = + 25 % Swedish.

| Option | Plan (spec) | €/mo **net** | €/mo **incl. 25 %** | Residency | Managed DB? | Notes |
|---|---|---|---|---|---|---|
| **Hetzner CX32** ⭐ | 4 vCPU / 8 GB / 80 GB NVMe | **6.80** | **8.50** | DE/FI (EU) | self | 20 TB egress incl.; best price-perf-reliability balance [S1] |
| Hetzner CX22 | 2 vCPU / 4 GB / 40 GB | 3.79 | 4.74 | DE/FI (EU) | self | The floor; cap JVM heap + MySQL buffer pool; build off-box (§3.1) [S1] |
| Hetzner CAX21 (ARM) | 4 vCPU / 8 GB / 80 GB | 10.49 | 13.11 | DE/FI only | self | ARM took the bigger 2026 hike - now dearer than x86 CX32; only if you want ARM [S2] |
| Netcup VPS (entry) | 2–4 vCPU / 4–8 GB / 128–256 GB | ~3.85 | ~4.81 | DE/AT (EU) | self | More disk/RAM per €; 12-mo term cheapest; list price shown incl. 19 % DE VAT [S4] |
| Contabo Cloud VPS 10 | 4 vCPU / 8 GB / SSD-NVMe tiers | ~3.03 | ~3.78 | DE (+non-EU opt.) | self | Cheapest headline **but** 12-mo commit, monthly billing +15–20 %, variable perf, slower support [S5] |
| **Fly.io** (runner-up) | ~2 GB app + ~1 GB db machine + 10 GB vol + IPv4 | ~11–16 | ~14–20 | US co., EU regions | self (no MySQL PaaS) | git-push deploys; USD-billed; free tier cut in 2026; MySQL self-run in a Machine [S6] |
| Railway | small app + managed MySQL, usage-based | ~9–13 | ~11–16 | US co., regions | **yes (1-click)** | Easiest deploy; $5 hobby credit then usage; can spike with egress/traffic [S7] |
| Cloudflare Pages | static frontend only | **0** | **0** | edge (global) | - | Unlimited-bw (fair use), 500 builds/mo, 20k files, 25 MiB/file. **Cannot host the JVM or MySQL** - complement, not host [S10] |
| Cloudflare R2 | object storage (backups / future audio) | **0** to 10 GB | **0** | edge | - | 10 GB free, **zero egress**, permanent; good for mysqldumps + XL audio later [S11] |
| **AWS** (contrast only) | EC2 t4g.small + RDS db.t4g.micro | ~26 (compute) → **~31–44 all-in** | +VAT | US co., eu-north-1 | yes (RDS) | 4–9× Hetzner; RDS db.t4g.micro alone ≈ $21.90/mo [S12][S13] |

### 3.1 Why 8 GB (and how 4 GB still works)

Working set on one box: Spring Boot JVM (~1–1.5 GB with a modest heap) + MySQL 8.4 (tunable down to ~0.5–1 GB via `innodb_buffer_pool_size`) + Caddy + OS ≈ **3–3.5 GB**. So **8 GB (CX32) is comfortable** with headroom for the Maven build spike and later XL audio. **4 GB (CX22) works** if you (a) cap the heap (`-Xmx768m`) and MySQL buffer pool (256 MB), and (b) **don't run the Maven build while MySQL is up** - build the image first (or in CI) then `up`, otherwise the build stage can OOM the box. On the 8 GB box this caveat disappears.

### 3.2 The three real archetypes

Everything above collapses to three choices:

- **Self-managed EU VPS** (Hetzner ⭐, or Netcup/Contabo to shave a euro): cheapest, full control, EU-owned, maps 1:1 to the existing compose file. You own patching/backups/uptime. **This is the "boring baseline" the issue named, and it wins.**
- **Managed PaaS** (Fly.io runner-up, or Railway for the easiest UX): ~2× the cost, less ops, but MySQL is either self-run (Fly) or managed-but-usage-billed (Railway), and residency is US-entity. The hedge if server ops aren't worth the savings.
- **Hyperscaler** (AWS): 4–9× for this shape. Included only as the contrast the issue asked for - it buys nothing a portfolio deploy needs and costs the most.

---

## 4. Scoring matrix

Weights reflect the issue: **cost is #1**. (5 = best.)

| Criterion (weight) | Hetzner CX32 | Fly.io | Railway | Netcup/Contabo | AWS |
|---|---|---|---|---|---|
| **Monthly cost @ hobby (×5)** | 5 | 3 | 3 | 5 | 1 |
| Compose compatibility (×4) | 5 | 3 | 3 | 5 | 2 |
| HTTPS + domain wiring (×3) | 4 (Caddy) | 5 (auto) | 5 (auto) | 4 | 3 |
| EU residency / GDPR (×3) | 5 | 3 | 3 | 5 | 3 |
| MySQL backup story - NIL-48 (×3) | 4 | 3 | 4 | 4 | 5 |
| Monitoring hooks - NIL-51 (×2) | 3 | 4 | 4 | 3 | 5 |
| Static-frontend placement (×2) | 5 | 4 | 4 | 5 | 4 |
| **Weighted total / 110** | **97** | **74** | **77** | **96** | **59** |

Hetzner and the Netcup/Contabo family top the table because cost and compose-fit dominate the weights. Hetzner edges the cheaper twins on **reliability and reputation** (a portfolio piece should stay up and be name-droppable); Netcup/Contabo are the "shave the bill" alternates in the same paradigm. Fly.io/Railway trade cost for lower ops; AWS is anchored by cost.

---

## 5. Trade-off analysis

- **Cost vs ops burden** is the only trade that matters here. Hetzner asks for ~30 min of one-time hardening + a nightly backup cron + OS patching (largely automatable via `unattended-upgrades`). Fly.io/Railway remove most of that for ~2× the money and a US billing entity. At €8.50 vs ~€15/mo the absolute delta is trivial (~€78/yr), so the decision is really *"do you want a server on your CV or not"* - and for an infra-adjacent portfolio piece, **yes**.
- **x86 vs ARM (Hetzner):** ARM (CAX) *was* the value pick but the 2026 memory-crisis hikes hit ARM harder; **x86 CX32 is now cheaper** and avoids multi-arch image faff. Go x86.
- **Managed MySQL vs self-run:** Railway's 1-click MySQL is the only true managed-MySQL option in budget; Fly.io only manages Postgres, so on Fly you self-run MySQL anyway - which erases much of its "managed" advantage for *this* stack. Another reason the runner-up is a close call, not a strong second.
- **Same-host static vs CDN:** serving the built frontend from Caddy on the box is simplest and same-origin (no CORS). Cloudflare Pages (free) only earns its place if you want edge caching / to shield the origin - and it reintroduces cross-origin, so keep CORS env-driven either way. For W30, **same host**.

---

## 6. The compose → live HTTPS path (primary pick, step-enumerated)

This is the NIL-47 runbook seed for Hetzner. Each step is a discrete, reviewable action; **secrets never get committed**; the destructive/prod steps are the NIL-47 approval gate.

1. **Domain (NIL-46, Nils):** register the pick (`.app`/`.io`/`.dev`/`.se` shortlist), then create an **A/AAAA record** → the VPS IP. (Optional: put the domain on Cloudflare DNS now for free TLS-edge/DDoS later; "grey-cloud" until go-live.)
2. **Provision:** Hetzner Cloud **CX32**, **Ubuntu 24.04 LTS**, an **EU location** (Nuremberg / Falkenstein / Helsinki). Add SSH key at create. Enable the **Hetzner Cloud Firewall**: 22 from Nils's IP only, 80 + 443 from anywhere.
3. **Harden (~30 min):** non-root sudo user, `ufw`, `fail2ban`, `unattended-upgrades`, disable SSH password auth. (Standard; scriptable.)
4. **Docker:** install Docker Engine + the compose plugin.
5. **App + secrets:** clone the api repo (or ship the built image + compose). Create a prod **`.env`** - `APP_JWT_SECRET`, `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `STIMULI_HOST_DIR` - **not committed** (already the `.env.example` pattern).
6. **Add Caddy via a prod overlay** (`docker-compose.prod.yml`): a `caddy` service on 80/443 that (a) **auto-provisions Let's Encrypt TLS** for the domain, (b) `reverse_proxy /api/* → api:8081`, (c) `file_server` for the built `dist/` on everything else. **Remove the api's host port publish** so only Caddy is public; api stays on the internal compose network.
7. **Frontend:** `pnpm build` → copy `dist/` to the box (or bind-mount). Set `VITE_API_BASE_URL` to the **same origin** (empty/relative) so calls are same-origin and prod CORS is a non-issue.
8. **Bring it up:** `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build`. MySQL seeds on first init; `ddl-auto=validate` confirms the schema.
9. **Verify:** `curl -I https://<domain>` for the cert, then the **NIL-49 smoke test** - register + Meaning Match + Rating Lab + Perception Ladder against prod.
10. **Backups (NIL-48):** nightly `mysqldump` (via `docker exec`) → gzip → **off-box**: a **Hetzner Storage Box BX11** (1 TB, €3.20/mo net → €4.00 incl. VAT, unlimited traffic, built-in snapshots) [S14] **or** **Cloudflare R2** (10 GB free, zero egress) [S11]. Add a Hetzner **snapshot/volume** for whole-disk restore. **Test a restore** before calling it done.
11. **Monitoring (NIL-51):** external HTTP check via **UptimeRobot (free)** against a Spring Boot `/actuator/health` endpoint; container logs via `docker logs`/journald; optional self-hosted **Uptime Kuma** on the same box. No paid APM needed at this scale.

**NIL-29 resolved:** static frontend, served by Caddy, **same host**, **no web container**. Cloudflare Pages remains a documented, free, opt-in CDN variant.

---

## 7. Consequences, constraints & the deploy blocker

**What gets easier:** one box, one compose file, one origin; the dev→prod delta is a single Caddy overlay + a prod `.env`. Egress is a non-issue (Hetzner bundles 20 TB). The whole thing is reproducible and cheap enough to rebuild from scratch in an afternoon.

**What gets harder / owned:** OS patching, TLS renewal (Caddy automates it), backup verification, and uptime are now Nils's. All are low-effort at this scale but they are real recurring chores.

**⚠ Deploy blocker (hard, pre-public) - the gorilla-tidy privacy gate.** `gorilla-tidy-*.csv` files carrying participant IDs are git-tracked in both repos, and NIL-80's raincloud reads one in place. SPEC-stats-dashboard §4.6 forbids raw trials in public repos. **Before any public deploy or public repo:** untrack them, gitignore them, and replace with vendored aggregates. This ADR does not clear that gate - it flags it as a precondition on NIL-47.

**Other standing constraints:** non-commercial deploy (CC BY attribution must remain visible - Observatory/landing footers); NIL-72 demographics/recruitment is still a *discussion*, so don't ship data-collection beyond current gameplay without that landing; keep prod CORS env-driven even in the same-origin default so the Pages variant stays a config flip.

---

## 8. What would change this recommendation

- **Traffic outgrows hobby** (sustained heavy egress, or a need for HA/horizontal scale): revisit toward a load-balanced VPS pair or a managed platform. Far off; the 20 TB bundle alone buys enormous runway.
- **XL audio storage lands (NIL-86 / NIL-60):** a large wav/m4a corpus should move off the VPS disk to **object storage** (Cloudflare R2 free-tier first, then Hetzner Object Storage / Storage Box) and be CDN-fronted - re-decide media placement at that point, not now.
- **Ops burden proves unwelcome:** switch to the **Fly.io** runner-up (accept ~2× + self-run MySQL) or **Railway** (easiest, managed MySQL, usage-billed).
- **A formal DPA / research-ethics requirement appears (NIL-72):** stay on an EU-owned provider with a signed DPA - which Hetzner already satisfies, so the primary pick is robust to this.

---

## 9. Sources

All fetched 2026-07-07.

- [S1] Hetzner Cloud CX pricing (CX22 €3.79, CX32 €6.80; 20 TB traffic incl.) - https://www.hetzner.com/cloud/regular-performance ; review corroboration https://betterstack.com/community/guides/web-servers/hetzner-cloud-review/
- [S2] Hetzner CAX ARM pricing (CAX21 €10.49; +€0.50 IPv4; DE/FI only) & 15 Jun 2026 adjustment - https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/ ; https://sparecores.com/server/hcloud/cax21
- [S3] Hetzner VAT policy (EU customer's-country rate; valid VAT-ID → reverse-charge) - https://docs.hetzner.com/general/billing-and-account-management/billing-at-hetzner/value-added-tax/
- [S4] Netcup VPS pricing 2026 (entry ~€3.99–4.58; RAMpocalypse increases) - https://www.netcup.com/en/server/vps ; https://forum.netcup.de/thread/21902-aktuell-neue-serverpreise-rampocalypse/
- [S5] Contabo Cloud VPS pricing 2026 (VPS 10 ~€3.60/12-mo; VAT-incl., monthly +15–20 %) - https://contabo.com/en/pricing/ ; https://cybernews.com/best-web-hosting/contabo-review/pricing/
- [S6] Fly.io pricing 2026 (single-app prod ~$10–20/mo; volumes $0.15/GB; Postgres-managed, MySQL self-run) - https://fly.io/docs/about/pricing/ ; https://kuberns.com/blogs/flyio-pricing/
- [S7] Railway pricing 2026 (Hobby $5 credit; small app + managed MySQL ~$10–15/mo, usage-based) - https://railway.com/pricing ; https://docs.railway.com/pricing/plans
- [S8] Sweden standard VAT 25 % (2026) - https://www.avalara.com/us/en/vatlive/country-guides/europe/sweden/swedish-vat-rates.html ; https://sweden.se/life/society/taxes-in-sweden
- [S9] EUR/USD ≈ 1.14 (1–6 Jul 2026, ECB reference range) - https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/eurofxref-graph-usd.en.html
- [S10] Cloudflare Pages free tier (unlimited-bw fair-use; 500 builds/mo; 20k files; 25 MiB/file) - https://developers.cloudflare.com/pages/platform/limits/
- [S11] Cloudflare R2 free tier (10 GB, zero egress, permanent) - https://developers.cloudflare.com/r2/pricing/
- [S12] AWS EC2 t4g.small (~$8.18/mo) - https://cloudprice.net/aws/ec2/instances/t4g.small ; RDS overview https://aws.amazon.com/rds/mysql/pricing/
- [S13] AWS RDS MySQL db.t4g.micro (~$21.90/mo) - https://instances.vantage.sh/aws/rds/db.t4g.micro
- [S14] Hetzner Storage Box BX11 (1 TB, €3.20/mo net, unlimited traffic, snapshots, no setup fee) - https://www.hetzner.com/storage/storage-box/bx11/

_Note: some plan prices were read from review aggregators mirroring the vendor pages; confirm the exact figure on the vendor's own checkout at provisioning time (prices moved twice in 2026 on the memory crisis). The recommendation is robust to ±20 % drift - the VPS tier is 4–9× cheaper than the hyperscaler contrast, and that gap does not close._

---

## 10. Action items

1. [ ] **Nils:** ratify the host pick (Hetzner CX32, or CX22 to shave) - NIL-55 exit.
2. [ ] **Nils:** pick + register the domain (NIL-46); consider registering an **enskild firma VAT-ID** first to reverse-charge the hosting VAT (~20 % standing saving).
3. [ ] **Pre-deploy, hard gate:** close the `gorilla-tidy-*.csv` privacy gate (untrack + gitignore + vendored aggregates) before public deploy/repos - rider on the next api/web session.
4. [ ] **NIL-47:** execute §6 steps 2–9 (provision → Caddy overlay → up → smoke test) behind the prod approval gate.
5. [ ] **NIL-48:** wire the nightly `mysqldump` → Storage Box **or** R2; test a restore.
6. [ ] **NIL-51:** UptimeRobot + `/actuator/health`; optional Uptime Kuma.
7. [ ] **Revisit** media placement if NIL-86/NIL-60 XL audio lands (§8).
