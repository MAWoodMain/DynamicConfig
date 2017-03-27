import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * DynamicConfig
 * Created by Matthew Wood on 27/03/2017.
 */
public class DynamicConfig extends Thread
{
    private static int SLEEP_TIME = 500;

    private final File file;

    private final HashMap<String, Boolean> booleans;
    private final HashMap<String, String> strings;
    private final HashMap<String, Integer> integers;
    private final HashMap<String, Double> doubles;

    private volatile boolean changed;

    private String lastChecksum = "";


    public DynamicConfig(File file, int sleepTime) throws IOException
    {
        this(file);
        SLEEP_TIME = sleepTime;
    }

    public DynamicConfig(File file) throws IOException
    {
        changed = false;

        if (!file.exists())
        {
            // if it is in a folder, create it
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            // create the file
            file.createNewFile();
        }
        this.file = file;

        booleans = new HashMap<>();
        strings = new HashMap<>();
        integers = new HashMap<>();
        doubles = new HashMap<>();

        loadFromFile();

        lastChecksum = getChecksum();

        this.start();
    }

    public void loadFromFile()
    {
        System.out.println("Reloading from file");
    }

    public void saveToFile()
    {
        System.out.println("Saving to file");
    }

    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            if (changed)
            {
                saveToFile();
                changed = false;
            } else
            {
                String newChecksum = getChecksum();
                if (!lastChecksum.equals(newChecksum))
                {
                    lastChecksum = newChecksum;
                    loadFromFile();
                } else
                    try
                    {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
            }
        }
    }

    private String getChecksum()
    {
        try
        {
            FileInputStream fileInputStream = new FileInputStream(this.file);
            String hash = DigestUtils.md5Hex(IOUtils.toByteArray(fileInputStream));
            fileInputStream.close();
            return hash;
        } catch (IOException ignored)
        {
        }
        // if checksum could not be made, do not dynamically update.
        return lastChecksum;
    }
}
