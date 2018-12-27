package glis.toolkit;

import java.util.List;

public class Main {

    private static String CR   = System.lineSeparator();
    public static String USAGE = String.format(
            "usage: toolkit <command>%1$s" +
            "where <command> is:%1$s" +
            "-r or --register to register a PGRFA%1$s" +
            "-h or --help to print a help message",
            System.lineSeparator());

    // toolkit -register
    public static void main(String[] args) {
        try {
            if (args.length == 1 && List.of("--register", "-r").contains(args[0])) {

                // TODO
            }
            if (args.length == 1 && List.of("--help", "-h").contains(args[0])) {
                System.out.println(USAGE);
            }
            else {
                System.err.println(USAGE);
                System.exit(1);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void register() {

        // TODO
    }
}
