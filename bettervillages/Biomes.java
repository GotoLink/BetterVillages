package bettervillages;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

/**
 * Created by Olivier on 02/11/2014.
 * Wrapper for {@link BiomeGenBase} collection
 */
public final class Biomes{
    private HashSet<String> biomeNames;
    private HashSet<BiomeDictionary.Type> biomeTypes;

    public Biomes(String... ID){
        biomeNames = new HashSet<String>();
        biomeTypes = new HashSet<BiomeDictionary.Type>();
        for (String txt : ID) {
            parse(txt);
        }
    }

    private void parse(String txt){
        if (txt.startsWith("-")) {
            txt = txt.substring(1).trim();
            try {
                biomeTypes.remove(BiomeDictionary.Type.valueOf(txt.toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException l) {
                biomeNames.remove(txt);
            }
        } else {
            if (txt.equals("*") || txt.equalsIgnoreCase("ALL")) {
                for (BiomeGenBase biome: BiomeGenBase.getBiomeGenArray()) {
                    if(biome!=null)
                        biomeNames.add(biome.biomeName);
                }
                Collections.addAll(biomeTypes, BiomeDictionary.Type.values());
            } else {
                try {
                    BiomeDictionary.Type type = BiomeDictionary.Type.valueOf(txt.toUpperCase(Locale.ENGLISH));
                    biomeTypes.add(type);
                    for (BiomeGenBase biome : BiomeDictionary.getBiomesForType(type)) {
                        biomeNames.add(biome.biomeName);
                    }
                } catch (IllegalArgumentException l) {
                    for(BiomeGenBase biome: BiomeGenBase.getBiomeGenArray()){
                        if(biome!=null && biome.biomeName.equals(txt)){
                            biomeNames.add(txt);
                            BiomeDictionary.Type[] types = BiomeDictionary.getTypesForBiome(biome);
                            if(types!=null)
                                Collections.addAll(biomeTypes, types);
                        }
                    }
                }
            }
        }
    }

    public boolean isEmpty(){
        return biomeNames.isEmpty();
    }

    public boolean contains(BiomeGenBase biome){
        return biome != null && biomeNames.contains(biome.biomeName) && biomeTypes.containsAll(Arrays.asList(BiomeDictionary.getTypesForBiome(biome)));
    }

    public boolean hasName(String biomeName){
        return biomeNames.contains(biomeName);
    }

    @Override
    public boolean equals(Object object){
        if(object == this)
            return true;
        if(object == null)
            return false;
        return object instanceof Biomes && this.biomeNames.equals(((Biomes) object).biomeNames) && this.biomeTypes.equals(((Biomes) object).biomeTypes);
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder().append(this.biomeNames).append(this.biomeTypes).toHashCode();
    }

    @Override
    public String toString(){
        return this.biomeNames.toString().replace("[", "").replace("]", "");
    }
}
