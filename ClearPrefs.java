import java.util.prefs.Preferences;
public class ClearPrefs {
    public static void main(String[] args) throws Exception {
        Preferences prefs = Preferences.userRoot().node("/ro/licenta/analiza");
        prefs.remove("tutorialVazut");
        prefs.flush();
        System.out.println("Gata! Am sters memoria pentru tutorial.");
    }
}
