package ro.licenta.analiza;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("--- Pornire Aplicație Licență: Sistem Previziune Vânzări ---");

            // 1. Încărcăm datele din Excel
            ManagerDate manager = new ManagerDate();
            manager.incarcaDate("date_vanzari.xlsx");
            System.out.println("Date încărcate cu succes: " + manager.intrari.length + " înregistrări.");

            // 2. Inițializăm rețeaua (4 intrări, 12 neuroni ascunși, 1 ieșire)
            ReteaNeuronala rn = new ReteaNeuronala(4, 12, 1);

            // 3. Antrenare
            System.out.println("Începe antrenarea rețelei...");
            rn.antreneaza(manager.intrari, manager.tinte, 100000);

            // 4. Testare "What-if"
            System.out.println("\n--- Simulare Managerială: Buget crescut în Iulie ---");
            double[] scenariu = { 1.0 / 12, 1.0 / 4, 2000.0 / 10000, 50.0 / 100 }; // Buget 9000€
            double[] rezultat = rn.prezice(scenariu);

            System.out.println("Previziune vânzări: " + (rezultat[0] * 50000) + " €");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}