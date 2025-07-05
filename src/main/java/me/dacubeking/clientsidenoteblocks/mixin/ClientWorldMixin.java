package me.dacubeking.clientsidenoteblocks.mixin;

import me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient;
import me.dacubeking.clientsidenoteblocks.mixininterfaces.ClientWorldInterface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient.NOTEBLOCK_SOUNDS_TO_CANCEL;
import static me.dacubeking.clientsidenoteblocks.client.ClientSideNoteblocksClient.NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin extends World implements ClientWorldInterface {

    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    @Final
    private static double PARTICLE_Y_OFFSET;


    // Ignored by Mixin
    protected ClientWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), cancellable = true)
    public void playSound(double x, double y, double z, SoundEvent event, SoundCategory category, float volume, float pitch, boolean useDistance, long seed, CallbackInfo ci) {
        BlockPos pos = new BlockPos((int) (x - 0.5), (int) (y - 0.5), (int) (z - 0.5));

        if (ClientSideNoteblocksClient.isEnabled()) {
            synchronized (NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK) {
                if (NOTEBLOCK_SOUNDS_TO_CANCEL.containsKey(pos)) {
                    AtomicInteger amount = NOTEBLOCK_SOUNDS_TO_CANCEL.get(pos);
                    amount.getAndUpdate(i -> {
                        if (i > 0) {
                            if (ClientSideNoteblocksClient.isDebug()) {
                                ClientSideNoteblocksClient.LOGGER.info("Cancelled server note block sound");
                            }
                            ci.cancel();
                            return i - 1;
                        } else {
                            if (ClientSideNoteblocksClient.isDebug()) {
                                ClientSideNoteblocksClient.LOGGER.info("Detected an extra server note block sound");
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
            @Nullable PlayerEntity except,
            double x, double y, double z,
            RegistryEntry<SoundEvent> sound,
            SoundCategory category,
            float volume, float pitch, long seed) {
        if (ClientSideNoteblocksClient.isDebug()) {
            ClientSideNoteblocksClient.LOGGER.info("Bypassed played sound");
        }
        PositionedSoundInstance positionedSoundInstance = new PositionedSoundInstance(sound.value(), category, volume, pitch, Random.create(seed), x, y, z);

        this.client.getSoundManager().play(positionedSoundInstance);
    }

    @Shadow
    private void playSound(double x, double y, double z, SoundEvent event, SoundCategory category, float volume, float pitch, boolean useDistance, long seed) {

    }
}
