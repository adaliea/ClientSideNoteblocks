package me.dacubeking.clientsidenoteblocks.mixininterfaces;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

public interface ClientWorldInterface {


    void clientSideNoteblocks$bypassedPlaySound(@Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed);
}
