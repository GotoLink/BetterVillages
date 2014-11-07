package bettervillages;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.JsonUtils;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by GotoLink on 02/11/2014.
 * Loads a json readable file for the replacement module
 */
public class VillageBlockReplacement extends FileParser{
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(BlockReplace.class, new BlockReplace.Deserializer()).create();
    private static final Type mapType = new TypeToken<Map<String, BlockReplace>>() { }.getType();
    private static final String ANY_BIOME = "ALL";
    private HashMap<BlockPlace, BlockAndMeta> replacements;

    public VillageBlockReplacement(Object mod){
        super(mod, Pattern.compile("assets/(.*)village_block_replace.json"));
    }

    @Override
    protected void parse(Reader reader){
        Map<String, BlockReplace> tempMap = gson.fromJson(reader, mapType);
        replacements = new HashMap<BlockPlace, BlockAndMeta>();
        for(BlockReplace entry : tempMap.values()){
            if(entry!=null && entry.blockStart.block!=Blocks.air){
                replacements.put(entry.asPlace(), entry.toPlace);
            }
        }
    }

    /**
     * Replacement method
     * Uses the replacement dictionary serialized from the json readable file.
     *
     * @param block
     * @param type
     * @param biome the selected biome, if any
     * @return the replacement block and meta wrapper, or null if none is valid
     */
    public BlockAndMeta doReplace(final Block block, final int type, final BiomeGenBase biome){
        BlockAndMeta temp = replacements.get(new BlockPlace(new BlockAndMeta(block, type), ANY_BIOME));
        if(temp!=null || biome==null)
            return temp;
        Optional<Map.Entry<BlockPlace, BlockAndMeta>> entryOptional = Iterators.tryFind(replacements.entrySet().iterator(), new Predicate<Map.Entry<BlockPlace, BlockAndMeta>>() {
            @Override
            public boolean apply(Map.Entry<BlockPlace, BlockAndMeta> input) {
                return input.getKey().blockStart.contains(block, type) && input.getKey().biome.contains(biome);
            }
        });
        if(entryOptional.isPresent()){
            return entryOptional.get().getValue();
        }
        return null;
    }
    
    public static class BlockPlace{
        public final BlockAndMeta blockStart;
        public final Biomes biome;

        private BlockPlace(String blockName, String biomeName){
            this(BlockAndMeta.fromString(blockName), new Biomes(biomeName.split(",")));
        }

        private BlockPlace(BlockAndMeta block, Biomes biomes){
            this.blockStart = block;
            this.biome = biomes;
        }

        public BlockPlace(BlockAndMeta block, String biomeName){
            if(block == null || biomeName == null)
                throw new IllegalArgumentException("Couldn't create Block placement, one argument is null");
            this.blockStart = block;
            this.biome = new Biomes(biomeName);
        }

        @Override
        public boolean equals(Object object){
            if(object == null)
                return false;
            if(object == this)
                return true;
            return object instanceof BlockPlace && this.blockStart.equals(((BlockPlace) object).blockStart) && this.biome.equals(((BlockPlace) object).biome);
        }
        
        @Override
        public int hashCode(){
            return new HashCodeBuilder().append(blockStart).append(biome).toHashCode();
        }

        @Override
        public String toString(){
            return "match:"+blockStart+", biome:"+biome;
        }
    }

    public static final class BlockReplace extends BlockPlace{
        public final BlockAndMeta toPlace;
        public BlockReplace(String from, String to, String biome){
            super(from, biome);
            toPlace = BlockAndMeta.fromString(to).trimMetaValues();
        }

        public BlockPlace asPlace(){
            return new BlockPlace(blockStart, biome);
        }

        @Override
        public boolean equals(Object object){
            return super.equals(object) && object instanceof BlockReplace && ((BlockReplace) object).toPlace.equals(this.toPlace);
        }

        @Override
        public int hashCode(){
            return new HashCodeBuilder().append(super.hashCode()).append(toPlace).toHashCode();
        }

        @Override
        public String toString(){
            return super.toString()+", place:"+toPlace;
        }

        public static final class Deserializer implements JsonDeserializer<BlockReplace>{

            @Override
            public BlockReplace deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                JsonObject jsonobject = json.getAsJsonObject();
                return new BlockReplace(getStringFromJsonOrDefault(jsonobject, "match", "air"), getStringFromJsonOrDefault(jsonobject, "place", "air"),getStringFromJsonOrDefault(jsonobject, "biome", ANY_BIOME));
            }

            public static String getStringFromJsonOrDefault(JsonObject json, String field, String defaultValue)
            {
                return json.has(field) ? JsonUtils.getJsonElementStringValue(json.get(field), field).trim() : defaultValue;
            }
        }
    }
}
