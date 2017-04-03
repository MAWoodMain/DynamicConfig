import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Stream;

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
        // override default sleep time (if <= 0 file watching disabled)
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

        // if dynamic updating is to be used
        if(SLEEP_TIME >= 0)
        {
            lastChecksum = getChecksum();
            this.start();
        }
    }

    public void loadFromFile()
    {

        // temp maps for incoming values
        HashMap<String, Boolean> booleansTemp = new HashMap<>();
        HashMap<String, String> stringsTemp = new HashMap<>();
        HashMap<String, Integer> integersTemp = new HashMap<>();
        HashMap<String, Double> doublesTemp = new HashMap<>();
        try (Stream<String> stream = Files.lines(file.toPath())) {
            stream.forEach(line ->
            {
                // remove all whitespace
                line = line.trim();
                // if anythings left
                if (line.length() > 0)
                {
                    // split on the first 3 ':' (so any in string literals are left)
                    String[] parts = line.split(":",3);
                    // if it fits the basic expression structure
                    if(parts.length == 3)
                    {
                        // extract the key part of the expression
                        String key = parts[1].substring(1,parts[1].length() - 1);
                        // switch on first part (the type) ignoring case
                        switch (line.toLowerCase().charAt(0))
                        {
                            case 'b':
                                booleansTemp.put(key, Boolean.valueOf(parts[2]));
                                break;
                            case 's':
                                stringsTemp.put(key, parts[2].substring(1,parts[2].length() - 1));
                                break;
                            case 'i':
                                integersTemp.put(key, Integer.valueOf(parts[2]));
                                break;
                            case 'd':
                                doublesTemp.put(key, Double.valueOf(parts[2]));
                                break;
                        }
                    }
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        // replace existing values
        // force the operations to be atomic using sync.
        synchronized (booleans)
        {
            booleans.clear();
            booleans.putAll(booleansTemp);
        }
        synchronized (strings)
        {
            strings.clear();
            strings.putAll(stringsTemp);
        }

        synchronized (integers)
        {
            integers.clear();
            integers.putAll(integersTemp);
        }

        synchronized (doubles)
        {
            doubles.clear();
            doubles.putAll(doublesTemp);
        }
    }

    public void saveToFile()
    {
        System.out.println("Saving to file *not yet implemented*");
    }

    @Override
    public void run()
    {
        while (!Thread.interrupted())
        {
            // if the values locally have been updated
            if (changed)
            {
                saveToFile();
                changed = false;
            } else
            {
                // get checksum of file
                String newChecksum = getChecksum();
                // if it differs from the last known checksum of the file
                if (!lastChecksum.equals(newChecksum))
                {
                    lastChecksum = newChecksum;
                    loadFromFile();
                } else
                    try
                    {
                        Thread.sleep(SLEEP_TIME);
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
