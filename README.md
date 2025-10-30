# Repositório de Exercícios e Projetos Java

Este repositório contém uma coleção de exercícios e projetos desenvolvidos em Java, abrangendo desde conceitos fundamentais até aplicações mais elaboradas com interfaces gráficas.

## Projetos Principais

### PubgSafePredictor

Uma aplicação Java Swing que tenta prever a primeira Zona Segura (safe zone) no PUBG com base na rota do avião desenhada pelo usuário.

#### Funcionalidades

*   **Seleção de Mapa:** Escolha entre os mapas disponíveis (Erangel, Miramar, Taego, Rondo).
*   **Desenho da Rota:** Clique e arraste no mapa para desenhar a trajetória de voo do avião.
*   **Previsão de Zona:** Calcula e exibe uma possível localização para a primeira Zona Segura.
*   **Visualização:** Mostra um círculo previsto sobreposto à imagem do mapa selecionado.
*   **Lógica Probabilística:** A previsão é baseada em uma matriz de probabilidades que correlaciona o tipo de rota do avião com a localização da zona.

#### Como Funciona

1.  **Rota do Avião:** O tipo de rota (Central, Periférica, Borda) é determinado pela distância da linha desenhada ao centro do mapa.
2.  **Previsão:** O centro da primeira safe é gerado probabilisticamente usando as regras definidas no array `PROBABILITIES`. O método `generateZonePoint` tenta encontrar um ponto que satisfaça essas condições.

#### Requisitos

*   Java Development Kit (JDK) instalado (versão 8 ou superior recomendada).

#### Configuração

Este programa carrega as imagens dos mapas a partir de uma pasta `maps` localizada na raiz do projeto. Para que funcione corretamente, a estrutura de pastas **DEVE** ser a seguinte:

```
.
├── maps
│   ├── erangel.png
│   ├── miramar.png
│   ├── rondo.png
│   └── taego.png
├── PUBGSafeZonePredictor.java
└── ... (outros arquivos)
```

**IMPORTANTE:** Crie a pasta `maps` na raiz do projeto e coloque as imagens `.png` dos mapas lá, com os nomes exatos como listado acima.

#### Como Executar

1.  **Via IDE (Recomendado - IntelliJ IDEA, Eclipse, etc.):**
    *   Importe o projeto no seu IDE.
    *   Certifique-se de que a estrutura de pastas `maps/` com as imagens está correta.
    *   Certifique-se de que o JDK do projeto está configurado no IDE.
    *   Execute a classe `safePubg.PUBGSafeZonePredictor` (ela contém o método `main`).

2.  **Via Linha de Comando:**
    *   Abra o terminal na **pasta raiz do projeto**.
    *   Compile o código Java:
        ```bash
        javac PUBGSafeZonePredictor.java
        ```
    *   Execute o programa:
        ```bash
        java PUBGSafeZonePredictor
        ```

#### Disclaimer

Este é um modelo probabilístico e educacional criado para explorar as mecânicas da Zona Segura do PUBG. As previsões são baseadas em regras simplificadas e não garantem resultados precisos ou vantagens no jogo real.

### Exercícios do Curso

Este repositório também inclui uma série de exercícios do diretório `EXERCICIOCURSO1`, divididos em:

*   **Fundamentos:** Exercícios básicos de Java.
*   **Orientada a Objeto:** Exemplos de classes e objetos.

## Como Contribuir

Contribuições são bem-vindas! Se você tiver alguma ideia para melhorar os projetos ou adicionar novos exercícios, sinta-se à vontade para abrir uma issue ou enviar um pull request.
