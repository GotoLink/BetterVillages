package bettervillages;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.registry.GameData;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;

/**
 * Created by Olivier on 04/11/2014.
 * A wrapper for a block type and a range of metadata
 */
public final class BlockAndMeta {
    /**
     * The block type
     */
    public final Block block;
    /**
     * Range of meta values
     */
    private final boolean[] meta = new boolean[16];

    public BlockAndMeta(String block, int... meta) {
        this(GameData.getBlockRegistry().getObject(block), meta);
    }

    public BlockAndMeta(Block block, int... meta) {
        this.block = block;
        for (int i : meta)
            this.meta[i] = true;
    }

    /**
     * Initialize with a non-specific range of meta values
     */
    public BlockAndMeta(String block) {
        this.block = GameData.getBlockRegistry().getObject(block);
        for (int i = 0; i < this.meta.length; i++)
            this.meta[i] = true;
    }

    /**
     * Helper to parse a string, following same format as #toString()
     */
    public static BlockAndMeta fromString(String blockAndMeta) {
        if (blockAndMeta.contains(" ")) {
            String[] split = blockAndMeta.split(" ");
            int[] meta = new int[split.length - 1];
            for (int i = 1; i < split.length; i++) {
                try {
                    meta[i - 1] = Integer.parseInt(split[i]);
                } catch (Exception ignored) {
                }
            }
            return new BlockAndMeta(split[0], meta);
        }
        return new BlockAndMeta(blockAndMeta);
    }

    /**
     * Check if given args are wrapped within this instance
     *
     * @param block the block type
     * @param meta  the meta value
     * @return true if this instance contains both args
     */
    public boolean contains(Block block, int meta) {
        return this.block == block && this.meta[meta];
    }

    /**
     * Trim meta range to leave only the lowest value
     */
    public BlockAndMeta trimMetaValues() {
        int count = 0;
        for (int i = 0; i < meta.length; i++) {
            if (meta[i]) {
                if (count != 0)
                    meta[i] = false;
                count++;
            }
        }
        return this;
    }

    /**
     * Get the lowest meta value within range
     * Or -1 if none is within
     */
    private int getMeta() {
        for (int i = 0; i < meta.length; i++) {
            if (meta[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null)
            return false;
        if (object == this)
            return true;
        return object instanceof BlockAndMeta && this.block == ((BlockAndMeta) object).block && Arrays.equals(this.meta, ((BlockAndMeta) object).meta);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.block).append(this.meta).toHashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(GameData.getBlockRegistry().getNameForObject(this.block).toString());
        for (int i = 0; i < meta.length; i++) {
            if (meta[i])
                builder.append(" ").append(i);
        }
        return builder.toString();
    }

    public IBlockState state() {
        int i = getMeta();
        if (i < 0)
            return null;
        return block.getStateFromMeta(i);
    }
}
