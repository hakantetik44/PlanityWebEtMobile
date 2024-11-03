package utils;

public class OS {
    public static String OS;

    public static boolean isAndroid() {
        return "Android".equalsIgnoreCase(OS);
    }

    public static boolean isWeb() {
        return "Web".equalsIgnoreCase(OS);
    }

    public static boolean isIOS() {
        return "iOS".equalsIgnoreCase(OS);
    }

}
