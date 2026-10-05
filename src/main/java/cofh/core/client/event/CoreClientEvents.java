package cofh.core.client.event;

import cofh.core.client.PostEffect;
import cofh.core.client.particle.CoFHParticle;
import cofh.core.common.config.CoreClientConfig;
import cofh.lib.client.renderer.entity.ITranslucentRenderer;
import cofh.lib.util.Utils;
import cofh.lib.util.constants.ModIds;
import cofh.lib.util.raytracer.VoxelShapeBlockHitResult;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Matrix4f;

import java.util.*;
import java.util.stream.Collectors;

import static cofh.core.init.CoreMobEffects.TRUE_INVISIBILITY;
import static cofh.lib.util.Constants.INVIS_STYLE;
import static cofh.lib.util.helpers.StringHelper.*;
import static net.minecraft.ChatFormatting.DARK_GRAY;
import static net.minecraft.ChatFormatting.GRAY;

@EventBusSubscriber (value = Dist.CLIENT, modid = ModIds.ID_COFH_CORE)
public class CoreClientEvents {

    public static int renderTime;
    public static float renderFrame;
    public static final Map<ParticleRenderType, Queue<CoFHParticle>> delayedRenderParticles = new Object2ObjectOpenHashMap<>();

    private static final Set<String> NAMESPACES = new ObjectOpenHashSet<>();

    public static boolean addNamespace(String namespace) {

        return NAMESPACES.add(namespace);
    }

    private CoreClientEvents() {

    }

    @SubscribeEvent
    public static void handleItemTooltipEvent(ItemTooltipEvent event) {

        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty()) {
            return;
        }
        ItemStack stack = event.getItemStack();

        if (CoreClientConfig.enableKeywords.get() && NAMESPACES.contains(Utils.getModId(stack.getItem()))) {
            String keywordKey = stack.getDescriptionId() + ".keyword";
            if (canLocalize(keywordKey)) {
                if (tooltip.get(0) instanceof MutableComponent mutable) {
                    mutable.append(getKeywordTextComponent(keywordKey));
                }
            }
        }
        if (CoreClientConfig.enableItemDescriptions.get() && NAMESPACES.contains(Utils.getModId(stack.getItem()))) {
            String infoKey = stack.getDescriptionId() + ".desc";
            if (canLocalize(infoKey)) {
                tooltip.add(1, getInfoTextComponent(infoKey));
            }
        }
        if (CoreClientConfig.enableEnchantmentDescriptions.get()) {
            ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (stored.size() == 1) {
                Holder<Enchantment> ench = stored.keySet().iterator().next();
                ResourceKey<Enchantment> key = ench.unwrapKey().orElse(null);
                if (key != null) {
                    String enchKey = Util.makeDescriptionId("enchantment", key.location()) + ".desc";
                    if (canLocalize(enchKey)) {
                        tooltip.add(getInfoTextComponent(enchKey));
                    }
                }
            }
        }
        //        if (CoreConfig.enableFoodDescriptions) {
        //            if (stack.isEdible()) {
        //
        //            }
        //        }
        if (CoreClientConfig.enableItemTags.get() && event.getFlags().isAdvanced()) {
            Item item = event.getItemStack().getItem();
            Block block = Block.byItem(item);

            Set<ResourceLocation> blockTags = block == Blocks.AIR ? Collections.emptySet() : Block.byItem(item).builtInRegistryHolder().tags().map(TagKey::location).collect(Collectors.toSet());
            Set<ResourceLocation> itemTags = item.builtInRegistryHolder().tags().map(TagKey::location).collect(Collectors.toSet());

            if (!blockTags.isEmpty() || !itemTags.isEmpty()) {
                if (Screen.hasControlDown()) {
                    if (!blockTags.isEmpty()) {
                        tooltip.add(getTextComponent("info.cofh.block_tags").withStyle(GRAY));
                        blockTags.stream()
                                .map(Object::toString)
                                .map(s -> "  " + s)
                                .map(t -> getTextComponent(t).withStyle(DARK_GRAY))
                                .forEach(tooltip::add);
                    }

                    if (!itemTags.isEmpty()) {
                        tooltip.add(getTextComponent("info.cofh.item_tags").withStyle(GRAY));
                        itemTags.stream()
                                .map(Object::toString)
                                .map(s -> "  " + s)
                                .map(t -> getTextComponent(t).withStyle(DARK_GRAY))
                                .forEach(tooltip::add);
                    }
                } else {
                    tooltip.add(getTextComponent("info.cofh.hold_ctrl_for_tags").withStyle(GRAY));
                }
            }
        }
    }

    @SubscribeEvent
    public static void handleRenderTooltipEvent(RenderTooltipEvent.GatherComponents event) {

        if (event.getTooltipElements().isEmpty()) {
            return;
        }
        event.getTooltipElements().get(0).left().ifPresent((text) -> {
            if (text instanceof MutableComponent mutable) {
                mutable.getSiblings().removeIf(string -> string.getStyle().equals(INVIS_STYLE));
            }
        });
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {

        renderTime++;
    }

    @SubscribeEvent
    public static void renderTick(RenderFrameEvent.Pre event) {

        renderFrame = event.getPartialTick().getGameTimeDeltaPartialTick(false);
    }

    @SubscribeEvent //(priority = EventPriority.LOWEST)
    public static void renderTranslucent(RenderLevelStageEvent event) {

        // POST SHADERS
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            for (PostEffect effect : PostEffect.getAllEffects()) {
                if (effect.isEnabled()) {
                    effect.begin(event.getPartialTick().getGameTimeDeltaPartialTick(false));
                }
            }
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            RenderSystem.enableBlend();
            Minecraft minecraft = Minecraft.getInstance();
            for (PostEffect effect : PostEffect.getAllEffects()) {
                if (effect.isEnabled()) {
                    effect.end(event.getPartialTick().getGameTimeDeltaPartialTick(false));
                    minecraft.getMainRenderTarget().bindWrite(false);
                    effect.apply(minecraft.getWindow());
                }
            }
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            //RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            //for (PostEffect effect : PostEffect.getAllEffects()) {
            //    if (effect.isEnabled()) {
            //    }
            //}
        }

        // PARTICLES
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            PoseStack stack = event.getPoseStack();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            Minecraft minecraft = Minecraft.getInstance();
            MultiBufferSource buffer = minecraft.renderBuffers().bufferSource();
            TextureManager manager = minecraft.getTextureManager();
            Tesselator tesselator = Tesselator.getInstance();
            LightTexture light = minecraft.gameRenderer.lightTexture();

            light.turnOnLightLayer();

            stack.pushPose();
            Vec3 pos = event.getCamera().getPosition();
            stack.translate(-pos.x, -pos.y, -pos.z);
            for (ParticleRenderType renderType : delayedRenderParticles.keySet()) {
                RenderSystem.setShader(GameRenderer::getParticleShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                Queue<CoFHParticle> particles = delayedRenderParticles.get(renderType);
                if (particles.isEmpty()) {
                    continue;
                }

                BufferBuilder particleBuffer = renderType.begin(tesselator, manager);
                while (!particles.isEmpty()) {
                    particles.poll().render(stack, buffer, particleBuffer, partialTick);
                }
                if (particleBuffer != null) {
                    // build() is null when nothing was queued for this type this frame; buildOrThrow() would throw.
                    MeshData mesh = particleBuffer.build();
                    if (mesh != null) {
                        BufferUploader.drawWithShader(mesh);
                    }
                }
            }
            stack.popPose();
            light.turnOffLightLayer();
            ITranslucentRenderer.renderTranslucent(stack, partialTick, event.getLevelRenderer(), event.getProjectionMatrix());
        }
    }

    @SubscribeEvent (priority = EventPriority.HIGH)
    public static <T extends LivingEntity, M extends EntityModel<T>> void handleTrueInvisibility(RenderLivingEvent.Pre<T, M> event) {

        LivingEntity entity = event.getEntity();
        if (entity.hasEffect(TRUE_INVISIBILITY) && entity.isInvisible()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent (priority = EventPriority.HIGH)
    public static <T extends LivingEntity, M extends EntityModel<T>> void handleTrueInvisibility(RenderHandEvent event) {

        Player player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(TRUE_INVISIBILITY) && player.isInvisible()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent (priority = EventPriority.LOW)
    public static void renderSubHitboxes(RenderHighlightEvent.Block event) {

        BlockHitResult hit = event.getTarget();
        if (hit instanceof VoxelShapeBlockHitResult voxelHit) {
            PoseStack stack = event.getPoseStack();
            BlockPos pos = voxelHit.getBlockPos();
            event.setCanceled(true);

            stack.pushPose();
            stack.translate(pos.getX(), pos.getY(), pos.getZ());

            bufferShapeHitBox(stack, event.getMultiBufferSource(), event.getCamera(), voxelHit.shape);

            stack.popPose();
        }
    }

    // region HELPERS
    private static void bufferShapeHitBox(PoseStack pStack, MultiBufferSource buffers, Camera renderInfo, VoxelShape shape) {

        Vec3 eye = renderInfo.getPosition();
        pStack.translate((float) -eye.x, (float) -eye.y, (float) -eye.z);
        bufferShapeOutline(buffers.getBuffer(RenderType.lines()), pStack.last().pose(), shape, 0.0F, 0.0F, 0.0F, 0.4F);
    }

    private static void bufferShapeOutline(VertexConsumer builder, Matrix4f mat, VoxelShape shape, float r, float g, float b, float a) {

        shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            double xn = x1 - x2;
            double yn = y1 - y2;
            double zn = z1 - z2;
            double d = Math.sqrt(xn * xn + yn * yn + zn * zn);
            xn /= d;
            yn /= d;
            zn /= d;

            builder.addVertex(mat, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal((float) xn, (float) yn, (float) zn);
            builder.addVertex(mat, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal((float) xn, (float) yn, (float) zn);
        });
    }
    // endregion
}
