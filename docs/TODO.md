# TODO

Phase A (1.21.1) is **code-complete**: all four repos build clean and boot headless on
NeoForge 21.1.251, and `runData` now runs in all four. Phase B (26.1.2) is under way on the
`26.1.2` branch: **all four repos compile and boot headless on 26.1.2** (B.0–B.10 done). What remains is
Joel's client pass and the Inbox below. See
[api-notes-1.21.1.md](api-notes-1.21.1.md) / [api-notes-26.1.2.md](api-notes-26.1.2.md) for
confirmed shapes and [progress-log.md](progress-log.md) for the chronology.

## Owed before Phase A is signed off

1. **Client pass (Joel).** The one exit criterion a headless boot cannot cover. Port plan §A.4:
   a machine GUI (TE), an energy/fluid/item cell in world and in item form (TC), a duct network
   moving items and energy (TD), a JEI machine recipe page, the Patchouli guidebook opening,
   wrench side-config, a placed fluid, particles from a dynamo or machine. This hop rewrote the
   whole vertex/model/particle surface, so **everything client-side is unverified**. The
   `runData` pass already caught one client crash (`LevelRendererMixin`, fixed in `fa87214`), so
   expect more of that kind. Also check the specific item below.
2. **`MouseHandlerMixin` (Chilled effect's mouse slowdown), unverified.** It is a
   `@ModifyVariable(method = "turnPlayer", ordinal = 3, at = @At("STORE"))` over `double`
   locals. On 1.21.1 `turnPlayer` gained a `double` parameter and NeoForge's
   `CalculatePlayerTurnEvent`, so the LVT is now `d2` (sensitivity) / `d3` (its cube) / `d4` /
   `d0`/`d1` (turn deltas) and ordinal 3 very likely lands on a different variable than on 1.20.4.
   Symptom if wrong: Chilled slows only one axis, over- or under-scales, or the mixin fails to
   apply when `MouseHandler` loads. The clean fix is to drop the mixin for a
   `CalculatePlayerTurnEvent` handler (`setMouseSensitivity`). In the client pass, apply
   Chilled II and turn the camera.
3. ~~`runData` has not been run~~ **done 2026-09-22**, all four repos. It needed `data()` rather
   than `clientData()` on 1.21.1, and ThermalExpansion needs `--existing-mod thermal`. The
   regenerated output fixed silently-broken data: CoFHCore's `c:` tags and a loot table were
   still in 1.20 plural folders, 86 advancements matched any item, 4 loot tables ignored silk
   touch, and 8 of TD's advancements were missing. Details are in the progress log.

## Phase B (26.1.2) — in progress, CoFHCore first

| Step | Commit | Errors after |
|---|---|---|
| B.0 build bump + forced AT sweep (B.9's AT half) | `6eb201f` | 2445 / 318 files (baseline) |
| B.1 mechanical renames | `6f0c7a7`, stragglers `e7896aa` | 1537 |
| B.2 registration | `f02ba1b` | (included above) |
| B.3 persistence — bridged at `BlockEntityCoFH`, entities native, `SavedDataType`, tag getters | `8bd2f62` | 1364 |
| B.4 transfer API — CoFH handlers also implement the new interfaces (per-storage journals), query sites wrap with `.of()` | `ccfc12d` | 1326 |
| B.5 items/tools/armour (`41d84d9`), then entities, blocks, fluids, commands, packets, util (`3936c2b`) | `41d84d9`, `3936c2b` | 895 |
| B.6 recipes, loot functions, datagen | `d21d54a` | 723 |
| B.7 client — five parallel agents (GUI; models/fluids/setup; entities; particles; render types/events) | `cc71b36` | 8 |
| B.8 CoFHCore resources (recipe folder, item definition, render_type; shaders in B.7) | `789370c` | — |
| B.9 mixins | `f89000a` | **0** |
| B.8 `runData` (output identical to what is committed) and a headless boot: `Done`, mixins applied | `mods.toml` fix below | 0 |
| **B.10 ThermalCore** — five parallel agents, then the recipe-template bridge, resources, `runData`, headless boot | `3884e7f` + follow-up | **0**, boots to `Done`, 1760 recipes |
| **B.10 ThermalExpansion** — two agents (code; datagen), boot | `0bf2bea` | **0**, boots to `Done`, 2287 recipes |
| **B.10 ThermalDynamics** — two agents (client; common), boot | TD `26.1.2` head | **0**, boots to `Done`, 1770 recipes |

[port-plan.md](port-plan.md) §6 has each category's contents. Carry-overs into Phase B:

- **B.1 table rows**: all swept as of B.7. `javax.annotation` (95 files) still resolves; leave it.
- **B.9 dropped `LevelRendererMixin`** (its injection point is gone with the frame graph). The depth-mask
  workaround it carried (Flywheel's, for Fabulous graphics) may or may not still be needed: **check
  Fabulous graphics with CoFH translucent effects in the client pass.**
- **B.10 inherits from B.3**: TC/TD block entities keep their `(CompoundTag, Provider)` overrides
  if they extend `BlockEntityCoFH` (check TD's `DuctBlockEntity`); their entities convert natively;
  TD drops `INBTSerializable` from its grid classes and moves `GridContainer` to `SavedDataType`.
  Friend lists and grid data won't carry over from 1.21.1 worlds (the saved-data file paths moved).
- **B.10 inherits from B.4**: capability registration moves to `Capabilities.Item/Fluid/Energy.BLOCK`,
  and `AugmentableBlockEntity`'s cached `itemCap`/`fluidCap`/`energyCap` fields narrow to the CoFH
  handler types. Also call `invalidateCapabilities()` after side-config changes and wrench
  rotation. B.4 is **unverified at runtime**: test a machine with a pipe mod or a GameTest,
  including an aborted simulation.
- **B.10 inherits from B.5**:
  - ThermalCore's armour takes `ArmorMaterial`/`ArmorType`. Its materials become static records with
    durability, a repair **item tag** and an equipment asset id, and it needs
    `assets/thermal/equipment/*.json` (B.8).
  - `setEnchantability` calls move into the properties. The augmentable items' per-stack
    enchantability must write the `ENCHANTABLE` component.
  - `getCreatorModId`/`getBurnTime`/`appendHoverText` take their new signatures everywhere.
  - `DevicePotionDiffuserBlockEntity` calls `applyInstantenousEffect(serverLevel, …)`.
  - The Thermal minecarts drop `getMinecartType` and adopt the new `destroy`/`activateMinecart`/
    `hurtServer` signatures.
  - `neighborChanged` takes an `Orientation` in `TilledChargedSoilBlock` and TD's `DuctBlock`.
  - **TD's ducts used the neighbour's `fromPos`** to decide which side changed
    (`DuctBlockEntity#neighborChanged`, `DuctBlock#getBlockEntity(fromPos)`). That position no
    longer exists, so the duct needs a different trigger. `ITileCallback#neighborChanged` now
    receives the block's own position.
- **B.10 inherits from B.6**:
  - `ThermalRecipe`, `ThermalFuel` and the device mappings/boosts override `getSerializer()`/`getType()`
    with the narrowed `RecipeSerializer<? extends Recipe<RecipeInput>>`/`RecipeType<…>` returns, drop
    `getResultItem`/`getIngredients`/`canCraftInDimensions`, take `assemble(input)`, and their
    serializers become `new RecipeSerializer<>(CODEC, STREAM_CODEC)` (records). Results that were
    `ItemStack` fields can stay so; only vanilla-facing results need `ItemStackTemplate`.
  - `Ingredient.EMPTY` → `EmptyIngredient.EMPTY`; `Ingredient.of(tag)` → `stack.is(tag)` at runtime,
    `items.getOrThrow(tag)` in datagen; `ingredient.getItems()` → `items()` (holders).
  - Recipe lookups are `serverLevel.recipeAccess().getRecipeFor(type, input, level)`; the client
    `RecipeAccess` has none, so `ThermalRecipeManagers` fill their client caches from
    `RecipesReceivedEvent` after `OnDatapackSyncEvent#sendRecipes` (port plan §B.6).
  - Datagen: each recipe provider gains a `RecipeProvider.Runner`, `GatherDataEvent.Client`, no
    `ExistingFileHelper`; delete the five Thermal model/blockstate providers.
- **B.10 inherits from B.7** (the agents' per-repo recipes are in api-notes B.7; the essentials):
  - **Screens** (TC/TD/TE): `ContainerScreenCoFH(menu, inv, title, imageWidth, imageHeight)`
    (`MachineCrafterScreen` 190, `ItemBufferScreen` 178 pass it up); `renderBg` → `extractBackground(g, mx, my, a)`,
    `renderLabels` → `extractLabels(g, mx, my)`, never override `render`; `mouseClicked(MouseButtonEvent, boolean)`,
    `mouseReleased(MouseButtonEvent)`, `keyPressed(KeyEvent)`, `hasClickedOutside` without the button,
    `tick` → `containerTick`; every element/panel draw method takes `GuiGraphicsExtractor`; direct
    `g.drawString(...)` calls (`EnergyCellScreen`, `FluidCellScreen`, `DeviceSoilInfuserScreen`,
    `EnergyLimiterAttachmentScreen`, the two servo screens) → `drawString(g, …)` or `g.text(font, …, 0xFF404040, false)`;
    `SatchelScreen`'s `setShaderTexture0` + `drawTexturedModalRect` → the texture-explicit overload.
    CoFH element callbacks (`mouseClicked(double, double, int)` etc.) are unchanged.
  - **Baked models** (TC's six, TD's duct): extend `DelegateBlockStateModel`, override `collectParts`, wrap
    each part in `ModelUtils.WrappedBakedModelBuilder`, read `level.getModelData(pos).get(ModelUtils.*)`,
    `ModelUtils.retexture(quad, sprite)`; register on `RegisterBlockStateModels` with
    `new SimpleModel.Loader(X::new).codec()`; the item side is a static `forItem(ItemStack, BlockStateModelPart)`
    registered on `RegisterItemModelsEvent` with `new SimpleItemModel.Loader(X::forItem).codec()`. TD's
    `BackfaceBakedQuad` marker becomes a separate list; its duct geometry loader is a real `UnbakedModelLoader`.
    `RenderHelper.mulColor` still exists.
  - **Item properties/tints**: `ProxyUtils.registerItemModelProperty` calls stay; each such item's
    `items/<name>.json` dispatches with `minecraft:range_dispatch` on the id. Colourable items list
    `{"type": "cofh_core:colorable", "index": n}` tints. `IClientItemExtensions#getHumanoidArmorModel` is gone —
    the Beekeeper/Hazmat/Diving suits need the equipment-asset route.
  - **Fluids** (TC's 14): delete `initializeClient`, register `FluidModel.Unbaked` in a `RegisterFluidModelsEvent`
    handler. Fluid-container items: `items/*.json` with `"type": "neoforge:fluid_container"`.
  - **Entity renderers** (TC's 12): the recipe in api-notes B.7(c) — state class, `createRenderState`,
    `render` → `extractRenderState` + `submit`, items via `ItemModelResolver`, blocks via `BlockModelResolver`,
    quads via `submitCustomGeometry`, delete `getTextureLocation`, `EntityModel<S>`; boat layers register
    `BoatModel::createBoatModel`/`createChestBoatModel`.
  - **Events**: `TCoreClientEvents.handleRenderLevelStageEvent` → `RenderLevelStageEvent.AfterTranslucentParticles`
    (camera = `getLevelRenderState().cameraRenderState.pos`; line vertices need `setNormal` + `setLineWidth`);
    TD `DebugRenderer` likewise, its render types on `CoreShaders.POSITION_COLOR_NO_DEPTH`/`LINES_NO_DEPTH`.
  - **Particles**: nothing at source level (TC only spawns `CoreParticles.*`).
  - **`DeferredRegisterCoFH.register(name, Type::new)`** is ambiguous for types with two constructors; write
    `() -> new Type()`.
- **B.10 inherits from B.9**: a CoFH shield item must carry `DataComponents.BLOCKS_ATTACKS` or
  `LivingShieldBlockEvent` never fires for it (vanilla's `ShieldItem` sets it via `delayedComponent`).
- **B.8: regenerate, don't hand-migrate.** The `26.1.2` branch predates the 1.21.1 `runData`
  commits, so its `src/main/generated` still has the stale 1.20 layout. On 26.1 the data run is
  `clientData()`, which the 26.1.2 `build.gradle` already declares. ThermalExpansion's
  `--existing-mod thermal` must carry over.

## After the port (Joel, 2026-09-22)

1. **Energy and fluid items aren't exposed as capabilities**, and weren't on 1.21.1 either (it's
   a pre-existing gap, not a regression). Energy cells and fluid cells in item form, the RF potato,
   the florb, satchels and any other `IEnergyContainerItem`/`IFluidContainerItem`/
   `IInventoryContainerItem` item register no item capability. So other mods' chargers, tanks and
   pipes can't see them, and `EnergyCellBlockItem`'s registration is commented out. On 26.1.2 this
   means `ItemAccess`-based handlers (`ItemAccessEnergyHandler`-style, exchanging the stack through
   the access) in place of `EnergyContainerItemWrapper`/`FluidContainerItemWrapper`/
   `InventoryContainerItemWrapper`, registered with
   `event.registerItem(Capabilities.Energy.ITEM, …)` etc. in ThermalCore. See
   api-notes-26.1.2.md, B.4 "Not covered".

## Inbox

- **Client pass findings (2026-09-26)**, from Joel's first `runClient` in ThermalExpansion:
  - ~~Joining a world disconnected with `Failed to encode packet 'clientbound/minecraft:custom_payload'`~~
    fixed: `SecureRecipe`'s map codec made a new instance per recipe while its stream codec was
    `StreamCodec.unit` of a different one, so `neoforge:recipe_content` couldn't encode it. Both now share
    `SecureRecipe.INSTANCE` (vanilla's `RepairItemRecipe` pattern). A headless boot never encodes that payload.
  - Model warnings, all **upstream gaps, not regressions** (the textures don't exist on `1.20.4` either):
    missing textures for `diamond_coin*`, `emerald_coin*`, `diamond_nugget`, `emerald_nugget` (the textures are
    at `item/`, the models point at `item/nuggets/`), `area_depth_augment`, `dual_filter_augment`,
    `upgrade_augment_4`; `upgrade_augment_4` also fails to bake (blocks and items atlas mixed). Missing
    block model `thermal:block/chunk_loader` and `minecraft:item/template_spawn_egg`.

- **B.5 behaviour changes the new API forced (2026-09-22)**, each to confirm in play:
  - **CoFH cake and feast blocks no longer apply food effects**, because `FoodProperties` lost them
    in favour of the `Consumable` component. Nothing in the four repos builds one; restoring it
    means the block holding its own effect list.
  - **Custom rail max speed is inert** (`getRailMaxSpeed` has no engine hook). Nothing uses it.
  - **Neutral effects are now cleared by milk.** NeoForge removed `EffectCure`, so there's no
    exemption.
  - **Custom effect particles are sent from the server**, because `applyEffectTick` is
    server-only. The Wrenched rotation and Chilled freezing run server-side too.
  - **Area-effect "tool" is `has(DataComponents.TOOL)`**, which now includes swords and shears.
    That's harmless without Excavating.
  - **Item-stored block-entity data needs a `BlockEntityType`.** `ItemHelper.setBlockEntityData`
    infers it. Energy/fluid cells and machines in item form must keep their data when placed.
  - **Lightning from CoFH uses the `TRIGGERED` spawn reason.**

- **B.7 behaviour changes the new API forced (2026-09-22)**, each to confirm in the client pass:
  - **Post effects are stubbed** (`PostEffect`/`PostBuffer`): the pixelate "stylized graphics" look is gone and
    `CoreClientConfig.stylizedGraphics` does nothing. A real port needs `assets/cofh_core/post_effect/pixelate.json`
    (`PostChainConfig`), the `program/pixelate*.fsh` shaders on 330 uniform blocks, loading through
    `getShaderManager().getPostChain(...)` and a frame-graph insertion (`FrameGraphSetupEvent` or
    `PostChain#addToFrame` from `AfterLevel`), and `PostBuffer` owning a `TextureTarget`.
  - **`ShockwaveRenderer` renders nothing** (`VFXHelper.renderShockwave` needs a collector-based rewrite with
    a `ShockwaveRenderState`). Unused by CoFHCore/ThermalCore registrations.
  - **Tooltips**: vanilla's slot-item tooltip and CoFH's element/panel tooltip no longer both draw; the first
    `setTooltipForNextFrame` in a frame (vanilla's) wins.
  - **GUI fills always alpha-blend** (`drawSizedRect` was unblended); list boxes clip with scissor, not stencil.
  - **Item colours and item-property models need the item JSON** (`cofh_core:colorable` tints,
    `range_dispatch` properties); `COLORABLE_ITEMS` in `CoreClientSetupEvents` is now write-only — drop it once
    the Thermal repos stop calling `addColorable`, or make `ColorableItemTint` consult it.
  - **Particles**: `ShardParticle`'s trail and body share one batch; the sprite particles lose the `z + 0.1`
    camera nudge and sample light at the current rather than interpolated position; CoFH custom particles are
    frustum-culled by bounding box and drawn in `RenderType` first-use order; the three CoFH particle layers
    write no depth (as before) while vanilla's translucent layer does.
  - **Render types**: `opaque(...)` now binds lightmap + overlay; `translucent*` use the entity pipeline with
    `NO_CARDINAL_LIGHTING`/`NO_OVERLAY`; `OVERLAY_LINES`/`OVERLAY_BOX` no longer carry a line width — callers
    set it per vertex.
  - **Outlines and block damage** run through vanilla's outline passes and breaking list (same visuals,
    correct layering); true invisibility depends on the render-state modifier.
  - `FluidHelper.color`/`RenderHelper.getFluidColor` return white for a fluid without a tint source.
  - Potion fluid: white in world, potion colour as a stack (same values, now split across `FluidTintSource`).
- **B.9 behaviour changes**: shield blocking is gated by `canBlock` inside vanilla's angle/`BlocksAttacks`
  resolution (before, `canBlock` replaced vanilla's check entirely); horse armour (all five, and wolf armour)
  gets enchantability 15 and shields 1 via default components, as the mixins did.
- **Mixins never loaded in dev runs before now, on either branch.** The config was declared only as the jar
  manifest's `MixinConfigs` attribute, which a classes-on-disk run has no jar for; NeoForge reads
  `[[mixins]] config = "mixins.cofhcore.json"` from `neoforge.mods.toml`, added on `26.1.2`. Add it on `1.21.1`
  too (its headless boots passed without `LivingEntityMixin` etc. ever applying).
- **`1.21.1` branch**: `data/cofh_core/recipes/securable.json` is in the pre-1.21 plural folder there too, so the
  securable crafting recipe does not load on 1.21.1. Move it to `recipe/` if that branch ships.

- **B.6 behaviour changes (2026-09-22)**:
  - **An invalid or missing CoFH recipe ingredient now matches nothing** (`EmptyIngredient`), where
    `Ingredient.EMPTY` used to match an empty stack. Only malformed JSON reaches it.
  - **`SecureRecipe` lost `canCraftInDimensions` (≥ 2 slots).** The method no longer exists; a
    1×1 grid can't hold a lock and a securable item anyway.
  - **`getStandardTileTable` still copies `Info`/`Items`/`Energy` into `CUSTOM_DATA`**, which nothing
    reads on 26.1 (item-form data is `BLOCK_ENTITY_DATA`). No table in the four repos uses it.
  - `IDismantleable#dismantleBlock` passes `includeData = false` to `getCloneItemStack`, matching
    the old default (`state.getCloneItemStack(level, pos)` carried no data).

- **B.10 ThermalCore behaviour changes the new API forced (2026-09-22)**, each to confirm in play:
  - **The Beekeeper/Diving/Hazmat full-suit model is gone** (`IClientItemExtensions#getHumanoidArmorModel`
    no longer exists); the three suits render through the standard humanoid equipment layer from
    `assets/thermal/equipment/*.json`. Restoring the one-piece look needs a custom `EquipmentLayerRenderer`.
  - **Armour repairs by item tag** (`thermal:repairs_{beekeeper,diving,hazmat}_armor`, generated), not by ingredient.
  - **Augmentable item enchantability is a per-stack `ENCHANTABLE` component** written when augments change
    (was computed live). Florbs carry `ENCHANTABLE` (from `FluidContainerItem`'s `enchantable(5)`) but block it
    via `isPrimaryItemFor`/`supportsEnchantment`.
  - **Diving/Hazmat/XP-crystal `inventoryTick` runs server-side only** (vanilla signature).
  - **Charge and Tinker benches only see items exposing `Capabilities.Energy.ITEM`** — CoFH's own container
    items still register none ("After the port", item 1).
  - **Entity models render on `entityCutout`** (`entityCutoutNoCull` no longer exists; vanilla's Blaze made the
    same move); Basalz's culling-box inflate lives in its renderer; `DetonateUtils.nuke` casts to `ServerLevel`
    for the `ServerExplosion` resistance probe; Blitz trail / potion-diffuser particles carry an explicit
    colour and power (`SpellParticleOption`).
  - **Fog in ender/redstone/crude oil**: no cylinder shape; sky and cloud fog end at the fluid's far distance,
    as vanilla water does.
  - **Operational-area wireframes** ignore depth in every pass (`OVERLAY_LINES` is `LINES_NO_DEPTH`); the old
    "outside" pass may have been depth-tested — check visually. Line width is `THICK_LINES` per vertex.
  - **Gourmand fuel** reads `CONSUMABLE` for effects and eat time (items with `FOOD` but no `CONSUMABLE` count
    as effect-free, normal eat time); converted dynamo fuels and pulverizer ingot conversions use
    `Ingredient.of(item)`, so the JEI entry for a converted enchanted book shows a plain book.
  - **`CrafterRecipeManager` uses `Recipe#display()` for the result**: a special recipe with no display is rejected.
  - **Only Thermal recipe types plus `CRAFTING`/`SMELTING`/`BLASTING` are sent to the client**
    (`OnDatapackSyncEvent#sendRecipes`); a manager reading any other type client-side sees nothing.
  - **Recipe outputs are templates, materialized lazily**: empty or invalid output entries are dropped at parse
    instead of stored as EMPTY; `getOutputItems()`/`getOutputFluids()` cache their stacks (mutating a returned stack
    mutates the cache, as before). The Insolator's default water ingredient materializes on first use.
  - **Managers refresh later** (`DefaultDataComponentsBoundEvent`, after tags), and the client refresh no longer runs
    from `TagsUpdatedEvent`.
  - **The 24 compat `c:` tags are now emitted empty** (`data/c/tags/item/…`) because a missing tag is a hard recipe
    error on 26.1; recipes using them load and match nothing, as on 1.21.1.
  - **Florbs show no fluid tint** until CoFH fluid-container items expose `Capabilities.Fluid.ITEM`
    (`neoforge:fluid_container` reads `FluidUtil.getFirstStackContained`) — "After the port", item 1.
  - **Item JSONs** are written for every ThermalCore item (`items/*.json`, count/`thrown`/`stored`/`primed`/`armed`/
    `color`/`has_data` as `range_dispatch`); the generated assets moved from `src/main/generated/assets` into
    `resources`, since no provider owns them and `HashCache` deletes unowned files on the next run.
  - **Upstream slip fixed**: `ThermalClientConfig` defined "Festive Vanilla Mobs" with `festiveMobs` as its default
    supplier, which 26.1's `validateSpec` rejects at registration.

- **B.10 ThermalExpansion / ThermalDynamics behaviour changes (2026-09-22)**, to confirm in play:
  - **Dynamo items render through the plain block model** (ThermalCore has no `thermal:dynamo` item loader; 1.21.1
    routed items through the block loader). Machine items use the `thermal:reconfigurable` item loader.
  - **JEI potion-bottler entries** are `RecipeHolder`s keyed `thermal:bottler_potion_<potion>` (custom-effect potions
    share `…_custom`); the plugin reads the client `RecipeMap` instead of bailing without a level.
  - **TE sends no recipe types of its own**: every machine/dynamo type lives in `ThermalCore.RECIPE_TYPES`, which
    `TCoreCommonSetupEvents#datapackSync` sends, plus `CRAFTING` for the crafter's client path.
  - **The insolator rubberwood-sapling recipe errors at load** (its item is not registered, so the output list is
    empty now that empties are dropped); the centrifuge oil-sand recipes get empty ingredients for the same reason.
    Pre-existing dead recipes; decide whether to delete them.
  - **Fourteen more compat `c:` tags are emitted empty** (`ores/{apatite,cinnabar,niter,ruby,sapphire,sulfur}`,
    `dusts/{bronze,constantan,electrum,invar}`, `gears/invar`, `ingots/{bronze,constantan}`, `plates/bronze`,
    `storage_blocks/quartz`) for TE's compat recipes.
  - **Ducts read the changed side from `updateShape`** (`neighborChanged` has no neighbour position): on a neighbour
    `setBlock` both hooks fire, so the redstone refresh and `TileRedstonePacket` run twice; a duct's own
    `updateNeighborsAt` no longer invalidates the adjacent duct's attachment facing it (refreshes on the next block
    change or grid `refreshCapabilities`); flag-2 `setBlock`s now invalidate attachments too.
  - **Grid saved data moved** to `data/thermal_dynamics/grids.dat`; 1.21.1 grids don't carry over.
    `onRemove` → `preRemoveSideEffects`: attachments drop and the grid updates server-side only, not for flag-256 sets.
  - **Grid storages are transactional** (`SnapshotJournal`); other mods' new-API inserts/extracts on a duct reach the
    grid through the attachment wrappers; B.4 is unverified at runtime here too (servo/limiter/filter, aborted simulations).
  - **Duct quads pick their chunk layer per quad from the sprite alpha**; the debug laser line no longer writes depth.
    `DuctModel.bakedModels` still grows on every reload and `DuctModelData.equals` still ignores `fillColor` (both pre-existing).

- **Behaviour differences the style pass found (2026-09-22), left alone because that pass was
  not allowed to change behaviour.** Each is a 1.21.1 fix to decide on:
  - **Fluid buffers drop components.** TD's `FluidDuctWindowedBlockEntity` and its 4 fluid
    filter/servo menus, plus CoFHCore's `FluidFilterMenu#getGuiPacket`, hand-encode fluid id and
    amount. `FluidHelper.writeFluidStack`/`readFluidStack` exists to keep components. It's
    harmless while the GUIs only show fluid and amount, but it's inconsistent with the API notes.
  - **`CropBlockCoFH` lost its main-hand-only harvest check**, because `useWithoutItem` has no
    hand. The check could move to `useItemOn`.
  - **`InventoryContainerItem` reads with `RegistryAccess.EMPTY`**, which drops registry-bound
    components. §11 of the API notes says to use `ProxyUtils.registryAccess()` outside
    persistence.
  - **`EntityBlockCoFH` routes `useWithoutItem` into `useItemOn` with an empty stack**, the
    pattern §10 of the API notes warns about.
  - **ThermalCore `WrenchItem`** applies its attribute modifiers to every slot group via
    `MAINHAND`. Check that's intended.

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
