package cofh.core.client.renderer.model;

import cofh.core.client.event.CoreClientSetupEvents.ColorableItemTint;
import cofh.core.util.helpers.FluidHelper;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.model.ComposedModelState;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.UnbakedElementsHelper;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item model for CoFH fluid containers: a base layer and the contained fluid, masked by a template.
 * The fluid is read from the item itself, so no fluid capability is needed.
 */
public class FluidContainerItemModel implements ItemModel {

    // Depth offset to prevent Z-fighting
    private static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());
    private static final ModelDebugName DEBUG_NAME = () -> "FluidContainerItemModel";

    private final Unbaked unbakedModel;
    private final BakingContext bakingContext;
    private final Matrix4fc transformation;
    private final ItemTransforms itemTransforms;
    private final Map<Fluid, ItemModel> cache = new IdentityHashMap<>();

    private FluidContainerItemModel(Unbaked unbakedModel, BakingContext bakingContext, Matrix4fc transformation) {

        this.unbakedModel = unbakedModel;
        this.bakingContext = bakingContext;
        this.transformation = transformation;
        this.itemTransforms = bakingContext.blockModelBaker().getModel(Identifier.withDefaultNamespace("item/generated")).getTopTransforms();
    }

    private ItemModel bakeModelForFluid(Fluid fluid) {

        ModelBaker baker = bakingContext.blockModelBaker();
        MaterialBaker materials = baker.materials();
        ModelState state = BlockModelRotation.IDENTITY;

        Material.Baked baseSprite = materials.get(unbakedModel.base, DEBUG_NAME);
        Material.Baked fluidSprite = fluid == Fluids.EMPTY ? null : Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()).stillMaterial();
        ModelRenderProperties renderProperties = new ModelRenderProperties(false, fluidSprite != null ? fluidSprite : baseSprite, itemTransforms);

        List<ItemModel> subModels = new ArrayList<>();
        subModels.add(new CuboidItemModelWrapper(List.of(), baker.compute(new ItemModelGenerator.ItemLayerKey(baseSprite, state, 0)), renderProperties, transformation));

        if (fluidSprite != null) {
            boolean emissive = fluid.getFluidType().getLightLevel() > 0;
            RenderType renderType = emissive ? NeoForgeRenderTypes.getItemCutoutUnlit(TextureAtlas.LOCATION_BLOCKS) : Sheets.cutoutBlockItemSheet();
            BakedQuad.MaterialInfo fluidInfo = baker.interner().materialInfo(new BakedQuad.MaterialInfo(fluidSprite.sprite(), ChunkSectionLayer.SOLID, renderType, 0, !emissive, emissive ? Level.MAX_BRIGHTNESS : 0, !emissive));
            QuadCollection quads = UnbakedElementsHelper.bakeItemMaskQuads(baker, materials.get(unbakedModel.fluidMask, DEBUG_NAME), fluidInfo, new ComposedModelState(state, FLUID_TRANSFORM), ExtraFaceData.DEFAULT);
            subModels.add(new CuboidItemModelWrapper(List.of(new ColorableItemTint(1)), quads, renderProperties, transformation));
        }
        return new CompositeModel(subModels);
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {

        Fluid fluid = FluidHelper.getFluidContainedInItem(stack).getFluid();
        cache.computeIfAbsent(fluid, this::bakeModelForFluid).update(renderState, stack, modelResolver, displayContext, level, owner, seed);
    }

    public record Unbaked(Material base, Material fluidMask) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Material.CODEC.fieldOf("base").forGetter(Unbaked::base),
                Material.CODEC.fieldOf("fluid_mask").forGetter(Unbaked::fluidMask)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {

            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(BakingContext bakingContext, Matrix4fc transformation) {

            return new FluidContainerItemModel(this, bakingContext, transformation);
        }

        @Override
        public void resolveDependencies(Resolver resolver) {

        }

    }

}
