package bettervillages;

import java.util.List;
import java.util.Random;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureVillagePieces;

public class ComponentBetterVillageTorch extends StructureVillagePieces.Torch {
	public ComponentBetterVillageTorch() {
	}

	public ComponentBetterVillageTorch(StructureVillagePieces.Start par1ComponentVillageStartPiece, int par2, Random par3Random, StructureBoundingBox par4StructureBoundingBox, int par5) {
		super(par1ComponentVillageStartPiece, par2, par3Random, par4StructureBoundingBox, par5);
	}

	@Override
	public boolean addComponentParts(World par1World, Random par2Random, StructureBoundingBox par3StructureBoundingBox) {
		if (this.field_143015_k < 0) {
			this.field_143015_k = this.getAverageGroundLevel(par1World, par3StructureBoundingBox);
			if (this.field_143015_k < 0) {
				return true;
			}
			this.boundingBox.offset(0, this.field_143015_k - this.boundingBox.maxY + 4 - 1, 0);
		}
		//NetherFenceAndGlowstone
		this.func_151549_a(par1World, par3StructureBoundingBox, 0, 0, 0, 0, 3, 0, Blocks.air, Blocks.air, false);
		this.func_151550_a(par1World, Blocks.nether_brick_fence, 0, 1, 0, 0, par3StructureBoundingBox);
		this.func_151550_a(par1World, Blocks.nether_brick_fence, 0, 1, 1, 0, par3StructureBoundingBox);
		this.func_151550_a(par1World, Blocks.nether_brick_fence, 0, 1, 2, 0, par3StructureBoundingBox);
		this.func_151550_a(par1World, Blocks.glowstone, 0, 1, 3, 0, par3StructureBoundingBox);
		return true;
	}

	public static ComponentBetterVillageTorch getTorch(StructureVillagePieces.Start startPiece, List<?> pieces, Random random, int p1, int p2, int p3, int p4, int p5) {
		StructureBoundingBox structureboundingbox = func_74904_a(startPiece, pieces, random, p1, p2, p3, p4);
		if (structureboundingbox != null) {
			return new ComponentBetterVillageTorch(startPiece, p5, random, structureboundingbox, p4);
		} else {
			return null;
		}
	}
}
