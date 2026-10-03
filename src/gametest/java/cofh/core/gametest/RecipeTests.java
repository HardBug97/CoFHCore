package cofh.core.gametest;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

public class RecipeTests {

    // Every recipe goes to the client in neoforge:recipe_content; one that can't round-trip disconnects the player.
    public static void recipesSync(GameTestHelper helper) {

        List<String> failures = new ArrayList<>();
        for (RecipeHolder<?> holder : helper.getLevel().getServer().getRecipeManager().getRecipes()) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                RecipeHolder.STREAM_CODEC.encode(buffer, holder);
                RecipeHolder.STREAM_CODEC.decode(buffer);
            } catch (Exception e) {
                failures.add(holder.id().identifier() + ": " + e.getMessage());
            } finally {
                buffer.release();
            }
        }
        helper.assertTrue(failures.isEmpty(), failures.size() + " recipes can't be sent to a client: " + String.join("; ", failures));
        helper.succeed();
    }

}
