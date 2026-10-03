# NeoForge 26.1.2 API notes (CoFHCore, Phase B)

Successor to [api-notes-1.21.1.md](api-notes-1.21.1.md). Same rule as that file: **every shape here
was confirmed against the real 26.1.2.109 mapped jar**, not against a primer, a doc page, or
recollection. Entries are in the port plan's Phase B category order.

Shape oracles, in precedence order (port plan §6):

| What | Where |
|---|---|
| A mod already built and running on 26.1.2.109 | `../Pyronetics` source + [its `docs/api-notes-26.1.2.md`](../../Pyronetics/docs/api-notes-26.1.2.md) — read this first |
| Vanilla + NeoForge patches, as source | `build/moddev/artifacts/minecraft-patched-26.1.2.109-sources.jar` (`unzip -p … net/minecraft/…/X.java`) |
| NeoForge's own classes, as source | `~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/26.1.2.109/*/neoforge-26.1.2.109-sources.jar` |
| Signatures only | `javap -p -cp build/moddev/artifacts/minecraft-patched-26.1.2.109.jar <fqcn>` |
| Migration primers | `docs/reference/primers/26.1.md` (and 1.21.2 … 1.21.11 for the path) |
| NeoForge docs (current site = 26.1) | `docs/reference/docs-26.1/**` |

Note the artifact naming differs from Phase A: 26.1's is `minecraft-patched-<neo_version>-sources.jar`,
1.21.1's was `neoforge-<neo_version>-sources.jar`.

---

## B.0 Build

`gradle.properties` for this hop:

```
java_version=25
mc_version=26.1.2
minecraft_version_range=[26.1.2]
neo_version=26.1.2.109
jei_version=29.40.0.101          # artifact prefix becomes jei-26.1.2-…
curios_version=15.0.0+26.1.2
```

`neoforge.mods.toml` needed no edit — it already interpolates `${mc_version}`/`${neo_version}` from
`gradle.properties` through `processResources`. `build.gradle` needed none either: the toolchain
reads `java_version`, and there was no Parchment block to remove. Temurin 25.0.4 is installed, so
foojay provisions nothing.

**`createMinecraftArtifacts` fails before `compileJava` ever runs** when
`validateAccessTransformers = true` and any AT line has a dead target — so the AT sweep (B.9's
second half) is not optional cleanup at the end of the hop, it is the **first** thing Phase B
forces you to do. See below.

---

## B.9 (forced early) Access transformers

21 of the 132 AT lines had dead targets on 26.1.2. The validator prints the file and line for each,
which makes this mechanical. Three outcomes, all confirmed in the sources jar:

**Moved class** — retarget the package:

| Was | Now |
|---|---|
| `net.minecraft.client.particle.ParticleEngine` `spriteSets` | `net.minecraft.client.particle.ParticleResources` `spriteSets` |
| `net.minecraft.world.entity.projectile.AbstractArrow` (`*`, `shouldFall()Z`, `startFalling()V`) | `net.minecraft.world.entity.projectile.arrow.AbstractArrow` |

**Changed signature** — retarget the descriptor:

| Member | 1.21.1 | 26.1.2 |
|---|---|---|
| `LevelRenderer#renderHitOutline` | `(PoseStack, VertexConsumer, Entity, DDD, BlockPos, BlockState)V` | `(PoseStack, VertexConsumer, DDD, BlockOutlineRenderState, int, float)V` — and `BlockOutlineRenderState` lives in `net.minecraft.client.renderer.state.level`, not `…renderer.blockentity` |
| `Projectile#checkLeftOwner` | `()Z` | `()V` |
| `Biome#getTemperature` | `(BlockPos)F` | `(BlockPos, int seaLevel)F` |
| `BlockHitResult#<init>` | `(Z, Vec3, Direction, BlockPos, Z)V` | `(Z, Vec3, Direction, BlockPos, Z, Z)V` — gained `worldBorderHit` |
| `AbstractCookingRecipe` `result` field | field on `AbstractCookingRecipe` | `protected ItemStackTemplate result()` on **`SingleItemRecipe`**; AT the accessor, and every read becomes a call |

**Gone outright** — the line is deleted (left in place as a `# REMOVED 26.1.2:` comment so the next
hop can see what was dropped and why):

- `ChatComponent#addMessage(…)` both overloads — the one surviving `addMessage` is private with a
  new `GuiMessageSource` parameter; the public entry points are now `addClientSystemMessage`,
  `addServerSystemMessage`, `addPlayerMessage`.
- `LevelRenderer` `capturedFrustum`, `frustumPos`, and both `addParticleInternal` overloads.
- `BlockEntityWithoutLevelRenderer` — **the whole class is deleted** (`tridentModel`,
  `entityModelSet`). Item-with-BE rendering is `RegisterSpecialModelRendererEvent` now (B.7b).
- `RenderStateShard` and `RenderStateShard$LineStateShard` — deleted; a render type is built from
  `net.minecraft.client.renderer.rendertype.RenderSetup` over a `RenderPipeline` (B.7g).
- `LivingEntity#onEffectRemoved(MobEffectInstance)` and the `ServerPlayer` override — the method no
  longer exists; NeoForge routes it through `EventHooks.onEffectRemoved`, called from
  `LivingEntity` at `:987`/`:1083`. `onEffectAdded`/`onEffectUpdated` are unchanged.

`net.minecraft.client.renderer.rendertype` is the new home of `RenderType`, `RenderTypes`,
`RenderSetup`, `LayeringTransform`, `OutputTarget`, `TextureTransform`.

---

## B.1 Mechanical renames

All confirmed in `minecraft-patched-26.1.2.109-sources.jar`. 2445 → 1537 errors across B.1 and its
stragglers.

| 1.21.1 | 26.1.2 | Note |
|---|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` | Every static factory kept (`fromNamespaceAndPath`, `parse`, `withDefaultNamespace`, `tryParse`) — 481 sites in 95 files were a pure rename |
| `FriendlyByteBuf#readResourceLocation/writeResourceLocation` | `readIdentifier/writeIdentifier` | |
| `ResourceKey#location()` | `ResourceKey#identifier()` | **`TagKey#location()` is unchanged** — only `ResourceKey`'s was renamed; a blind `.location()` → `.identifier()` sweep breaks tag code |
| `net.minecraft.Util` | `net.minecraft.util.Util` | |
| `net.minecraft.advancements.critereon.*` | `net.minecraft.advancements.criterion.*` | spelling fix |
| `world.entity.projectile.AbstractArrow` | `world.entity.projectile.arrow.AbstractArrow` | |
| `world.entity.projectile.ThrowableItemProjectile` | `world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile` | |
| `world.entity.vehicle.AbstractMinecart` | `world.entity.vehicle.minecart.AbstractMinecart` | |
| `world.entity.vehicle.Boat` / `ChestBoat` | `world.entity.vehicle.boat.Boat` / `ChestBoat` | |
| `client.model.BoatModel` | `client.model.object.boat.BoatModel` | |
| `neoforge.client.model.data.ModelData/ModelProperty` | `neoforge.model.data.ModelData/ModelProperty` | |
| `Level#isClientSide` (field) | `Level#isClientSide()` | **Receivers other than `level` are easy to miss** — the first sweep missed five `tile.world().isClientSide` reads |
| `Level#random` (public field) | `Level#getRandom()` | the field is `protected` now |
| `FMLEnvironment.dist` / `.production` | `FMLEnvironment.getDist()` / `isProduction()` | |
| `RegistryAccess#registryOrThrow` / `Registry#getHolderOrThrow` | `lookupOrThrow` / `getOrThrow` | |

### `InteractionResult` (collapsed in 1.21.2)

`InteractionResultHolder<ItemStack>` and `ItemInteractionResult` are both gone; everything returns
`InteractionResult`.

| Was | Now |
|---|---|
| `InteractionResultHolder.pass/fail/consume/success(stack)` | `InteractionResult.PASS/FAIL/CONSUME/SUCCESS` |
| `InteractionResult.sidedSuccess(level.isClientSide())` | `InteractionResult.SUCCESS` |
| `ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION` | `InteractionResult.TRY_WITH_EMPTY_HAND` |

The sealed interface's constants are `SUCCESS` (client swing), `SUCCESS_SERVER` (server swing),
`CONSUME` (no swing) — all three `InteractionResult.Success` — plus `FAIL`, `PASS` and
`TRY_WITH_EMPTY_HAND`.

Every CoFHCore site returned the held stack unchanged, so none needed
`InteractionResult.SUCCESS.heldItemTransformedTo(stack)` — ThermalCore's bucket-like items will.
`useItemOn`'s parameter list is unchanged; only its return type moved.

---

## B.2 Registration

### Blocks and items need their id before construction

`BlockBehaviour.Properties` and `Item.Properties` both require `setId(ResourceKey)` before the
`Block`/`Item` constructor runs. The registration therefore has to see its own id, and
`DeferredRegisterCoFH` gained the overload NeoForge's own `DeferredRegister` already has:

```java
public synchronized <I extends T> DeferredHolder<T, I> register(String name, Function<Identifier, ? extends I> func)
```

Call shape (`CoreBlocks`):

```java
BLOCKS.register(ID_GLOSSED_MAGMA, id -> new GlossedMagmaBlock(ofFullCopy(Blocks.MAGMA_BLOCK)
        .lightLevel(lightValue(6)).setId(ResourceKey.create(Registries.BLOCK, id))));
```

Items are the same with `Registries.ITEM`. CoFHCore registers only five blocks and no plain items
this way; the overload exists for ThermalCore/TD/TE (B.10), which register hundreds through
`RegistrationHelper`.

### Block entity and entity types

| 1.21.1 | 26.1.2 |
|---|---|
| `BlockEntityType.Builder.of(Ctor::new, blocks…).build(null)` | `new BlockEntityType<>(Ctor::new, blocks…)` — the builder is gone; NeoForge makes the constructor public |
| `EntityType.Builder#build(String)` | `build(ResourceKey<EntityType<?>>)` — so entity registration takes the `Function<Identifier, …>` overload too: `id -> …build(ResourceKey.create(Registries.ENTITY_TYPE, id))` |

### `DirectionProperty` is deleted (1.21.2)

Vanilla's `FACING`/`HORIZONTAL_FACING` are plain `EnumProperty<Direction>` now:

```java
EnumProperty.create("facing", Direction.class, Direction.Plane.HORIZONTAL)
```

### Key mapping categories

A category is `KeyMapping.Category`, a record over an `Identifier`, not a free string.

- **Build it with `new KeyMapping.Category(id)`**, not the deprecated
  `KeyMapping.Category.register(Identifier)`. That one registers into a static set and **throws on a
  duplicate id**. The port plan suggested `register`; the constructor is what works.
- Register it with `RegisterKeyMappingsEvent#registerCategory(category)`, which is what puts it in
  the controls screen's sort order.
- Its label is `id.toLanguageKey("key.category")`: CoFH's category `cofh_core:cofh` reads
  `key.category.cofh_core.cofh`, so en_us.json needs that key. The old category was a bare `"CoFH"`
  string with no lang entry.

### Deferred to later categories

`CoreRecipeSerializers` (the `RecipeSerializer` record and the `RecipeType` `minecraft:` prefix) is
B.6, and `CoreShaders` is B.7g. Both still fail to compile after B.2.

---

## B.3 Persistence

**Decision (Joel, 2026-09-22): bridge at the edge, don't convert the chain.** CoFH's controls,
storages, filters and augments all `read`/`write` a `CompoundTag`, and they keep doing so. Only
the vanilla hooks change. This keeps the upstream diff small and the on-disk layout identical to
1.21.1's.

### Block entities: `BlockEntityCoFH` owns the bridge

```java
@SuppressWarnings ("deprecation")
@Override
protected final void loadAdditional(ValueInput input) {

    super.loadAdditional(input);
    loadAdditional(input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new), input.lookup());
}

@Override
protected final void saveAdditional(ValueOutput output) {

    super.saveAdditional(output);
    CompoundTag nbt = new CompoundTag();
    saveAdditional(nbt, level != null ? level.registryAccess() : RegistryAccess.EMPTY);
    output.store(nbt);
}
```

- **Subclasses keep their 1.21.1 overrides unchanged**: `loadAdditional(CompoundTag,
  HolderLookup.Provider)` and `saveAdditional(CompoundTag, HolderLookup.Provider)` are now CoFH
  overloads. ThermalCore's 26 and TD's block entities need no edits if they extend
  `BlockEntityCoFH`; any that extend `BlockEntity` directly need the same bridge.
- `ValueOutputExtension#store(CompoundTag)` (NeoForge) writes a tag's entries at the output's
  root. Reading the whole root back needs `MapCodec.assumeMapUnsafe(CompoundTag.CODEC)`, which is
  deprecated; NeoForge's own `ValueInputExtension#keySet()` uses it the same way.
- `ValueOutput` has no `lookup()`. The registries for the save side come from
  `level.registryAccess()`, falling back to `RegistryAccess.EMPTY`, which is exactly what
  `BlockEntity#saveAdditional` itself does (`BlockEntity.java:117`).
- `getUpdateTag(HolderLookup.Provider)` / `saveWithoutMetadata(HolderLookup.Provider)` are
  unchanged. `onDataPacket`/`handleUpdateTag` default to `loadWithComponents(ValueInput)`, which
  reaches the bridge.

### `onRemove` → `BlockEntity#preRemoveSideEffects(BlockPos, BlockState oldState)`

`LevelChunk#setBlockState` (`:311`) calls it only when the block changes, on the server, just
before removing the block entity. That's the same guard `EntityBlockCoFH#onRemove` had
(`state.getBlock() != newState.getBlock()`). By then the chunk already holds the new state, so
`BlockEntityCoFH` passes `level.getBlockState(pos)` as `newState`, and
**`ITileCallback#onReplaced` keeps its signature**. The base implementation only drops `Container`
contents; no CoFH block entity is a `Container`, so it isn't called. `EntityBlockCoFH#onRemove` is
gone.

### Entities: converted natively (no common CoFH base)

| 1.21.1 | 26.1.2 |
|---|---|
| `readAdditionalSaveData(CompoundTag)` / `addAdditionalSaveData(CompoundTag)` | `(ValueInput input)` / `(ValueOutput output)` |
| `tag.getInt(k)` etc. | `input.getIntOr(k, 0)`, `getFloatOr`, … |
| `putUUID` / `hasUUID` / `getUUID` | `output.storeNullable(k, UUIDUtil.CODEC, uuid)` / `input.read(k, UUIDUtil.CODEC)`. Same int-array format |
| codec + `registryAccess().createSerializationContext(NbtOps.INSTANCE)` | `input.read(k, CODEC)` / `output.store(k, CODEC, v)`. The ValueIO already carries registry ops |
| `stack.save(registries)` / `ItemStack.parseOptional(registries, tag)` | `output.store(k, ItemStack.OPTIONAL_CODEC, stack)` / `input.read(k, ItemStack.OPTIONAL_CODEC)` |

### `SavedData`

`new SavedData.Factory<>(ctor, (nbt, registries) -> …)` and `computeIfAbsent(factory, "name")`
became `SavedDataType<T>(Identifier id, Supplier<T>, Codec<T>[, DataFixTypes])` and
`computeIfAbsent(TYPE)`. `SavedData#save` is no longer abstract. Minimal bridge:
`CompoundTag.CODEC.xmap(T::new, data -> data.save(new CompoundTag()))`.

**The file path comes from the id**: `id.withSuffix(".dat")` resolved under `data/`
(`SavedDataStorage#getDataFile`). `cofh:friends` is now `data/cofh/friends.dat`, where 1.21.1
wrote `data/cofh:friends.dat`, so **friend lists from a 1.21.1 world don't carry over**.
`ServerPlayer#serverLevel()` is gone; `ServerPlayer#level()` returns `ServerLevel`.

### `CompoundTag` / `ListTag` getters return `Optional`

`getInt(k)` → `getIntOr(k, 0)` (likewise `Byte`/`Short`/`Long`/`Float`/`Double`/`Boolean`/`String`
with the old absent-key default), `getCompound(k)` → `getCompoundOrEmpty(k)`,
`getList(k, TAG_X)` → `getListOrEmpty(k)` (no element-type argument), `ListTag#getCompound(i)` →
`getCompoundOrEmpty(i)`, `getByteArray(k)` → `Optional<byte[]>` (`.orElse(new byte[0])`),
`getAllKeys()` → `keySet()`, and `contains(k, TAG_X)` → `contains(k)`. The UUID helpers are gone.

**A leftover `Optional` getter can still compile** wherever an `Object` is accepted: in a ternary
or string concatenation it yields `"Optional[…]"` at runtime. javac reports the ternary case as
"bad type in conditional expression", not "incompatible types".

### Stacks ↔ `CompoundTag`

`ItemStack`/`FluidStack` `parseOptional(registries, tag)`, `save(registries[, prefix])` and
`saveOptional(registries)` are gone; only the codecs remain. CoFH wraps them as
`ItemHelper`/`FluidHelper` `parseOptional(provider, tag)` / `saveOptional(provider, stack)`:
`OPTIONAL_CODEC` through `provider.createSerializationContext(NbtOps.INSTANCE)`. Extra keys in
the tag (a slot index) are ignored on parse, as before, and an empty stack round-trips as `{}`.

### `INBTSerializable` is deleted

NeoForge 26.1.2 has only `ValueIOSerializable` (`serialize(ValueOutput)` /
`deserialize(ValueInput)`). Every `serializeNBT`/`deserializeNBT` caller in the family is CoFH or
TD code, so the interface is simply dropped from `IFilter`, `EnergyStorageCoFH` and `XpStorage`,
and the methods stay as plain CoFH API. TD's `Grid`, `GridNode`, `IAttachment`, `EnergyGridStorage`
and `FluidGridStorage` take the same treatment in B.10.

### authlib 7: `GameProfile` is a record

`getId()`/`getName()` → `id()`/`name()`, and **`equals` now compares `properties()` too**. A
logged-in player's profile carries textures while a stored or command-built one doesn't, so
`set.contains(player.getGameProfile())` silently fails. Compare
`new GameProfile(profile.id(), profile.name())` instead (`SocialUtils`).
`GameProfileArgument.getGameProfiles` returns `Collection<NameAndId>` (`record NameAndId(UUID id,
String name)`).

### B.1 stragglers found here

`Registry#get(Identifier)` returns `Optional<Holder.Reference<T>>`; the plain lookup is
`getValue(Identifier)` (8 sites).

---

## B.4 Transfer API

**Same principle as B.3: bridge at the edge.** NeoForge 26.1.2 keeps `IItemHandler`,
`IFluidHandler` and `IEnergyStorage`, deprecated for removal, but **capabilities speak only
`ResourceHandler<ItemResource>` / `ResourceHandler<FluidResource>` / `EnergyHandler`**. NeoForge
adapts only new → old (`IItemHandler.of`, `IFluidHandler.of`, `IEnergyStorage.of`; the adapters
are the package-private `*ResourceHandlerAdapter` / `EnergyHandlerAdapter`). There is no old →
new adapter, and a general one can't honour transactions. So:

- **CoFH internals keep the legacy interfaces.** Machines, ThermalCore and ThermalExpansion keep
  calling `insertItem`/`fill`/`receiveEnergy` on the storages. That produces removal warnings,
  not errors.
- **Exposing (CoFH → other mods):** CoFH's handlers *also* implement the new interfaces. The two
  sets of method names don't clash (`getSlots`/`insertItem` vs `size`/`insert`).
- **Consuming (other mods → CoFH):** query sites wrap the capability result with
  `IItemHandler.of` / `IFluidHandler.of` / `IEnergyStorage.of`.

### Transactions: one journal per storage, callbacks on root commit

`SnapshotJournal<T>`: `createSnapshot()`, `revertToSnapshot(T)`, `onRootCommit(T original)`, and
`updateSnapshots(TransactionContext)` before the first mutation at each depth. The pattern is
mutate immediately, revert on abort, the same as `StacksResourceHandler`.

- **The journal lives on the storage** (`ItemStorageCoFH`, `FluidStorageCoFH`, `EnergyStorageCoFH`,
  exposed as `updateSnapshots(tx)`), not on the handler. `ManagedItemInv` builds five handlers over
  the same slots, and `Transaction#close` walks `journalsToClose` **in registration order**. Two
  handler-level journals snapshotting one slot would revert out of order.
- Snapshots copy the stack (`item.copy()`, `fluid.copy()`), because the legacy paths mutate in
  place (`setCount`, `grow`). Revert assigns the field directly, bypassing setters and subclass
  side effects.
- **Change callbacks wait for root commit.** Each handler keeps a `BitSet` of touched indices in
  its own journal (reverted on abort) and calls `onInventoryChange`/`onTankChange` from
  `onRootCommit`. This matters: `MachineBlockEntity#onInventoryChanged` re-validates inputs and
  can `processOff()`, so a pipe that merely *simulates* pulling an input (nested transaction,
  aborted) would stop an active machine if the callback fired immediately.
- New-API calls go to the **storage directly**, not through the handler's legacy
  `insertItem`/`fill`, which fire their callbacks inline. Handler rules are protected hooks:
  `canInsert(index)`/`canExtract(index)` on `SimpleItemHandler`/`SimpleFluidHandler`, overridden by
  `ManagedItemHandler`/`ManagedFluidHandler` (inputs accept, outputs give, `restrict()` blocks input
  extraction) and by `IOItemHandler` (its allow suppliers). The legacy methods are unchanged.
- Fluid handlers were never per-tank in the legacy API (`fill` walks tanks, first match wins).
  The new API is per index, so each index maps to its tank.

### Surface for B.10

- `SimpleItemInv`/`IOItemInv`/`ManagedItemInv#getHandler` now return `SimpleItemHandler`, and
  `SimpleTankInv`/`ManagedTankInv#getHandler` return `SimpleFluidHandler`. Both still are the
  legacy types, so existing callers compile, and registration needs no cast.
- `FluidStorageCoFH` is itself a one-index `ResourceHandler<FluidResource>` (the fluid cell
  exposes it directly), and `EnergyStorageCoFH` is an `EnergyHandler`.
- `FluidHandlerRestrictionWrapper`/`EnergyHandlerRestrictionWrapper` take
  `<T extends IFluidHandler & ResourceHandler<FluidResource>>` (or the energy pair), so the energy
  and fluid cells' call sites compile unchanged and the wrapper holds a typed view of both.
- Registration (ThermalCore/TD/TE) becomes
  `event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> …)`, with
  `Capabilities.Fluid.BLOCK` and `Capabilities.Energy.BLOCK` likewise. `AugmentableBlockEntity`'s
  cached `IItemHandler itemCap` etc. need their field types narrowed to match.

### Query sites

| 1.21.1 | 26.1.2 |
|---|---|
| `Capabilities.ItemHandler/FluidHandler/EnergyStorage.BLOCK` | `Capabilities.Item/Fluid/Energy.BLOCK`, then `IItemHandler.of(h)` / `IFluidHandler.of(h)` / `IEnergyStorage.of(h)` |
| `stack.getCapability(Capabilities.FluidHandler.ITEM)` | `FluidUtil.getFluidHandler(stack).orElse(null)`. This is the deprecated `neoforge.fluids.FluidUtil`, which returns an `IFluidHandlerItem` whose `getContainer()` still reports a changed item (bucket → water bucket) |
| `stack.getCapability(Capabilities.EnergyStorage.ITEM)` | `ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM)`. **`forStack` throws on an empty stack**, and it mutates the stack in place but can never change its `Item` |
| charging a player's items | `ItemAccess.forPlayerSlot(player, i)` over `0 … inventory.getContainerSize()`, then `insert` inside `Transaction.openRoot()` … `commit()` |
| `FluidStack#isFluidEqual(other)` / `areFluidStackTagsEqual` | `FluidStack.isSameFluidSameComponents(a, b)` (it was already the deprecated alias) |
| `Slot.slot` | `getContainerSlot()` (the field is private again since B.0) |

Other 26.1 changes met here: `Inventory#items`/`armor`/`offhand` are private, with a single index
space (0–35 main, 36–39 armor, 40 offhand, 41 body armor). `ChunkAccess#setUnsaved(true)` is
`markUnsaved()`. **New-API `insert`/`extract` reject negative amounts** (`TransferPreconditions`).
`EnergyChargeMobEffect` had passed its negative drain amount straight through, which on the
legacy API *added* energy, so it now negates at the call site.

### Not covered (parity with 1.21.1)

No item capabilities (energy/fluid items, satchels) are registered on 1.21.1 either;
`EnergyCellBlockItem`'s registration is commented out. `EnergyContainerItemWrapper`,
`FluidContainerItemWrapper` and `InventoryContainerItemWrapper` still take an `ItemStack`, and
exposing CoFH items to other mods would need `ItemAccess`-based handlers. That's a feature gap,
not a regression.

**Owed verification**: nothing in B.4 has run. Once 26.1.2 compiles and boots, move items/fluids/energy
in and out of a machine with a pipe mod (or a GameTest), including an aborted simulation.

---

## B.5 Items, tools, armour

### Tools: `ToolMaterial` + `Item.Properties`

`SwordItem`, `DiggerItem`, `PickaxeItem`, `TieredItem` and `Tier` are gone.
`ToolMaterial(TagKey<Block> incorrectBlocksForDrops, int durability, float speed, float attackDamageBonus,
int enchantmentValue, TagKey<Item> repairItems)` is a record, and a tool is a plain `Item` built with
`Item.Properties#sword/pickaxe/axe/hoe/shovel(material, damage, speed)` or
`tool(material, TagKey<Block> minesEfficiently, damage, speed, float disableBlockingSeconds)`.
**`AxeItem`, `HoeItem` and `ShovelItem` survive** as `Item` subclasses taking
`(ToolMaterial, float, float, Properties)`, and they still carry stripping, tilling and pathing.

- **The tool properties overwrite durability** from the material (`applyCommonProperties`), so
  `builder.durability(n).pickaxe(...)` loses `n`. CoFH's hammer, excavator and sickle scale the
  *material* instead: `new ToolMaterial(…, material.durability() * 4, …)`.
- `canDisableShield` is gone. Shield-disabling is `Weapon(itemDamagePerAttack, disableBlockingSeconds)`,
  which `tool(...)` sets. The axe uses 5.0 s, and CoFH's hammer copies it.
- `Item#getDestroySpeed` is gone; mining speed is only the `TOOL` component's rules. To add a
  special case (the sickle's cobweb, speed 15), replace the component after `tool(...)` with
  `new Tool(List.of(Tool.Rule.overrideSpeed(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
  Tool.Rule.deniesDrops(…incorrect…), Tool.Rule.minesAndDrops(…tag…, speed)), 1.0F, 1, true)`. Resolve
  tags with `BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)`, as vanilla does.
- `ItemTierCoFH` is no longer a `Tier`. It holds a `ToolMaterial` (`getMaterial()`) plus CoFH's numeric
  `getLevel()`, and takes a repair **tag** rather than an `Ingredient` supplier.

### Armour: the equipment `ArmorMaterial` record

`ArmorItem`, `AnimalArmorItem` and the `ArmorMaterial` *registry* are gone.
`net.minecraft.world.item.equipment.ArmorMaterial(int durability, Map<ArmorType, Integer> defense,
int enchantmentValue, Holder<SoundEvent> equipSound, float toughness, float knockbackResistance,
TagKey<Item> repairIngredient, ResourceKey<EquipmentAsset> assetId)` is a plain record.
`Item.Properties#humanoidArmor(material, ArmorType)` sets durability, attributes, enchantability, repair
and the `EQUIPPABLE` component. `horseArmor(material)` and `wolfArmor(material)` do the same for animals.

- `ArmorItem.Type` → `ArmorType` (`HELMET, CHESTPLATE, LEGGINGS, BOOTS, BODY`, with `getSlot()` and
  `getDurability(int)`).
- `ArmorItemCoFH` keeps a `getType()` returning the `ArmorType`, because ThermalCore's armour calls
  `getType().getSlot()` throughout.
- `ArmorMaterialCoFH.create(durability, int[] defense, enchantability, equipSound, toughness, kbResist,
  TagKey<Item> repairItems, ResourceKey<EquipmentAsset> assetId)`. The defense array is in `ArmorType`
  order (helmet first), as upstream's was in `ArmorItem.Type` order.
- **B.8/B.10**: each material needs `assets/<ns>/equipment/<name>.json`, and its textures move to
  `textures/entity/equipment/humanoid[_leggings]/`. The repair ingredient must be an **item tag**.
- Dyeability is the `minecraft:dyeable` item tag plus the `DYED_COLOR` component, not a class, so the
  `Dyeable*` classes are now the same as their parents.

### `IItemExtension` / `Item` signatures

| 1.21.1 | 26.1.2 |
|---|---|
| `getCreatorModId(ItemStack)` | `getCreatorModId(HolderLookup.Provider registries, ItemStack)` |
| `getBurnTime(ItemStack, RecipeType<?>)` | `getBurnTime(ItemStack, @Nullable RecipeType<?>, FuelValues)` |
| `appendHoverText(ItemStack, TooltipContext, List<Component>, TooltipFlag)` | `appendHoverText(ItemStack, TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)` |
| `isEnchantable(ItemStack)`, `getEnchantmentValue(ItemStack)` | **gone**. `DataComponents.ENCHANTABLE`, set with `Properties#enchantable(n)`; `EnchantmentHelper` reads only the component. A per-stack value means `stack.set(ENCHANTABLE, …)` |
| `releaseUsing(…)` returns `void` | returns `boolean` (whether the release did something) |
| `getUseAnimation` → `UseAnim` | `ItemUseAnimation` |
| `getDescriptionId()` overridable, `getOrCreateDescriptionId()` | **final**. Use `Properties#useItemDescriptionPrefix()` / `useBlockDescriptionPrefix()` / `overrideDescription(id)` |
| `ItemCooldowns#addCooldown(Item, int)` | `addCooldown(ItemStack, int)` (cooldown group from the stack) |
| `LivingEntity.getSlotForHand(hand)` | `hand.asEquipmentSlot()` |
| `ExperienceOrb.value` | `getValue()` / `setValue(int)` |
| `RailShape#isAscending()` | `isSlope()` |
| `SignItem(Properties, Block, Block)` | `SignItem(Block sign, Block wallSign, Properties)` |
| `ChargedProjectiles.of(ItemStack)` / `getItems()` | `ofNonEmpty(List<ItemStack>)` (or `of(ItemStackTemplate)`) / `itemCopies()` |
| `Screen.hasShiftDown()` | `Minecraft.getInstance().hasShiftDown()` |
| NeoForge `DeferredSpawnEggItem(Supplier<EntityType>, bg, hl, props)` | vanilla `SpawnEggItem(props.spawnEgg(type))`. Entity types now register before items, so the supplier resolves at construction. The egg's tint colours are no longer vanilla's concern: eggs have individual textures since 1.21.5 (B.7) |

Enchantability had been set **after** construction (`setEnchantability(n)`, chained). Components are
fixed at construction, so CoFHCore's setters are removed and its container items pass
`builder.enchantable(5)`. B.10: ThermalCore's `setEnchantability` calls move into the properties, and its
augmentable items' **per-stack** scaling (`getEnchantmentValue` × augment modifier) must write the
`ENCHANTABLE` component when augments change.

### Entities, effects, events

| 1.21.1 | 26.1.2 |
|---|---|
| `Entity#hurt` → boolean, overridable | `final void hurt`. Override `hurtServer(ServerLevel, DamageSource, float)`, and call `hurtOrSimulate(src, amt)` when you need the result |
| `Entity#moveTo(...)` | `snapTo(...)` (same overloads) |
| `Entity#makeBoundingBox()` overridable | `final`. Override `makeBoundingBox(Vec3 position)` |
| `Entity#spawnAtLocation(stack[, y])` | `spawnAtLocation(ServerLevel, stack[, y])` |
| `Entity#causeFallDamage(float, …)` | `(double, float, DamageSource)` |
| `Entity#getCommandSenderWorld()` | `level()` |
| `entity.getType().is(TagKey)` | `entity.is(TagKey)` (`TypedInstance`) |
| `EntityType#create(Level)` | `create(Level, EntitySpawnReason)` |
| `walkDist`, `updateInWaterStateAndDoFluidPushing`, `checkInsideBlocks` | gone. Use `updateFluidInteraction()` and `applyEffectsFromBlocks()` |
| `Boat`/`ChestBoat(type, level)` | `(type, level, Supplier<Item> dropItem)`. Drops and pick result come from the supplier |
| `VehicleEntity#destroy(DamageSource)` | `destroy(ServerLevel, DamageSource)` |
| `AbstractMinecart#getMinecartType()`/`Type` | gone (`isRideable()`/`isFurnace()`); `activateMinecart(ServerLevel, x, y, z, powered)` |
| `ThrowableItemProjectile(type, x, y, z, level)` | takes a trailing `ItemStack` (or `super(type, level)` + `setPos`) |
| `PrimedTnt.owner` | `@Nullable EntityReference<LivingEntity>` via `EntityReference.of(entity)` |
| `GameRules` (`world.level`), `getBoolean(RULE_DOENTITYDROPS)` | `world.level.gamerules.GameRules`, `get(GameRules.ENTITY_DROPS)` |
| `LivingEntity#getArmorSlots/getAllSlots` | loop over `EquipmentSlot`s with `getItemBySlot` |
| `Inventory#items`/`armor`/`selected` | `getNonEquipmentItems()` (0–35) / `getItemBySlot(slot)` / `getSelectedSlot()` |
| `AbstractArrow#getBaseDamage()` | gone. Read the `baseDamage` field (AT) |
| `MobEffect#applyEffectTick(LivingEntity, int)` | `(ServerLevel, LivingEntity, int)`, **server only**, so client particles must be sent with `ServerLevel#sendParticles` |
| `MobEffect#applyInstantenousEffect(src, …)` | gains a leading `ServerLevel` |
| NeoForge `EffectCure` / `fillEffectCures` | **removed**. `removeAllEffects` (milk) clears everything |
| `LivingEntity#onEffectRemoved(instance)` | gone. Use `removeEffect(holder)`, which posts `MobEffectEvent.Remove` and calls `onEffectsRemoved` |
| `EntityTeleportEvent.ChorusFruit` | `EntityTeleportEvent.ItemConsumption` |
| `BlockEvent.BreakEvent` | `event.level.block.BreakBlockEvent` |
| `LivingShieldBlockEvent` cancellable | not cancellable. Check `getBlocked()` |
| `DiggerItem` / `*_DIG` abilities | gone. "Is a mining tool" means `stack.has(DataComponents.TOOL)` |
| `BlockTags.TALL_FLOWERS`, `MossBlock` | gone / `BonemealableFeaturePlacerBlock` |

### Blocks and fluids

| 1.21.1 | 26.1.2 |
|---|---|
| `neoforge.common.util.TriState` | `net.minecraft.util.TriState` (`canSustainPlant` unchanged) |
| `BushBlock` as "any plant" | `VegetationBlock`. `BushBlock` is now just the bush plant; `DeadBushBlock` → `DryVegetationBlock`, `FungusBlock` → `NetherFungusBlock` |
| `neighborChanged(…, BlockPos fromPos, boolean)` | `(…, @Nullable Orientation, boolean)`. **The neighbour's position is gone**, and `Orientation` is null outside experimental redstone |
| `getAnalogOutputSignal(state, level, pos)` | `(…, Direction)` |
| `getCloneItemStack(LevelReader, pos, state)` | `(…, boolean includeData)` |
| `Block#getDescriptionId()` | `final`. Override `getName()` |
| `updateShape(state, dir, nState, LevelAccessor, pos, nPos)` | `updateShape(state, LevelReader, ScheduledTickAccess, pos, dir, nPos, nState, RandomSource)` |
| `entityInside(state, level, pos, entity)` | `(…, InsideBlockEffectApplier, boolean isPrecise)` |
| `fallOn(…, float)` / `updateEntityAfterFallOn` | `fallOn(…, double)` / `updateEntityMovementAfterFallOn` |
| `onCaughtFire` → void, `wasExploded(Level, …)` | returns `boolean` (TNT only removes itself when `true`), `wasExploded(ServerLevel, …)` |
| `getMaxBuildHeight()` | `getMaxY()` (inclusive, one lower) |
| `IBaseRailBlockExtension#getRailMaxSpeed` | removed, no hook (`MinecartBehavior`) |
| `Equipable` | gone. Use the `EQUIPPABLE` component on the item |
| `FoodProperties.effects()` / `PossibleEffect` | gone. Effects live on the `Consumable` component |
| `ParticleTypes.INSTANT_EFFECT` | `SpellParticleOption.create(type, color, power)` |
| `PotionContents(potion, color, effects)` | gains `Optional<String> customName`. `Potion.getName(…)` → `PotionContents#getName(prefix)` |
| `DataComponents.HIDE_ADDITIONAL_TOOLTIP` | `TOOLTIP_DISPLAY` |

### Commands, network, menus, util

| 1.21.1 | 26.1.2 |
|---|---|
| `CommandSourceStack#hasPermission(int)` | `source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(n)))` (`server.permissions`). CoFH wraps it as `CoFHCommand.hasPermission(source, level)` |
| `Player#hasPermissions(4)` | `permissions().hasPermission(Permissions.COMMANDS_OWNER)` |
| `PacketDistributor.sendToServer` | `client.network.ClientPacketDistributor.sendToServer` |
| `ServerPlayer#serverLevel()` | `level()` |
| `FriendlyByteBuf#writeVec3/readVec3` | `Vec3.STREAM_CODEC` |
| `ClickType` | `ContainerInput` |
| `Style#withFont(Identifier)` | `withFont(new FontDescription.Resource(id))` |
| `Component.Serializer.toJson/fromJsonLenient` | `ComponentSerialization.CODEC` with `createSerializationContext(JsonOps.INSTANCE)` |
| `WeightedEntry.IntrusiveBase` | gone. `WeightedRandom` takes a weight `ToIntFunction` |
| `Registry#getOrCreateTag` / `getHolder(id)` | `getOrThrow(tag)`/`get(tag)` / `get(id)` → `Optional<Holder.Reference>` |
| `BlockHitResult(…)` | gains a trailing `worldBorderHit` |
| `Item#getCraftingRemainingItem(stack)` | NeoForge `getCraftingRemainder(ItemInstance)` → nullable `ItemStackTemplate` (`.create()`) |
| `BLOCK_ENTITY_DATA` as `CustomData` | `TypedEntityData<BlockEntityType<?>>`: `TypedEntityData.of(type, tag)` / `copyTagWithoutId()`. `BlockItem` applies it only when the type matches. CoFH's `ItemHelper.setBlockEntityData` infers the type (the stack's, else the first type valid for the item's block) |
| JEI `IIngredientSubtypeInterpreter#apply` → String | `ISubtypeInterpreter<T>#getSubtypeData(T, UidContext)` → Object (`null` = none); `IRecipeManagerPlugin` takes `IRecipeType<T>` |

## B.6 Recipes, loot functions, datagen

### `Recipe<T extends RecipeInput>` (1.21.2 "Recipe Changes", 26.1 serializer records)

| 1.21.1 | 26.1.2 |
|---|---|
| `assemble(input, HolderLookup.Provider)` | `assemble(input)` |
| `getResultItem(registries)`, `getIngredients()`, `canCraftInDimensions(w, h)`, `getToastSymbol()` | **gone**. There is no result accessor; `display()` (`List<RecipeDisplay>`, default empty) is what the recipe book and JEI see |
| — | **new abstract**: `showNotification()`, `group()`, `placementInfo()` (`PlacementInfo.NOT_PLACEABLE` for anything not placeable by the recipe book), `recipeBookCategory()` (`RecipeBookCategories.*`; only read while iterating `display()`, so any value works for a recipe with no display) |
| `getSerializer()` → `RecipeSerializer<?>` | `RecipeSerializer<? extends Recipe<T>>`; `getType()` → `RecipeType<? extends Recipe<T>>`. **B.10**: ThermalCore's overrides must narrow to these |
| `RecipeSerializer` interface with `codec()`/`streamCodec()` | `record RecipeSerializer<T>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec)`; register `() -> new RecipeSerializer<>(CODEC, STREAM_CODEC)` |
| `SimpleCraftingRecipeSerializer<>(Factory)` | gone. A `CustomRecipe` serializer is `new RecipeSerializer<>(MapCodec.unit(X::new), StreamCodec.unit(new X()))`, as vanilla's `RepairItemRecipe` does |
| `CustomRecipe(CraftingBookCategory)` | no-arg; `category()` is `MISC`, `isSpecial()` true, `placementInfo()` not placeable |
| `ShapedRecipe(group, category, pattern, ItemStack)` | `ShapedRecipe(Recipe.CommonInfo(showNotification), CraftingRecipe.CraftingBookInfo(category, group), ShapedRecipePattern, ItemStackTemplate)`; `MAP_CODEC` composes `CommonInfo.MAP_CODEC`, `CraftingBookInfo.MAP_CODEC`, `ShapedRecipePattern.MAP_CODEC` and `ItemStackTemplate.CODEC.fieldOf("result")`. `pattern` and `result` are public fields; `commonInfo`/`bookInfo` are protected on `NormalCraftingRecipe`, so a wrapper rebuilds them from `showNotification()`/`category()`/`group()` |
| `ItemStack` result | `ItemStackTemplate(Holder<Item>, count, DataComponentPatch)`: `create()` → validated `ItemStack`, `fromNonEmptyStack(stack)`, `apply(patch)` |

`ShapedPotionNBTRecipe` keeps wrapping a `ShapedRecipe` and delegates `placementInfo`/`display`/
`group`/`showNotification`; `assemble` starts from `wrappedRecipe.assemble(input)`. `SerializableRecipe`
(the shim base for Thermal's machine recipes) answers the new methods with `false`/`""`/`NOT_PLACEABLE`/
`CRAFTING_MISC`.

### `Ingredient`

- `Ingredient.EMPTY` is gone and **an `Ingredient` cannot be empty**: the constructor throws on an empty
  direct `HolderSet` (and on air). Vanilla models absence as `Optional<Ingredient>`. CoFH's lenient JSON
  parser needs a matches-nothing value, so `EmptyIngredient` (`cofh_core:empty`, an `ICustomIngredient`
  with no items and `SlotDisplay.Empty`) provides `EmptyIngredient.EMPTY`. Custom ingredients sync by
  type over NeoForge connections (`IngredientCodecs`), so an empty one survives the trip.
- `Ingredient.of(TagKey)` is gone: `Ingredient.of(HolderSet<Item>)`. At runtime a stack test is just
  `stack.is(tag)` (`SecureRecipe`); in datagen use `items.getOrThrow(tag)` (the `HolderGetter<Item>`
  field on `RecipeProvider`), which is what `ShapedRecipeBuilder.define(char, TagKey)` does.
- `Ingredient#getItems()` (`ItemStack[]`) → `items()` (`Stream<Holder<Item>>`, deprecated) and
  `getValues()` (`HolderSet`, throws for custom ingredients). `ICustomIngredient#getItems()` →
  `items()` returning holders; `display()` is the hook for showing stacks with a count:
  `new SlotDisplay.Composite(items().<SlotDisplay>map(h -> new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(h, count))).toList())`.
- `IngredientWithCount` stays as CoFH's counted ingredient (`cofh_core:with_count`). NeoForge's
  equivalent is `SizedIngredient(Ingredient, int)` with `NESTED_CODEC` (`{"ingredient": …, "count": n}`).
- `TagParser.parseTag(String)` → `TagParser.parseCompoundFully(String)`.

### Loot functions

| 1.21.1 | 26.1.2 |
|---|---|
| `LootItemFunctionType` registered in `BuiltInRegistries.LOOT_FUNCTION_TYPE`; `getType()` | the registry holds `MapCodec<? extends LootItemFunction>` directly; implement `MapCodec<X> codec()` |
| `LootContext#getParamOrNull(key)` | `getOptionalParameter(ContextKey<T>)`; `getParameter`/`hasParameter` |
| `CopyNameFunction.copyName(NameSource.BLOCK_ENTITY)` | `copyName(LootContext.BlockEntityTarget.BLOCK_ENTITY)` (a `LootContextArg`) |
| `CopyCustomDataFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)` | the block-entity `NbtProvider` has no public factory (only `forContextEntity`); build it from `ContextNbtProvider.INLINE_CODEC.parse(JavaOps.INSTANCE, "block_entity")`. Vanilla's own tile drops use `CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(...)` instead |

### Datagen

- `GatherDataEvent` is abstract; subscribe to **`GatherDataEvent.Client`** (all data, client and
  server, in one run — `clientData()` in `build.gradle`). `includeServer()`/`includeClient()`,
  `getExistingFileHelper()` and `ExistingFileHelper` are gone. Providers are added with
  `event.createProvider(Ctor::new)` (`(PackOutput)` or `(PackOutput, CompletableFuture<Provider>)`)
  and block+item tags with `event.createBlockAndItemTags(Block::new, Item::new)`, where the item
  provider's factory takes `(PackOutput, lookup, CompletableFuture<TagLookup<Block>>)`.
- `@EventBusSubscriber` has no `bus` parameter; the bus is inferred from the event type.
- Tag providers drop the trailing `ExistingFileHelper`: NeoForge `BlockTagsProvider(output, lookup, modId)`,
  `BlockTagCopyingItemTagProvider(output, lookup, blockTags, modId)` (has `copy(blockTag, itemTag)`;
  vanilla's `ItemTagsProvider` is gone), vanilla `FluidTagsProvider(output, lookup, modId)` and
  `DamageTypeTagsProvider(output, lookup, modId)`. `tag(key).add(T...)` and `addTags(...)` are unchanged.
- `RecipeProvider` is no longer a `DataProvider`. Constructor `(HolderLookup.Provider registries,
  RecipeOutput output)`, fields `registries`, `items` (`HolderGetter<Item>`), `output`; `buildRecipes()`
  takes no argument, `has(...)` is an instance method. A mod registers a `RecipeProvider.Runner`
  (`(PackOutput, CompletableFuture<Provider>)`, `createRecipeProvider(registries, output)`, `getName()`).
  `RecipeProviderCoFH` keeps its `RecipeOutput consumer` helper parameters, so **B.10**: TC/TE's
  providers pass `this.output` (or the argument) unchanged and gain a `Runner`. `IConditionBuilder`
  is gone (nothing used it); conditions go through `RecipeOutput#withConditions(...)`.
- Builders: `ShapedRecipeBuilder.shaped(HolderGetter<Item>, category, ItemLike[, count] | ItemStackTemplate)`,
  `ShapelessRecipeBuilder.shapeless(items, …)`, `SimpleCookingRecipeBuilder.smelting/blasting(ingredient,
  RecipeCategory, CookingBookCategory, result, xp, time)` (`smoking`/`campfireCooking` imply `FOOD`),
  `SingleItemRecipeBuilder.stonecutting(ingredient, category, result, count)`. `save(output, String id)`
  still exists but throws if the id equals the default one.
- NeoForge's `BlockStateProvider`/`ItemModelProvider` and `client.model.generators` are gone. Since
  every generated model is committed, `BlockStateProviderCoFH`, `ItemModelProviderCoFH` and the Core
  providers are deleted rather than rewritten on vanilla's `ModelProvider`. **B.10**: the same for
  `TCoreBlockStateProvider`/`TCoreItemModelProvider`, `TDynItemModelProvider`, `TExpBlockStateProvider`/
  `TExpItemModelProvider`.

## B.7 Client

Done as five parallel agents with exclusive file ownership (GUI; models + fluids + client setup;
entity renderers; particles; render types/shaders/post effects/events). All shapes below were read
from `minecraft-patched-26.1.2.109-sources.jar` (MC) or `neoforge-26.1.2.109-sources.jar` (NF).

### (a) GUI framework

| 1.21.1 | 26.1.2 |
|---|---|
| `GuiGraphics` | `net.minecraft.client.gui.GuiGraphicsExtractor` |
| `Screen#render(g, mx, my, pt)` | `extractRenderState(g, mx, my, float a)`; never override it on a container screen. `Screen#extractRenderStateWithTooltipAndSubtitles` (final) calls `extractBackground`, `extractRenderState`, `extractDeferredElements` |
| `AbstractContainerScreen#renderBg(g, pt, mx, my)` | `Screen#extractBackground(g, mx, my, a)` (public; `super` = blur + menu background). Untranslated pose |
| `renderLabels(g, mx, my)` | `protected extractLabels(g, xm, ym)`, called from `extractContents` with the pose already translated by `(leftPos, topPos)` |
| `renderTooltip(g, mx, my)` | `protected extractTooltip(g, mx, my)`, called last; override and call `super` to add more |
| `imageWidth`/`imageHeight` mutable | `protected final`; `AbstractContainerScreen(menu, inv, title, int imageWidth, int imageHeight)` |
| `hasClickedOutside(mx, my, xo, yo, button)` | `hasClickedOutside(double, double, int, int)`. NeoForge's `mouseClicked` treats a click on a slot as never "outside" |
| `mouseClicked(double, double, int)` | `mouseClicked(MouseButtonEvent event, boolean doubleClick)`; `MouseButtonEvent(double x, double y, MouseButtonInfo)` record: `button()`, `modifiers()`, `hasShiftDown()` (`InputWithModifiers`) |
| `mouseReleased(double, double, int)` / `keyPressed(int, int, int)` | `mouseReleased(MouseButtonEvent)` / `keyPressed(KeyEvent(int key, int scancode, int modifiers))`; `mouseScrolled` unchanged |
| `tick()` | final; `containerTick()` |
| `Screen.hasShiftDown()`/`hasControlDown()`/`hasAltDown()` (static) | `Minecraft.getInstance().hasShiftDown()` etc. |
| `g.pose()` → `PoseStack` | `Matrix3x2fStack`: `pushMatrix()`, `translate(x, y)`, `popMatrix()` |
| `g.drawString(font, …, x, y, color, shadow)` | `g.text(font, String\|FormattedCharSequence\|Component, x, y, argb, dropShadow)`. **`text()` skips the draw when `ARGB.alpha(color) == 0`** — 1.21.1's `Font` promoted alpha-less colours, 26.1 does not. `IGuiAccess.drawString` promotes them (`(c & 0xFC000000) == 0 ? c \| 0xFF000000 : c`). `centeredText` exists without a shadow flag |
| `g.blit(texture, x, y, u, v, w, h)` | `g.blit(RenderPipelines.GUI_TEXTURED, Identifier, x, y, float u, float v, w, h, texW, texH[, int argb])`; the 13-arg form adds `srcWidth, srcHeight` before `texW, texH`; `g.blit(Identifier, x0, y0, x1, y1, u0, u1, v0, v1)` takes raw UVs, no tint |
| `g.blit(x, y, z, w, h, sprite)` | `g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, w, h[, argb])` — stretches the whole sprite; the sub-rectangle overload is private, so tiling is 16×16 steps under `enableScissor/disableScissor` |
| `fill`, `fillGradient` | unchanged; `hLine/vLine` → `horizontalLine/verticalLine`; `renderOutline` → `outline` |
| `renderItem`/`renderFakeItem` | `g.item(stack, x, y[, seed])` / `g.fakeItem(…)`; `g.itemDecorations(font, stack, x, y[, countText])` |
| `renderTooltip(font, List, Optional<TooltipComponent>, x, y)` | `setTooltipForNextFrame(...)`, deferred; **the first caller in a frame wins** unless the `replaceExisting` overload is used |
| `enableScissor/disableScissor` | same names; the rectangle is transformed by the current pose |
| GL stencil, `RenderSystem.setShader/setShaderColor/setShaderTexture/enableBlend/blendFunc`, `GlStateManager`, `BufferUploader.drawWithShader` | gone. Textures and tints are per-`blit` parameters; clipping is scissor |
| `getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)` | `Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)` (`net.minecraft.data.AtlasIds`); `TextureAtlas#getSprite` returns the missing sprite when absent |
| `Minecraft#getTimer()` | `getDeltaTracker()` (`getGameTimeDeltaPartialTick(boolean)`, `getGameTimeDeltaTicks()`) |
| `Minecraft#getItemRenderer()/getBlockRenderer()` | gone, with `ItemRenderer` and `BlockRenderDispatcher` |
| `options.graphicsMode() >= FABULOUS` | `GraphicsStatus` is gone; `options.improvedTransparency().get()` |
| `GameProfile#getId()` | `id()` (record) |
| `MultiPlayerGameMode#handleInventoryButtonClick(containerId, id)` | unchanged |
| `ARGB` | `opaque(int)`, `scaleRGB(int, float)`, `alpha/red/green/blue`, `color(a, rgb)`, `multiply(a, b)` |

Fluid in a GUI: `Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState())`
→ `FluidModel` (`stillMaterial().sprite()`, `fluidTintSource()` nullable → `colorAsStack(FluidStack)`);
`RenderHelper.getFluidModel/getFluidTexture/getFluidColor` and `FluidHelper.color` wrap it.
`RenderHelper.drawFluid(g, x, y, stack, w, h)` / `drawTiledTexture(g, x, y, sprite, w, h[, argb])` take
**pose-relative** coordinates (elements pass `posX()/posY()`, not `guiLeft() + posX()`).

`IGuiAccess`/`ElementBase` draw helpers take the texture explicitly: `drawTexturedModalRect(g, Identifier,
x, y, u, v, w, h[, color])` (256×256) and `(…, texW, texH[, color])`, `drawIcon(g, texture[, color], x, y)`,
`drawSprite(g, sprite[, color], x, y)`, `drawSizedRect/drawColoredModalRect(g, x1, y1, x2, y2, color)`,
`drawString(g, …, x, y, color, shadow)`. `IGuiAccess.blitOffset()` and `Color.apply(GuiGraphics)` are gone.
CoFH's own element callbacks (`mouseClicked(double, double, int)`, `mouseReleased`, `keyTyped`,
`mouseWheel`, `addTooltip`) are **unchanged**.

`RenderHelper.mulColor(BakedQuad, int)` survives, rebuilt on NeoForge's `MutableQuad`
(`setFrom(quad)`, `setColor(i, ARGB.multiply(quad.bakedColors().color(i), color))`, `toBakedQuad()`).
Removed with no counterpart: `unpackLight/packLight/vertexColorIndex`, `drawStencil`, the `setShader*`/
`set*TextureSheet` family, the immediate-mode `drawFluid/drawIcon/drawTiledTexture/
drawScaledTexturedModalRectFromSprite` overloads, `renderItem()`, `renderBlock()`.

### (b) Models

| 1.21.1 | 26.1.2 |
|---|---|
| `IGeometryLoader` + `SimpleUnbakedGeometry` (model JSON `"loader"`) | `UnbakedModelLoader<T extends UnbakedModel>` in `ModelEvent.RegisterLoaders` — yields `UnbakedModel`/`UnbakedGeometry` → `QuadCollection` only; no level/pos/`ModelData` hook |
| `IDynamicBakedModel#getQuads(state, side, rand, ModelData, RenderType)` | `BlockStateModel#collectParts(BlockAndTintGetter, BlockPos, BlockState, RandomSource, List<BlockStateModelPart>)` (NF `BlockStateModelExtension`); `DelegateBlockStateModel(delegate)`, `DynamicBlockStateModel`; data via `level.getModelData(pos).get(PROPERTY)` |
| a custom baked model per block | `CustomUnbakedBlockStateModel` (`bake(ModelBaker)`, `resolveDependencies`, `codec()`) registered in **`RegisterBlockStateModels#registerModel(Identifier, MapCodec)`** (NF `BlockStateModelHooks`, fired from `ClientHooks.initClientRegistries`); the blockstate variant JSON gets `"type": "<id>"` (`NeoForgeExtraCodecs.dispatchMapOrElse("type", …)`, fallback `SingleVariant.Unbaked.MAP_CODEC`). Alternative: swap in `ModelEvent.ModifyBakingResult#getBakingResult().blockStateModels()` |
| `BakedModel` | `BlockStateModelPart#getQuads(@Nullable Direction)`, `useAmbientOcclusion()`, `particleMaterial()` (`Material.Baked`), `materialFlags()`; impl `SimpleModelWrapper(QuadCollection, boolean ao, Material.Baked)`; `QuadCollection.Builder#addCulledFace(dir, quad)/addUnculledFace(quad)/build()` |
| `BakedQuad(int[] vertices, tint, dir, sprite, shade)` | `net.minecraft.client.resources.model.geometry.BakedQuad` record `(position0..3 Vector3fc, packedUV0..3 long, direction, MaterialInfo, BakedNormals, BakedColors)`; `MaterialInfo(sprite, ChunkSectionLayer layer, RenderType itemRenderType, tintIndex, shade, lightEmission, ambientOcclusion)`; `bakedColors().color(vertex)` (NF `BakedColors.PerQuad/PerVertex`) |
| `RetexturedBakedQuad` | `new MutableQuad().setFrom(quad).setSpriteAndMoveUv(Material.Baked).toBakedQuad()` (NF `client/model/quad/MutableQuad`); layer from `sprite.transparency()` — **the chunk layer is per quad, from the sprite's alpha** |
| `ItemOverrides#resolve(...)` | `ItemModel#update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, ItemOwner, seed)`; `ItemModel.Unbaked` (`type()`, `bake(BakingContext, Matrix4fc)`); `RegisterItemModelsEvent#register(Identifier, MapCodec)`; vanilla `CuboidItemModelWrapper` is the `minecraft:model` reference |
| `ItemProperties.register(item, id, fn)` | `RangeSelectItemModelProperty { float get(ItemStack, ClientLevel, ItemOwner, int seed); MapCodec type(); }` in `RegisterRangeSelectItemModelPropertyEvent#register(Identifier, MapCodec)`; the item JSON dispatches with `minecraft:range_dispatch`, `"property": "<id>"`. Posted from `ClientBootstrap.bootstrap()` in the `Minecraft` constructor, after registries |
| `RegisterColorHandlersEvent.Item` / `ItemColors` | `RegisterColorHandlersEvent.ItemTintSources#register(Identifier, MapCodec<? extends ItemTintSource>)`; `ItemTintSource { int calculate(ItemStack, ClientLevel, LivingEntity); MapCodec type(); }`; listed per item in `items/*.json` `"tints"`; ARGB |
| `RegisterClientReloadListenersEvent#registerReloadListener(l)` | `AddClientReloadListenersEvent#addListener(Identifier key, PreparableReloadListener)` (+ `addDependency`) |
| `@EventBusSubscriber(bus = MOD)` | no `bus` parameter; the bus is inferred from the event type |

CoFHCore's shape: `SimpleModel` is the `CustomUnbakedBlockStateModel` wrapper (`SimpleModel.Loader(IFactory).codec()`
registers it; `IFactory<T extends BlockStateModel>.create(BlockStateModel)`), `SimpleItemModel` its item half
(`IFactory.create(ItemStack, BlockStateModelPart)` per stack; JSON keys `model`, `transformation`, `tints`),
`ModelUtils.WrappedBakedModelBuilder` takes/returns `BlockStateModelPart` with `retexture(BakedQuad, sprite)`
and `getAllQuads(part)`. `ElementsModelWrapped` (vanilla bakes `elements`), `FluidContainerItemModel`
(NeoForge `neoforge:fluid_container`), `RetexturedBakedQuad` and `BackfaceBakedQuad` (a marker type; `BakedQuad`
is final) are deleted. Item tints (`cofh_core:colorable`, `{"index": n}`) and item properties (`ProxyClient`
registers one `RangeSelectItemModelProperty` per id, resolved by `stack.getItem()`) now need the item JSON.

### (c) Entity renderers and models

| 1.21.1 | 26.1.2 |
|---|---|
| `EntityRenderer<T>`; `render(T, yaw, pt, PoseStack, MultiBufferSource, light)`; `getTextureLocation(T)` | `EntityRenderer<T extends Entity, S extends EntityRenderState>`: `createRenderState()`; `extractRenderState(T, S, float)` (call super first: `x/y/z`, `ageInTicks`, `eyeHeight`, `lightCoords`, `outlineColor`, name tag, leash, shadow); `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` (super draws leash + name tag). No `getTextureLocation`; a per-entity texture goes into the state |
| `ThrownItemRenderer`: `ItemRenderer.renderStatic(...)` | `ThrownItemRenderState.item` (`ItemStackRenderState`); extract `ctx.getItemModelResolver().updateForNonLiving(state.item, stack, ItemDisplayContext.GROUND, entity)`; submit `state.item.submit(poseStack, collector, light, overlay, outlineColor)` |
| `NothingRenderer` empty `render` | `NoopRenderer<T> extends EntityRenderer<T, EntityRenderState>` |
| `TntRenderer`: `getBlockRenderer().renderSingleBlock` | `TntRenderState { fuseRemainingInTicks; BlockModelRenderState blockState }`; `ctx.getBlockModelResolver().update(state.blockState, blockState, TntRenderer.BLOCK_DISPLAY_CONTEXT)`; `TntMinecartRenderer.submitWhiteSolidBlock(BlockModelRenderState, PoseStack, collector, light, boolean white, outlineColor)` or `blockModel.submit(pose, collector, light, overlay, outlineColor)` |
| `MinecartRenderer<T>` + `renderMinecartContents(...)` | `AbstractMinecartRenderer<T extends AbstractMinecart, S extends MinecartRenderState>(ctx, ModelLayerLocation)`; `submitMinecartContents(S, BlockModelRenderState, PoseStack, collector, light)`; `MinecartTntRenderState.fuseRemainingInTicks` (-1 = unlit) |
| `BoatRenderer(ctx, chestBoat)`, `ChestBoatModel` | `AbstractBoatRenderer(ctx, Identifier texture)` with `model()` and `submitTypeAdditions(...)`; `BoatRenderer(ctx, layer)` derives its texture from the layer path; `ChestBoatModel` is gone — `BoatModel.createBoatModel()`/`createChestBoatModel()` layer definitions. `BoatRendererCoFH` extends `AbstractBoatRenderer` to keep CoFH's explicit texture |
| `buffer.getBuffer(RenderType)` + hand-built vertices | `collector.submitCustomGeometry(PoseStack, RenderType, (pose, buffer) -> …)`; `RenderTypes.entityTranslucent(Identifier)` from `net.minecraft.client.renderer.rendertype`; all six attributes per vertex. `submitModel(Model<? super S>, S, PoseStack, Identifier, light, overlay, outlineColor, @Nullable CrumblingOverlay)`, `submitModelPart(ModelPart, PoseStack, RenderType, light, overlay, @Nullable sprite)` |
| `HumanoidModel<T extends LivingEntity>` | `HumanoidModel<T extends HumanoidRenderState>`; `createMesh(CubeDeformation, yOffset)` kept, but **`hat` is a child of `head`** (the constructor does `head.getChild("hat")`) |
| `Context.getBlockRenderDispatcher()` | `getItemModelResolver()`, `getBlockModelResolver()`, `bakeLayer()`, `getModelSet()` |
| `AbstractArrow.inGround` | `net.minecraft.world.entity.projectile.arrow.AbstractArrow`: protected `isInGround()` over public `IN_GROUND` accessor |
| `EntityRenderersEvent.RegisterRenderers/RegisterLayerDefinitions` | unchanged |

`ITranslucentRenderer` (CoFH's second "after particles" pass) is deleted; the pipeline orders translucent
render types itself. `ShockwaveRenderer` renders nothing (its `VFXHelper` path needs a rewrite on the collector).

### (d) Block-entity renderers

Not exercised in CoFHCore (no BER here). Shapes are in the port plan and Pyronetics' notes:
`BlockEntityRenderer<T, S extends BlockEntityRenderState>`, `createRenderState()`, `extractRenderState(be,
state, partialTick, cameraPos, crumblingOverlay)` (super first), `submit(state, poseStack, collector, camera)`.

### (e) Fluids

| 1.21.1 | 26.1.2 |
|---|---|
| `IClientFluidTypeExtensions#getStillTexture/getFlowingTexture/getTintColor` | `RegisterFluidModelsEvent#register(new FluidModel.Unbaked(Material still, Material flow, @Nullable Material overlay, @Nullable FluidTintSource), still, flowing)` (mod bus); `Material` = `net.minecraft.client.resources.model.sprite.Material(Identifier)` (block atlas); `FluidTintSource { int color(FluidState); default colorInWorld(...); default colorAsStack(FluidStack) }`, `FluidTintSources.constant(argb)`. `IClientFluidTypeExtensions` keeps overlay/fog only |
| `getTintColor(stack)` at a call site | `getModelManager().getFluidStateModelSet().get(fluidState)` → `FluidModel(layer, stillMaterial, flowingMaterial, overlayMaterial, fluidTintSource, customRenderer)` |

The three CoFH fluids lost their `initializeClient` overrides; `CoreClientSetupEvents` registers their models
(potion with a `FluidTintSource`: white in world, `0xFF000000 | potion colour` as a stack).

### (f) Particles

| 1.21.1 | 26.1.2 |
|---|---|
| `Particle#roll/oRoll`, `rCol/gCol/bCol/alpha`, `sprite` | moved to `SingleQuadParticle`. `Particle` keeps `x/y/z`, `xo/yo/zo`, `xd/yd/zd`, `age`, `lifetime`, `random`, `gravity`, `friction`, `onGround`, `hasPhysics`, `bbWidth/bbHeight` |
| `getRenderType()` → `ParticleRenderType` | `getGroup()` → `ParticleRenderType`, now `record(String name)`: `SINGLE_QUADS`, `ITEM_PICKUP`, `ELDER_GUARDIANS`, `NO_RENDER`; `new ParticleRenderType("modid:name")` keys a custom group |
| `Particle#render(VertexConsumer, Camera, float)` | gone. Rendering is `ParticleGroup<P>(ParticleEngine)#extractRenderState(Frustum, Camera, float)` → `ParticleGroupRenderState#submit(SubmitNodeCollector, CameraRenderState)`; register with NF `RegisterParticleGroupsEvent#register(ParticleRenderType, Function<ParticleEngine, ParticleGroup<?>>)` (mod bus). `Frustum#isVisible(AABB)`, `Camera#position()` |
| `getLightColor(float)` | `getLightCoords(float)`; `LevelRenderer.getLightColor(level, pos)` → `getLightCoords(BlockAndLightGetter, pos)`; packing unchanged |
| `TextureSheetParticle(level, x, y, z[, xd, yd, zd])` + `pickSprite(SpriteSet)` | `SingleQuadParticle(level, x, y, z[, xa, ya, za], TextureAtlasSprite)`; `setSprite(spriteSet.get(random))`; `setSpriteFromAge`, `scale`, `setColor`, `setAlpha` kept; `SpriteSet#get(int, int)`, `get(RandomSource)`, `first()` |
| sheet `getRenderType()` | `protected Layer getLayer()`; `Layer(boolean translucent, Identifier atlas, RenderPipeline)` record: `OPAQUE`, `TRANSLUCENT`, `*_TERRAIN`, `*_ITEMS`, `bySprite(sprite)` |
| quad emission | `extract(QuadParticleRenderState, Camera, float)` → `state.add(layer, x, y, z, quat, getQuadSize(pt), u0, u1, v0, v1, argb, light)`; `quadSize` is the half-extent |
| `ParticleProvider#createParticle(T, level, x, y, z, xd, yd, zd)` | `+ RandomSource` (9th parameter); `X::new` constructor references no longer fit |
| `RegisterParticleProvidersEvent#registerSpriteSet/registerSpecial` | unchanged |
| `SubmitNodeCollector#submitCustomGeometry` | **one `VertexConsumer` per `RenderType`** per feature pass, so a callback must not request other types; the pose is camera-relative |
| `VertexConsumer` abstract set | `addVertex(f, f, f)`, `setColor(i, i, i, i)`, `setColor(int)`, `setUv`, `setUv1`, `setUv2`, `setNormal(f, f, f)`, `setLineWidth` |
| `ParticleType#streamCodec()` | `StreamCodec<? super RegistryFriendlyByteBuf, T>` |

CoFHCore's shape: `SpriteParticle extends SingleQuadParticle` (`getQuadSize = size * 0.5F`, per-frame hook
`animate(time, pTicks)`); the `PoseStack` particles keep `CoFHParticle` (`render(PoseStack, MultiBufferSource,
int packedLight, float time, float pTicks)` — the dead `VertexConsumer` parameter is gone) under
`CoFHParticle.GROUP` (`cofh_core:custom`), whose `CoFHParticleGroup` frustum-culls, records each particle into
a per-`RenderType` recorder and submits custom geometry once per type. `DeferredRegisterCoFH#register(name,
Type::new)` is ambiguous for a type with two constructors (B.2 added the `Function<Identifier, …>` overload);
use `() -> new Type()`.

### (g) Render types, shaders, post effects, VFX

| 1.21.1 | 26.1.2 |
|---|---|
| `ShaderInstance` + `RegisterShadersEvent` + `shaders/core/<x>.json` | `RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET).withLocation(id).withVertexShader(id "ns:core/x").withFragmentShader(...).withColorTargetState(new ColorTargetState(BlendFunction)).withDepthStencilState(new DepthStencilState(CompareOp, writeDepth)).build()`, registered in NF `RegisterRenderPipelinesEvent#registerPipeline` (mod bus). Shader id `ns:core/x` → `assets/ns/shaders/core/x.vsh/.fsh` (`#version 330`, `#moj_import <minecraft:fog.glsl>`/`dynamictransforms.glsl`/`projection.glsl`); the JSON program files are gone |
| `RenderType.create(name, format, mode, size, crumbling, sort, CompositeState)` | `RenderType.create(name, RenderSetup.builder(pipeline).withTexture("Sampler0", id[, sampler]).useLightmap().useOverlay().sortOnUpload().bufferSize(n).setOutputTarget(OutputTarget).setLayeringTransform(LayeringTransform).createRenderSetup())` |
| `RenderStateShard.TRANSLUCENT_TRANSPARENCY` / `NO_DEPTH_TEST` / `COLOR_WRITE` / `CULL` | `BlendFunction.TRANSLUCENT` (`new BlendFunction(SRC_ALPHA, ONE_MINUS_SRC_ALPHA[, srcA, dstA])`) / `DepthStencilState(CompareOp.ALWAYS_PASS, false)` / `DepthStencilState(LESS_THAN_OR_EQUAL, false)` / `Builder#withCull(boolean)`; `DepthStencilState.DEFAULT` = LEQUAL + write |
| `RENDERTYPE_TRANSLUCENT_SHADER` on `NEW_ENTITY` | `ENTITY_SNIPPET` (`DefaultVertexFormat.ENTITY`) + `withShaderDefine("NO_CARDINAL_LIGHTING")` + `"NO_OVERLAY"` (entity defines: `EMISSIVE`, `NO_OVERLAY`, `PER_FACE_LIGHTING`, `NO_CARDINAL_LIGHTING`, `ALPHA_CUTOUT`, `APPLY_TEXTURE_MATRIX`); `RENDERTYPE_ENTITY_SOLID_SHADER` → `RenderPipelines.ENTITY_SOLID` (needs `useLightmap().useOverlay()`) |
| `RENDERTYPE_LINES_SHADER` + `LineStateShard` | `RenderPipelines.LINES_SNIPPET` (`POSITION_COLOR_NORMAL_LINE_WIDTH`, cull off); the width is **per vertex**: `VertexConsumer#setLineWidth(float)`; vanilla's is `mc.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth`. `POSITION_COLOR` quads → `DEBUG_FILLED_SNIPPET` |
| `RenderType.lines()` etc. | `net.minecraft.client.renderer.rendertype.RenderTypes.lines()`, `solidMovingBlock()/cutoutMovingBlock()/translucentMovingBlock()`, `crumbling(id)`; `ModelBakery.DESTROY_TYPES` |
| `ParticleRenderType` with `begin(Tesselator, TextureManager)` | `SingleQuadParticle.Layer(translucent, atlas, RenderPipeline)` |
| `OutputStateShard`, `RenderType.MAIN_TARGET` | `OutputTarget(String, Supplier<@Nullable RenderTarget>)`, `OutputTarget.MAIN_TARGET` |
| `PostChain(TextureManager, ResourceManager, RenderTarget, Identifier)`, `process(float)`, `resize` | `Minecraft#getShaderManager().getPostChain(Identifier, Set<Identifier>)`; `addToFrame(FrameGraphBuilder, w, h, TargetBundle)`, `process(RenderTarget, GraphicsResourceAllocator)`; config `assets/ns/post_effect/<name>.json` (`PostChainConfig`) |
| `RenderTarget#blitToScreen(w, h, bool)`, `clear(bool)`, `bindWrite` | `blitToScreen()`, `copyDepthFrom(RenderTarget)`; clears via `RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(...)`; no `bindWrite` |
| `BlockRenderDispatcher#renderBatched`, `getBlockModel(state).getRenderTypes(...)` | `new ModelBlockRenderer(ao, false, mc.getBlockColors()).tesselateBlock((x, y, z, quad, instance) -> …, 0, 0, 0, BlockAndTintGetter, pos, state, mc.getModelManager().getBlockStateModelSet().get(state), state.getSeed(pos))`; layer per quad from `quad.materialInfo().layer()` (`ChunkSectionLayer`) → moving-block render type; `VertexConsumer#putBakedQuad(Pose, BakedQuad, QuadInstance)`. `BlockAndTintGetter` is `net.minecraft.client.renderer.block.BlockAndTintGetter`, implemented by `ClientLevel` but **not** `Level` |

CoFHCore's `RenderTypes` keeps its constant and factory names (`FLAT_CUTOUT`, `FLAT_TRANSLUCENT`, `LINEAR_GLOW`,
`ROUND_GLOW`, `OVERLAY_LINES`, `OVERLAY_BOX`; `opaque(name, Identifier)`, `translucent*(Identifier)`), and
`PARTICLE_SHEET_OVER/ADDITIVE_MULTIPLY/ADDITIVE_SCREEN` are now `SingleQuadParticle.Layer`s on the
`CoreShaders` pipelines (which also add `LINES_NO_DEPTH` and `POSITION_COLOR_NO_DEPTH` for reuse).
`CoreRenderType.THICK_LINES` is a `float` to pass to `setLineWidth`. **Post effects are stubbed**: `PostEffect`
never loads a chain, `PostBuffer.getBuffer(...)` draws through to the level buffers, so `CoreShaders.PIXELATE`
and `CoreClientConfig.stylizedGraphics` do nothing (TODO below). `VFXHelper` refers to vanilla's `RenderTypes`
by qualified name because it shares a package with CoFH's.

### (h) Client events

| 1.21.1 | 26.1.2 |
|---|---|
| `RenderLevelStageEvent` + `getStage()`/`getCamera()`/`getPartialTick()` | subclasses `AfterSky`, `AfterOpaqueBlocks`, `AfterOpaqueFeatures`, `AfterTranslucentFeatures`, `AfterTranslucentBlocks`, `AfterTranslucentParticles`, `AfterWeather`, `AfterLevel`; `getLevelRenderer()`, `getLevelRenderState()` (camera position = `levelRenderState.cameraRenderState.pos`), `getPoseStack()` (nullable on Sky/OpaqueBlocks/Weather/Level), `getModelViewMatrix()`, `getRenderableSections()`; no partial tick — `RenderFrameEvent.Pre#getPartialTick().getGameTimeDeltaPartialTick(false)` |
| `RenderHighlightEvent.Block` | `ExtractBlockOutlineRenderStateEvent` (extract phase; cancelling removes the outline entirely): `getHitResult()`, `getBlockPos()`, `getBlockState()`, `getCollisionContext()`, `isInTranslucentPass()`, `isHighContrast()`, `getCamera()`, `getLevelRenderer()`, `getLevelRenderState()`, `addCustomRenderer(CustomBlockOutlineRenderer)`. `CustomBlockOutlineRenderer#render(BlockOutlineRenderState, MultiBufferSource.BufferSource, PoseStack, boolean translucentPass, LevelRenderState)` runs **twice per frame** (gate on `translucentPass == state.isTranslucent()`), returns `true` to suppress vanilla's outline, and must only use extracted data. `LevelRenderer#renderHitOutline(PoseStack, VertexConsumer, camX, camY, camZ, BlockOutlineRenderState, int argb, float width)` (AT'd; vanilla colour `ARGB.black(102)`) |
| crumbling via `renderBreakingTexture` | add `new BlockBreakingRenderState(pos, state, progress 0..9)` to `LevelRenderState#blockBreakingRenderStates` in `ExtractLevelRenderStateEvent` (after vanilla's extraction); vanilla submits them |
| `RenderLivingEvent.Pre<T, M>` with `getEntity()` | `RenderLivingEvent.Pre<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>`; `getRenderState()`, `getRenderer()`, `getPoseStack()`, `getSubmitNodeCollector()`, `getPartialTick()` — **no entity**. Copy what you need in `RegisterRenderStateModifiersEvent#registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {}, (entity, state) -> state.setRenderData(ContextKey, value))` (mod bus) and read `state.getRenderData(key)` |
| `RenderHandEvent` | unchanged |
| `MouseHandler#turnPlayer` mixin | NF `CalculatePlayerTurnEvent` (`getMouseSensitivity()`/`setMouseSensitivity`, `getCinematicCameraEnabled()`); the option value is cubed inside `turnPlayer`, so a multiplier on the old cubed value becomes `cbrt` on `(value * 0.6 + 0.2)` |
| `Camera#getPosition()`; `Entity#getCommandSenderWorld()`; `ItemStack#getDescriptionId()` | `position()`; `level()`; `getItem().getDescriptionId()` |

`CoreClientEvents` lost its delayed-particle pass and the `ITranslucentRenderer` call; sub-hitbox and
area-effect outlines are `CustomBlockOutlineRenderer`s; area-effect block damage goes through vanilla's
breaking list; true invisibility reads a render-state flag set by a modifier in `CoreClientSetupEvents`.

## B.8 Resources (CoFHCore's own)

- **`data/<ns>/recipe/`**, singular, since 1.21. `data/cofh_core/recipes/securable.json` was in the plural
  folder on the `1.21.1` branch too, so the securable recipe had not loaded since that hop.
- `assets/<ns>/items/<name>.json` for every item (`{"model": {"type": "minecraft:model", "model": "<ns>:item/<name>"}}`);
  `"render_type"` removed from block models (ignored: the layer is per quad from the sprite's alpha).
- Fluid textures legitimately carry partial alpha (translucent fluids); the alpha audit is for block art.
- Enchantment JSON (`holding.json`) and damage-type JSON are unchanged in shape. Curios 15.0.0+26.1.2 resolves,
  so `data/cofh_core/curios/**` stays. `key.category.<ns>.<name>` lang keys were already in place.
- Shaders: program/post JSON files deleted; GLSL under `shaders/core/` on the 330 includes.
- Regenerated data (tags, loot) comes from `runData` (`clientData()`), not by hand.

## B.9 Mixins and ATs

| Mixin | 26.1.2 |
|---|---|
| `LivingEntityMixin` `hurt(DamageSource;F)Z` | `hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z`; the level is the first parameter |
| `LivingEntityMixin` `isDamageSourceBlocked` | gone; blocking runs in `applyItemBlocking(ServerLevel, DamageSource, float)` over the `BLOCKS_ATTACKS` component, which posts NF `LivingShieldBlockEvent` with a settable `setBlocked(boolean)` — `ShieldEvents` now gates on `IShieldItem#canBlock` there. A CoFH shield must carry `BLOCKS_ATTACKS` or the event never fires |
| `HorseArmorItemMixin`, `ShieldItemMixin` (`getEnchantmentValue`) | deleted; `ModifyDefaultComponentsEvent#modify(item, b -> b.set(DataComponents.ENCHANTABLE, new Enchantable(n)))` (mod bus, `CoreCommonEvents`). Vanilla's `horseArmor(material)` and the shield set no `ENCHANTABLE`; `AnimalArmorItem` no longer exists |
| `MultiPlayerGameModeMixin` (`sameDestroyTarget`) | deleted: vanilla now checks `destroyingItem.shouldCauseBlockBreakReset(selected)` itself |
| `MouseHandlerMixin` | deleted: `CalculatePlayerTurnEvent` (B.7h) |
| `LevelRendererMixin` (`PostChain#process(F)` depth-mask workaround) | deleted: the transparency chain is a frame-graph pass and the injection point is gone. Verify Fabulous graphics with CoFH translucency in the client pass |
| `GameRendererMixin` (`resize`), `ShearsItemMixin` (`doesSneakBypassUse`), `LivingEntityMixin`'s other targets (`canBeAffected`, `canFreeze`, `updateInvisibilityStatus`) | unchanged targets, confirmed in the jar |

ATs: `validateAccessTransformers` passes; `LevelRenderer#renderHitOutline` and `renderBuffers`,
`ParticleResources#spriteSets`, `MultiPlayerGameMode#destroyProgress` were already retargeted in B.0.

---

## B.10 ThermalCore

Shapes ThermalCore's port confirmed that CoFHCore's categories did not cover. Same jar oracles;
NeoForge classes are in `neoforge-26.1.2.109-sources.jar`.

### Registration (B.2, again)

- `Item`/`Block` throw at construction without an id (`itemIdOrThrow`, "Block id not set"), so every
  `register(name, () -> new X(of()…))` supplier is a boot failure, not a compile error. ThermalCore's
  `RegistrationHelper` gained `blockProperties(Identifier)`, `itemProperties(Identifier)` (CoFH's
  `Utils.itemProperties()` plus `setId`), `blockItemProperties(Identifier)` (adds
  `useBlockDescriptionPrefix()`, which is what vanilla's `Items.registerBlock` applies) and
  `Function<Identifier, …>` overloads of `registerBlock`/`registerBlockOnly`/`registerAugmentableBlock`/
  `registerItem`; the `Supplier` overloads stay. Fluid classes register their block and bucket the same way.
- `Blocks.woodenButton` is gone (`new ButtonBlock(BlockSetType, 30, props)`); `Blocks.buttonProperties()` is public.
- `HoneyBottleItem` is gone: `new ItemCoFH(props.craftRemainder(GLASS_BOTTLE).food(Foods.HONEY_BOTTLE,
  Consumables.HONEY_BOTTLE).usingConvertsTo(GLASS_BOTTLE).stacksTo(16))`; `Item.Properties#food(FoodProperties, Consumable)`.
- `MobSpawnSettings.SpawnerData(type, minCount, maxCount)` — the weight moved to `Weighted<SpawnerData>`;
  `BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(HolderSet<Biome>, Weighted<SpawnerData>)`,
  `WeightedList.of(Weighted<E>...)` (`net.minecraft.util.random`).
- `Registry#get(TagKey)` → `Optional<HolderSet.Named<T>>` replaces `getTag`; `Registry#listElements()` replaces `holders()`.

### Items and blocks (B.5, again)

- `Item#inventoryTick(ItemStack, ServerLevel, Entity, @Nullable EquipmentSlot)` — server-only now;
  `Item#hurtEnemy` returns `void` (`postHurtEnemy` exists); `Item#getCraftingRemainder()` is final and
  `IItemExtension#getCraftingRemainder(ItemInstance)` returns `@Nullable ItemStackTemplate` (`new ItemStackTemplate(item)`).
- Per-stack enchantability: `Enchantable(int)` throws for ≤ 0; NeoForge's enchanting-table filter calls
  `isPrimaryItemFor`. `ItemStack#getPrototype()` is the item's default component map.
- Armour materials are static records built by `ArmorMaterialCoFH.create(durability, int[], ench, sound, tough, kb,
  TagKey<Item> repair, ResourceKey<EquipmentAsset>)` with `EquipmentAssets.ROOT_ID`; the asset JSON lives at
  `assets/<ns>/equipment/<name>.json` with `humanoid`/`humanoid_leggings` layers, and
  `EquipmentClientInfo.Layer#getTextureLocation` resolves to `textures/entity/equipment/<layer>/<path>.png`
  (the old `models/armor/<n>_layer_1/2.png` move there).
- `NbtUtils.writeBlockPos/readBlockPos` are gone: `BlockPos.CODEC` with `NbtOps` is the same int-array layout.
- `ItemEntityPickupEvent.Pre#canPickup()` returns `net.minecraft.util.TriState`; `Inventory#getSelectedSlot()`.
- Energy on items: `ItemAccess.forStack(stack).getCapability(Capabilities.Energy.ITEM)` (throws on an empty stack),
  wrapped with `IEnergyStorage.of(cap)`; `EnergyHandler` is in `net.neoforged.neoforge.transfer.energy`,
  `ItemAccess` in `…transfer.access`. Fluids on items: `FluidUtil.getFluidHandler(stack)`.

### Entities (B.5, again)

- `net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile` (package move); its
  `tick()` applies inertia first and `getLiquidInertia()` replaces `getWaterInertia()`. `Projectile#checkLeftOwner()`
  is `public void` with a public `leftOwnerChecked` flag reset each tick. `Entity#applyEffectsFromBlocks()`
  replaces `checkInsideBlocks()`.
- `Entity#hurtOrSimulate(DamageSource, float)` when the boolean is needed; `LivingEntity#isInvulnerableTo(ServerLevel, DamageSource)`;
  `Mob#doHurtTarget(ServerLevel, Entity)` with `Goal.getServerLevel(Entity)`; `Mob#getPickResult()` replaces
  NeoForge's `getPickedResult(HitResult)`; `Entity#getBoundingBoxForCulling()` is gone — it is
  `EntityRenderer#getBoundingBoxForCulling(T)` now; `causeFallDamage(double, float, DamageSource)`.
- `PathType.DANGER_FIRE/DAMAGE_FIRE` → `FIRE_IN_NEIGHBOR/FIRE`. `SpawnPlacements.SpawnPredicate<T>#test(EntityType<T>,
  ServerLevelAccessor, EntitySpawnReason, BlockPos, RandomSource)`; `Monster.checkMonsterSpawnRules` takes the same.
- `ThrowableItemProjectile` constructors want a trailing `ItemStack`; CoFH's `AbstractGrenade` shape
  (`super(type, level); setPos(...)`, then `setOwner`) avoids it.
- `AreaEffectCloud#setCustomParticle(@Nullable ParticleOptions)`; `ParticleTypes.EFFECT`/`INSTANT_EFFECT` are
  `ParticleType<SpellParticleOption>` — `SpellParticleOption.create(type, int argb, float power)` or
  `(type, r, g, b, power)`; `power` 1.0 is velocity-neutral. `ClientLevel#addParticle(options, x, y, z, xd, yd, zd)`
  applies `getType().getOverrideLimiter()`, which is what the removed `LevelRenderer#addParticleInternal` did.
- `Explosion` is an interface: `new ServerExplosion(ServerLevel, @Nullable Entity, @Nullable DamageSource,
  @Nullable ExplosionDamageCalculator, Vec3, float, boolean fire, Explosion.BlockInteraction)` and static
  `ServerExplosion.getSeenPercent(Vec3, Entity)`.
- `Capabilities.Fluid.ENTITY` is `EntityCapability<ResourceHandler<FluidResource>, @Nullable Direction>`; an
  entity's `read/addAdditionalSaveData` take `ValueInput`/`ValueOutput` and bridge like `BlockEntityCoFH`
  (`input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC))` / `output.store(nbt)`).

### Entity renderers (B.7c, again)

- `EntityModel<S extends EntityRenderState>` with `protected EntityModel(ModelPart)` (default
  `RenderTypes::entityCutout`) or `(ModelPart, Function<Identifier, RenderType>)`; `Model#root()` and
  `renderToBuffer` are final; `setupAnim(S)` resets the pose first; `HierarchicalModel` is gone.
  `RenderTypes.entityCutoutNoCull` no longer exists (vanilla's `BlazeModel` went to `entityCutout` too).
- `MobRenderer<T extends Mob, S extends LivingEntityRenderState, M extends EntityModel<? super S>>`:
  `getBlockLightLevel(T, BlockPos)` still takes the entity, `getTextureLocation(S)` is abstract on
  `LivingEntityRenderer`, `createRenderState()`/`extractRenderState(T, S, float)`. `LivingEntityRenderState`
  carries `bodyRot/yRot/xRot/ageInTicks/scale`; `EntityRenderState.partialTick/boundingBoxHeight`.
- `RenderLayer<S, M>#submit(PoseStack, SubmitNodeCollector, int light, S state, float yRot, float xRot)`.
  `OrderedSubmitNodeCollector#submitModel` ten-arg form: `(Model<? super S>, S, PoseStack, RenderType, light,
  overlay, int tint, @Nullable TextureAtlasSprite, outlineColor, @Nullable CrumblingOverlay)`;
  `ModelFeatureRenderer` calls `setupAnim(state)` at draw time. `Context#bakeLayer(ModelLayerLocation)`.
- `RenderType`/`RenderTypes` live in `net.minecraft.client.renderer.rendertype`.

### Client (B.7, again)

- `IClientFluidTypeExtensions` keeps only `getRenderOverlayTexture(Minecraft)`, `renderOverlay(...)`,
  `modifyFogColor(Camera, float, ClientLevel, int, float, Vector4f)` and
  `modifyFogRender(Camera, @Nullable FogEnvironment, float renderDistanceInChunks, float partialTick, FogData)`;
  `FluidType#initializeClient` is gone and extensions register on `RegisterClientExtensionsEvent#registerFluidType`.
  `FogData` has public `environmentalStart/End`, `renderDistanceStart/End`, `skyEnd`, `cloudEnd`; there is no
  `FogShape`.
- `FluidModel.Unbaked` has two four-arg constructors (`FluidTintSource` vs `BlockTintSource`), so a literal `null`
  tint needs a cast. `IBlockGetterExtension#getModelData(BlockPos)` is on `BlockGetter` (default `ModelData.EMPTY`).
- Blockstate variants take `"type": "<ns>:<loader>"` next to `"model"` (`NeoForgeExtraCodecs.dispatchMapOrElse`
  keys on `type`); the `"loader"` key in block models is dead. Item models: `items/<n>.json` with
  `{"model": {"type": "<ns>:<loader>", "model": "<ns>:block/<n>"}}` for a `SimpleItemModel.Loader`.
- `RegisterColorHandlersEvent.BlockTintSources#register(List<BlockTintSource>, Block...)`.
- `AbstractContainerScreen#checkHotbarKeyPressed(KeyEvent)`; `imageWidth/imageHeight` are set through the
  `ContainerScreenCoFH` constructor (final); `ContainerScreenCoFH#drawBackgroundTexture(GuiGraphicsExtractor)`
  is the hook for a screen that layers a second background texture.
- `BlockBehaviour.Properties#noCollission()` → `noCollision()`.

### Datagen (B.6, again)

- `GatherDataEvent#addProvider(T)`/`createProvider(...)` return the provider; `getLookupProvider()` only reflects
  modded datapack entries after `createDatapackRegistryObjects(...)`, so a CoFH `DatapackBuiltinEntriesProvider`
  subclass hands its `getRegistryProvider()` to later providers by hand.
- `EntityTypeTagsProvider(PackOutput, CompletableFuture<Provider>, String modId)` — vanilla, no `ExistingFileHelper`.
- `RecipeProvider.Runner`: `protected Runner(PackOutput, CompletableFuture<Provider>)`, abstract
  `createRecipeProvider(HolderLookup.Provider, RecipeOutput)` and `getName()`.
- `Tags.Items.SLIME_BALLS` (was `SLIMEBALLS`); `GUNPOWDERS`, `OBSIDIANS`, `SANDS`, `STRINGS`, `DUSTS_*`, `ENDER_PEARLS`,
  `INGOTS_COPPER`, `GEMS_QUARTZ` unchanged.
- Hand-written vanilla recipe JSON: ingredients are strings (`"minecraft:sand"`, `"#c:slag"`), never
  `{"item": …}`/`{"tag": …}` objects (those fail with "List is too short: 0"); a result with data is
  `"components": {…}`, not `"nbt"`.

### Recipes (B.6, again) — parse-time stacks are a boot failure

- **Item and fluid default components are bound after the datapack reload**, in
  `ReloadableServerResources#updateComponentsAndStaticRegistryTags()` (tags apply → `TagsUpdatedEvent.ServerDataLoad`
  → `DataComponentInitializers` apply → `DefaultDataComponentsBoundEvent`). Recipes are parsed before that, so on a
  server's first load `new ItemStack(...)`/`new FluidStack(...)` in a recipe codec NPEs in `Holder.Reference#components()`
  ("Components not bound yet") and `ItemStack.CODEC`/`FluidStack.CODEC` return a DataResult error ("does not have
  components yet") that silently drops the recipe. CoFHCore's B.6 note that `ItemStack` fields could stay was wrong at runtime.
- The bridge: recipes hold `ItemStackTemplate` / `FluidStackTemplate` (`net.neoforged.neoforge.fluids.FluidStackTemplate`:
  `(Holder<Fluid>|Fluid, int amount[, DataComponentPatch])`, `MAP_CODEC`/`CODEC`/`STREAM_CODEC`, `fromNonEmptyStack`,
  `withAmount`, `create()`; throws for an empty fluid or amount ≤ 0) and materialize stacks lazily in the getters.
  `RecipeJsonUtils.parseItemStackTemplate/parseFluidStackTemplate/parseOutputTemplates` are the JSON side;
  `ThermalRecipe#getOutputItems()/getOutputFluids()` keep their signatures and cache the created stacks.
  `ItemStackTemplate` throws for air or count 0; `withCount(int)`.
- `FluidIngredient.SingleFluidList` holds a template for the same reason; `TagList` already resolved lazily.
- **Managers refresh on `DefaultDataComponentsBoundEvent`** (`getUpdateCause() == SERVER_DATA_LOAD`), the first point at
  which `create()` / `SingleItemRecipe#result().create()` work; `TagsUpdatedEvent.ServerDataLoad` only captures the
  `RecipeManager`. The client keeps `RecipesReceivedEvent` (`RegistryDataCollector` binds client components in the
  configuration phase, before recipes arrive).
- **`Ingredient.CODEC` needs `RegistryOps`**: `HolderSetCodec` decodes strings and `#tags` only through
  `RegistryOps#getter`, so `Ingredient.CODEC.parse(JsonOps.INSTANCE, …)` fails for every string form. CoFH's re-parse of
  recipe JSON goes through `JsonMapCodec.of(fromJson, toJson)`, which keeps the recipe manager's ops in
  `RecipeJsonUtils.withOps` while `fromJson` runs; `RecipeJsonUtils.jsonOps()` hands out `registryOps.withParent(JsonOps.INSTANCE)`
  (static-registry ops as the fallback). `RecipeJsonUtils.INGREDIENT_CODEC` is the ops-carrying `Codec<Ingredient>` for
  `RecordCodecBuilder` recipes; it also accepts the pre-1.21.2 `{"item"}`/`{"tag"}` objects (`legacyIngredient`), so
  existing datapacks and TE's hand-written recipes keep loading.
- A tag an ingredient references must exist: `HolderSetCodec.lookupTag` errors on a missing tag where 1.21.1 matched
  nothing. ThermalCore's tag provider now emits the 24 compat `c:` tags empty.
- `ModConfigSpec#validateSpec` runs at `registerConfig` and calls every value's default supplier: a `define(key, otherConfigValue)`
  throws "Cannot get config value before config is loaded" (ThermalCore's "Festive Vanilla Mobs" did that).
- `RecipeMap` (`byType`, `byKey`, `values()`, `EMPTY`), `RecipeManager#recipeMap()`, `Registries.RECIPE`;
  `RecipesReceivedEvent#getRecipeMap()`; `OnDatapackSyncEvent#sendRecipes(RecipeType<?>...)`; `AddReloadListenerEvent`
  is `AddServerReloadListenersEvent#addListener(Identifier, PreparableReloadListener)`.
- `AbstractCookingRecipe`/`SingleItemRecipe`: `input()`, `result()` (`ItemStackTemplate`), `experience()`, `cookingTime()`;
  `ShapedRecipe#getIngredients() → List<Optional<Ingredient>>`; `RecipeDisplay#result()`, `SlotDisplay#resolveForFirstStack(ContextMap)`.
- `IItemStackExtension#getBurnTime(@Nullable RecipeType<?>, FuelValues)`, `MinecraftServer#fuelValues()`;
  `FoodProperties(nutrition, saturation, canAlwaysEat)` with effects on `Consumable` (`ApplyStatusEffectsConsumeEffect#effects()`,
  `consumeTicks()`); `EnchantmentHelper.createBook(EnchantmentInstance)`.
- JEI 29: `IRecipeCategory#getWidth()/getHeight()`, `draw(T, IRecipeSlotsView, GuiGraphicsExtractor, double, double)`,
  `getTooltip(ITooltipBuilder, …)`; `IRecipeSlotBuilder#addRichTooltipCallback`; JEI starts after `RecipesReceivedEvent`, so a
  HIGH-priority handler fills the CoFH caches first. Patchouli 26.1-94's processor API is unchanged from 1.21.1.

---

## B.10 ThermalExpansion and ThermalDynamics

Only what ThermalCore's section did not already cover.

### Recipes and JEI (ThermalExpansion)

- A CoFH ingredient union — `"value": [{"tag": …}, {"tag": …}]` — is not a vanilla list any more (a list holds
  item ids only); `RecipeJsonUtils.legacyIngredient` rewrites a list containing a tag into
  `{"neoforge:ingredient_type": "neoforge:compound", "children": […]}`. `MachineCatalystSerializer` (a
  `RecordCodecBuilder` codec) uses `RecipeJsonUtils.INGREDIENT_CODEC` for the same reason as the device boosts.
- `RecipeMap#getRecipesFor(RecipeType<T>, I input, Level) → Stream<RecipeHolder<T>>` is the client-side substitute
  for `RecipeManager#getRecipeFor`; `Level#recipeAccess()` is the abstract `RecipeAccess` (`propertySet`,
  `stonecutterRecipes`), `ServerLevel#recipeAccess()` is the `RecipeManager`. `ResultContainer#getRecipeUsed/setRecipeUsed(@Nullable RecipeHolder<?>)`;
  `FriendlyByteBuf#writeResourceKey/readResourceKey(ResourceKey<? extends Registry<T>>)` carries a recipe id.
- JEI 29 `IRecipeManagerPlugin`: `<V> List<IRecipeType<?>> getRecipeTypes(IFocus<V>)`, `<T, V> List<T> getRecipes(IRecipeType<T>, IFocus<V>)`,
  `<T> List<T> getRecipes(IRecipeType<T>)`; `IRecipeType` is `mezz.jei.api.recipe.types.IRecipeType` and `RecipeType`
  implements it; a category's type is `RecipeHolder<…>`, so synthesized recipes need a synthetic key.
- `FluidStack implements MutableDataComponentHolder` (`getOrDefault`, `has`); `PotionContents.EMPTY`,
  `potion() → Optional<Holder<Potion>>`; `Holder#unwrapKey()`.
- A data run has no `--existing-mod` option (there is no `ExistingFileHelper`); drop it from `build.gradle`.
- `AbstractContainerScreen.imageWidth/imageHeight` are `protected final`: a subclass sizes itself only through the
  5-arg constructor, so `MachineScreen`/`AugmentableTileScreen` gained `(…, int imageWidth, int imageHeight)` overloads.

### Custom geometry loader (ThermalDynamics duct model)

- `net.neoforged.neoforge.client.model.DelegateUnbakedModel(UnbakedModel)` is the idiom for a loader that keeps
  vanilla's parse (`ctx.deserialize(json, CuboidModel.class)`) and adds data (NeoForge `NewModelLoaderTest`).
- Renames: `BlockElement` → `net.minecraft.client.resources.model.cuboid.CuboidModelElement` (record `from/to Vector3fc`,
  `faces Map<Direction, CuboidFace>`, `rotation CuboidRotation`, `shade`, `lightEmission`), `BlockElementFace` →
  `CuboidFace` (`cullForDirection`, `tintIndex`, `texture`, `uvs`, `rotation Quadrant`), `BlockModel` → `CuboidModel`
  (`GSON` registers both deserializers; also parses `visibility`/`transform`).
- `FaceBakery.bakeQuad(ModelBaker, Vector3fc from, Vector3fc to, CuboidFace, Material.Baked, Direction, ModelState,
  @Nullable CuboidRotation, boolean shade, int lightEmission)` replaces `BlockModel.bakeFace`.
  `MaterialBaker#resolveSlot(TextureSlots, String reference, ModelDebugName)` takes the raw `"#0"` reference.
- `ResolvedModel`: `wrapped()`, `parent()`, `getTopTextureSlots()`, `getTopAmbientOcclusion()`,
  `resolveParticleMaterial(TextureSlots, ModelBaker)`; NeoForge `getTopAdditionalProperties()` →
  `ContextMap#getOrDefault(NeoForgeModelProperties.PART_VISIBILITY, Map.of())` is where `"visibility"` lives.
- A `CustomUnbakedBlockStateModel` that needs the model id wraps a `Variant` (`modelLocation()`, `modelState().asModelState()`,
  `MAP_CODEC`, `resolveDependencies`); the item half is an `ItemModel.Unbaked` baking with `BlockModelRotation.IDENTITY`.
  `ModelBaker#missingBlockModelPart()` is the fallback part; `BakedQuad.MaterialInfo#flags()` feeds a hand-built
  `BlockStateModel#materialFlags()`. `BlockStateModel#collectParts` receives the position, so `level.getModelData(pos)`
  is read there.
- Blockstate JSON for a custom model is a single variant object with `"type": "<ns>:<loader>"`, not a one-element list.
- `RenderPipelines.DEBUG_QUADS` (no cull, translucent, depth-tested, no write) is the drop-in for a translucent
  `POSITION_COLOR` quad type; `LayeringTransform.VIEW_OFFSET_Z_LAYERING` via `RenderSetupBuilder#setLayeringTransform`.
  `Direction#getNormal()` → `getUnitVec3i()` (also `getUnitVec3()`, `getUnitVec3f()`).

### Persistence, transfer, blocks (ThermalDynamics)

- NeoForge's `SavedDataType` has level-aware overloads: `SavedDataType(Identifier, Factory<T>, Factory<Codec<T>>)` with
  `Factory<T>#create(@Nullable ServerLevel)`; `SavedDataStorage` passes its `ServerLevel` to both factories and
  encodes under a `"data"` key with `registries.createSerializationContext(NbtOps.INSTANCE)`. `SavedData` is only the
  dirty flag. The file moves to `data/<id-path>.dat` (TD's grids: `thermal_dynamics/grids.dat`).
- `CompoundTag#store(String, Codec<T>, T)` / `read(String, Codec<T>)` default to `NbtOps` — the replacement for
  `NbtUtils.writeBlockPos/readBlockPos` and `putUUID/getUUID` (`BlockPos.CODEC`, `UUIDUtil.CODEC`).
  `ChunkPos#pack()` / `ChunkPos.pack(int, int)` replace `toLong()` / `asLong(int, int)`.
- `Level#markAndNotifyBlock`: `updateNeighborsAt(pos, oldState.getBlock())` when flag 1 is set (so
  `neighborChanged`'s `Block` is the notifier's *old* block), `updateNeighbourShapes` unless flag 16 — a default
  `setBlock` reaches the neighbours' `updateShape(state, LevelReader, ScheduledTickAccess, pos, direction, neighbourPos,
  neighbourState, random)`, which is where a block that needs the changed side (TD's ducts) gets it.
  `ScheduledTickAccess#scheduleTick(BlockPos, Block|Fluid, int[, TickPriority])`.
- `LevelChunk#setBlockState` calls `preRemoveSideEffects` only when `!oldState.is(newBlock)`, server side, and
  `(flags & 256) == 0` — narrower than `onRemove` was.
- `IBlockExtension#getCloneItemStack(LevelReader, BlockPos, BlockState, boolean includeData, Player)`.
- Transfer: `TransferPreconditions.checkNonEmptyNonNegative(resource, amount)`, `FluidResource#toStack(int)`/`matches`/`isEmpty()`;
  a CoFH wrapper that must serve both worlds implements `IFluidHandler, ResourceHandler<FluidResource>` (or
  `IEnergyStorage, EnergyHandler`) over the new handler and exposes the legacy view through `IFluidHandler.of` /
  `IEnergyStorage.of`; grid storages carry a `SnapshotJournal`.
- `ClientPacketDistributor.sendToServer` for client → server packets.

## Client pass (2026-09-26 to 09-28)

What a headless boot can't see, found in Joel's first `runClient`:

- **`StreamCodec.unit(x)` only encodes `x` itself** (it checks equality). A recipe whose `MapCodec.unit` builds a
  new instance per JSON entry fails the `neoforge:recipe_content` sync and disconnects the client. Share one
  `INSTANCE` for both codecs (vanilla's `RepairItemRecipe`).
- **A quad's chunk layer comes from its sprite.** `MutableQuad#setSpriteAndMoveUv(Material.Baked)` picks the
  layer from the new sprite's transparency, and layers draw solid, then cutout, then translucent. So an
  *underlay* retextured to a fluid lands in the translucent layer and covers the face meant to sit over it, while
  an *overlay* (side config, level indicators) must keep its sprite's layer: forced into solid, its transparent
  pixels draw black. `ModelUtils.retexture` stays sprite-derived; `ModelUtils.retextureUnderlay` forces
  `ChunkSectionLayer.SOLID`, used only where quads go through `addUnderlayQuad`.
- **Placing fluid from a thrown or held container without an item capability**:
  `transfer.fluid.FluidUtil.tryPlaceFluid(FluidResource, Player, Level, BlockPos, false)` places a bucket of the
  resource (`false` mirrors `BucketItem#emptyContents`). The legacy `fluids.FluidUtil.tryPlaceFluid(…, ItemStack, …)`
  goes through `Capabilities.Fluid.ITEM` and fails silently for CoFH items.
- **Dyeing is a recipe per item**: `minecraft:crafting_dye` (`DyeRecipe`, `target`/`dye`/`result`), built like
  vanilla `RecipeProvider#dyedItem` with `CustomCraftingRecipeBuilder.customCrafting`. There's no dyeable tag
  or `DyeableLeatherItem` any more.
- **Item tints come only from `items/*.json`** (`"tints": [...]`, one entry per tint index). CoFH's
  `cofh_core:colorable` source (`index` = colour index for `IColorableItem#getColor`) is the bridge.
- **`neoforge:fluid_container` needs `Capabilities.Fluid.ITEM`** (`FluidUtil.getFirstStackContained`,
  `FluidContentsTint`); its model class has a private constructor. CoFH items don't register the capability, so
  they use `cofh_core:fluid_container` (`base`, `fluid_mask`), which reads the fluid from the item.
- **`item/template_spawn_egg` is gone** (1.21.5: every egg has its own texture). CoFH ships
  `cofh_core:item/template_spawn_egg` (greyscale base + spots) for `SpawnEggItemCoFH`, tinted by `cofh_core:colorable`.
- **Item strings use components**: Patchouli's `player_head{SkullOwner:'X'}` is `player_head[profile='X']`
  (`ResolvableProfile.CODEC` takes a bare name). One unparsable icon empties the whole book.
- Recipes for items another mod may not register: `"neoforge:conditions": [{"type": "neoforge:registered",
  "value": "<item id>"}]` (registry defaults to items).
- A dev client can't open a window from Claude's shell (`glfwGetPrimaryMonitor failed`), so item models and
  anything drawn are only verifiable in Joel's own runs.

## GameTests (2026-10-02)

- **Registration is two registries.** A test *function* is a `Consumer<GameTestHelper>` in the static
  `Registries.TEST_FUNCTION` (register it in `RegisterEvent`); a test *instance* lives in the datapack registry
  `Registries.TEST_INSTANCE`, added in NeoForge's `RegisterGameTestsEvent` (fired only when GameTests are enabled):
  `event.registerEnvironment(id)` returns the environment holder, then
  `event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), new TestData<>(environment, structureId, maxTicks, setupTicks, required)))`.
- **Every test needs a structure template** from a data pack (`data/<ns>/structure/<name>.nbt`, gzipped NBT:
  `DataVersion` 4790, `size`, empty `palette`/`blocks`/`entities`). Each repo ships an empty 8×4×8 one.
- `neoforge.enabledGameTestNamespaces` is no longer read; every registered test runs. `runGameTestServer` exits with
  the number of failed required tests, so a failure fails the Gradle build.
- Tests live in a `gametest` source set (never in the jar): `addModdingDependenciesTo sourceSets.gametest`, the
  `gameTestServer` run's `sourceSet = sourceSets.gametest`, and `sourceSet sourceSets.gametest` in the mod's group.
  Other runs still boot when that output doesn't exist.
- `assertValueEqual(actual, expected, name)` prints **"Expected name to be *actual*: was *expected*"** (the
  translation's argument order), so read failures backwards.
- To test what a client receives, encode with `new RegistryFriendlyByteBuf(buf, access, ConnectionType.NEOFORGE)`.
  The two-argument constructor means a vanilla connection, where NeoForge flattens custom ingredients to item lists:
  a compound of empty tags then fails to decode, which never happens to a NeoForge client.
- **`ItemStack.EMPTY.getMaxStackSize()` is 1** (no components), where 1.20.4's was 64. Anything that asks an empty
  slot's stack for its limit needs `Item.DEFAULT_MAX_STACK_SIZE` instead.
