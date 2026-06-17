package ro.licenta.analiza;

import java.util.Random;

public class Neuron implements java.io.Serializable {
    private static final long serialVersionUID = 1L; // Variabilele de membru (Field-uri) - trebuie să fie vizibile
                                                     // pentru rețea
    public double[] ponderi;
    public double bias;
    public double iesire;
    public double gradient;
    public double[] deltaPonderi; // Variabile pentru Momentum (memorează modificarea anterioară)
    public double deltaBias;

    // Constructorul neuronului (backward-compatible, pentru deserializare modele
    // vechi)
    public Neuron(int numarIntrari) {
        this(numarIntrari, true); // Comportament implicit: Xavier/Sigmoid
    }

    // Constructorul neuronului cu inițializare inteligentă He/Xavier [cite: 362]

    public Neuron(int numarIntrari, boolean esteStratIesire) {
        Random r = new Random();
        this.ponderi = new double[numarIntrari];
        this.deltaPonderi = new double[numarIntrari];

        double deviatieStandard;
        if (esteStratIesire) {
            // Xavier/Glorot Initialization — optim pentru funcția Sigmoid
            deviatieStandard = Math.sqrt(1.0 / numarIntrari);
        } else {
            // He Initialization — optim pentru funcția LeakyReLU
            deviatieStandard = Math.sqrt(2.0 / numarIntrari);
        }

        for (int i = 0; i < numarIntrari; i++) {
            this.ponderi[i] = r.nextGaussian() * deviatieStandard;
        }
        this.bias = 0.0; // Inițializăm bias-ul cu 0 (standard în deep learning)
    }

    // Funcția de activare logistică (Sigmoida) pentru Output Layer [cite: 455]

    public double activare(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // Derivata funcției de activare Sigmoid

    public double derivataActivare(double x) {
        return x * (1.0 - x);
    }

    // Funcția Leaky ReLU pentru Hidden Layer (Zero Vanishing Gradient)

    public double activareLeakyReLU(double x) {
        return x > 0 ? x : 0.01 * x;
    }

    // Derivata Leaky ReLU (Atenție, x aici este suma bruto 'z', dar noi in backprop
    // ii transmitem iesirea y,
    // asa ca trebuie sa interpretam: daca y > 0 e zona pozitiva, daca y <= 0 e zona
    // negativa)

    public double derivataLeakyReLU(double iesire) {
        return iesire > 0 ? 1.0 : 0.01;
    }
}