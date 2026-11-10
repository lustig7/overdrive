# Overdrive

NeoForge 1.21.1. An Overdrive block overclocks the machine behind it,
from any mod. The speed costs exponential energy, heat, and wear on the Overclock Chip.

## How it works

- Place the Overdrive against a machine. Its **back** (connector plate, `FACING`) targets that
  machine, and the glowing front faces you.
- Pick a multiplier in the GUI: 1×, 2×, 4×, 8×, 16×, 32× or 64×. Each server tick the target's
  `BlockEntityTicker` runs `m − 1` extra times.
- **Energy:** `100 × m²` FE/t (64× = 409,600 FE/t), with a 10M FE buffer fed through the energy
  capability. If the buffer can't pay, the multiplier steps down instead of shutting off. 1× is free.
- **Heat:** moves toward `min(100, m² / cooling × 40)` %. Cooling capacity:
  - none: 4
  - passive: up to 16 with four adjacent `#overdrive:passive_coolants` blocks (ice, packed/blue ice, snow)
  - water in the 16,000 mB tank: 128, using `m² × 0.02` mB/t (16× = 5.1 mB/t)
  - Cryo Coolant in the tank: 2048, using `m² × 0.0005` mB/t (64× = 2 mB/t)

  Fill the tank with a bucket or any pipe, from any side. Accepted fluids come from the tags
  `#overdrive:water_coolants` and `#overdrive:cryo_coolants`, so packs can add their own. When the tank
  runs dry, cooling falls back to passive. By the spec's numbers, cryo keeps 32× at 20 %, but 64× sits
  at 80 %, in the throttle band.

- **Heat thresholds:** < 70 % stable · 70–89 % throttled (multiplier halved, 5 % hysteresis) · 90–99 % chip wears 4×
  and a warning sounds · 100 % meltdown (explode / destroy / shutdown, configurable).
- **Chips:** T1 8×, T2 16×, T3 32×, T4 64×. Without a chip the limit is 2×. Wear scales with
  multiplier and heat. A chip that runs out of durability breaks.

## Upgrades, redstone and maintenance

- **Two upgrade slots.** Cards of the same kind multiply, so two Heat Sinks give ×2.25.
  - **Heat Sink:** cooling capacity × 1.5 (`upgrades.heatSinkCooling`).
  - **Efficiency:** energy cost × 0.75 (`upgrades.efficiencyCost`).
  - **Overcharge:** doubles the chip's multiplier limit (T3 → 64×), but heat × 1.5
    (`upgrades.overchargeHeat`). `multiplier.max` still caps it.
- **Redstone mode** (the button next to the upgrade slots): ignore / run with a signal / run without a
  signal. While held off, the Overdrive idles at 1×, which costs nothing.
- **Comparator output:** heat 0–100 % maps to signal 0–15. Good for automatic shutdown circuits.
- **Thermal Paste:**
  - Use it on an Overdrive to remove 30 % heat (`thermalPaste.heatReduction`).
  - Craft a damaged chip with 1–4 paste to restore 25 % durability per paste (`thermalPaste.chipRepair`).
- **Sounds and particles:**
  - A hum whose pitch rises with the multiplier, an alarm in the damage zone, and a hiss and break sound.
  - All sounds are defined in `sounds.json` and point at vanilla sound files, so a resource pack can
    replace them.
  - Particles: steam when running on water, frost on cryo, sparks above 90 %.

## Integrations

- **Mekanism / AE2 recipes:** the block, chips T1–T3 and the upgrades each have three recipe variants,
  and exactly one loads:
  - Mekanism installed: circuits and alloys (`#c:circuits/*`, `#c:alloys/*`).
  - Otherwise, AE2 installed: processors, certus and fluix.
  - Neither installed: vanilla ingredients.
- **Jade:** the tooltip shows the multiplier, heat, FE/t, cooling and the redstone state. Jade's own
  bars show energy and the tank.
- **JEI:** info pages for the block, chips, upgrades, Thermal Paste and Cryo Coolant.
- Jade and JEI are `compileOnly`; neither is required at runtime.
- **Allthemodium:** if the mod is installed, the T4 chip recipe uses four Allthemodium ingots
  (`#c:ingots/allthemodium`) instead of echo shards. Otherwise it uses the vanilla recipe. Both are plain
  recipes with `neoforge:conditions` (`overclock_chip_t4.json` / `overclock_chip_t4_allthemodium.json`).
- **Advancements:** an Overdrive tab with 9 advancements. Two custom triggers can also be used by packs
  and quest mods:
  - `overdrive:overclock` with `min_multiplier` (default 2) and an optional `coolant`
    (`none`/`passive`/`water`/`cryo`). Checked once a second for players within 16 blocks.
  - `overdrive:meltdown`. Fires for players within 16 blocks when an Overdrive hits 100 % heat.
- **FTB Quests:** if FTB Quests is installed, the chapter `overdrive.snbt` (19 quests: item,
  advancement and checkmark tasks) is copied into `config/ftbquests/quests/chapters/`.
  - The marker file `config/overdrive/ftbquests_chapter.installed` records the chapter version. A newer
    Overdrive replaces an older chapter; packs that edit it should set `integration.ftbquests.updateChapter = false`.
  - A deleted chapter isn't re-added. Delete the marker file to reinstall it.
  - `integration.ftbquests.installChapter` turns the whole feature off. Both settings are in
    `config/overdrive-common.toml`.
  - Quest IDs are fixed and always positive, because FTB Quests re-rolls negative IDs and that breaks
    dependencies.

## Limits

- Never accelerates another Overdrive, or anything in the block entity type tag
  `#overdrive:blacklisted` (spawners, AE2 controller, …). Packs can extend that tag with KubeJS or a
  datapack.
- `performance.maxExtraTicksPerChunk` (default 2048) caps the extra ticks per chunk per server tick.
- If a machine's ticker throws during an extra tick, that block entity type stops being accelerated
  until the server restarts, and the error is logged.
- **Machines that measure progress with `level.getGameTime()`**, rather than counting their own
  ticks, can't be accelerated. They tick more often, but the game time doesn't move any faster.
  Only the server ticker is accelerated; client-side animations run at normal speed.

All balance values live in the server config (`serverconfig/overdrive-server.toml`), which is synced
to clients.

## Development

```sh
./gradlew build               # jar in build/libs
./gradlew runClient
./gradlew runGameTestServer   # 20 GameTests in run-gametest/ (ignores run/mods)
python3 tools/gen_assets.py   # textures, block/item models, blockstates, the test structure
python3 tools/gen_data.py     # recipes (+ mod variants), advancements, their translations, the FTB Quests chapter
```

Textures are generated from the pixel maps in `tools/gen_assets.py`. Glow pixels go into separate
overlay textures, rendered fullbright through `neoforge_data` in the block models.

## Roadmap

- [x] Phases 1–3: block, ticking, energy, config, budget, blacklist
- [x] Simple heat (passive cooling), chip slot and wear, GUI, GameTests
- [x] Phase 4: water/cryo coolant tank, Cryo Coolant fluid + bucket
- [x] Allthemodium T4 recipe, advancements, FTB Quests chapter
- [x] Upgrades, redstone control, comparator output, Thermal Paste, chip repair
- [x] Phase 5: Mekanism/AE2 recipe variants (`tools/gen_data.py`)
- [x] Phase 6: sounds (hum, alarm, hiss) and coolant particles
- [x] Phase 7: Jade, JEI (EMI shows JEI info pages through its JEI compat)
- [ ] Phase 8: Spark profiling (100 × 64×), release
