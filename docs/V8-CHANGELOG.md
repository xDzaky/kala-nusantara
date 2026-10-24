# Kala Nusantara V8 Changelog

**Release:** Version 0.1.0-V8 (Audit Resolution, Disaster Recovery & Nusantara Discovery)  
**Date:** 2026-10-25  
**Target Platform:** PaperMC 26.3 (Java 21 Toolchain) + Geyser / Floodgate Crossplay  

---

## 1. Security, Authentication & Git Hygiene (P0)

- **Authentication Alignment:**
  - Synchronized `online-mode=true` across `server/server.properties`, `server/server.properties.example`, and `docs/COMPATIBILITY.md`.
  - Configured secure standalone Paper mode: Mojang authentication for Java Edition and Floodgate authentication for Bedrock clients (preventing Java account spoofing).
- **Runtime & Player Data Untracking:**
  - Untracked runtime files from git: `server/plugins/Geyser-Spigot/cache/`, `server/plugins/GriefPreventionData/PlayerData/`, `server/usercache.json`, `ops.json`, `banned-players.json`, `banned-ips.json`, and `whitelist.json`.
  - Added strict ignoring rules in `.gitignore` for player data, cache, and runtime logs.
- **Continuous Integration (CI):**
  - Added `.github/workflows/ci.yml` running automated unit tests and shadowJar builds with OpenJDK 21 on all pushes and pull requests.

---

## 2. Payout Ledger Outbox Pattern & SQLite Serialization (P0 / P1)

- **Outbox State Machine:**
  - Refactored `EventContributionLedger.java` to use a stateful outbox pattern with states `PENDING`, `PAID`, `FAILED_RETRYABLE`.
  - Added schema columns to `event_claims`: `claim_id`, `player_name`, `score`, `status`, and `paid_at`.
  - Guaranteed SQLite as the Single Source of Truth: players are added to in-memory `claimedCache` only after database commit and payout succeed.
  - Implemented automatic startup reconciliation: any unfinalized `PENDING` claims from an unexpected server crash or reboot are automatically paid and transitioned to `PAID` during plugin initialization.
- **Truly Atomic Economy Transactions:**
  - Refactored `RupiahEconomy.java`: unified transfer logic into an atomic SQLite transaction under `dbLock`.
  - Sender deduction, receiver credit (whether online or offline), and dual audit logging occur in the exact same transaction before returning `TransferResult.SUCCESS`.
  - Added rigorous validation: nominal amounts are rounded to 2 decimal places, and negative, zero, NaN, infinite, or self-transfers are rejected immediately.

---

## 3. Disaster Recovery & Hazard Fail-Closed Guardrails (P1)

- **Disaster Recovery Dual Category:**
  - Refactored `BackupManager.java` to support:
    - **Database Snapshot (`/ka backup db`):** Atomic SQLite snapshot with WAL checkpointing under `dbLock` to `backups/db/`.
    - **Full Server Backup (`/ka backup full`):** Complete ZIP archive containing database, configurations, and a `manifest.json` with SHA-256 integrity checksums.
  - Automated retention pruning: automatically retains only the 10 most recent backups.
  - Added `verifyBackup` method for verifying zip integrity and internal database presence.
- **Fail-Closed Hazard Validation:**
  - Refactored `HazardDirector.java`: `isInsidePlayerClaim` now returns `true` (unsafe) if GriefPrevention reflection encounters any exception (fail-closed).
  - Explicit Y offset separation: `highestY + 1.0` for air impact targets vs solid surface block beneath.
  - Applied `isValidHazardTarget` checks to fallback targets; safely cancels rockfall if no valid solid ground exists outside spawn safe-zones.
- **Startup Entity Sweep:**
  - Added `sweepStrandedEventEntities()` in `WorldImpactPolicy.java`, automatically executed 20 ticks post-startup to clean up orphaned event entities left from ungraceful process terminations.
- **Worldgen Scope Enforcement:**
  - Explicitly locked `kala_main: false` in `BetterStructures/ValidWorlds.yml` and `config.yml` to preserve hand-crafted island lore and prevent accidental procedural terrain overwrites.

---

## 4. Crossplay UX & Nusantara Content Expansion (V8-B & V8-C)

- **Bedrock-Friendly Seasonal Events:**
  - `SeasonalEventDirector.java`: Badai Selat crates now intelligently locate shoreline/beach anchors rather than arbitrary inland rectangles.
  - Abu dan Bara spawns physical interactive markers (`ArmorStand` with Magma Block head and nametag) providing direct touch targets for Bedrock/MCPE screens.
  - Added entity interact listener in `EventHazardListener.java`.
- **Nusantara Discovery Catalog:**
  - Added `NusantaraDiscoveryManager.java` allowing non-violent observation of fauna, geological formations, and island landmarks with SQLite persistence and milestone rewards.
- **Weekly Village Contracts (Kontrak Gotong Royong):**
  - Added `VillageContractManager.java` offering rotating weekly village contracts with caps to prevent economic hyperinflation.
- **Resource Pack Standardized Structure:**
  - Established `resource-packs/` directory containing separate Java (`pack.mcmeta`) and Bedrock (`manifest.json`) manifests, Geyser item mappings (`custom_mappings.json`), and vanilla fallbacks for custom utility items.
