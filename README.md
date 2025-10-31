# PubgSafePredictor

Uma aplicação Java Swing que tenta prever a sequência das Zonas Seguras (safe zones) no PUBG com base na rota do avião desenhada pelo usuário.

Este projeto utiliza regras probabilísticas e heurísticas inspiradas nas mecânicas do jogo e análises competitivas (como as configurações SUPER e o documento de análise aprofundada) para simular o fechamento dos círculos da Fase 1 até a Fase 7.

## Funcionalidades

* **Seleção de Mapa:** Escolha entre os mapas disponíveis (Erangel, Miramar, Taego, Rondo).
* **Desenho da Rota:** Clique e arraste no mapa para desenhar a trajetória de voo do avião.
* **Previsão de Sequência:** Calcula e exibe a sequência completa das 7 fases da Zona Segura.
* **Visualização:** Mostra os círculos previstos (em branco, estilo PUBG) sobrepostos à imagem do mapa selecionado.
* **Detecção de Água:** Tenta evitar que os centros das safes caiam em áreas de água, analisando a cor dos pixels do mapa.
* **Lógica Avançada:**
    * A **Fase 1** é influenciada pela rota do avião (simulando a regra de correlação).
    * As **Fases 2-7** simulam "Soft Shifts" e "Hard Shifts" com base em probabilidades (50/50).
    * A **Fase 4** implementa uma lógica especial para simular a "Regra da Proporção de Terra", forçando um afastamento da água.
    * Garante a **contenção** (cada nova safe está completamente dentro da anterior).

## Como Funciona (Visão Geral da Lógica)

1.  **Rota do Avião:** O tipo de rota (Central, Periférica, Borda) é determinado pela distância da linha desenhada ao centro do mapa.
2.  **Fase 1:** O centro da primeira safe é gerado probabilisticamente usando as regras definidas no array `PROBABILITIES`, que correlacionam o tipo de rota com a zona e a distância da safe. O método `generateZonePoint` tenta encontrar um ponto que satisfaça essas condições E esteja em terra.
3.  **Fases 2-7:** O método `predictSafeZoneSequence` entra em um loop:
    * Calcula o `searchRadius` (Raio Anterior - Raio Novo) para garantir a contenção.
    * Para a **Fase 4 (i == 3)**, força um "Hard Shift" (centro na borda externa do `searchRadius`) para simular a Regra da Proporção de Terra.
    * Para as **outras fases**, sorteia (50/50) entre um "Soft Shift" (centro perto do centro do `searchRadius`) e um "Hard Shift".
    * Usa o método `generateRandomPointInCircle` para obter o novo centro dentro dos limites calculados.
    * Verifica se o centro gerado está em terra usando `isLand()`. Se não estiver, tenta gerar um novo ponto várias vezes antes de desistir.

## Requisitos

* Java Development Kit (JDK) instalado (versão 8 ou superior recomendada).

## Configuração (Imagens dos Mapas)

Este programa carrega as imagens dos mapas como **recursos internos**. Para que funcione corretamente, a estrutura de pastas **DEVE** ser a seguinte:

* **IMPORTANTE:** Crie a pasta `maps` **dentro** da pasta `safePubg` (que está dentro de `src`). Coloque as imagens `.png` dos mapas lá, com os nomes exatos como listado acima.

## Como Executar

1.  **Via IDE (Recomendado - IntelliJ IDEA, Eclipse, etc.):**
    * Importe o projeto no seu IDE.
    * Certifique-se de que a estrutura de pastas `src/safePubg/maps/` com as imagens está correta.
    * Certifique-se de que o JDK do projeto está configurado no IDE.
    * Execute a classe `safePubg.PUBGSafeZonePredictor` (ela contém o método `main`).

2.  **Via Linha de Comando:**
    * Abra o terminal na **pasta raiz do projeto** (a que contém a pasta `src`).
    * Compile o código Java (pode precisar ajustar o classpath dependendo da sua configuração):
        ```bash
        javac src/safePubg/PUBGSafeZonePredictor.java -d bin
        ```
        *(Isso criará uma pasta `bin` com os arquivos `.class`)*
    * Execute o programa a partir da pasta raiz:
        ```bash
        java -cp bin safePubg.PUBGSafeZonePredictor
        ```
        *(O `-cp bin` diz ao Java para procurar as classes compiladas na pasta `bin`)


## Disclaimer

Este é um modelo probabilístico e educacional criado para explorar as mecânicas da Zona Segura do PUBG. As previsões são baseadas nas regras implementadas (inspiradas em análises do jogo) e na aleatoriedade inerente aos sorteios. Não garante resultados precisos ou vantagens no jogo real. Use por sua conta e risco.




