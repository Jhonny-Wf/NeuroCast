package ro.licenta.analiza;

public class ReteaNeuronala implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Neuron[] stratAscuns;
    private Neuron[] stratIesire;
    private double rataInvatare = 0.05;

    // Tipul de arhitectură: true = LeakyReLU Optimizat, false = Sigmoid Clasic
    private boolean folosesteLeakyReLU = true;
    private double momentum = 0.9; // Factor de inerție (configurabil din UI)

    public void setMomentum(double m) { this.momentum = m; }
    public double getMomentum() { return this.momentum; }
    public double getRataInvatare() { return this.rataInvatare; }
    public int getNeuroniAscunsi() { return stratAscuns != null ? stratAscuns.length : 0; }

    // Factori de normalizare persistenți

    public double factorLuna = 12, factorSezon = 4, factorBuget = 10000, factorPret = 200, factorVanzari = 50000;

    public void setRataInvatare(double rata) {
        this.rataInvatare = rata;
    }

    // Metodă pentru a actualiza factorii după încărcarea datelor
    public void setFactoriNormalizare(double l, double s, double b, double p, double v) {
        this.factorLuna = l;
        this.factorSezon = s;
        this.factorBuget = b;
        this.factorPret = p;
        this.factorVanzari = v;
    }

    // Constructor implicit — folosește modul LeakyReLU Optimizat
    public ReteaNeuronala(int intrari, int ascunsi, int iesiri) {
        this(intrari, ascunsi, iesiri, true);
    }

    // Constructor cu selecție de arhitectură
    public ReteaNeuronala(int intrari, int ascunsi, int iesiri, boolean folosesteLeakyReLU) {
        this.folosesteLeakyReLU = folosesteLeakyReLU;

        stratAscuns = new Neuron[ascunsi];
        for (int i = 0; i < ascunsi; i++) {
            if (folosesteLeakyReLU) {
                stratAscuns[i] = new Neuron(intrari, false); // He Initialization (LeakyReLU)
            } else {
                stratAscuns[i] = new Neuron(intrari);        // Inițializare clasică (Sigmoid)
            }
        }

        stratIesire = new Neuron[iesiri];
        for (int i = 0; i < iesiri; i++) {
            if (folosesteLeakyReLU) {
                stratIesire[i] = new Neuron(ascunsi, true);  // Xavier Initialization (Sigmoid)
            } else {
                stratIesire[i] = new Neuron(ascunsi);        // Inițializare clasică (Sigmoid)
            }
        }
    }

    // Getter pentru UI — permite afișarea modului activ al modelului
    public boolean isFolosesteLeakyReLU() {
        return folosesteLeakyReLU;
    }

    public double[] prezice(double[] dateIntrare) {
        double[] iesiriAscunse = new double[stratAscuns.length];
        for (int i = 0; i < stratAscuns.length; i++) {
            double suma = stratAscuns[i].bias;
            for (int j = 0; j < dateIntrare.length; j++) {
                suma += dateIntrare[j] * stratAscuns[i].ponderi[j];
            }
            if (folosesteLeakyReLU) {
                stratAscuns[i].iesire = stratAscuns[i].activareLeakyReLU(suma);
            } else {
                stratAscuns[i].iesire = stratAscuns[i].activare(suma);
            }
            iesiriAscunse[i] = stratAscuns[i].iesire;
        }
        double[] rezultateFinale = new double[stratIesire.length];
        for (int i = 0; i < stratIesire.length; i++) {
            double suma = stratIesire[i].bias;
            for (int j = 0; j < iesiriAscunse.length; j++) {
                suma += iesiriAscunse[j] * stratIesire[i].ponderi[j];
            }
            stratIesire[i].iesire = stratIesire[i].activare(suma);
            rezultateFinale[i] = stratIesire[i].iesire;
        }
        return rezultateFinale;
    }

    public void actualizeazaPonderi(double[] dateIntrare) {
        // Strat Ieșire
        for (int i = 0; i < stratIesire.length; i++) {
            for (int j = 0; j < stratAscuns.length; j++) {
                double delta = rataInvatare * stratIesire[i].gradient * stratAscuns[j].iesire;
                // Aplicăm regula de actualizare cu Momentum:
                stratIesire[i].ponderi[j] += delta + momentum * stratIesire[i].deltaPonderi[j];
                stratIesire[i].deltaPonderi[j] = delta + momentum * stratIesire[i].deltaPonderi[j];
            }
            double deltaB = rataInvatare * stratIesire[i].gradient;
            stratIesire[i].bias += deltaB + momentum * stratIesire[i].deltaBias;
            stratIesire[i].deltaBias = deltaB + momentum * stratIesire[i].deltaBias;
        }
        // Strat Ascuns
        for (int i = 0; i < stratAscuns.length; i++) {
            for (int j = 0; j < dateIntrare.length; j++) {
                double delta = rataInvatare * stratAscuns[i].gradient * dateIntrare[j];
                stratAscuns[i].ponderi[j] += delta + momentum * stratAscuns[i].deltaPonderi[j];
                stratAscuns[i].deltaPonderi[j] = delta + momentum * stratAscuns[i].deltaPonderi[j];
            }
            double deltaB = rataInvatare * stratAscuns[i].gradient;
            stratAscuns[i].bias += deltaB + momentum * stratAscuns[i].deltaBias;
            stratAscuns[i].deltaBias = deltaB + momentum * stratAscuns[i].deltaBias;
        }
    }

    // Calculează eroarea medie pe un set de date fără a antrena (folosită la validare)
    public double calculeazaEroare(double[][] date, double[][] tinte) {
        double eroareTotala = 0;
        for (int i = 0; i < date.length; i++) {
            double[] predictie = prezice(date[i]);
            for (int k = 0; k < predictie.length; k++) {
                eroareTotala += Math.abs(tinte[i][k] - predictie[k]);
            }
        }
        return eroareTotala / date.length;
    }

    public double antreneazaEpoca(double[][] setDate, double[][] tinte) {
        double eroareMedieEpoca = 0;
        for (int i = 0; i < setDate.length; i++) {
            double[] predictie = prezice(setDate[i]);
            for (int k = 0; k < stratIesire.length; k++) {
                double eroare = tinte[i][k] - predictie[k];
                stratIesire[k].gradient = eroare * stratIesire[k].derivataActivare(predictie[k]);
                eroareMedieEpoca += Math.abs(eroare);
            }
            for (int h = 0; h < stratAscuns.length; h++) {
                double eroareSuma = 0;
                for (int k = 0; k < stratIesire.length; k++) {
                    eroareSuma += stratIesire[k].gradient * stratIesire[k].ponderi[h];
                } if (folosesteLeakyReLU) {
                    stratAscuns[h].gradient = eroareSuma * stratAscuns[h].derivataLeakyReLU(stratAscuns[h].iesire);
                } else {
                    stratAscuns[h].gradient = eroareSuma * stratAscuns[h].derivataActivare(stratAscuns[h].iesire);
                }
            } actualizeazaPonderi(setDate[i]);
        } return eroareMedieEpoca / setDate.length;
    }

    public void antreneaza(double[][] setDate, double[][] tinte, int epociMax) {
        for (int e = 1; e <= epociMax; e++) {
            double eroareMedie = antreneazaEpoca(setDate, tinte);

            // Afișăm eroarea la fiecare 500 de epoci pentru a vedea convergența
            if (e % 500 == 0 || e == 1) {
                System.out.println("Epoca #" + e + " | Eroare Medie: " + eroareMedie);
            }

            // Dacă eroarea este suficient de mică, ne oprim mai devreme
            if (eroareMedie < 0.001) {
                System.out.println("Antrenare finalizată cu succes în epoca " + e);
                break;
            }
        }
    }

    public Neuron[] getStratIesire() {
        return stratIesire;
    }

    public Neuron[] getStratAscuns() {
        return stratAscuns;
    }

    public void salveazaModel(String cale) throws java.io.IOException {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream(cale))) {
            oos.writeObject(this);
        }
    }
    public static ReteaNeuronala incarcaModel(String cale) throws java.io.IOException, ClassNotFoundException {
        try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(cale))) {
            return (ReteaNeuronala) ois.readObject();
        } 
    }
}