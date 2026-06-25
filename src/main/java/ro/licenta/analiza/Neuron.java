package ro.licenta.analiza;

import java.util.Random;

public class Neuron implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public double[] ponderi;
    public double bias;
    public double iesire;
    public double gradient;
    // Memorează modificarea anterioară a ponderilor/bias-ului, pentru momentum
    public double[] deltaPonderi;
    public double deltaBias;

    // Constructor păstrat pentru compatibilitatea cu modelele salvate anterior
    public Neuron(int numarIntrari) {
        this(numarIntrari, true);
    }

    // Inițializare He pentru stratul ascuns sau Xavier pentru stratul de ieșire
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

    // Funcția de activare Sigmoid, folosită pe stratul de ieșire
    public double activare(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    // Derivata Sigmoid, exprimată în funcție de ieșirea deja calculată
    public double derivataActivare(double x) {
        return x * (1.0 - x);
    }

    // Leaky ReLU, folosită pe stratul ascuns (evită problema vanishing gradient)
    public double activareLeakyReLU(double x) {
        return x > 0 ? x : 0.01 * x;
    }

    // Derivata Leaky ReLU. Primește ieșirea neuronului: pozitivă => 1, altfel => 0.01
    public double derivataLeakyReLU(double iesire) {
        return iesire > 0 ? 1.0 : 0.01;
    }
}