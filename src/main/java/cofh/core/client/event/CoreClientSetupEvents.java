package cofh.core.client.event;

import cofh.core.client.PostEffect;
import cofh.core.client.particle.CoFHParticle;
import cofh.core.client.particle.CoFHParticleGroup;
import cofh.core.client.particle.impl.*;
import cofh.core.client.renderer.model.FluidContainerItemModel;
import cofh.core.common.fluid.ExperienceFluid;
import cofh.core.common.fluid.HoneyFluid;
import cofh.core.common.fluid.PotionFluid;
import cofh.core.util.ProxyClient;
import cofh.lib.api.item.IColorableItem;
import com.google.common.reflect.TypeToken;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleGroupsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static cofh.core.client.CoreKeys.CATEGORY;
import static cofh.core.client.CoreKeys.MULTIMODE_DECREMENT;
import static cofh.core.client.CoreKeys.MULTIMODE_INCREMENT;
import static cofh.core.init.CoreMobEffects.TRUE_INVISIBILITY;
import static cofh.core.init.CoreParticles.*;
import static cofh.lib.util.constants.ModIds.ID_COFH_CORE;

@EventBusSubscriber (value = Dist.CLIENT, modid = ID_COFH_CORE)
public class CoreClientSetupEvents {

    private static final List<Item> COLORABLE_ITEMS = new ArrayList<>();

    private static final FluidTintSource POTION_TINT = new FluidTintSource() {

        @Override
        public int color(FluidState state) {

            return 0xFFFFFFFF;
        }

        @Override
        public int colorAsStack(FluidStack stack) {

            return 0xFF000000 | PotionFluid.getPotionColor(stack);
        }
    };

    private CoreClientSetupEvents() {

    }

    @SubscribeEvent
    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {

        event.registerCategory(CATEGORY);
        event.register(MULTIMODE_INCREMENT);
        event.register(MULTIMODE_DECREMENT);
    }

    @SubscribeEvent
    public static void colorSetupItem(final RegisterColorHandlersEvent.ItemTintSources event) {

        event.register(Identifier.fromNamespaceAndPath(ID_COFH_CORE, "colorable"), ColorableItemTint.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerItemModels(final RegisterItemModelsEvent event) {

        event.register(Identifier.fromNamespaceAndPath(ID_COFH_CORE, "fluid_container"), FluidContainerItemModel.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerItemModelProperties(final RegisterRangeSelectItemModelPropertyEvent event) {

        ProxyClient.registerItemModelProperties(event);
    }

    @SubscribeEvent
    public static void registerFluidModels(final RegisterFluidModelsEvent event) {

        event.register(fluidModel("potion", POTION_TINT), PotionFluid.create().still(), PotionFluid.create().flowing());
        event.register(fluidModel("honey", null), HoneyFluid.create().still(), HoneyFluid.create().flowing());
        event.register(fluidModel("experience", null), ExperienceFluid.create().still(), ExperienceFluid.create().flowing());
    }

    @SubscribeEvent
    public static void registerRenderStateModifiers(final RegisterRenderStateModifiersEvent event) {

        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {
        }, (entity, state) -> state.setRenderData(CoreClientEvents.TRUE_INVISIBLE, entity.hasEffect(TRUE_INVISIBILITY) && entity.isInvisible()));
    }

    @SubscribeEvent
    public static void registerParticleGroups(final RegisterParticleGroupsEvent event) {

        event.register(CoFHParticle.GROUP, CoFHParticleGroup::new);
    }

    @SubscribeEvent
    public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {

        event.registerSpriteSet(FROST.get(), FrostParticle::factory);
        event.registerSpriteSet(PLASMA.get(), PlasmaBallParticle::factory);
        event.registerSpriteSet(SPARK.get(), SparkParticle::factory);

        event.registerSpriteSet(FIRE.get(), FireParticle::factory);
        event.registerSpriteSet(BLAST.get(), BlastParticle::factory);
        event.registerSpriteSet(MIST.get(), MistParticle::factory);

        event.registerSpriteSet(SHOCKWAVE.get(), ShockwaveParticle::factory);
        event.registerSpriteSet(BLAST_WAVE.get(), BlastWaveParticle::factory);
        event.registerSpriteSet(WIND_VORTEX.get(), WindVortexParticle::factory);
        event.registerSpriteSet(WIND_SPIRAL.get(), WindSpiralParticle::factory);

        event.registerSpriteSet(BEAM.get(), BeamParticle::factory);
        event.registerSpriteSet(STRAIGHT_ARC.get(), ArcParticle::factory);
        event.registerSpriteSet(SHARD.get(), ShardParticle::factory);
        event.registerSpriteSet(STREAM.get(), StreamParticle::factory);
    }

    @SubscribeEvent
    public static void registerReloadListeners(final AddClientReloadListenersEvent event) {

        int i = 0;
        for (PostEffect effect : PostEffect.getAllEffects()) {
            event.addListener(Identifier.fromNamespaceAndPath(ID_COFH_CORE, "post_effect/" + i++), effect);
        }
    }

    // region HELPERS
    public static void addColorable(Item colorable) {

        if (colorable instanceof IColorableItem) {
            COLORABLE_ITEMS.add(colorable);
        }
    }

    private static FluidModel.Unbaked fluidModel(String name, @Nullable FluidTintSource tint) {

        return new FluidModel.Unbaked(new Material(Identifier.fromNamespaceAndPath(ID_COFH_CORE, "block/fluids/" + name + "_still")), new Material(Identifier.fromNamespaceAndPath(ID_COFH_CORE, "block/fluids/" + name + "_flow")), null, tint);
    }
    // endregion

    public record ColorableItemTint(int index) implements ItemTintSource {

        public static final MapCodec<ColorableItemTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(ColorableItemTint::index)
        ).apply(instance, ColorableItemTint::new));

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {

            return stack.getItem() instanceof IColorableItem colorable ? 0xFF000000 | colorable.getColor(stack, index) : 0xFFFFFFFF;
        }

        @Override
        public MapCodec<ColorableItemTint> type() {

            return MAP_CODEC;
        }

    }

}
