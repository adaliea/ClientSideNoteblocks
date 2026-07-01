package me.dacubeking.clientsidenoteblocks.mixin;

import me.dacubeking.clientsidenoteblocks.mixininterfaces.NoteblockInterface;
import net.minecraft.block.Block;
import net.minecraft.block.NoteBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NoteBlock.class)
public abstract class NoteblockMixin extends Block implements NoteblockInterface {
    public NoteblockMixin(Settings settings) {
        super(settings);
    }

    @Shadow
    @Nullable
    protected abstract Identifier getCustomSound(World world, BlockPos pos);


    @Override
    public Identifier clientSideNoteblocks$getCustomSoundPublic(World world, BlockPos pos) {
        return getCustomSound(world, pos);
    }
}
