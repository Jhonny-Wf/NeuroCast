package ro.licenta.analiza;

public class Launcher {
    public static void main(String[] args) {
        // Pornește aplicația JavaFX dintr-o clasă separată, pentru a evita
        // erorile de modul JavaFX la lansarea directă a clasei Application
        DashboardApp.main(args);
    }
}