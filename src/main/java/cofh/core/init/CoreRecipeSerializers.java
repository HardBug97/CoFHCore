package cofh.core.init;

import cofh.core.util.crafting.SecureRecipe;
import cofh.core.util.crafting.ShapedPotionNBTRecipe;
import cofh.lib.common.conditions.FlagSetCondition;
import cofh.lib.common.conditions.TagExistsCondition;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;

import static cofh.core.CoFHCore.CONDITION_CODECS;
import static cofh.core.CoFHCore.RECIPE_SERIALIZERS;
import static cofh.core.util.references.CoreIDs.ID_CRAFTING_POTION;
import static cofh.core.util.references.CoreIDs.ID_CRAFTING_SECURABLE;

public class CoreRecipeSerializers {

    private CoreRecipeSerializers() {

    }

    public static void register() {

    }

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<FlagSetCondition>> FLAG_SET_CONDITION = CONDITION_CODECS.register("flag_set", () -> FlagSetCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<TagExistsCondition>> TAG_EXISTS_CONDITION = CONDITION_CODECS.register("tag_exists", () -> TagExistsCondition.CODEC);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapedPotionNBTRecipe>> SHAPED_POTION_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(ID_CRAFTING_POTION, () -> new RecipeSerializer<>(ShapedPotionNBTRecipe.CODEC, ShapedPotionNBTRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SecureRecipe>> SECURE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(ID_CRAFTING_SECURABLE, () -> new RecipeSerializer<>(MapCodec.unit(SecureRecipe.INSTANCE), StreamCodec.unit(SecureRecipe.INSTANCE)));

}
