package bettervillages;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.HashSet;

/**
 * Created by Olivier on 02/11/2014.
 * Wrapper for {@link BiomeGenBase} collection
 */
public final class Biomes {
    private HashSet<String> biomeNames;
    private HashSet<BiomeDictionary.Type> biomeTypes;

    public Biomes(String... ID) {
        biomeNames = new HashSet<String>();
        biomeTypes = new HashSet<BiomeDictionary.Type>();
        for (String txt : ID) {
            parse(txt);
        }
    }

    private void parse(String txt) {
        if (txt.startsWith("-")) {
            txt = txt.substring(1);
            try {
                biomeTypes.remove(BiomeDictionary.Type.valueOf(txt.trim()));
            } catch (IllegalArgumentException l) {
                biomeNames.remove(txt);
            }
        } else {
            if (txt.equals("*") || txt.equalsIgnoreCase("ALL")) {
                for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
                    if (biome != null)
                        biomeNames.add(biome.biomeName);
                }
                if(!txt.equals("*"))
                    Collections.addAll(biomeTypes, BiomeDictionary.Type.values());
            } else {
                try {
                    BiomeDictionary.Type type = BiomeDictionary.Type.valueOf(txt.trim());
                    biomeTypes.add(type);
                } catch (IllegalArgumentException l) {
                    for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
                        if (biome != null && biome.biomeName.equals(txt)) {
                            biomeNames.add(txt);
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean isEmpty() {
        return biomeNames.isEmpty() && biomeTypes.isEmpty();
    }

    public boolean contains(BiomeGenBase biome) {
        if(biome == null)
            return false;
        if(biomeNames.contains(biome.biomeName))
            return true;
        if(isEmpty())
            return false;
        BiomeDictionary.Type[] types = BiomeDictionary.getTypesForBiome(biome);
        for(BiomeDictionary.Type type : types){
            if(biomeTypes.contains(type)){
                return true;
            }
        }
        return false;
    }

    public boolean hasName(String biomeName) {
        return biomeNames.contains(biomeName);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this)
            return true;
        return object instanceof Biomes && this.biomeNames.equals(((Biomes) object).biomeNames) && this.biomeTypes.equals(((Biomes) object).biomeTypes);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.biomeNames).append(this.biomeTypes).toHashCode();
    }

    @Override
    public String toString() {
        if(!biomeNames.isEmpty())
            return this.biomeNames.toString().replace("[", "").replace("]", "");
        else
            return this.biomeTypes.toString().replace("[", "").replace("]", "");
    }
}
