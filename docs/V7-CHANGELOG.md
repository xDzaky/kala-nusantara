# Kala Nusantara V7 Changelog

**Release:** Version 0.1.0-V7 (Robust Stability, Audit & Crossplay Release)  
**Date:** 2026-10-24  
**Target Platform:** PaperMC 26.3 (Java 21 Toolchain) + Geyser / Floodgate Crossplay  

---

## 1. Security & Hygiene (P0)

- **Sanitized Secrets:**
  - Redacted internal `management-server-secret=` in `server/server.properties`.
  - Added clean template `server/server.properties.example` for secure deployment.
- **Repository Optimization:**
  - Removed 296 tracked WorldEdit language/archive unpacking files (`.archive-unpack/`).
  - Updated `.gitignore` to permanently ignore `.archive-unpack/`, runtime logs (`*.log`), and server caches.

---

## 2. HazardDirector & Event Integrity Fixes (P0 & P1)

- **Resolved Hazard Task Leaks:**
  - Fixed `scheduleImpactCheck` in `HazardDirector.java` by converting anonymous tasks to tracked `BukkitRunnable` instances that invoke `this.cancel()` and deregister from `activeTasks`.
  - Wrapped rockfall impact handling in an `AtomicBoolean resolved` guard to ensure impact damage, block particles, and cleanup fire strictly once per falling block.
- **Damage Cooldown Protections:**
  - Added `playerRockfallCooldown` (2000ms) and `playerVentCooldown` (1500ms) preventing damage ticks from stacking and instantly killing players.
- **Spawn & Claim Protection Guardrails:**
  - Implemented `findSafeTargetLocation` to guarantee hazards never spawn inside the world spawn safe-zone (`spawn-safe-zone-radius`), in water/deep ocean, or within GriefPrevention player land claims (via dynamic reflection decoupling).
- **Scheduled Vent Expiry:**
  - Fixed `spawnHeatVent` timer tasks to call `this.cancel()` upon reaching the configured lifetime duration (`400 ticks / 20 seconds`).

---

## 3. Volcano Encounter & Seasonal Event Timeline Unification (P1)

- **Arena & Timeline Synchronization:**
  - Defined explicit arena coordinates in `config.yml` (`volcano.center.x/y/z` and `volcano.arena-radius: 120`).
  - Unified `PUNCAK_DURATION_SEC = 600` (10 minutes) across `KalaEventManager.java` and `VolcanoEncounterDirector.java`.
  - Created JUnit 5 unit tests verifying that all 6 encounter subphases sum contiguously to exactly 600 seconds.
- **Mutual Exclusivity:**
  - Enforced server-wide mutual exclusivity: Seasonal micro-events cannot be scheduled or manually triggered while Gunung Murka's eruption cycle is active.
- **Realistic Objectives & State Snapshotting:**
  - Fixed seasonal event target calculation: dynamically sets `objectiveTarget = spawnedEventItems.size()`.
  - Added weather state snapshots (`savedStormState`, `savedThunderingState`, `savedWeatherDuration`) to restore natural weather when events terminate.
  - Tagged seasonal collectible items with `NamespacedKey("kala", "item_type") = "volcano_ash_sample"` using `PersistentDataType.STRING`.
- **Event Isolation:**
  - Refactored `WorldImpactPolicy.cleanupEvent(String eventId)` to exclusively despawn entities tagged with matching `kala:event_id`, preventing accidental wipes of coexisting entities.

---

## 4. Durable Storage, Economy & Ledger (P1)

- **Atomic Claims & WAL Synchronization:**
  - Added `event_claims` table in SQLite with `UNIQUE(event_id, uuid)` compound key to guarantee single-claim payout integrity.
  - Added startup reconstruction in `EventContributionLedger.java` from persistent SQLite history.
  - Replaced immediate auto-payout with `/kala claim-event <id>` to preserve server resources during high-traffic finishes.
  - Added thread-safe `dbLock` synchronization and WAL checkpointing (`PRAGMA wal_checkpoint(TRUNCATE)`) in `StorageManager.java` and `BackupManager.java`.
- **Thread Safety in Economy:**
  - Captured `Player#getName()` and `Player#getUniqueId()` synchronously on the primary server thread before delegating queries to asynchronous database tasks.
  - Added atomic transfer synchronization and offline deposit support in `RupiahEconomy.java`.

---

## 5. UI, Notification & Crossplay Optimization (P2)

- **Flicker-Free Fast Scoreboard:**
  - Re-architected `KalaScoreboardManager.java`: stopped discarding and recreating Bukkit Objectives every 40 ticks. Reuses static teams and updates prefixes only when text values change.
  - Bound scoreboard to exactly 7 clean lines adhering strictly to Geyser Bedrock UI constraints.
- **Notification Deduplication:**
  - Added actionbar deduplication and priority preemption in `KalaNotificationService.java`.
