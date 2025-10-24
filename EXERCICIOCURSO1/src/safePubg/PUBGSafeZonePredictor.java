
//feito por PsychoKiller

package safePubg;

// Importações necessárias para a interface gráfica (Swing e AWT)
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * PUBGSafeZonePredictor
 * * Um aplicativo Java Swing que prevê a sequência de zonas seguras (safes) do PUBG
 * com base na rota do avião e em regras probabilísticas avançadas.
 * * Lógica implementada:
 * 1. Fase 1: Prevista com base na rota do avião (simulando a Regra da Rota do Avião - Seção 1.1).
 * 2. Fases 2-7: Previstas com lógica de "Soft Shift" vs. "Hard Shift" (Seção 2.1).
 * 3. Fase 4: Implementa uma simulação da "Regra da Proporção de Terra" (Seção 1.2).
 * 4. Detecção de Água: Impede que as safes se centrem na água (Seção 1.1).
 * 5. Contenção: Garante que cada safe esteja contida na anterior (Seção 2.3).
 */
public class PUBGSafeZonePredictor {
    // --- CONSTANTES DE CONFIGURAÇÃO DO JOGO ---
    
    // Lista de mapas disponíveis
    private static final String[] MAPS = {"Erangel", "Miramar", "Taego", "Rondo"};
    // Cor de fundo padrão caso o mapa não carregue
    private static final Color MAP_COLOR = new Color(100, 150, 100);
    // Cor da linha da rota do avião
    private static final Color PLANE_ROUTE_COLOR = new Color(0, 0, 255, 150);
    // Cor dos círculos da safe (Branco semi-transparente, como no jogo)
    private static final Color PREDICTED_ZONE_COLOR = new Color(255, 255, 255, 40);
    
    // Constantes para os tipos de zona (baseado no código original)
    private static final int ZONE_CENTER = 0;
    private static final int ZONE_PERIPHERAL = 1;
    private static final int ZONE_EDGE = 2;
    
    // Proporções de raio (em pixels) para cada fase, baseadas na Seção 3.1
    private static final double[] PHASE_RADII = {
        300,    // Fase 1 (Nosso valor base em pixels)
        195,    // Fase 2 (~65% da Fase 1)
        97,     // Fase 3 (~50% da Fase 2)
        48,     // Fase 4 (~50% da Fase 3)
        24,     // Fase 5 (~50% da Fase 4)
        12,     // Fase 6 (~50% da Fase 5)
        6       // Fase 7 (~50% da Fase 6)
    };
    
    // O "Cérebro" da Fase 1: O array 3D de Probabilidades
    // Dimensão 1: Tipo de Rota (Central, Periférica, Borda)
    // Dimensão 2: Zona da Safe (Central, Periférica, Borda)
    // Dimensão 3: Distância da Rota (Sobre, Próximo, Distante)
    private static final double[][][] PROBABILITIES = {
        // Rota atravessa Zona Central
        {
            {10, 25, 30},  // Safe na Zona Central
            {5, 12, 15},   // Safe na Zona Periférica
            {0, 1, 0}      // Safe na Zona de Borda (Balanceado, 'Distante' é 0 pois é água)
        },
        // Rota atravessa apenas Zona Periférica
        {
            {5, 15, 25},
            {8, 20, 22},
            {1, 2, 0}      // Safe na Zona de Borda (Balanceado, 'Distante' é 0 pois é água)
        },
        // Rota atravessa apenas Zona de Borda
        {
            {2, 8, 15},
            {10, 25, 30},
            {3, 4, 3}
        }
    };
    
    // --- VARIÁVEIS DE ESTADO DA APLICAÇÃO ---
    
    // Pontos para desenhar a rota do avião
    private static Point2D startPoint = null;
    private static Point2D endPoint = null;
    // Armazena a sequência de círculos previstos
    private static List<SafeZone> predictedZones = new ArrayList<>();
    // Índice do mapa selecionado (0 = Erangel, 1 = Miramar, etc.)
    private static int selectedMap = 0;
    // Tipo da rota do avião (0=Central, 1=Periférica, 2=Borda)
    private static int routeZoneType = -1;
    // Cache para as imagens de mapa carregadas
    private static HashMap<String, BufferedImage> mapImages = new HashMap<>();

    /**
     * O método principal. Inicia a aplicação Swing.
     */
    public static void main(String[] args) {
        // Carrega as imagens dos mapas na memória
        loadMapImages();
        
        // Cria a janela principal
        JFrame frame = new JFrame("PUBG Safe Zone Predictor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // --- PAINEL SUPERIOR (Botões e ComboBox) ---
        JPanel topPanel = new JPanel();
        JLabel mapLabel = new JLabel("Selecione o mapa:");
        JComboBox<String> mapComboBox = new JComboBox<>(MAPS);
        JButton predictButton = new JButton("Prever Safe Zone");
        JButton clearButton = new JButton("Limpar");
        
        topPanel.add(mapLabel);
        topPanel.add(mapComboBox);
        topPanel.add(predictButton);
        topPanel.add(clearButton);
        
        // --- PAINEL DE DESENHO (O Mapa) ---
        // Usamos uma classe anônima que herda de JPanel
        JPanel drawingPanel = new JPanel() {
            /**
             * Este método é o coração visual. Ele é chamado toda vez
             * que o painel precisa ser redesenhado (ex: no início, ou
             * quando chamamos drawingPanel.repaint()).
             */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                // Deixa os círculos com bordas suaves
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); 

                // 1. Desenha a imagem do mapa (ou um fundo colorido se falhar)
                BufferedImage mapImg = mapImages.get(MAPS[selectedMap]);
                if (mapImg != null) {
                    g2d.drawImage(mapImg, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2d.setColor(MAP_COLOR);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                
                // 2. Desenha a rota do avião (se start e end existirem)
                if (startPoint != null && endPoint != null) {
                    g2d.setColor(PLANE_ROUTE_COLOR);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.draw(new Line2D.Double(startPoint, endPoint));
                    
                    String zoneType = "";
                    switch (routeZoneType) {
                        case ZONE_CENTER: zoneType = "Rota atravessa Zona Central"; break;
                        case ZONE_PERIPHERAL: zoneType = "Rota atravessa Zona Periférica"; break;
                        case ZONE_EDGE: zoneType = "Rota atravessa Zona de Borda"; break;
                    }
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(zoneType, 10, 20);
                }
                
                // 3. Desenha a sequência de SAFES (se a lista não estiver vazia)
                if (!predictedZones.isEmpty()) {
                    String firstZoneType = "";
                    
                    // Loop para desenhar cada círculo
                    for (SafeZone zone : predictedZones) {
                        int x = (int) (zone.center.getX() - zone.radius);
                        int y = (int) (zone.center.getY() - zone.radius);
                        int size = (int) (zone.radius * 2);
                        
                        // 3a. Desenha o preenchimento branco semi-transparente
                        g2d.setColor(PREDICTED_ZONE_COLOR); 
                        g2d.fillOval(x, y, size, size); 
                        
                        // 3b. Desenha uma borda branca sólida por cima (estilo PUBG)
                        g2d.setColor(Color.WHITE);
                        g2d.setStroke(new BasicStroke(2)); 
                        g2d.drawOval(x, y, size, size);
                    }
                    
                    // Mostra o tipo da Fase 1
                    firstZoneType = getZoneType(predictedZones.get(0).center);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("Safe Zone prevista: " + firstZoneType, 10, 40);
                }
                
                // 4. Desenha o nome do mapa
                g2d.setColor(Color.WHITE);
                g2d.drawString("Mapa: " + MAPS[selectedMap], 10, getHeight() - 10);
            }
        }; // Fim da classe anônima 'drawingPanel'
        
        // --- LISTENERS (Eventos do Usuário) ---
        
        // Listener para cliques do mouse
        drawingPanel.addMouseListener(new MouseAdapter() {
            /**
             * Chamado quando o mouse é pressionado.
             * Inicia a rota do avião.
             */
            @Override
            public void mousePressed(MouseEvent e) {
                predictedZones.clear(); // Limpa as safes antigas
                startPoint = e.getPoint(); // Define o início da rota
                endPoint = null;
                routeZoneType = -1; // Reseta o tipo de rota
                drawingPanel.repaint(); // Redesenha a tela
            }
            
            /**
             * Chamado quando o mouse é solto.
             * Finaliza a rota e determina o tipo.
             */
            @Override
            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint(); // Define o fim da rota
                determineRouteZoneType(); // Calcula se a rota foi Central, Periférica, etc.
                drawingPanel.repaint(); // Redesenha
            }
        });
        
        // Listener para arrastar do mouse
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            /**
             * Chamado continuamente enquanto o mouse é arrastado.
             * Atualiza o 'endPoint' para feedback visual.
             */
            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                drawingPanel.repaint();
            }
        });
        
        // Listener para a ComboBox de Mapas
        mapComboBox.addActionListener(e -> {
            selectedMap = mapComboBox.getSelectedIndex();
            drawingPanel.repaint();
        });
        
        // Listener para o botão "Prever Safe Zone"
        predictButton.addActionListener(e -> {
            if (startPoint != null && endPoint != null) {
                predictSafeZoneSequence(); // Chama o cérebro principal
                drawingPanel.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Por favor, desenhe a rota do avião primeiro.");
            }
        });
        
        // Listener para o botão "Limpar"
        clearButton.addActionListener(e -> {
            startPoint = null;
            endPoint = null;
            predictedZones.clear(); // Limpa a lista de safes
            routeZoneType = -1;
            drawingPanel.repaint();
        });
        
        // Monta a janela
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(drawingPanel, BorderLayout.CENTER);
        
        frame.add(mainPanel);
        frame.setVisible(true);
    } // --- FIM DO MÉTODO main ---

    /**
     * Carrega as imagens dos mapas da pasta 'resources' (dentro do pacote).
     * Este método usa getResourceAsStream, que funciona mesmo
     * depois que o projeto é compilado em um arquivo .jar.
     */
    private static void loadMapImages() {
        String[] mapNames = {"erangel", "miramar", "taego", "rondo"};
        
        try {
            for (String mapName : mapNames) {
                // O caminho DEVE começar com '/' e ser relativo ao source folder
                String path = "/safePubg/maps/" + mapName + ".png"; 
                InputStream imageStream = PUBGSafeZonePredictor.class.getResourceAsStream(path);
                
                if (imageStream == null) {
                    throw new IOException("Recurso não encontrado: " + path);
                }
                
                BufferedImage img = ImageIO.read(imageStream);
                // Capitaliza o nome para usar como chave (ex: "Erangel")
                String capitalizedMapName = mapName.substring(0, 1).toUpperCase() + mapName.substring(1);
                mapImages.put(capitalizedMapName, img);
            }
            System.out.println("Mapas carregados com sucesso como recursos!");

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Erro ao carregar imagens como recurso!\n" +
                "Verifique se a pasta 'maps' está dentro da pasta 'safePubg'.\n" +
                "Detalhes: " + e.getMessage());
            // Se falhar, cria imagens coloridas simples
            createDefaultMapImages();
        }
    }

    /**
     * Método de fallback. Cria imagens coloridas simples se os
     * arquivos de mapa não forem encontrados.
     */
    private static void createDefaultMapImages() {
        for (int i = 0; i < MAPS.length; i++) {
            BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = img.createGraphics();
            Color color;
            switch (i) {
                case 0: color = new Color(100, 150, 100); break; // Erangel
                case 1: color = new Color(180, 150, 100); break; // Miramar
                case 2: color = new Color(100, 100, 150); break; // Taego
                default: color = new Color(150, 100, 150); break; // Rondo
            }
            g2d.setColor(color);
            g2d.fillRect(0, 0, 800, 600);
            g2d.setColor(Color.WHITE);
            g2d.drawString(MAPS[i], 350, 300);
            g2d.dispose();
            mapImages.put(MAPS[i], img);
        }
    }
    
    /**
     * Calcula a qual zona (Central, Periférica, Borda) a rota do avião
     * mais se aproxima, com base na sua distância ao centro do mapa.
     */
    private static void determineRouteZoneType() {
        if (startPoint == null || endPoint == null) return;
        
        double centerX = 400; // Largura / 2
        double centerY = 300; // Altura / 2
        double mapRadius = 400; // Raio do mapa em pixels
        
        // Cria uma linha 2D para a rota do avião
        Line2D route = new Line2D.Double(startPoint, endPoint);
        // Calcula a menor distância da linha até o centro do mapa
        double distToCenter = route.ptLineDist(centerX, centerY);
        
        // Classifica a rota
        if (distToCenter < mapRadius * 0.25) {
            routeZoneType = ZONE_CENTER;
        } else if (distToCenter < mapRadius * 0.75) {
            routeZoneType = ZONE_PERIPHERAL;
        } else {
            routeZoneType = ZONE_EDGE;
        }
    }
    
    // --- MÉTODOS PRINCIPAIS DA LÓGICA DO JOGO ---

    /**
     * O CÉREBRO PRINCIPAL (Substitui o predictSafeZone original).
     * Calcula a SEQUÊNCIA INTEIRA de 7 safes com base nas regras do PUBG.
     */
    private static void predictSafeZoneSequence() {
        if (routeZoneType < 0 || routeZoneType > 2) return;

        predictedZones.clear();
        Random rand = new Random(); 

        // --- FASE 1: USA A LÓGICA COMPLEXA (PROBABILIDADES) ---
        // (Simula a Regra da Rota do Avião da Seção 1.1)
        
        // 1. Pega a tabela de regras 3x3 baseada no tipo de rota
        double[][] probs = PROBABILITIES[routeZoneType];
        
        // 2. Achata a tabela 3x3 para uma lista 1D de 9 elementos
        double[] flatProbs = new double[9];
        String[] zoneTypes = new String[9];
        int index = 0;
        for (int zone = 0; zone < 3; zone++) {
            for (int dist = 0; dist < 3; dist++) {
                flatProbs[index] = probs[zone][dist];
                zoneTypes[index] = zone + "," + dist; // Guarda o que a regra significa (ex: "0,2")
                if (index > 0) flatProbs[index] += flatProbs[index - 1]; // Cria probabilidade cumulativa
                index++;
            }
        }
        
        // 3. Normaliza a roleta (para ir de 0 a 100)
        double total = flatProbs[8];
        if (total <= 0) total = 1; // Evita divisão por zero se todas as chances forem 0
        for (int i = 0; i < 9; i++) flatProbs[i] = flatProbs[i] / total * 100;
        
        // 4. Gira a roleta (sorteia um número de 0 a 100)
        double randomValue = rand.nextDouble() * 100;
        int selected = 0;
        for (; selected < 9; selected++) {
            if (randomValue <= flatProbs[selected]) break;
        }
        if(selected > 8) selected = 8; // Garante que não saia do array
        
        // 5. Decodifica o resultado
        String[] parts = zoneTypes[selected].split(",");
        int zone = Integer.parseInt(parts[0]);
        int distance = Integer.parseInt(parts[1]);
        
        // --- FIM DO SORTEIO DA FASE 1 ---
        
        // 6. Gera o CENTRO da Fase 1 usando as regras sorteadas
        Point2D firstCenter = generateZonePoint(zone, distance, rand); 
        if (firstCenter == null) {
            System.out.println("ERRO: Falha crítica ao gerar a Fase 1.");
            return;
        }

        // 7. Adiciona a Fase 1 à lista
        SafeZone previousSafe = new SafeZone(firstCenter, PHASE_RADII[0]);
        predictedZones.add(previousSafe);
        
        // --- LÓGICA DAS FASES 2-7 (Baseada no Documento) ---
        for (int i = 1; i < PHASE_RADII.length; i++) {
            
            Point2D nextCenter = null;
            int attempts = 0;

            // CORREÇÃO DE LÓGICA (Seção 2.3):
            // O novo centro (C5) deve estar dentro de um raio de (R4 - R5)
            // para garantir que o Círculo 5 caiba inteiramente dentro do Círculo 4.
            double searchRadius = previousSafe.radius - PHASE_RADII[i]; 
            if (searchRadius < 0) searchRadius = 0;

            // Tenta gerar um ponto para a próxima safe que esteja EM TERRA
            do {
                // --- LÓGICA FASE-A-FASE ---
                if (i == 3) { 
                    // ----- FASE 4: REGRA DA "PROPORÇÃO DE TERRA" (Seção 1.2) -----
                    // O documento diz que esta fase é determinística e força
                    // um "hard shift" para longe da água.
                    System.out.println("LOG: Calculando Fase 4 (Regra da Proporção de Terra)");
                    nextCenter = generateRandomPointInCircle(
                        previousSafe.center, 
                        searchRadius, // Usa o raio de busca corrigido
                        0.5, // Força o centro a estar na metade externa (Hard Shift)
                        1.0, 
                        rand
                    );

                } else {
                    // ----- FASES 2, 3, 5, 6, 7: LÓGICA "SHIFT" (Seção 2.1) -----
                    // O documento sugere que "hard shifts" são muito comuns (50%+).
                    double roll = rand.nextDouble() * 100;
                    double minRadiusPercent, maxRadiusPercent;

                    if (roll < 50) { // 50% "Soft Shift" (perto do centro)
                        minRadiusPercent = 0.0;
                        maxRadiusPercent = 0.6;
                    } else { // 50% "Hard Shift" (perto da borda)
                        minRadiusPercent = 0.4;
                        maxRadiusPercent = 1.0;
                    }
                    
                    nextCenter = generateRandomPointInCircle(
                        previousSafe.center, 
                        searchRadius, // Usa o raio de busca corrigido
                        minRadiusPercent, 
                        maxRadiusPercent, 
                        rand
                    );
                }
                
                attempts++;
                // Se tentar 2000 vezes e não achar terra, desiste
                if (attempts > 2000) { 
                    System.out.println("AVISO: Não foi possível achar terra para a Fase " + (i+1) + ".");
                    break;
                }

            } while (!isLand(nextCenter)); // Continua tentando se o ponto for na água
            
            // Adiciona a nova safe (Fase 2, 3, etc.) à lista
            SafeZone nextSafe = new SafeZone(nextCenter, PHASE_RADII[i]);
            predictedZones.add(nextSafe);
            previousSafe = nextSafe; // A safe atual vira a "anterior"
        }
    }

    /**
     * MÉTODO 2 (Substitui generateZonePoint original)
     * Gera o ponto de centro para a FASE 1, com base nas regras de probabilidade
     * e garantindo que esteja em terra.
     */
    private static Point2D generateZonePoint(int zone, int distance, Random rand) {
        double centerX = 400;
        double centerY = 300;
        double mapRadius = 400;

        // Define os raios da Zona (Central, Periférica, Borda)
        double innerRadius, outerRadius;
        if (zone == ZONE_CENTER) {
            innerRadius = 0;
            outerRadius = mapRadius * 0.25;
        } else if (zone == ZONE_PERIPHERAL) {
            innerRadius = mapRadius * 0.25;
            outerRadius = mapRadius * 0.75;
        } else { // ZONE_EDGE
            innerRadius = mapRadius * 0.75;
            outerRadius = mapRadius;
        }
        
        // Define as distâncias da Rota (Sobre, Próximo, Distante)
        double minDistFromRoute, maxDistFromRoute;
        if (distance == 0) { // Sobre
            minDistFromRoute = 0;
            maxDistFromRoute = 0.2 * mapRadius;
        } else if (distance == 1) { // Próximo
            minDistFromRoute = 0.2 * mapRadius;
            maxDistFromRoute = 0.6 * mapRadius;
        } else { // Distante
            minDistFromRoute = 0.6 * mapRadius;
            maxDistFromRoute = mapRadius;
        }

        Point2D point = null;
        int attempts = 0;
        
        // --- TENTATIVA 1: ACHAR O PONTO PERFEITO (ZONA + DISTÂNCIA + TERRA) ---
        do {
            // Gera um ponto aleatório dentro da ZONA sorteada
            double angle = rand.nextDouble() * 2 * Math.PI;
            double radius = innerRadius + rand.nextDouble() * (outerRadius - innerRadius);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            point = new Point2D.Double(x, y);

            // Calcula a distância do ponto até a rota
            Line2D route = new Line2D.Double(startPoint, endPoint);
            double distToRoute = route.ptLineDist(point);
            
            // Verifica as 3 condições:
            // 1. Está na Distância certa? 
            // 2. Está na Zona certa? (já garantido pelo 'radius')
            // 3. É Terra?
            if (distToRoute >= minDistFromRoute && distToRoute <= maxDistFromRoute && isLand(point)) {
                return point; // Achou!
            }
            attempts++;
        } while (attempts < 15000); // Tenta 15.000 vezes

        // --- TENTATIVA 2: ACHAR QUALQUER PONTO EM TERRA (IGNORANDO A DISTÂNCIA) ---
        // Se falhar, é porque a combinação (ex: Borda + Distante) era impossível/água
        System.out.println("LOG: Não foi possível achar um ponto perfeito. Tentando achar qualquer ponto em terra...");
        attempts = 0;
        do {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double radius = innerRadius + rand.nextDouble() * (outerRadius - innerRadius);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            point = new Point2D.Double(x, y);

            if (isLand(point)) {
                System.out.println("LOG: Ponto em terra encontrado. Ignorando distância da rota.");
                return point;
            }
            attempts++;
        } while (attempts < 25000); // Tenta mais 25.000 vezes

        // --- TENTATIVA 3: DESISTIR ---
        System.out.println("AVISO: Não foi possível encontrar um ponto em terra. Retornando último ponto.");
        return point; // Retorna o último ponto (pode ser na água)
    }

    /**
     * MÉTODO 3 (Novo Método Auxiliar)
     * Gera um ponto aleatório para as Fases 2-7, obedecendo à faixa de raio
     * (minRadiusPercent, maxRadiusPercent) para simular Soft/Hard Shifts.
     * @param center O centro do círculo anterior.
     * @param searchRadius O raio MÁXIMO onde o novo centro pode estar (R_anterior - R_novo).
     * @param minRadiusPercent A % mínima de 'searchRadius' (ex: 0.4 para Hard Shift).
     * @param maxRadiusPercent A % máxima de 'searchRadius' (ex: 0.6 para Soft Shift).
     * @param rand O gerador aleatório.
     */
    private static Point2D generateRandomPointInCircle(Point2D center, double searchRadius, double minRadiusPercent, double maxRadiusPercent, Random rand) {
        // Pega um ângulo aleatório (0 a 360 graus)
        double angle = 2 * Math.PI * rand.nextDouble();
        
        // Sorteia uma distância normalizada (0.0 a 1.0)
        // Math.sqrt() garante distribuição uniforme PELA ÁREA, não pelo raio.
        double rNorm = Math.sqrt(rand.nextDouble()); 
        
        // Mapeia o valor (0.0 a 1.0) para a faixa de % desejada
        // Ex: (0.7 * (0.6 - 0.0)) + 0.0 = 0.42 (Soft Shift)
        // Ex: (0.7 * (1.0 - 0.4)) + 0.4 = 0.82 (Hard Shift)
        double rPercent = (rNorm * (maxRadiusPercent - minRadiusPercent)) + minRadiusPercent;
        
        // Aplica o raio de busca
        double r = searchRadius * rPercent; 
        
        // Converte de polar (ângulo, raio) para cartesiano (x, y)
        double x = center.getX() + r * Math.cos(angle);
        double y = center.getY() + r * Math.sin(angle);
        
        return new Point2D.Double(x, y);
    }
    
    /**
     * MÉTODO 4 (Novo Método Auxiliar de Detecção de Água)
     * Verifica se um ponto (coordenada) está em terra ou na água.
     * Usa a regra de cor customizada (Azul > Verde E Azul > Vermelho + 10)
     * que encontramos usando os seus dados de debug.
     */
    private static boolean isLand(Point2D point) {
        BufferedImage currentMapImage = mapImages.get(MAPS[selectedMap]);
        if (currentMapImage == null) {
            return true; // Se o mapa não carregou, não podemos checar. Assume terra.
        }

        int px = (int) point.getX();
        int py = (int) point.getY();

        // Verifica se o ponto está dentro dos limites da imagem
        if (px >= 0 && px < currentMapImage.getWidth() && py >= 0 && py < currentMapImage.getHeight()) {
            
            Color pixelColor = new Color(currentMapImage.getRGB(px, py));
            int red = pixelColor.getRed();
            int green = pixelColor.getGreen();
            int blue = pixelColor.getBlue();
            
            // **** A REGRA DE DETECÇÃO DE ÁGUA ****
            if (blue > green && blue > (red + 10)) {
                return false; // É água!
            }
        }
        // Se estiver fora do mapa (px, py) ou não for água, é terra.
        return true; 
    }

    /**
     * MÉTODO 5 (Substitui o getZoneType original)
     * Retorna o nome da zona (Central, Periférica, Borda) de um ponto.
     * Inclui uma verificação para evitar erro se o ponto for nulo.
     */
    private static String getZoneType(Point2D point) {
        if (point == null) return "Desconhecida"; // Correção de bug

        double centerX = 400; 
        double centerY = 300; 
        double mapRadius = 400;
        
        double dx = point.getX() - centerX;
        double dy = point.getY() - centerY;
        double distance = Math.sqrt(dx*dx + dy*dy);
        
        if (distance < mapRadius * 0.25) {
            return "Zona Central";
        } else if (distance < mapRadius * 0.75) {
            return "Zona Periférica";
        } else {
            return "Zona de Borda";
        }
    }

    /**
     * ITEM 6 (Nova Classe Auxiliar)
     * Uma classe simples ("struct") para guardar o centro
     * e o raio de uma safe zone.
     */
    private static class SafeZone {
        Point2D center;
        double radius;
        
        SafeZone(Point2D center, double radius) {
            this.center = center;
            this.radius = radius;
        }
    }
    
} // --- FIM DA CLASSE PUBGSafeZonePredictor ---
