package me.dacubeking.clientsidenoteblocks.mixin;

import me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient;
import me.dacubeking.clientsidenoteblocks.mixininterfaces.ClientLevelInterface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;

import static me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient.NOTEBLOCK_SOUNDS_TO_CANCEL;
import static me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient.NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK;
import static net.minecraft.world.level.block.NoteBlock.INSTRUMENT;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level implements ClientLevelInterface {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Shadow
    @Final
    private static double FLUID_PARTICLE_SPAWN_OFFSET;


    // Ignored by Mixin
    protected ClientLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZJ)V", at = @At("HEAD"), cancellable = true)
    public void playSound(double x, double y, double z, SoundEvent event, SoundSource category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        BlockPos pos = new BlockPos((int) (x - 0.5), (int) (y - 0.5), (int) (z - 0.5));

        if (ClientSideNoteblocksClient.isEnabled()) {
            synchronized (NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK) {
                if (NOTEBLOCK_SOUNDS_TO_CANCEL.containsKey(pos)) {
                    AtomicInteger amount = NOTEBLOCK_SOUNDS_TO_CANCEL.get(pos);
                    amount.getAndUpdate(i -> {
                        if (i > 0) {
                            if (ClientSideNoteblocksClient.isDebug()) {
                                ClientSideNoteblocksClient.LOGGER.info("Cancelled server note block sound. Remaining: " + i);
                            }
                            ci.cancel();
                            return i - 1;
                        } else {
                            if (ClientSideNoteblocksClient.isDebug()) {
                                ClientSideNoteblocksClient.LOGGER.info("Detected an extra server note block sound. Remaining:" + i);
                            }
                            if (ClientSideNoteblocksClient.shouldCancelStraySounds()) {
                                ci.cancel();
                            }
                            return 0;
                        }
                    });
                }
            }
        }
    }


    @Override
    public void clientSideNoteblocks$bypassedPlaySound(
            @Nullable Player except,
            double x, double y, double z,
            Holder<SoundEvent> sound,
            SoundSource category,
            float volume, float pitch, long seed) {
        if (ClientSideNoteblocksClient.isDebug()) {
            ClientSideNoteblocksClient.LOGGER.info("Bypassed played sound");
        }
        SimpleSoundInstance positionedSoundInstance = new SimpleSoundInstance(sound.value(), category, volume, pitch, RandomSource.create(seed), x, y, z);

        this.minecraft.getSoundManager().play(positionedSoundInstance);
    }

    /**
     * 26.3 moved the block mining sound off MultiPlayerGameMode.continueDestroyBlock and into
     * ClientLevel.playBreakingSound, which is now the only place it is produced: the first hit
     * reaches it through addBreakingBlockEffects, and every repeat after that arrives from the
     * server as level event 2020 (PARTICLES_AND_SOUND_DESTROY_PROGRESS) rather than being
     * generated client side. Redirecting here therefore covers what the redirect on
     * continueDestroyBlock used to cover, and nothing else plays that sound any more.
     */
    @Redirect(method = "playBreakingSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"))
    public SoundEngine.PlayResult cancelBlockBreakSound(SoundManager instance, SoundInstance sound, BlockPos pos, BlockState state) {
        Level world = this.minecraft.level;
        LocalPlayer player = this.minecraft.player;
        if (!ClientSideNoteblocksClient.isEnabled()
                || world == null || player == null
                || player.isCreative() || player.isSpectator()
                || state.getBlock() != Blocks.NOTE_BLOCK
                || (state.getValue(INSTRUMENT).worksAboveNoteBlock() || !world.getBlockState(pos.above()).isAir())) {
            this.minecraft.getSoundManager().play(sound);
        } else if (ClientSideNoteblocksClient.isDebug()) {
            ClientSideNoteblocksClient.LOGGER.info("Cancelled block break sound");
        }
        return null;
    }

    @Shadow
    private void playSound(double x, double y, double z, SoundEvent event, SoundSource category, float volume, float pitch, boolean useDistance, long seed) {

    }
}
