import java.util.Random;

public class SupraMethod {
    private static final Random random = new Random();
    private static final int MAX_COUNT_AB = 5;   // Maximálny počet krokov bez zmeny
    private final double B; // Parameter zabúdania
    private final double C; // Parameter učenia
    private final int max_s; // Maximálny počet generovaných bodov
    private double[] w; // Parameter učenia/pamäť
    private static final int VECTOR_SIZE = 4;   // Veľkosť vektorov určená podľa počtu potrebných parametrov
    private int p_max; // Hodnota doteraz najlepšieho nájdeného riešenia
    private double[] pk; // Vektor parametrov počiatočného bodu
    private int initialPointCost; // Na uloženie hodnoty účelovej funkcie počiatočného bodu
    private double[] statisticalGradient; // Štatistický gradient r

    public SupraMethod(int s, double B, double C) {
        GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm(200, 0.3, 0.5, 30);
        geneticAlgorithm.run();
        this.initialPointCost = geneticAlgorithm.getSolutionCost();
        this.p_max = initialPointCost;

        this.B = B;
        this.C = C;
        this.max_s = s; // Počet bodov na vytvorenie

        this.w = new double[]{0.0, 0.0, 0.0, 0.0};                       // Na začiatku pamäť prázdna/neinicializovaná
        this.statisticalGradient = new double[]{0.0, 0.0, 0.0, 0.0};     // Na začiatku štatistický gradient prázdny/neinicializovaný

        this.pk = new double[]{geneticAlgorithm.getPopulationSize(), geneticAlgorithm.getMutationProbability(), geneticAlgorithm.getCrossoverProbability(), geneticAlgorithm.getTimeLimit()};
    }

    /**
     * Beh metódy.
     */
    public void runSupraMethod() {
        this.firstPhase();
        double[] bestPoint = this.secondPhase(statisticalGradient);

        System.out.println("Best point: ");
        System.out.println("Population: " + (int) bestPoint[0] + ", Mutation probability: " + bestPoint[1] + ", Crossover probability: "
                + bestPoint[2] + ", Time limit: " + (int) bestPoint[3] + " sec.");
    }

    /**
     * Prvá fáza metódy Supra.
     * Výstupom prvej fázy je vytvorenie štatistického gradientu reprezentujúceho posun.
     */
    private void firstPhase() {
        int j = 0;
        while (j < max_s) {
            // Uloženie vektora posunu
            double[] r = generateRandomVectorR();

            // Vytvorenie nového bodu na základe vzťahu p^kj = R(p^k + r^j)
            double[] newPoint = this.createNewPoint(r);

            // Spustenie genetického algoritmu pre každý vytvorený bod p^kj
            GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm((int) newPoint[0], newPoint[1], newPoint[2], (int) newPoint[3]);
            geneticAlgorithm.run();

            // Aktualizácia hodnôt štatistického gradientu(r), pamäte(w) a ceny bodu
            updateValues(newPoint, geneticAlgorithm.getSolutionCost());

            j++;
        }

        System.out.println("Statistical gradient parameters:");
        System.out.print("Population size: " + (int) statisticalGradient[0] + ", Mutation probability: " + statisticalGradient[1] + ", Crossover probability: "
                + statisticalGradient[2] + ", Time limit: " + (int) statisticalGradient[3] + " sec.\n");
    }

    /**
     * Druhá fáza metódy Supra.
     * Výstupom druhej fázy je bod s najlepšími nastaveniami parametrov(najnižšia hodnota účelovej funkcie).
     */
    private double[] secondPhase(double[] statisticalGradient) {
        int CountAb = 0;      // Hodnota krokov od posledného zlepšenia
        double[] alpha = new double[]{generatePopulationSize(), generateMutationProbability(), generateCrossoverProbability(), generateTimeLimit()};         // Dĺžka kroku

        // Výstupný bod s najlepšou hodnotou účelovej funkcie
        double[] bestPoint = pk;
        while (CountAb < MAX_COUNT_AB) {
            // Vypočítaj nový bod p = pk + alpha * r / ||r||
            double[] calculatedPoint = calculateNewPoint(alpha, statisticalGradient);

            // Vyhodnotí hodnotu účelovej funkcie pre nový bod
            GeneticAlgorithm geneticAlgorithm = new GeneticAlgorithm((int) calculatedPoint[0], calculatedPoint[1], calculatedPoint[2], (int) calculatedPoint[3]);
            geneticAlgorithm.run();
            int newCost = geneticAlgorithm.getSolutionCost();

            // Aktualizácia hodnôt štatistického gradientu(r), pamäte(w) a ceny bodu
            //updateValues(calculatedPoint, newCost);

            // Ak F(p) < F(p_max), aktualizuj hodnotu p_max a resetuj PocetAb
            if (newCost < p_max) {
                p_max = newCost;
                bestPoint = calculatedPoint;
                CountAb = 0;
            } else {
                CountAb++;
            }

            // Aktualizuj hodnotu alpha; Dĺžku kroku skrátime o polovicu
            for (int i = 0; i < alpha.length; i++) {
                alpha[i] /= 2;
            }
        }

        // Po skončení druhej fázy premazávame pamäť
        this.w = new double[]{0.0, 0.0, 0.0, 0.0};
        this.statisticalGradient = new double[]{0.0, 0.0, 0.0, 0.0};

        return bestPoint;
    }

    /**
     * Metóda na výpočet nového bodu v 2.fáze metódy Supra podľa vzťahu p = pk + alpha * r / ||r||.
     */
    private double[] calculateNewPoint(double[] alpha, double[] statisticalGradient) {
        double[] calculatedPoint = new double[VECTOR_SIZE];

        for (int i = 0; i < VECTOR_SIZE; i++) {
            calculatedPoint[i] = pk[i] + alpha[i] * Math.signum(statisticalGradient[i]);   // Signum na zistenie smeru, či budeme zväčšovať či zmenšovať
        }

        // Úprava neprípustných hodnôt
        this.fixUnfeasibleValues(calculatedPoint);

        return calculatedPoint;
    }

    /**
     * Metóda na vytvorenie nového bodu p^kj na základe vzťahu p^kj = p^k +r^j.
     */
    private double[] createNewPoint(double[] r) {
        double[] pkj = new double[VECTOR_SIZE];

        for (int i = 0; i < VECTOR_SIZE; i++) {
            pkj[i] = pk[i] + r[i];
        }
        return this.fixUnfeasibleValues(pkj);
    }

    /**
     * Metóda na upravenie neprípustných hodnôt úpravou do rozsahu
     * (pri prekročení hraničných hodnôt) podľa vzťahu p^kj= R(p^k + r^j)
     */
    private double[] fixUnfeasibleValues(double[] vector) {
        // Populácia
        // Najviac 1000 jedincov
        if (vector[0] > 1000) {
            vector[0] = 1000;
        }
        // Najmenej dvaja jedinci
        if (vector[0] < 2) {
            vector[0] = 2;
        }

        // Mutácia
        if (vector[1] > 1) {
            vector[1] = 1;
        }
        if (vector[1] < 0.02) {
            vector[1] = 0.02;
        }

        // Kríženie
        if (vector[2] > 1) {
            vector[2] = 1;
        }
        if (vector[2] < 0.02) {
            vector[2] = 0.02;
        }

        // Časový limit
        if (vector[3] > 40) {
            vector[3] = 40;
        }
        if (vector[3] < 15) {
            vector[3] = 15;
        }
        return vector;
    }


    /**
     * Generovanie vektora posunu r^j podľa vzťahu r^j = w + x.
     */
    private double[] generateRandomVectorR() {
        double[] x = new double[]{this.generatePopulationSize(), this.generateMutationProbability(), this.generateCrossoverProbability(), this.generateTimeLimit()};

        // Súčet jednotlivých zložiek vektorov
        for (int i = 0; i < VECTOR_SIZE; i++) {
            x[i] += w[i];
        }
        return x;
    }

    /**
     * Metóda na aktualizovanie hodnôt štatistického gradientu r, pamäte w a hodnoty doteraz najlepšieho riešenia
     * v 1. fáze metódy Supra.
     */
    private void updateValues(double[] newPoint, int cost) {
        // Aktualizácia hodnoty p_max, ak bolo nájdené lepšie riešenie
        if (cost < p_max) {
            p_max = cost;
        }

        // Aktualizácia hodnoty štatistického gradientu r
        for (int i = 0; i < VECTOR_SIZE; i++) {
            statisticalGradient[i] += (cost - this.initialPointCost) * (newPoint[i] - pk[i]);
        }

        // Aktualizácia hodnoty vektoru w
        for (int i = 0; i < VECTOR_SIZE; i++) {
            w[i] = B * w[i] + C * ((cost - this.initialPointCost) * (newPoint[i] - pk[i]));
        }
    }

    /**
     * Generovanie náhodnej veľkosti populácie v okolí bodu.
     * A je maximálna veľkosť generovanej zmeny parametra (10%).
     * Horná hranica populácie je 1000
     */
    private int generatePopulationSize() {
        int pkValue = 1000;
        int A = pkValue / 10;
        return random.nextInt(A * 2 + 1) - A;
    }

    /**
     * Generovanie náhodnej pravdepodobnosti mutácie v okolí bodu.
     * A je maximálna veľkosť generovanej zmeny parametra (10%).
     * Horná hranica mutácie je 1
     */
    private double generateMutationProbability() {
        double pkValue = 1;
        double A = 0.1 * pkValue;
        return random.nextDouble(-A, A);
    }

    /**
     * Generovanie náhodnej pravdepodobnosti kríženia v okolí bodu.
     * A je maximálna veľkosť generovanej zmeny parametra (10%).
     * Horná hranica kríženia je 1
     */
    private double generateCrossoverProbability() {
        double pkValue = 1;
        double A = 0.1 * pkValue;
        return random.nextDouble(-A, A);
    }

    /**
     * Generovanie náhodného časového limitu v okolí bodu.
     * A je maximálna veľkosť generovanej zmeny parametra (10%).
     * Horná hranica časového limitu je 1200 sekúnd (20 minút)
     */
    private int generateTimeLimit() {
        int pkValue = 40;
        int A = pkValue / 10;
        return random.nextInt(A * 2 + 1) - A;
    }
}
