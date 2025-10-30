package OrientadaaObjeto;

import java.util.HashSet;

/**
 * Um exemplo simples que demonstra o uso da classe HashSet em Java.
 * O programa cria um HashSet de strings, adiciona alguns elementos,
 * e verifica a presença de um elemento específico.
 */
public class HashSetExemplo {
    /**
     * Ponto de entrada principal do programa.
     *
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        // Criando um HashSet de Strings
        HashSet<String> conjunto = new HashSet<>();

        // Adicionando elementos
        conjunto.add("3.8");
        conjunto.add("C");
        conjunto.add("Melancia");

        // Mostrando o tamanho
        System.out.println("Tamanho do conjunto: " + conjunto.size());

        // Verificando se contém "C"
        System.out.println("O conjunto contém 'C'? " + conjunto.contains("C"));
    }
}
