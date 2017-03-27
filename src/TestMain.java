import java.io.File;
import java.io.IOException;

/**
 * DynamicConfig
 * Created by Matthew Wood on 27/03/2017.
 */
public class TestMain
{
    public static void main(String[] args) throws IOException
    {
        DynamicConfig config = new DynamicConfig(new File("test.cfg"));
        while(true);
    }
}
