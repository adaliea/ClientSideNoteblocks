package me.dacubeking.clientsidenoteblocks.client;

import me.dacubeking.clientsidenoteblocks.expiringmap.SelfExpiringHashMap;
import me.dacubeking.clientsidenoteblocks.mixininterfaces.ClientWorldInterface;
import me.dacubeking.clientsidenoteblocks.mixininterfaces.NoteblockInterface;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.NoteBlock;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static net.minecraft.block.NoteBlock.INSTRUMENT;
import static net.minecraft.block.NoteBlock.NOTE;

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

    @Override
    public void onInitializeClient() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        lastMaxTimeToServerSound = config.maxTimeToServerSound;
        NOTEBLOCK_SOUNDS_TO_CANCEL = new SelfExpiringHashMap<>((long) (config.maxTimeToServerSound * 1000), 100);


        KeyBinding toggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("Toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, "Client Side Noteblocks"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeybind.wasPressed()) {
                if (client.player == null) return;

                config.enabled = !config.enabled;
                if (config.enabled) {
                    client.player.sendMessage(Text.translatableWithFallback("text.clientsidenoteblocks.chat.enabled",
                            "ClientSideNoteblocks is Enabled"), false);

                } else {
                    client.player.sendMessage(Text.translatableWithFallback("text.clientsidenoteblocks.chat.disabled",
                            "ClientSideNoteblocks is Disabled"), false);
                }
            }
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (config.enabled && lastMaxTimeToServerSound != config.maxTimeToServerSound) {
                lastMaxTimeToServerSound = config.maxTimeToServerSound;
                NOTEBLOCK_SOUNDS_TO_CANCEL = new SelfExpiringHashMap<>((long) (config.maxTimeToServerSound * 1000), 100);
                LOGGER.info("Max time to server sound changed to " + lastMaxTimeToServerSound);
            }
            
            if (!isEnabled()) return ActionResult.PASS;
            if (world.isClient && !player.isCreative() && !player.isSpectator()
                    && world.getBlockState(pos).getBlock().getClass() == NoteBlock.class) {
                BlockState state = world.getBlockState(pos);

                if (MinecraftClient.getInstance().world != null &&
                        (state.get(INSTRUMENT).isNotBaseBlock() || world.getBlockState(pos.up()).isAir())) {
                    ClientWorldInterface clientWorldInterface = ((ClientWorldInterface) MinecraftClient.getInstance().world);

                    RegistryEntry<SoundEvent> registryEntry;
                    float f;
                    NoteBlockInstrument instrument = state.get(INSTRUMENT);
                    if (instrument.canBePitched()) {
                        int i = state.get(NOTE);
                        f = NoteBlock.getNotePitch(i);
                    } else {
                        f = 1.0f;
                    }

                    if (instrument.hasCustomSound()) {
                        Identifier identifier = ((NoteblockInterface) state.getBlock()).clientSideNoteblocks$getCustomSoundPublic(world, pos);
                        if (identifier == null) {
                            return ActionResult.PASS;
                        }
                        registryEntry = RegistryEntry.of(SoundEvent.of(identifier));
                    } else {
                        registryEntry = instrument.getSound();
                    }

                    clientWorldInterface.clientSideNoteblocks$bypassedPlaySound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, registryEntry, SoundCategory.RECORDS, 3.0f, f, world.random.nextLong());


                    synchronized (NOTEBLOCK_SOUNDS_TO_CANCEL_LOCK) {
                        if (NOTEBLOCK_SOUNDS_TO_CANCEL.containsKey(pos)) {
                            NOTEBLOCK_SOUNDS_TO_CANCEL.get(pos).addAndGet(2);
                        } else {
                            NOTEBLOCK_SOUNDS_TO_CANCEL.put(pos, new AtomicInteger(2));
                        }
                    }
                }


            }
            return ActionResult.PASS;
        });
    }
}
