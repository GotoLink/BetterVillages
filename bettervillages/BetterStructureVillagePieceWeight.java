package bettervillages;

import net.minecraft.world.gen.structure.StructureVillagePieces;

/**
 * Modified version of village pieces weight to make for bigger villages
 */
public class BetterStructureVillagePieceWeight extends StructureVillagePieces.PieceWeight {
	public BetterStructureVillagePieceWeight(Class<?> par1Class, int par2, int par3) {
		super(par1Class, par2, par3);
	}

	@Override
	public boolean canSpawnMoreVillagePiecesOfType(int par1) {
		return this.villagePiecesLimit == 10 || this.villagePiecesSpawned < this.villagePiecesLimit;
	}

	@Override
	public boolean canSpawnMoreVillagePieces() {
		return this.villagePiecesLimit == 10 || this.villagePiecesSpawned < this.villagePiecesLimit;
	}
}
