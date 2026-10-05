package cofh.lib.common.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ArmorMaterialCoFH {

    private ArmorMaterialCoFH() {

    }

    public static ArmorMaterial create(int[] damageReductionAmountsIn, int enchantabilityIn, Holder<SoundEvent> equipSoundIn, float toughnessIn, float knockbackResistanceIn, Supplier<Ingredient> repairMaterialSupplier) {

        return create(null, damageReductionAmountsIn, enchantabilityIn, equipSoundIn, toughnessIn, knockbackResistanceIn, repairMaterialSupplier);
    }

    /**
     * @param texture Armor texture name, resolved by vanilla to {@code <namespace>:textures/models/armor/<path>_layer_<1|2>.png}.
     *                Without a layer, worn armor renders nothing - 1.20's material name played this role.
     */
    public static ArmorMaterial create(ResourceLocation texture, int[] damageReductionAmountsIn, int enchantabilityIn, Holder<SoundEvent> equipSoundIn, float toughnessIn, float knockbackResistanceIn, Supplier<Ingredient> repairMaterialSupplier) {

        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        ArmorItem.Type[] types = ArmorItem.Type.values();
        for (int i = 0; i < types.length && i < damageReductionAmountsIn.length; ++i) {
            defense.put(types[i], damageReductionAmountsIn[i]);
        }
        return new ArmorMaterial(defense, enchantabilityIn, equipSoundIn, repairMaterialSupplier, texture == null ? List.of() : List.of(new ArmorMaterial.Layer(texture)), toughnessIn, knockbackResistanceIn);
    }

}
