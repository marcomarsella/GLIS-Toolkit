package glis.toolkit;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {


    @Test
    void help() throws Exception {
        var out    = new StringBuilder();
        var err    = new StringBuilder();
        var status = execute("java -cp target/classes glis.toolkit.Main -h", out, err);
        assertEquals(0, status);
        assertEquals(Main.USAGE + System.lineSeparator(), out.toString());
        assertEquals("", err.toString());
    }

    @Test
    void usage() throws Exception {
        var out    = new StringBuilder();
        var err    = new StringBuilder();
        var status = execute("java -cp target/classes glis.toolkit.Main", out, err);
        assertEquals(1, status);
        assertEquals("", out.toString());
        assertEquals(Main.USAGE + System.lineSeparator(), err.toString());
    }

    private int execute(String command, StringBuilder out, StringBuilder err) throws Exception {
        var process = Runtime.getRuntime().exec(command);
        var outThread = capture(process.getInputStream(), out);
        var errThread = capture(process.getErrorStream(), err);
        var status = process.waitFor();
        outThread.join();
        errThread.join();
        return status;
    }

    private Thread capture(InputStream is, StringBuilder sb) {
        var outThread  = new Thread(() -> {
            try {
                sb.append(IOUtils.toString(is, StandardCharsets.UTF_8));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
        outThread.start();
        return outThread;
    }
}