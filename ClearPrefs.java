import java.util.prefs.Preferences;
public class ClearPrefs {
    public static void main(String[] args) throws Exception {
        Preferences prefs = Preferences.userRoot().node("/ro/licenta/analiza");
        System.out.println("Inainte: tutorialVazut=" + prefs.getBoolean("tutorialVazut", false));
        prefs.remove("tutorialVazut");
        prefs.flush();
        System.out.println("Dupa: tutorialVazut=" + prefs.getBoolean("tutorialVazut", false));
    }
}
