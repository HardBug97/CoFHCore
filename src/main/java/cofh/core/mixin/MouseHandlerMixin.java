package cofh.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static cofh.core.init.CoreMobEffects.CHILLED;

/**
 * Hooks into mouse sensitivity.
 * <p>
 * Modifies the sensitivity option as {@code turnPlayer} reads it, rather than a local by ordinal:
 * 1.20.5 gave {@code turnPlayer} a {@code double} parameter, which shifted every double local's
 * ordinal by one. The adjusted value is chosen so that vanilla's cubed sensitivity term ends up
 * multiplied by the same factor the original mixin applied to it.
 *
 * @author Hekera
 */
@Mixin (MouseHandler.class)
public abstract class MouseHandlerMixin {

    @ModifyExpressionValue (
            method = "turnPlayer",
            at = @At (
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 0
            )
    )
    private Object adjustSensitivity(Object original) {

        if (!(original instanceof Double sensitivity)) {
            return original;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return original;
        }
        MobEffectInstance effect = player.getEffect(CHILLED);
        if (effect == null) {
            return original;
        }
        int amplifier = effect.getAmplifier();
        if (amplifier < 0) {
            return original;
        }
        float reduction = 15F / (16F + amplifier);
        reduction *= reduction;
        reduction *= reduction;
        double factor = Math.max(reduction * reduction, 0.1F);
        // Vanilla: f = sensitivity * 0.6 + 0.2; the turn speed scales with f^3.
        double f = sensitivity * 0.6D + 0.2D;
        return (f * Math.cbrt(factor) - 0.2D) / 0.6D;
    }

}
