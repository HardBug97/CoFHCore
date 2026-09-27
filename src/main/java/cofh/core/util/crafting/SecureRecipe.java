package cofh.core.util.crafting;

import cofh.lib.api.control.ISecurable.AccessMode;
import cofh.lib.init.tags.ItemTagsCoFH;
import cofh.lib.util.helpers.SecurityHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import static cofh.core.init.CoreRecipeSerializers.SECURE_RECIPE_SERIALIZER;

public class SecureRecipe extends CustomRecipe {

    public static final SecureRecipe INSTANCE = new SecureRecipe();

    public SecureRecipe() {

    }

    @Override
    public boolean matches(CraftingInput inv, Level worldIn) {

        // boolean flag
        boolean lockItem = false;
        boolean securableItem = false;

        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ItemTagsCoFH.LOCKS)) {
                    lockItem = true;
                } else if (stack.is(ItemTagsCoFH.SECURABLE) && !SecurityHelper.hasSecurity(stack)) {
                    securableItem = true;
                }
            }
        }
        return lockItem && securableItem;
    }

    @Override
    public ItemStack assemble(CraftingInput inv) {

        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < inv.size(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ItemTagsCoFH.SECURABLE)) {
                    result = stack.copy();
                    break;
                }
            }
        }
        if (!result.isEmpty()) {
            SecurityHelper.createSecurityTag(result);
            SecurityHelper.setAccess(result, AccessMode.PUBLIC);
        }
        return result;
    }

    @Override
    public RecipeSerializer<SecureRecipe> getSerializer() {

        return SECURE_RECIPE_SERIALIZER.get();
    }

}
