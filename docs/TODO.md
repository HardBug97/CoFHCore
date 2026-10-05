# TODO

Phase A (1.21.1) is **code-complete**: all four repos build clean and boot headless on
NeoForge 21.1.251. See [api-notes-1.21.1.md](api-notes-1.21.1.md) for the confirmed shapes and
[progress-log.md](progress-log.md) for the chronology.

## Owed before Phase A is signed off

1. **Client pass (Joel).** The one exit criterion a headless boot cannot cover. Port plan §A.4:
   a machine GUI (TE), an energy/fluid/item cell in world and in item form (TC), a duct network
   moving items and energy (TD), a JEI machine recipe page, the Patchouli guidebook opening,
   wrench side-config, a placed fluid, particles from a dynamo or machine. This hop rewrote the
   whole vertex/model/particle surface, so **everything client-side is unverified**.
2. **`runData` has not been run** in any repo. The committed generated output was hand-migrated
   (recipe result keys, singular folders); regenerating it would confirm the providers agree.

## Phase B (26.1.2)

Not started. [port-plan.md](port-plan.md) §6 has the full category list; B.4 (the transfer API
rework) and B.7 (the client rewrite) are the two XL buckets. Read
[api-notes-1.21.1.md](api-notes-1.21.1.md) first — several 1.21.1 shapes are replaced again at
26.1 (tool/armour item classes, `INBTSerializable`, `ResourceLocation` itself), and the notes
say which.

## Inbox

- **Client pass 2026-10-05 on branch `1.21.1-client`: built, `runServer` and `runClient` run.**
  See the progress log's two entries of that date. Seen working in a real client (Xvfb + Mesa,
  CoFH + TC + TE + TD jar): main menu, world load, worn Thermal armour, item tints in the hotbar,
  placed machine/cell/dynamo/duct models, Redstone Furnace GUI with side-config panel, JEI recipe
  page, Patchouli guidebook (categories and entries), duct attachment GUI and its +/- packets,
  chilled-effect mouse turning with no mixin error. Not looked at: particles up close, fluids in
  world, energy actually flowing through a duct network, the wrench area overlay.
- **`bottler_water_bottle` (TE) gives a potion with `custom_data {Potion: water}`** instead of
  `potion_contents`, because `RecipeJsonUtils#parseItemStack` maps the old `nbt` key onto
  `CUSTOM_DATA`. Loads fine, wrong item. Needs `components` support in `parseItemStack`.
- ~~`RecipeManager#byType`~~ resolved: ThermalCore's 26 call sites use the public
  `getAllRecipesFor(type)`, which returns a `List<RecipeHolder<T>>` rather than a Map.
- MDG artifact names for 1.21.1 are `build/moddev/artifacts/neoforge-21.1.251{,-sources,-merged}.jar`
  (Pyronetics' `minecraft-patched-<ver>` naming is the 26.1 layout). Use the `-sources` jar.

- `ThermalExpansion`'s hand-written machine recipes use `{"item": …, "count": n}` /
  `{"tag": "forge:…"}` parsed by CoFH's own `RecipeJsonUtils` — survives the 1.21.2 ingredient
  format change, but check `RecipeJsonUtils.parseIngredient` compiles (`Ingredient.fromJson` is gone).
- ~~`PotionColorCalculationEvent`~~ resolved in category 5: replaced by
  `CustomParticleMobEffect#createParticleOptions` returning null (per-effect, not global).
- ~~Cross-repo API changes ThermalCore/TD/TE must follow~~ all applied; kept here as the record
  of what the family's own API looks like on 1.21.1 (see also api-notes-1.21.1.md):
  `IEnergyContainerItem#getOrCreateEnergyTag` → `getEnergyTag` + `mutateEnergyTag`,
  `IFluidContainerItem#getOrCreateTankTag` → `getTankTag` + `mutateTankTag`,
  `IMultiModeItem#getOrCreateModeTag` → `getModeTag`/`setModeTag`, `ConfigManager#register` takes
  the `ModContainer`, and `Constants`' `UUID_*` modifier ids are `ResourceLocation`s now.
  TC's `EnergyCellBlockItem` overrides two of those.
- ~~Two unlisted mixin classes~~ resolved: `ClientPacketListenerMixin` and
  `ClientboundSetEntityMotionPacketMixin` are **entirely commented out** upstream, so leaving
  them unlisted is correct. What did need fixing: `MultiPlayerGameModeMixin` was listed under
  `mixins` rather than `client` and so failed to apply on a dedicated server, and
  `compatibilityLevel` is `JAVA_21` now.
