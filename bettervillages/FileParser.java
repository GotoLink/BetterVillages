package bettervillages;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;

import java.io.*;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by GotoLink on 06/11/2014.
 * Loads a file following the {@code Pattern}, within the {@code ModContainer} source, or in server resource packs
 */
public abstract class FileParser {
    public FileParser(Object mod, Pattern pattern) {
            File source = FMLCommonHandler.instance().findContainerFor(mod).getSource();
            FMLLog.fine("%s parsing for %s", mod.toString(), pattern.pattern());
        try {
            if (source.isDirectory()) {
                searchDir(source.getParentFile().getParentFile(), pattern, "");
            } else {
                ZipFile zf = new ZipFile(source);
                searchZip(zf, pattern);
                zf.close();
                source = FMLCommonHandler.instance().getMinecraftServerInstance().getFile("server-resource-packs");
                if(!source.exists())
                    return;
                FMLLog.fine("%s parsing in server resource packs", mod.toString());
                for(File file : source.listFiles()) {
                    if(file.isDirectory())
                        searchDir(file, pattern, "");
                    else {
                        zf = new ZipFile(file);
                        boolean flag = searchZip(zf, pattern);
                        zf.close();
                        if(flag)
                            break;
                    }
                }
            }
        } catch (Exception stuffGoneWrong) {
            stuffGoneWrong.printStackTrace();
        }
    }

    /**
     * Helper to find and parse a file within a compressed file
     * @return true if parsing was successful
     */
    private boolean searchZip(ZipFile zf, Pattern pattern){
        for (ZipEntry ze : Collections.list(zf.entries())) {
            Matcher matcher = pattern.matcher(ze.getName());
            if (matcher.matches()) {
                try {
                    parse(zf.getInputStream(ze));
                    FMLLog.finer("Parsed %s from %s", ze.toString(), zf.toString());
                    return true;
                }catch (IOException io){
                    io.printStackTrace();
                }
                break;
            }
        }
        return false;
    }

    /**
     * Helper to find and parse a file within file path
     */
    private void searchDir(File source, Pattern pattern, String path)  {
        for (File file : source.listFiles()) {
            String currPath = path + file.getName();
            if (file.isDirectory()) {
                searchDir(file, pattern, currPath + File.separatorChar);
            }else {
                Matcher matcher = pattern.matcher(currPath);
                if (matcher.matches()) {
                    try {
                        parse(new FileInputStream(file));
                        FMLLog.finer("Parsed from %s", file.toString());
                    }catch (IOException io){
                        io.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    /**
     * The delegating parsing method
     *
     * @param inputStream the input to parse from
     */
    private void parse(InputStream inputStream) {
        parse(new InputStreamReader(new BufferedInputStream(inputStream)));
    }

    /**
     * The actual parsing method
     */
    protected abstract void parse(Reader reader);
}
