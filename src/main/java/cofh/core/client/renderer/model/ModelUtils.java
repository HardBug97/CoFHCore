package cofh.core.client.renderer.model;

import cofh.core.util.helpers.FluidHelper;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.model.data.ModelProperty;

import java.util.*;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.Constants.DIRECTIONS;

public class ModelUtils {

    private ModelUtils() {

    }

    public static final ModelProperty<Direction> FACING = new ModelProperty<>();
    public static final ModelProperty<FluidStack> FLUID = new ModelProperty<>();
    public static final ModelProperty<Integer> LEVEL = new ModelProperty<>();
    public static final ModelProperty<byte[]> SIDES = new ModelProperty<>();
    public static final ModelProperty<Identifier> UNDERLAY = new ModelProperty<>();

    // region QUADS
    public static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {

        // Keep the source quad's layer, so underlays still draw beneath the faces over them.
        BakedQuad.MaterialInfo info = quad.materialInfo();
        return new MutableQuad().setFrom(quad).setSpriteAndMoveUv(sprite, info.layer(), info.itemRenderType()).toBakedQuad();
    }

    public static List<BakedQuad> getAllQuads(BlockStateModelPart model) {

        List<BakedQuad> quads = new ArrayList<>(model.getQuads(null));
        for (Direction dir : DIRECTIONS) {
            quads.addAll(model.getQuads(dir));
        }
        return quads;
    }
    // endregion

    public static class WrappedBakedModelBuilder {

        private final List<BakedQuad> builderGeneralQuads = new ArrayList<>();
        private final Map<Direction, List<BakedQuad>> builderUnderlayQuads = new EnumMap<>(Direction.class);
        private final Map<Direction, List<BakedQuad>> builderFaceQuads = new EnumMap<>(Direction.class);
        private final boolean builderAmbientOcclusion;
        private Material.Baked builderTexture;

        public WrappedBakedModelBuilder(BlockStateModelPart model) {

            for (Direction dir : DIRECTIONS) {
                this.builderUnderlayQuads.put(dir, new ArrayList<>());
                this.builderFaceQuads.put(dir, new LinkedList<>(model.getQuads(dir)));
            }
            this.builderGeneralQuads.addAll(model.getQuads(null));

            builderAmbientOcclusion = model.useAmbientOcclusion();
            builderTexture = model.particleMaterial();
        }

        public WrappedBakedModelBuilder addUnderlayQuad(Direction facing, BakedQuad quad) {

            this.builderUnderlayQuads.get(facing).add(quad);
            return this;
        }

        public WrappedBakedModelBuilder addFaceQuad(Direction facing, BakedQuad quad) {

            this.builderFaceQuads.get(facing).add(quad);
            return this;
        }

        public WrappedBakedModelBuilder addGeneralQuad(BakedQuad quad) {

            this.builderGeneralQuads.add(quad);
            return this;
        }

        public List<BakedQuad> getQuads(Direction facing) {

            return facing == null ? builderGeneralQuads : builderFaceQuads.get(facing);
        }

        public WrappedBakedModelBuilder setTexture(Material.Baked texture) {

            this.builderTexture = texture;
            return this;
        }

        public BlockStateModelPart build() {

            if (this.builderTexture == null) {
                throw new RuntimeException("Missing particle!");
            } else {
                QuadCollection.Builder builder = new QuadCollection.Builder();
                for (BakedQuad quad : builderGeneralQuads) {
                    builder.addUnculledFace(quad);
                }
                for (Direction dir : DIRECTIONS) {
                    for (BakedQuad quad : builderUnderlayQuads.get(dir)) {
                        builder.addCulledFace(dir, quad);
                    }
                    for (BakedQuad quad : builderFaceQuads.get(dir)) {
                        builder.addCulledFace(dir, quad);
                    }
                }
                return new SimpleModelWrapper(builder.build(), this.builderAmbientOcclusion, this.builderTexture);
            }
        }

    }

    public static class FluidCacheWrapper {

        BlockState state;
        FluidStack stack;

        public FluidCacheWrapper(BlockState state, FluidStack stack) {

            this.state = state;
            this.stack = stack.copyWithAmount(BUCKET_VOLUME);
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FluidCacheWrapper that = (FluidCacheWrapper) o;
            return Objects.equals(state, that.state) && Objects.equals(stack, that.stack);
        }

        @Override
        public int hashCode() {

            return Objects.hash(state, FluidHelper.fluidHashcode(stack));
        }

    }

}
