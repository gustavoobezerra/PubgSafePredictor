package OrientadaaObjeto;

/**
 * Representa um carro com um motor, permitindo ações como acelerar, frear,
 * ligar e desligar. A injeção de combustível do motor é ajustada com base
 * nessas ações.
 */
public class Carro {
    /**
     * O motor do carro.
     */
    public Motor motor = new Motor();
    
    /**
     * Arredonda um valor double para uma casa decimal.
     *
     * @param valor O valor a ser arredondado.
     * @return O valor arredondado.
     */
    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
    
    /**
     * Acelera o carro, aumentando o fator de injeção do motor.
     */
    void acelerar() {
        motor.fatorInjecao += 0.4;
        motor.fatorInjecao = arredondar(motor.fatorInjecao);
        System.out.printf("Acelerando! Fator injeção: %.1f\n", motor.fatorInjecao);
    }

    /**
     * Freia o carro, diminuindo o fator de injeção do motor.
     */
    void frear() {
        motor.fatorInjecao -= 0.4;
        motor.fatorInjecao = Math.max(motor.fatorInjecao, 0);
        motor.fatorInjecao = arredondar(motor.fatorInjecao);
        System.out.printf("Freando! Fator injeção: %.1f\n", motor.fatorInjecao);
    }

    /**
     * Liga o motor do carro.
     */
    void ligar() {
        motor.ligado = true;
        motor.fatorInjecao = 1.0;
        System.out.println("Carro ligado!");
    }

    /**
     * Desliga o motor do carro.
     */
    void desligar() {
        motor.ligado = false;
        motor.fatorInjecao = 1.0;
        System.out.println("Carro desligado!");
    }

    /**
     * Verifica se o carro está ligado.
     *
     * @return true se o carro estiver ligado, false caso contrário.
     */
    boolean estaLigado() {
        return motor.ligado;
    }
}
