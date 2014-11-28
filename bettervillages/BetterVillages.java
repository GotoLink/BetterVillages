package bettervillages;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry.IVillageCreationHandler;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenDesert;
import net.minecraft.world.biome.BiomeGenOcean;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureVillagePieces;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = "bettervillages", name = "Better Villages Mod", acceptableRemoteVersions = "*")
public final class BetterVillages {
	public static final Block FLAG_ID = Blocks.planks;
	public static Block pathWay, fieldFence;
	public static boolean lilies = true, fields = true, gates = true, wells = true, woodHut = true, torch = true, big = true, replace = true;
	public static final String biomeNames = "DesertHills,Extreme Hills,Extreme Hills Edge,Jungle,JungleHills,Ocean,Swampland,Taiga,TaigaHills,Ice Plains,Ice Mountains,Forest";
	public static Biomes biomes;
    public static final List<IVillageCreationHandler> handlers = new ArrayList<IVillageCreationHandler>();
	static {
		handlers.add(new VillageCreationHandler(StructureVillagePieces.House4Garden.class, 4, 2, 4, 2));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.Church.class, 20, 0, 1, 1));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.WoodHut.class, 3, 2, 5, 3));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.Hall.class, 15, 0, 2, 1));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.Field1.class, 3, 1, 4, 1));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.Field2.class, 3, 2, 4, 2));
        handlers.add(new VillageCreationHandler(StructureVillagePieces.House1.class, 20, 0, 2, 1));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.House2.class, 15, 0, 1, 1));
		handlers.add(new VillageCreationHandler(StructureVillagePieces.House3.class, 8, 0, 3, 2));
	}
    private static VillageBlockReplacement replacer;
    /**
     * Load configuration parameters, using defaults if necessary
     */
	@EventHandler
	public void configLoad(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(event.getSuggestedConfigurationFile());
		pathWay = GameData.getBlockRegistry().getObject(config.getString("Ocean_villages_path", "general", "planks", "Block used for streets of villages built in Ocean biome"));
        if(pathWay == Blocks.air){
            pathWay = FLAG_ID;
        }
        StringBuilder build = new StringBuilder("Available biome tags are: ");
        for (BiomeDictionary.Type t : BiomeDictionary.Type.values()) {
            build.append(t);
            build.append(",");
        }
        config.addCustomCategoryComment("general", build.toString());
        biomes = new Biomes(config.get("General", "Available_biomes", biomeNames,
                "Biomes where villages should be added, use ALL or * for all biomes, select with biome name or biome tags, prefix with - to exclude")
                .getString().split(","));
		lilies = config.getBoolean("Spawn_waterlily", "general", lilies, "Water lilies can be found on water in villages");
		wells = config.getBoolean("Decorate_wells", "general", wells, "Village wells should be improved");
		fields = config.getBoolean("Decorate_fields", "general", fields, "Village fields should be improved");
		woodHut = config.getBoolean("Decorate_huts", "general", woodHut, "Village wood huts should be improved");
		gates = config.getBoolean("Add_gates", "general", gates, "Fence gates added to village fields");
        fieldFence = GameData.getBlockRegistry().getObject(config.get("general", "Villages_fields_fencing", "fence", "Block used for fencing villages fields").getString());
        if(fieldFence == Blocks.air){
            fieldFence = Blocks.fence;
        }
		torch = config.getBoolean("Add_new_torch", "general", torch, "Better torch has a chance to appear in villages");
		big = config.getBoolean("Bigger_Villages", "general", big, "Villages generates in clusters");
        replace = config.getBoolean("Load_Replacement_Module", "general", replace, "Attempt to load block replacement rules from json");
		if (config.hasChanged())
			config.save();
        if(event.getSourceFile().getName().endsWith(".jar") && event.getSide().isClient()){
            try {
                Class.forName("mods.mud.ModUpdateDetector").getDeclaredMethod("registerMod", ModContainer.class, String.class, String.class).invoke(null,
                        FMLCommonHandler.instance().findContainerFor(this),
                        "https://raw.github.com/GotoLink/BetterVillages/master/update.xml",
                        "https://raw.github.com/GotoLink/BetterVillages/master/changelog.md"
                );
            } catch (Throwable ignored) {
            }
        }
	}

    /**
     * Add village to biome
     * Register event listeners, village handler, new torch component
     */
	@EventHandler
	public void load(FMLInitializationEvent event) {
		if (biomes != null && !biomes.isEmpty()) {
			for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
				if (biome != null && biomes.contains(biome)) {
					BiomeManager.addVillageBiome(biome, true);//boolean has no effect ?
				}
			}
		}
		MinecraftForge.EVENT_BUS.register(this);//For the populating event
        if (replace || biomes.hasName(BiomeGenBase.ocean.biomeName)|| biomes.hasName(BiomeGenBase.deepOcean.biomeName)){
		    MinecraftForge.TERRAIN_GEN_BUS.register(this);//For the pathway / replacement module
        }
		if (torch) {
            MapGenStructureIO.func_143031_a(ComponentBetterVillageTorch.class, "BViT");
			VillagerRegistry.instance().registerVillageCreationHandler(new VillageCreationHandler(ComponentBetterVillageTorch.class, 15, 0, 1, 1));
		}
		if (big) {
			for (IVillageCreationHandler handler : handlers) {
				VillagerRegistry.instance().registerVillageCreationHandler(handler);
			}
		}
        if(replace){
            replacer = new VillageBlockReplacement("bettervillages");
        }
	}

    /**
     * Listen to the populating event.
     * Decorates stuff in villages
     *
     * @param event The populating event
     */
	@SubscribeEvent
	public void onPopulating(PopulateChunkEvent.Post event) {
		if (event.hasVillageGenerated) {
			int i = event.chunkX * 16 + 8;//Villages are offset
			int k = event.chunkZ * 16 + 8;
			int y;
            Block id;
			int[] field;
			List<int[]> list;
            BiomeGenBase biome = event.world.getBiomeGenForCoords(i, k);
            Block borderId = getEquivalentVillageBlock(Blocks.log, biome);
            Block cobbleEquivalent = null;
            if (wells)
                cobbleEquivalent = getEquivalentVillageBlock(Blocks.cobblestone, null);//Wells aren't biome specific
			for (int x = i; x < i + 16; x++) {
				for (int z = k; z < k + 16; z++) {//Search within chunk
					if (biome instanceof BiomeGenOcean) {
						y = event.world.getTopSolidOrLiquidBlock(x, z) - 1;//ignores water
						id = event.world.getBlock(x, y, z);
						if (id == Blocks.wool) {
							//Definitely a common village torch
                            if (hasAround(event.world, Blocks.torch, x, y-1, z) && isReplaceable(event.world, x, y - 4, z))
								event.world.setBlock(x, y - 4, z, pathWay);//Add support below
							continue;
						}
						if (id == Blocks.oak_stairs) {
							do {
								y--;
                                id = event.world.getBlock(x, y, z);
							} while (id.isAir(event.world, x, y, z) || isWaterId(id));
						}
						if (id == FLAG_ID) {//found flag
							id = event.world.getBlock(x, y + 1, z);
							if (isWaterId(id)) {//underwater
								event.world.setBlock(x, y, z, id, 0, 2);//destroy flag
								while (!event.world.isAirBlock(x, y, z))
									y++;
								event.world.setBlock(x, y, z, pathWay);//rebuilt pathway on top of water
							}
							continue;
						}
					}
					y = event.world.getHeightValue(x, z);//block on top of a "solid" block
					if (y > 1) {
						y--;
						id = event.world.getBlock(x, y, z);
						while (id.isAir(event.world, x, y, z) || id.isLeaves(event.world, x, y, z)) {
							y--;
							id = event.world.getBlock(x, y, z);
						}
						if (isWaterId(id)) {//found water in open air
							if (lilies && event.world.isAirBlock(x, y + 1, z) && event.rand.nextInt(10) == 0)
								event.world.setBlock(x, y + 1, z, Blocks.waterlily);//place waterlily randomly
							if (gates) {
								field = new int[] { x, y, z };
								list = getBorder(event.world, id, field);
								if (list.size() == 1) {//found 2 water blocks
									list = getBorder(event.world, borderId, field);
									if (list.size() == 3) {//found a 3 blocks border, assuming water in a village field
										field = list.get(1);//get middle border block
										if (isReplaceable(event.world, field[0], field[1] + 1, field[2])) {
											//find orientation for fence gate
											int p = 0;//south
											if (x - field[0] < 0)
												p = 1;//west
											else if (x - field[0] > 0)
												p = 3;//east
											else if (z - field[2] < 0)
												p = 2;//north
											event.world.setBlock(field[0], field[1] + 1, field[2], Blocks.fence_gate, p, 3);//place fence gate
										}
									}
								}
							}
							continue;
						}
						if (fields && id == Blocks.farmland) {//found tilled field in open air, assuming this is a village field
							field = new int[] { x, y, z };
							list = getBorder(event.world, borderId, field);
							if (!list.isEmpty()) {
								switch (list.size()) {
								case 3://simple border case
									field = list.get(1);//get middle border block
									if (isReplaceable(event.world, field[0], field[1] + 1, field[2]))
										event.world.setBlock(field[0], field[1] + 1, field[2], fieldFence);//place fence on top
									break;
								case 5://corner case
									field = list.remove(1);
									if (isReplaceable(event.world, field[0], field[1] + 1, field[2]))
										event.world.setBlock(field[0], field[1] + 1, field[2], fieldFence);
									field = list.remove(2);
									if (isReplaceable(event.world, field[0], field[1] + 1, field[2]))
										event.world.setBlock(field[0], field[1] + 1, field[2], fieldFence);
									for (int[] pos : list) {
										if (isReplaceable(event.world, pos[0], pos[1] + 1, pos[2])) {
											event.world.setBlock(pos[0], pos[1] + 1, pos[2], fieldFence);
											if (isReplaceable(event.world, pos[0], pos[1] + 2, pos[2]) && isCorner(event.world, borderId, pos))
												event.world.setBlock(pos[0], pos[1] + 2, pos[2], Blocks.torch);
										}
									}
									break;
								default:
									break;
								}
							}
							continue;
						}
						if (wells && id == cobbleEquivalent) {//found cobblestone in open air
                            id = event.world.getBlock(x, y - 4, z);
                            if (isWaterId(id)) {//found water under cobblestone layer
                                y -= 4;
                                field = new int[]{x, y, z};
                                list = getBorder(event.world, id, field);
                                if (list.size() == 3) {//found 4 water blocks
                                    list = getBorder(event.world, cobbleEquivalent, field);
                                    if (list.size() == 5) {//found 5 cobblestone surrounding one water block, assuming this is a village well
                                        field = list.remove(1);
                                        event.world.setBlock(field[0], field[1] + 1, field[2], Blocks.stone_slab);
                                        field = list.remove(2);
                                        event.world.setBlock(field[0], field[1] + 1, field[2], Blocks.stone_slab);
                                        for (int[] pos : list) {
                                            for (int[] posb : getBorder(event.world, Blocks.gravel, pos))
                                                event.world.setBlock(posb[0], posb[1], posb[2], Blocks.stone_slab);
                                        }
                                        while (event.world.getBlock(x, y, z) == id) {
                                            y--;
                                        }
                                        field = new int[]{x, y, z};
                                        list = getBorder(event.world, cobbleEquivalent, field);
                                        for (int[] pos : list)
                                            event.world.setBlock(pos[0], pos[1], pos[2], Blocks.iron_block);
                                        event.world.setBlock(field[0], field[1], field[2], Blocks.iron_block);
                                    }
                                }
                            }
                            continue;
						}
						if (woodHut && id == borderId) {//Found top
							do {
								y--;
								id = event.world.getBlock(x, y, z);
							} while (id.isAir(event.world, x, y, z) || !id.isOpaqueCube());//not opaque
							if (id == Blocks.dirt) {//Found dirt floor
								event.world.setBlock(x, y, z, borderId);
								list = getBorder(event.world, Blocks.cobblestone, new int[] { x, y, z });
								for (int[] pos : list) {
									event.world.setBlock(pos[0], pos[1], pos[2], Blocks.stone);
								}
							}
						}
					}
				}
			}
		}
	}
    
    private static Block getEquivalentVillageBlock(Block block, BiomeGenBase biome){
        BiomeEvent.GetVillageBlockID getBlock = new BiomeEvent.GetVillageBlockID(biome, block, 0);
        MinecraftForge.TERRAIN_GEN_BUS.post(getBlock);
        if (getBlock.getResult() == Result.DENY)
            return getBlock.replacement;
        else if(biome instanceof BiomeGenDesert)
            return getVanillaEquivalentVillageBlock(block);
        return block;
    }

    /**
     * The change that vanilla does in desert village block setting
     * @see StructureVillagePieces.Village#func_151558_b(Block, int)
     *
     * @param block Base block set in structure template
     * @return the equivalent block according to vanilla for desert villages
     */
    private static Block getVanillaEquivalentVillageBlock(Block block){
        if (block == Blocks.log || block == Blocks.log2 || block == Blocks.cobblestone || block == Blocks.planks || block == Blocks.gravel)
        {
            return Blocks.sandstone;
        }
        else if (block == Blocks.oak_stairs || block == Blocks.stone_stairs)
        {
            return Blocks.sandstone_stairs;
        }
        return block;
    }

    /**
     * Listen to the village block id event.
     * Replaces gravel by a flag in ocean biome to reconstruct the pathway
     * Or use the replacement module
     *
     * @param event The village block id event
     */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onSettingVillageBlock(BiomeEvent.GetVillageBlockID event) {
		if (event.biome instanceof BiomeGenOcean && event.original == Blocks.gravel) {
			event.replacement = FLAG_ID;//flag used to reconstruct pathway afterward
			event.setResult(Result.DENY);
		}else if(replace && event.getResult() != Result.DENY){//Do not interfere with other village handlers
            BlockAndMeta temp = replacer.doReplace(event.original, event.type, event.biome);
            if(temp != null){
                event.replacement = temp.block;
                event.setResult(Result.DENY);
            }
        }
	}

    /**
     * Listen to the village block meta event.
     * Second part of the replacement module.
     *
     * @param event the block meta event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSettingVillageMeta(BiomeEvent.GetVillageBlockMeta event){
        if(replace && event.getResult() != Result.DENY){//Do not interfere with other village handlers
            BlockAndMeta temp = replacer.doReplace(event.original, event.type, event.biome);
            if(temp != null){
                int i = temp.getMeta();
                if(i<0)
                    return;
                if(i!=event.type){
                    event.replacement = i;
                    event.setResult(Result.DENY);
                }
            }
        }
    }
    
    /**
     *
     * @param world The world containing the blocks
     * @param id The block searched
     * @param x, y, z The coordinates of the center to search around
     * @return true if all faces of the center are attached to the searched block
     */
    private static boolean hasAround(World world, Block id, int x, int y, int z){
        return world.getBlock(x-1, y, z) == id && world.getBlock(x+1, y, z) == id && world.getBlock(x, y, z-1) == id && world.getBlock(x, y, z+1) == id;
    }

    /**
     *
     * @param world The world containing the blocks
     * @param id The block searched
     * @param field The coordinates of the center to search around
     * @return A list of coordinates that contain the same block, around the center, at the same height
     */
	private static List<int[]> getBorder(World world, Block id, int[] field) {
		List<int[]> list = new ArrayList<int[]>();
		for (int x = field[0] - 1; x < field[0] + 2; x++) {
			for (int z = field[2] - 1; z < field[2] + 2; z++) {
				if ((x != field[0] || z != field[2]) && world.getBlock(x, field[1], z) == id)
					list.add(new int[] { x, field[1], z });
			}
		}
		return list;
	}

    /**
     * Roughly estimates if a center block is cornered by blocks of the given type.
     * Actually searching for two unaligned border blocks
     *
     * @param world The world containing the blocks
     * @param id The block searched
     * @param pos The coordinates of the center to search around
     * @return true if the center block is cornered by the block type
     */
	private static boolean isCorner(World world, Block id, int[] pos) {
		List<int[]> list = getBorder(world, id, pos);
		if (list.size() < 2)
			return false;
		int[] a = list.get(0);
		int[] b = list.get(1);
		return a[0] != b[0] && a[2] != b[2];
	}

    /**
     * Wrapper method to identify replaceable blocks.
     *
     * @return true if the position is replaceable by any block
     */
	private static boolean isReplaceable(World world, int x, int y, int z) {
		return world.getBlock(x, y, z).isReplaceable(world, x, y, z);
	}

    /**
     * Wrapper method to identify water type blocks.
     *
     * @param id The block to compare
     * @return true if the block material is water
     */
	private static boolean isWaterId(Block id) {
		return id.getMaterial() == Material.water;
	}
}
