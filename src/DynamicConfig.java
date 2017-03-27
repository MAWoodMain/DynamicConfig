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
        SLEEP_TIME = sleepTime;
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
        if(sleepTime >= 0) this.start();
    }

    public DynamicConfig(File file) throws IOException
    {
        this(file, SLEEP_TIME);
    }

    public synchronized void loadFromFile()
    {

        HashMap<String, Boolean> booleansTemp = new HashMap<>();
        HashMap<String, String> stringsTemp = new HashMap<>();
        HashMap<String, Integer> integersTemp = new HashMap<>();
        HashMap<String, Double> doublesTemp = new HashMap<>();
        try (Stream<String> stream = Files.lines(file.toPath())) {
            stream.forEach(line ->
            {
                // if not all whitespace
                line = line.trim();
                if (line.length() > 0)
                {
                    String[] parts = line.split(":",3);
                    if(parts.length == 3)
                    {
                        String key = parts[1].substring(1,parts[1].length() - 1);
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
        booleans.clear();
        booleans.putAll(booleansTemp);
        strings.clear();
        strings.putAll(stringsTemp);
        integers.clear();
        integers.putAll(integersTemp);
        doubles.clear();
        doubles.putAll(doublesTemp);
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
