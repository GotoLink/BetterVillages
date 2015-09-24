package bettervillages;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
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
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameData;
import net.minecraftforge.fml.common.registry.VillagerRegistry;
import net.minecraftforge.fml.common.registry.VillagerRegistry.IVillageCreationHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Mod(modid = "bettervillages", name = "Better Villages Mod", acceptableRemoteVersions = "*")
public final class BetterVillages {
    private static final Block FLAG_ID = Blocks.planks;
    private IBlockState fieldFence;
    private Block pathWay, fieldGate;
    private boolean lilies = true, fields = true, gates = true, wells = true, woodHut = true, torch = true, big = true, replace = true;
    private static final String biomeNames = "DesertHills,Extreme Hills,Extreme Hills Edge,Jungle,JungleHills,Ocean,Swampland,Taiga,TaigaHills,Ice Plains,Ice Mountains,Forest";
    private Biomes biomes;
    private static final List<IVillageCreationHandler> handlers = new ArrayList<IVillageCreationHandler>();
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

    private final HashSet<Integer> globalDimensionBlacklist = new HashSet<Integer>();
    private VillageBlockReplacement replacer;

    /**
     * Load configuration parameters, using defaults if necessary
     */
    @EventHandler
    public void configLoad(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        pathWay = GameData.getBlockRegistry().getObject(config.getString("Ocean_villages_path", "general", "planks", "Block used for streets of villages built in Ocean biome"));
        if (pathWay == Blocks.air) {
            pathWay = FLAG_ID;
        }
        StringBuilder build = new StringBuilder("Available biome tags are: ");
        for (BiomeDictionary.Type t : BiomeDictionary.Type.values()) {
            if(t == BiomeDictionary.Type.DESERT || t == BiomeDictionary.Type.FROZEN)//Deprecated
                continue;
            build.append(t);
            build.append(",");
        }
        config.addCustomCategoryComment("general", build.toString());
        biomes = new Biomes(config.get("general", "Available_biomes", biomeNames,
                "Biomes where villages should be added, use ALL or * for all biomes, select with biome name or biome tags, prefix with - to exclude")
                .getString().split(","));
        String[] blackList = config.getString("Dimension blacklist", "general", "-1,1", "Prevent all village decorations steps in a world, by dimension ids. Use [id1;id2] to add a range of id, prefix with - to exclude. Doesn't apply to block replacement module.").split(",");
        for (String text : blackList) {
            if (text != null && !text.isEmpty()) {
                boolean done = false;
                if (text.contains("[") && text.contains("]")) {
                    String[] results = text.substring(text.indexOf("[") + 1, text.indexOf("]")).split(";");
                    if (results.length == 2) {
                        try {
                            int a = Integer.parseInt(results[0]);
                            int b = Integer.parseInt(results[1]);
                            boolean remove = text.startsWith("-");
                            for (int x = a; x <= b; x++) {
                                if (remove)
                                    globalDimensionBlacklist.remove(x);
                                else
                                    globalDimensionBlacklist.add(x);
                            }
                            done = true;
                        } catch (NumberFormatException ignored) {

                        }
                    }
                }
                if (!done) {
                    try {
                        globalDimensionBlacklist.add(Integer.parseInt(text.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        lilies = config.getBoolean("Spawn_waterlily", "general", lilies, "Water lilies can be found on water in villages");
        wells = config.getBoolean("Decorate_wells", "general", wells, "Village wells should be improved");
        fields = config.getBoolean("Decorate_fields", "general", fields, "Village fields should be improved");
        woodHut = config.getBoolean("Decorate_huts", "general", woodHut, "Village wood huts should be improved");
        gates = config.getBoolean("Add_gates", "general", gates, "Fence gates added to village fields");
        String temp = config.get("general", "Villages_fields_fencing", "fence", "Block used for fencing villages fields").getString();
        if(fields) {
            fieldFence = BlockAndMeta.fromString(temp).state();
            if (fieldFence == null)
                fieldFence = Blocks.oak_fence.getDefaultState();
        }
        if (gates) {
            fieldGate = GameData.getBlockRegistry().getObject(temp.split(" ")[0] + "_gate");
            if (fieldGate == GameData.getBlockRegistry().getDefaultValue())
                fieldGate = Blocks.oak_fence_gate;
        }
        torch = config.getBoolean("Add_new_torch", "general", torch, "Better torch has a chance to appear in villages");
        big = config.getBoolean("Bigger_Villages", "general", big, "Villages generates in clusters");
        replace = config.getBoolean("Load_Replacement_Module", "general", replace, "Attempt to load block replacement rules from json");
        if (config.hasChanged())
            config.save();
        if (event.getSourceFile().getName().endsWith(".jar") && event.getSide().isClient()) {
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
        if (replace || biomes.contains(BiomeGenBase.ocean) || biomes.contains(BiomeGenBase.deepOcean)) {
            MinecraftForge.TERRAIN_GEN_BUS.register(this);//For the pathway / replacement module
        }
        if (torch) {
            MapGenStructureIO.registerStructureComponent(ComponentBetterVillageTorch.class, "BViT");
            VillagerRegistry.instance().registerVillageCreationHandler(new VillageCreationHandler(ComponentBetterVillageTorch.class, 15, 0, 1, 1));
        }
        if (big) {
            for (IVillageCreationHandler handler : handlers) {
                VillagerRegistry.instance().registerVillageCreationHandler(handler);
            }
        }
        if (replace) {
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
        if (event.hasVillageGenerated && !globalDimensionBlacklist.contains(event.world.provider.getDimensionId())) {
            int i = event.chunkX * 16 + 8;//Villages are offset
            int k = event.chunkZ * 16 + 8;
            BlockPos pos = new BlockPos(i, event.world.getActualHeight()/2, k);
            BiomeGenBase biome = event.world.getBiomeGenForCoords(pos);
            IBlockState borderId = getEquivalentVillageBlock(Blocks.log.getDefaultState(), biome);
            IBlockState cobbleEquivalent = null;
            IBlockState slabEquivalent = null;
            IBlockState ironEquivalent = null;
            if (wells) {//Wells aren't biome specific
                cobbleEquivalent = getEquivalentVillageBlock(Blocks.cobblestone.getDefaultState(), null);
                slabEquivalent = getEquivalentVillageBlock(Blocks.stone_slab.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM), null);
                ironEquivalent = getEquivalentVillageBlock(Blocks.iron_block.getDefaultState(), null);
            }
            IBlockState lilyEquivalent = null;
            if (lilies)
                lilyEquivalent = getEquivalentVillageBlock(Blocks.waterlily.getDefaultState(), biome);
            IBlockState torchEquivalent = null;
            IBlockState fenceEquivalent = null;
            if(fields || biome instanceof BiomeGenOcean){
                torchEquivalent = getEquivalentVillageBlock(Blocks.torch.getDefaultState(), biome);
                fenceEquivalent = getEquivalentVillageBlock(fieldFence, biome);
            }
            IBlockState id;
            List<BlockPos> list;
            for (int x = i; x < i + 16; x++) {
                for (int z = k; z < k + 16; z++) {//Search within chunk
                    pos = new BlockPos(x, event.world.getActualHeight()/2, z);
                    if (biome instanceof BiomeGenOcean) {
                        pos = event.world.getTopSolidOrLiquidBlock(pos).down();//ignores water
                        id = event.world.getBlockState(pos);
                        if (id.equals(Blocks.wool.getDefaultState()) && event.world.getBlockState(pos.down()).equals(fenceEquivalent)) {
                            //Definitely a common village torch
                            if (hasAround(event.world, torchEquivalent.getBlock(), pos) && isReplaceable(event.world, pos.down(4)))
                                event.world.setBlockState(pos.down(4), pathWay.getDefaultState());//Add support below
                            continue;
                        }
                        if (id.getBlock() instanceof BlockStairs) {
                            do {
                                pos = pos.down();
                                id = event.world.getBlockState(pos);
                            } while (id.getBlock().isAir(event.world, pos) || isWaterId(id.getBlock()));
                        }
                        if (id.getBlock() == FLAG_ID) {//found flag
                            id = event.world.getBlockState(pos.up());
                            if (isWaterId(id.getBlock())) {//underwater
                                if(event.world.rand.nextInt(10) != 0) {
                                    event.world.setBlockState(pos, id, 2);//destroy flag
                                    while (!event.world.isAirBlock(pos))
                                        pos = pos.up();
                                }else{//build columns
                                    while (!event.world.isAirBlock(pos)) {
                                        event.world.setBlockState(pos, pathWay.getDefaultState());
                                        pos = pos.up();
                                    }
                                }
                                event.world.setBlockState(pos, pathWay.getDefaultState());//rebuilt pathway on top of water
                            }
                            continue;
                        }
                    }
                    pos = event.world.getHeight(pos);//block on top of a "solid" block
                    if (pos.getY() > 1) {
                        pos = pos.down();
                        id = event.world.getBlockState(pos);
                        while (id.getBlock().isAir(event.world, pos) || id.getBlock().isLeaves(event.world, pos)) {
                            pos = pos.down();
                            id = event.world.getBlockState(pos);
                        }
                        if (isWaterId(id.getBlock())) {//found water in open air
                            if (lilies && event.world.isAirBlock(pos.up()) && event.rand.nextInt(10) == 0)
                                event.world.setBlockState(pos.up(), lilyEquivalent);//place waterlily randomly
                            if (gates) {
                                list = getBorder(event.world, id, pos);
                                if (list.size() == 1) {//found 2 water blocks
                                    list = getBorder(event.world, borderId, pos);
                                    if (list.size() == 3) {//found a 3 blocks border, assuming water in a village field
                                        pos = list.get(1);//get middle border block
                                        if (isReplaceable(event.world, pos.up())) {
                                            //find orientation for fence gate
                                            EnumFacing p = EnumFacing.SOUTH;
                                            if (x - pos.getX() < 0)
                                                p = EnumFacing.WEST;
                                            else if (x - pos.getX() > 0)
                                                p = EnumFacing.EAST;
                                            else if (z - pos.getZ() < 0)
                                                p = EnumFacing.NORTH;
                                            IBlockState tempState = fieldGate.getDefaultState();
                                            try {
                                                tempState = tempState.withProperty(BlockDirectional.FACING, p).withProperty(BlockFenceGate.OPEN, true);
                                            }catch (IllegalArgumentException ignored){}
                                            event.world.setBlockState(pos.up(), tempState);//place fence gate
                                        }
                                    }
                                }
                            }
                            continue;
                        }
                        if (fields && id.getBlock() instanceof BlockFarmland) {//found tilled field in open air, assuming this is a village field
                            list = getBorder(event.world, borderId, pos);
                            if (!list.isEmpty()) {
                                switch (list.size()) {
                                    case 3://simple border case
                                        pos = list.get(1);//get middle border block
                                        if (isReplaceable(event.world, pos.up()))
                                            event.world.setBlockState(pos.up(), fenceEquivalent);//place fence on top
                                        break;
                                    case 5://corner case
                                        pos = list.remove(1);
                                        if (isReplaceable(event.world, pos.up()))
                                            event.world.setBlockState(pos.up(), fenceEquivalent);
                                        pos = list.remove(2);
                                        if (isReplaceable(event.world, pos.up()))
                                            event.world.setBlockState(pos.up(), fenceEquivalent);
                                        for (BlockPos post : list) {
                                            if (isReplaceable(event.world, post.up())) {
                                                event.world.setBlockState(post.up(), fenceEquivalent);
                                                if (isReplaceable(event.world, post.up(2)) && isCorner(event.world, borderId, post))
                                                    event.world.setBlockState(post.up(2), torchEquivalent);
                                            }
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            continue;
                        }
                        if (wells && id.equals(cobbleEquivalent)) {//found cobblestone in open air
                            id = event.world.getBlockState(pos.down(4));
                            if (isWaterId(id.getBlock())) {//found water under cobblestone layer
                                pos = pos.down(4);
                                BlockPos field = pos;
                                list = getBorder(event.world, id, field);
                                if (list.size() == 3) {//found 4 water blocks
                                    list = getBorder(event.world, cobbleEquivalent, field);
                                    if (list.size() == 5) {//found 5 cobblestone surrounding one water block, assuming this is a village well
                                        field = list.remove(1);
                                        event.world.setBlockState(field.up(), slabEquivalent);
                                        field = list.remove(2);
                                        event.world.setBlockState(field.up(), slabEquivalent);
                                        for (BlockPos post : list) {
                                            for (BlockPos posb : getBorder(event.world, Blocks.gravel.getDefaultState(), post))
                                                event.world.setBlockState(posb, slabEquivalent);
                                        }
                                        while (event.world.getBlockState(pos).equals(id)) {
                                            pos = pos.down();
                                        }
                                        if(pos.getY() == field.getY()){
                                            pos = pos.down(12);
                                        }
                                        list = getBorder(event.world, cobbleEquivalent, pos);
                                        for (BlockPos post : list)
                                            event.world.setBlockState(post, ironEquivalent);
                                        event.world.setBlockState(pos, ironEquivalent);
                                    }
                                }
                            }
                            continue;
                        }
                        if (woodHut && id.equals(borderId)) {//Found top
                            do {
                                pos = pos.down();
                                id = event.world.getBlockState(pos);
                            } while (id.getBlock().isAir(event.world, pos) || !id.getBlock().isOpaqueCube());//not opaque
                            if (id.getBlock() == Blocks.dirt) {//Found dirt floor
                                event.world.setBlockState(pos, borderId);
                                list = getBorder(event.world, Blocks.cobblestone.getDefaultState(), pos);
                                for (BlockPos post : list) {
                                    event.world.setBlockState(post, Blocks.stone.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * The equivalent Forge hook to handle replacing village blocks
     *
     * @param block to replace
     * @param biome if any
     * @return the replaced block
     */
    private static IBlockState getEquivalentVillageBlock(IBlockState block, BiomeGenBase biome) {
        BiomeEvent.GetVillageBlockID getBlock = new BiomeEvent.GetVillageBlockID(biome, block);
        MinecraftForge.TERRAIN_GEN_BUS.post(getBlock);
        if (getBlock.getResult() == Result.DENY)
            return getBlock.replacement;
        else if (biome instanceof BiomeGenDesert)
            return getVanillaEquivalentVillageBlock(block);
        return block;
    }

    /**
     * The change that vanilla does in desert village block setting
     *
     * @param state Base block set in structure template
     * @return the equivalent block according to vanilla for desert villages
     * @see StructureVillagePieces.Village#func_175847_a(IBlockState)
     */
    private static IBlockState getVanillaEquivalentVillageBlock(IBlockState state) {
        if (state.getBlock() == Blocks.log || state.getBlock() == Blocks.log2 || state.getBlock() == Blocks.gravel || state.getBlock() == Blocks.cobblestone) {
            return Blocks.sandstone.getDefaultState();
        }
        if (state.getBlock() == Blocks.planks) {
            return Blocks.sandstone.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.SMOOTH);
        }
        if (state.getBlock() == Blocks.oak_stairs || state.getBlock() == Blocks.stone_stairs) {
            return Blocks.sandstone_stairs.getDefaultState().withProperty(BlockStairs.FACING, state.getValue(BlockStairs.FACING));
        }
        return state;
    }

    /**
     * Listen to the village block state event.
     * Replaces gravel by a flag in ocean biome to reconstruct the pathway
     * Or use the replacement module
     *
     * @param event The village block id event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSettingVillageBlock(BiomeEvent.GetVillageBlockID event) {
        if (event.biome instanceof BiomeGenOcean && event.original.getBlock() == Blocks.gravel) {
            event.replacement = FLAG_ID.getDefaultState();//flag used to reconstruct pathway afterward
            event.setResult(Result.DENY);
        } else if (replace && event.getResult() != Result.DENY) {//Do not interfere with other village handlers
            BlockAndMeta temp = replacer.doReplace(event.original, event.biome);
            if (temp != null) {
                IBlockState state = temp.state();
                if (state != null) {
                    event.replacement = state;
                    event.setResult(Result.DENY);
                }
            }
        }
    }

    /**
     * @param world The world containing the blocks
     * @param id    The block searched
     * @param pos   The coordinates of the center to search around
     * @return true if all faces of the center are attached to the searched block
     */
    private static boolean hasAround(World world, Block id, BlockPos pos) {
        return world.getBlockState(pos.west()).getBlock() == id && world.getBlockState(pos.east()).getBlock() == id && world.getBlockState(pos.north()).getBlock() == id && world.getBlockState(pos.south()).getBlock() == id;
    }

    /**
     * @param world The world containing the blocks
     * @param id    The block searched
     * @param field The coordinates of the center to search around
     * @return A list of coordinates that contain the same block, around the center, at the same height
     */
    private static List<BlockPos> getBorder(World world, IBlockState id, BlockPos field) {
        List<BlockPos> list = new ArrayList<BlockPos>();
        for (int x = field.getX() - 1; x < field.getX() + 2; x++) {
            for (int z = field.getZ() - 1; z < field.getZ() + 2; z++) {
                BlockPos pos = new BlockPos(x, field.getY(), z);
                if ((x != field.getX() || z != field.getZ()) && world.getBlockState(pos).equals(id))
                    list.add(pos);
            }
        }
        return list;
    }

    /**
     * Roughly estimates if a center block is cornered by blocks of the given type.
     * Actually searching for two unaligned border blocks
     *
     * @param world The world containing the blocks
     * @param id    The block searched
     * @param pos   The coordinates of the center to search around
     * @return true if the center block is cornered by the block type
     */
    private static boolean isCorner(World world, IBlockState id, BlockPos pos) {
        List<BlockPos> list = getBorder(world, id, pos);
        if (list.size() < 2)
            return false;
        BlockPos a = list.get(0);
        BlockPos b = list.get(1);
        return a.getX() != b.getX() && a.getZ() != b.getZ();
    }

    /**
     * Wrapper method to identify replaceable blocks.
     *
     * @return true if the position is replaceable by any block
     */
    private static boolean isReplaceable(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
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
