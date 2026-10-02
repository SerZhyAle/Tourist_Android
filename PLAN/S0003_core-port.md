# S0003 - Core port: sensors, GNSS, solar calculator, units, torch

**Status:** Draft
**Phase:** 3 of 10 (handoff section 8)
**Verification level:** unit tests of the ported classes, run green; every ported file has a ledger row.

## 0. Source

`docs/BOOTSTRAP_HANDOFF.md` section 8 item 3, section 5 (phone module: sensors and sun, GNSS, units, the torch
seam), section 6a (full port, ledger).

## 1. Goal

The kit's non-UI core exists in this repository, ported in full from FastMediaSorter and cleaned, with its unit
tests - so S0004 onward builds screens on ported behaviour, never on a rewrite.

## 2. Scope

Source paths are relative to `P:\ANDROID\FastMediaSorter_mob_v2` (read-only), `A` =
`app_v2/src/main/java/com/sza/fastmediasorter`. Re-measure the line counts before porting.

- Reading sources: `A/data/sensors/` (motion, orientation, step count, availability), the sensor use cases and
  models under `A/domain/usecase/sensors/` and `A/domain/model/sensors/`. `SensorSeriesRepositoryImpl` and the
  `SensorSeriesPointEntity` Room table serve launcher gadgets only - left behind.
- Sun: `A/domain/util/SolarCalculator.kt`.
- GNSS: `GnssStatusDataSource` and its use cases under Network Monitor - port what the dashboard reads (satellite
  count, used satellites), cut the `MonitorSection` / `SectionAvailability` coupling. Whether the track recorder
  (`record_gnss_track`, `GnssTrackRecorderTest`) ports is a tactical decision of this ticket, written next to
  its ledger rows.
- Units: `QuantityFormatter`, `UnitScale`, `Quantity`, `UnitSystemProvider`, `ObserveUnitSystemUseCase`.
  Imperial units and the 12-hour clock as FastMediaSorter ships them (handoff goal 5).
- The torch, extracted from `A/core/screencapture/gesture/DeviceActionHandler.kt` (shared there by
  `SosTorchManager`, `WaterFlashlightActivity` and the torch toggle, next to unrelated gesture and brightness
  code): the torch part only.
- Package rename `com.sza.fastmediasorter` -> `com.sza.tourist`; every sorter reference cut; every file recorded
  in the port ledger (S0002) with its source SHA-256 at port time.
- Unit tests ported with their classes: `SolarCalculatorTest`, `QuantityFormatterTest`, `UnitScaleTest`,
  `UnitSystemProviderTest`, `GnssTrackRecorderTest` if the recorder is ported.
- Classes uncovered in the source (the three reading sources, among them) are ported first and tested here
  afterwards; behaviour is never changed to make a test pass.

### 3.3 Owner inputs (Approval gate)

- **Related tickets:** S0002 (skeleton, port ledger and drift check) must be closed first.
- **Owner questions:** n/a - no open owner decision of handoff section 4 is needed by this phase.

## 4. Acceptance

- [ ] Every ported file has a ledger row; the drift check runs and lists nothing uncarried or decided.
- [ ] The ported unit tests pass; exit code and counts cited.
- [ ] No sorter surface (`MonitorSection`, program registry, Room series tables) remains in the tree.
