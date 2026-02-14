# SkyblockFlipperBackend

> **API-First Engine für Hypixel SkyBlock Flips** – einheitliches Datenmodell, reproduzierbare Snapshots und erweiterbare Flip-Analytik.

## Vision

**SkyblockFlipperBackend** soll die technische Grundlage für eine stabile, versionierbare Flip-API im Hypixel-SkyBlock-Ökosystem werden.

Zielbild:
- Ein **Unified Flip Model** über alle Flip-Arten hinweg.
- Eine klare Pipeline von **Ingestion → Normalisierung → Berechnung → Persistenz → API-Auslieferung**.
- Fokus auf **deterministische Berechnungen**, **ROI/ROI-h**, **Kapitalbindung** und später **Risk/Liquidity-Scoring**.
- API-First statt UI-First: Das Backend ist als Plattform gedacht, auf der Dashboards, Bots oder Research-Tools aufsetzen können.

## Features

Aktueller Stand (im Repository vorhanden):
- Spring Boot 4 Backend mit Java 21.
- Persistenz mit Spring Data JPA.
- Datenquellen-Clients für:
  - Hypixel Auction API.
  - NEU-Item-Daten (Download/Refresh aus dem NotEnoughUpdates-Repo).
- Geplante/angelegte Domain-Struktur für Flips mit:
  - `Flip`, `Step`, `Constraint`, `Recipe`.
  - Berechnung von Gesamt-/Aktiv-/Passivdauer pro Flip.
- Scheduling-Infrastruktur (ThreadPool + geplante Jobs).
- Dockerfile + docker-compose für Container-Betrieb.

## Architecture

### High-Level

```text
[Hypixel API]        [NEU Repo / Items]
      |                     |
      v                     v
 HypixelClient         NEUClient + Filter/Mapper
      |                     |
      +--------- Ingestion & Normalisierung --------+
                                                    v
                                          Domain Model (Flip/Step/Recipe)
                                                    |
                                                    v
                                           Spring Data Repositories
                                                    |
                                                    v
                                                REST API
```

### Technologie-Stack

- **Runtime:** Java 21
- **Framework:** Spring Boot 4 (`web`, `validation`, `actuator`)
- **Persistenz:** Spring Data JPA
- **Datenbanken:** PostgreSQL (runtime), H2 (Tests)
- **Scheduling:** `@EnableScheduling`, `@Scheduled`, `ThreadPoolTaskScheduler`
- **Externe Clients:**
  - Hypixel REST via `RestClient`
  - NEU-Repo Download/Refresh via `HttpClient` + ZIP-Extraktion
- **Build/Test:** Maven Wrapper, Surefire, JaCoCo
- **Container:** Multi-stage Docker Build + Distroless Runtime Image

### Komponenten (vereinfacht)

- **API Layer:** `StatusController`
- **Source Jobs:** periodische Refresh-/Ingestion-Jobs (`SourceJobs`)
- **Domain/Model:** Flips, Steps, Constraints, Recipes
- **Repositories:** `FlipRepository`, `RecipeRepository`, `ItemRepository`, etc.

## Supported Flip Types

### Bereits im Domain-Modell als `FlipType` vorhanden
- **Bazaar** (`BAZAAR`)
- **Crafting** (`CRAFTING`)
- **Forge** (`FORGE`)
- **Fusion** (`FUSION`)

### Zielbild (Roadmap)
- Auction Flips
- Bazaar Flips
- Craft Flips
- Forge Flips
- Shard Flips
- Fusion Flips

> Hinweis: Aktuell sind im Code bereits die grundlegenden Flip-Domainobjekte vorhanden; die vollständige End-to-End-Abdeckung aller Ziel-Fliptypen ist als nächster Ausbauschritt zu sehen.

## Unified Flip Schema (JSON Sample)

Das folgende JSON zeigt ein **geplantes, vereinheitlichtes API-Schema** für Flips.

```json
{
  "id": "a91e7fe3-4ee8-4f7d-8d84-8a9fd9b44884",
  "flipType": "FORGE",
  "snapshotTimestamp": "2026-01-12T20:45:00Z",
  "requiredCapital": 1250000,
  "expectedProfit": 185000,
  "roi": 0.148,
  "roiPerHour": 0.032,
  "durationSeconds": 16600,
  "fees": {
    "auctionTax": 0,
    "bazaarTax": 0,
    "other": 2500
  },
  "liquidityScore": 0.71,
  "riskScore": 0.38,
  "inputItems": [
    { "itemId": "MITHRIL_PLATE", "amount": 1, "source": "BAZAAR", "unitPrice": 890000 },
    { "itemId": "REFINED_DIAMOND", "amount": 2, "source": "BAZAAR", "unitPrice": 180000 }
  ],
  "outputItems": [
    { "itemId": "GEMSTONE_MIXTURE", "amount": 1, "targetMarket": "AUCTION", "expectedUnitPrice": 1430000 }
  ],
  "steps": [
    {
      "type": "BUY",
      "durationType": "MARKET_BASED",
      "baseDurationSeconds": 45,
      "resource": "NONE",
      "schedulingPolicy": "BEST_EFFORT",
      "params": { "itemId": "MITHRIL_PLATE", "amount": 1 }
    },
    {
      "type": "FORGE",
      "durationType": "FIXED",
      "baseDurationSeconds": 16500,
      "resource": "FORGE_SLOT",
      "resourceUnits": 1,
      "schedulingPolicy": "LIMITED_BY_RESOURCE"
    },
    {
      "type": "SELL",
      "durationType": "MARKET_BASED",
      "baseDurationSeconds": 55,
      "resource": "NONE",
      "schedulingPolicy": "BEST_EFFORT",
      "params": { "itemId": "GEMSTONE_MIXTURE", "amount": 1 }
    }
  ],
  "constraints": [
    { "type": "MIN_CAPITAL", "longValue": 1250000 },
    { "type": "MIN_FORGE_SLOTS", "intValue": 1 }
  ]
}
```

## API Endpoints (planned)

### Bereits vorhanden
- `GET /api/status` – einfacher Health-/Connectivity-Check (triggert aktuell einen Auction-Fetch).

### Geplante v1-Endpunkte

- `GET /api/v1/flips`
  - Filter: `flipType`, `minProfit`, `maxDurationSeconds`, `minRoi`, `sort`, `limit`, `offset`.
- `GET /api/v1/flips/{id}`
  - Details zu einem Flip inkl. Steps, Constraints, Snapshot-Metadaten.
- `GET /api/v1/items`
  - Item-Metadaten (NEU-basiert), Such-/Filterparameter.
- `GET /api/v1/recipes`
  - Rezeptdaten (Craft/Forge) inkl. Ingredients und Prozessdauer.
- `GET /api/v1/snapshots`
  - Verfügbare Snapshot-Zeitpunkte.
- `GET /api/v1/snapshots/{timestamp}/flips`
  - Reproduzierbarer Flip-Stand für Analysen/Backtests.

### API-Design-Prinzipien
- Versionierung über `/api/v1/...`
- Konsistente DTOs über alle Flip-Typen
- Deterministische Antworten pro Snapshot
- Erweiterbar ohne Breaking Changes (deprecate-first)

## Build & Run (Docker & Local)

### Voraussetzungen
- Java 21
- Docker (optional, für Containerbetrieb)

### Lokal (Dev)

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Hinweise:
- Das Standardprofil erwartet DB-Variablen:
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
- Der Server-Port ist über `SERVER_PORT` steuerbar (Default fallback im Config-File).

Beispiel:

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/skyblock'
export SPRING_DATASOURCE_USERNAME='postgres'
export SPRING_DATASOURCE_PASSWORD='postgres'
export SERVER_PORT=8080
./mvnw spring-boot:run
```

### Docker

```bash
docker compose up --build
```

Danach läuft der Service via `docker-compose.yml` auf Port `8080`.

## Roadmap / Differentiators

### P0 – Kritisch
- Unified Flip DTO und stabile Read-API (`/api/v1/flips`, `/api/v1/flips/{id}`)
- End-to-End Pipeline je Flip-Typ (Ingestion → Compute → Persist → Serve)
- Konsistente Profit-/Fee-Berechnung

### P1 – Wichtig
- Snapshot-System (zeitpunktgenaue Reproduzierbarkeit)
- Zeitgewichtete ROI-Kennzahlen (`ROI/h`, aktive vs. passive Zeit)
- Kapitalbindungslogik und Ressourcen-Constraints (z. B. Forge-Slots)

### P2 – Differenzierung / USP
- Liquidity Score + Risk Score
- Risk-adjusted Ranking statt reinem Profit-Sorting
- Slippage/Fill-Probability Modell
- Multi-Step Flip Chains (DAG) inkl. Optimierung
- Backtesting API für historische Snapshots

### Mögliche Marktlücken / USP
- **Unified Flip Abstraction Layer:** ein Contract für alle Flip-Typen.
- **API-First Product:** ideal für externe Clients, Tools und Automationen.
- **Snapshot + Backtesting:** reproduzierbare, auditierbare Entscheidungen.
- **Risk/Liquidity-Normalisierung:** realitätsnähere Scores statt statischer ROI-Listen.

## Contributing

Contributions sind willkommen.

Empfohlener Ablauf:
1. Fork/Branch erstellen (`feature/...`, `fix/...`).
2. Änderungen mit Tests ergänzen.
3. Pull Request mit klarer Beschreibung (Problem, Lösung, Auswirkungen) öffnen.
4. Auf konsistente API-Verträge und Rückwärtskompatibilität achten.

Leitlinien:
- Kleine, fokussierte PRs.
- Keine Breaking Changes ohne Versionierungsstrategie.
- Neue Flip-Typen über das Unified Model integrieren.

## License

Aktuell liegt eine `LICENSE`-Datei im Repository.

> Falls noch nicht final definiert: Lizenztext/Typ hier finalisieren (z. B. MIT, Apache-2.0, proprietär).
