package OrientadaaObjeto;

public class Carro {
    public Motor motor = new Motor();
    
    // Método auxiliar para arredondar
    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
    
    void acelerar() {
        motor.fatorInjecao += 0.4;
        motor.fatorInjecao = arredondar(motor.fatorInjecao);  // ✅ Arredonda o valor real
        System.out.printf("Acelerando! Fator injeção: %.1f\n", motor.fatorInjecao);
    }

    void frear() {
        motor.fatorInjecao -= 0.4;
        motor.fatorInjecao = Math.max(motor.fatorInjecao, 0);
        motor.fatorInjecao = arredondar(motor.fatorInjecao);  // ✅ Arredonda o valor real
        System.out.printf("Freando! Fator injeção: %.1f\n", motor.fatorInjecao);
    }

    void ligar() {
        motor.ligado = true;
        motor.fatorInjecao = 1.0;  // ✅ Já arredondado
        System.out.println("Carro ligado!");
    }

    void desligar() {
        motor.ligado = false;
        motor.fatorInjecao = 1.0;  // ✅ Já arredondado
        System.out.println("Carro desligado!");
    }

    boolean estaLigado() {
        return motor.ligado;
    }
}