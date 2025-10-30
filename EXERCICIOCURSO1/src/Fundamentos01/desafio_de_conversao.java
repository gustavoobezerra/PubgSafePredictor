package Fundamentos01;

import java.util.Scanner;

/**
 * Um programa simples que calcula a média salarial de três trabalhadores.
 * O programa solicita o nome e o salário de cada trabalhador, calcula a média
 * e exibe o resultado formatado.
 */
public class desafio_de_conversao {
    /**
     * Ponto de entrada principal do programa.
     *
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        Scanner entrada = new Scanner(System.in);

        System.out.println("Digite o nome do primeiro trabalhador:");
        String trabalhador1 = entrada.nextLine();

        System.out.println("Digite o nome do segundo trabalhador:");
        String trabalhador2 = entrada.nextLine();

        System.out.println("Digite o nome do terceiro trabalhador:");
        String trabalhador3 = entrada.nextLine();

        System.out.printf("Digite o salário de %s: R$ ", trabalhador1);
        double salario1 = entrada.nextDouble();

        System.out.printf("Digite o salário de %s: R$ ", trabalhador2);
        double salario2 = entrada.nextDouble();

        System.out.printf("Digite o salário de %s: R$ ", trabalhador3);
        double salario3 = entrada.nextDouble();

        double media = (salario1 + salario2 + salario3) / 3;

        System.out.printf("A média salarial dos três trabalhadores é: R$ %.2f\n", media);

        entrada.close();
    }
}
