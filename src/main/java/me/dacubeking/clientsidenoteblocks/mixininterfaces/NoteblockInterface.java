package me.dacubeking.clientsidenoteblocks.mixininterfaces;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface NoteblockInterface {
    Identifier clientSideNoteblocks$getCustomSoundPublic(World world, BlockPos pos);
}
