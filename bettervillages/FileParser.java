package bettervillages;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.*;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by GotoLink on 06/11/2014.
 * Loads a file following the {@code Pattern}, within the {@code ModContainer} source
 */
public abstract class FileParser {
    public FileParser(Object mod, Pattern pattern) {
        try {
            final File source = FMLCommonHandler.instance().findContainerFor(mod).getSource();
            if (source.isDirectory()) {
                searchDir(source, pattern, "");
            } else {
                ZipFile zf = new ZipFile(source);
                for (ZipEntry ze : Collections.list(zf.entries())) {
                    Matcher matcher = pattern.matcher(ze.getName());
                    if (matcher.matches()) {
                        parse(zf.getInputStream(ze));
                    }
                }
                zf.close();
            }
        } catch (Exception stuffGoneWrong) {
            stuffGoneWrong.printStackTrace();
        }
    }

    /**
     * Helper to find and parse a file within file path
     */
    private void searchDir(File source, Pattern pattern, String path) throws IOException {
        for (File file : source.listFiles()) {
            String currPath = path + file.getName();
            if (file.isDirectory()) {
                searchDir(file, pattern, currPath + '/');
            }
            Matcher matcher = pattern.matcher(currPath);
            if (matcher.matches()) {
                parse(new FileInputStream(file));
            }
        }
    }

    /**
     * The delegating parsing method
     *
     * @param inputStream
     */
    private void parse(InputStream inputStream) {
        parse(new InputStreamReader(new BufferedInputStream(inputStream)));
    }

    /**
     * The actual parsing method
     */
    protected abstract void parse(Reader reader);
}
