package me.dacubeking.clientsidenoteblocks.mixin;

import me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.minecraft.block.NoteBlock.INSTRUMENT;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    @Final
    private ClientPlayNetworkHandler networkHandler;

    @Redirect(method = "updateBlockBreakingProgress",  at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundManager;play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;"))
    public SoundSystem.PlayResult cancelBlockBreakSound(SoundManager instance, SoundInstance sound, BlockPos pos, Direction direction) {
        World world = this.client.world;
        ClientPlayerEntity player = this.client.player;
        if (!ClientSideNoteblocksClient.isEnabled()
                || world == null || player == null
                || player.isCreative() || player.isSpectator()
                || world.getBlockState(pos).getBlock() != Blocks.NOTE_BLOCK
                || (world.getBlockState(pos).get(INSTRUMENT).isNotBaseBlock() || !world.getBlockState(pos.up()).isAir())) {
            this.client.getSoundManager().play(sound);
        } else if (ClientSideNoteblocksClient.isDebug()) {
            ClientSideNoteblocksClient.LOGGER.info("Cancelled block break sound");
        }
        return null;
    }
}
