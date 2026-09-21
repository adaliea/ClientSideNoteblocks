package me.dacubeking.clientsidenoteblocks.client;

import me.dacubeking.clientsidenoteblocks.expiringmap.SelfExpiringHashMap;
import me.dacubeking.clientsidenoteblocks.mixininterfaces.ClientLevelInterface;
import me.dacubeking.clientsidenoteblocks.mixininterfaces.NoteblockInterface;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static net.minecraft.world.level.block.NoteBlock.INSTRUMENT;
import static net.minecraft.world.level.block.NoteBlock.NOTE;

import com.mojang.blaze3d.platform.InputConstants;

@Environment(EnvType.CLIENT)
public class ClientSideNoteblocksClient implements ClientModInitializer {

    public static ModConfig config;

    public static final Logger LOGGER = Logger.getLogger("ClientSideNoteblocks");

    public static boolean isDebug() {
        return config.debug;
    }

    public static boolean isEnabled() {
        return config.enabled;
    }

    public static boolean shouldCancelStraySounds() {
        return config.alwaysCancelPlayedNoteblockServerSounds;
    }


    public static final Object NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK = new Object();
    public static SelfExpiringHashMap<BlockPos, AtomicInteger> NOTEBLOCK_SOUNDS_TO_CANCEL = new SelfExpiringHashMap<>(50000, 100);

    private double lastMaxTimeToServerSound = 0;


    public static String namespace = "clientsidenoteblocks";
    public static KeyMapping.Category keybindCategory = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(namespace, "keybinds"));
    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        lastMaxTimeToServerSound = config.maxTimeToServerSound;
        NOTEBLOCK_SOUNDS_TO_CANCEL = new SelfExpiringHashMap<>((long) (config.maxTimeToServerSound * 1000), 100);


        KeyMapping toggleKeybind = KeyMappingHelper.registerKeyMapping(new KeyMapping("Toggle", InputConstants.Type.KEYBOARD, InputConstants.KEY_LBRACKET, keybindCategory));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeybind.consumeClick()) {
                if (client.player == null) return;

                config.enabled = !config.enabled;
                if (config.enabled) {
                    client.player.sendSystemMessage(Component.translatableWithFallback("text.clientsidenoteblocks.chat.enabled",
                            "ClientSideNoteblocks is Enabled"));

                } else {
                    client.player.sendSystemMessage(Component.translatableWithFallback("text.clientsidenoteblocks.chat.disabled",
                            "ClientSideNoteblocks is Disabled"));
                }
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (config.enabled && lastMaxTimeToServerSound != config.maxTimeToServerSound) {
                lastMaxTimeToServerSound = config.maxTimeToServerSound;
                NOTEBLOCK_SOUNDS_TO_CANCEL = new SelfExpiringHashMap<>((long) (config.maxTimeToServerSound * 1000), 100);
                LOGGER.info("Max time to server sound changed to " + lastMaxTimeToServerSound);
            }
            
            if (!isEnabled()) return InteractionResult.PASS;
            if (world.isClientSide() && !player.isCreative() && !player.isSpectator()
                    && world.getBlockState(pos).getBlock().getClass() == NoteBlock.class) {
                BlockState state = world.getBlockState(pos);

                if (Minecraft.getInstance().level != null &&
                        (state.getValue(INSTRUMENT).worksAboveNoteBlock() || world.getBlockState(pos.above()).isAir())) {
                    ClientLevelInterface clientLevelInterface = ((ClientLevelInterface) Minecraft.getInstance().level);

                    Holder<SoundEvent> registryEntry;
                    float f;
                    NoteBlockInstrument instrument = state.getValue(INSTRUMENT);
                    if (instrument.isTunable()) {
                        int i = state.getValue(NOTE);
                        f = NoteBlock.getPitchFromNote(i);
                    } else {
                        f = 1.0f;
                    }

                    if (instrument.hasCustomSound()) {
                        Identifier identifier = ((NoteblockInterface) state.getBlock()).clientSideNoteblocks$getCustomSoundPublic(world, pos);
                        if (identifier == null) {
                            return InteractionResult.PASS;
                        }
                        registryEntry = Holder.direct(SoundEvent.createVariableRangeEvent(identifier));
                    } else {
                        registryEntry = instrument.getSoundEvent();
                    }

                    clientLevelInterface.clientSideNoteblocks$bypassedPlaySound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, registryEntry, SoundSource.RECORDS, 3.0f, f, world.getRandom().nextLong());


                    synchronized (NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK) {
                        if (NOTEBLOCK_SOUNDS_TO_CANCEL.containsKey(pos)) {
                            NOTEBLOCK_SOUNDS_TO_CANCEL.get(pos).addAndGet(1);
                        } else {
                            NOTEBLOCK_SOUNDS_TO_CANCEL.put(pos, new AtomicInteger(1));
                        }
                    }
                }


            }
            return InteractionResult.PASS;
        });
    }
}
