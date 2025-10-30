package safePubg;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Uma aplicação Swing para prever a Zona Segura (Safe Zone) no jogo PUBG.
 * O usuário pode selecionar um mapa, desenhar a rota do avião, e o programa
 * irá prever uma possível localização para a primeira Zona Segura com base
 * em probabilidades predefinidas.
 */
public class PUBGSafeZonePredictor {
    /**
     * Nomes dos mapas disponíveis para seleção.
     */
    private static final String[] MAPS = {"Erangel", "Miramar", "Taego", "Rondo"};
    /**
     * Cor de fundo padrão para os mapas, caso a imagem não seja carregada.
     */
    private static final Color MAP_COLOR = new Color(100, 150, 100);
    /**
     * Cor da linha que representa a rota do avião.
     */
    private static final Color PLANE_ROUTE_COLOR = new Color(0, 0, 255, 150);
    /**
     * Cor do círculo que representa a Zona Segura prevista.
     */
    private static final Color PREDICTED_ZONE_COLOR = new Color(255, 0, 0, 100);
    /**
     * Constante para identificar a zona central do mapa.
     */
    private static final int ZONE_CENTER = 0;
    /**
     * Constante para identificar a zona periférica do mapa.
     */
    private static final int ZONE_PERIPHERAL = 1;
    /**
     * Constante para identificar a zona de borda do mapa.
     */
    private static final int ZONE_EDGE = 2;

    /**
     * Matriz tridimensional de probabilidades usada para prever a Zona Segura.
     * A lógica é baseada no tipo de rota do avião (central, periférica, borda)
     * e na localização da zona prevista (central, periférica, borda) em relação
     * à distância da rota (sobre, próximo, distante).
     */
    private static final double[][][] PROBABILITIES = {
        // Rota atravessa Zona Central
        {
            {10, 25, 30},  // Zona Central (sobre, próximo, distante)
            {5, 12, 15},    // Zona Periférica
            {0, 1, 2}       // Zona de Borda
        },
        // Rota atravessa apenas Zona Periférica
        {
            {5, 15, 25},
            {8, 20, 22},
            {1, 2, 2}
        },
        // Rota atravessa apenas Zona de Borda
        {
            {2, 8, 15},
            {10, 25, 30},
            {3, 4, 3}
        }
    };

    private static Point2D startPoint = null;
    private static Point2D endPoint = null;
    private static Point2D predictedZone = null;
    private static int selectedMap = 0;
    private static int routeZoneType = -1;

    /**
     * Armazena as imagens dos mapas carregadas para evitar a releitura do disco.
     */
    private static HashMap<String, BufferedImage> mapImages = new HashMap<>();

    /**
     * Ponto de entrada principal da aplicação.
     * Cria a interface gráfica (GUI), carrega os recursos e configura os
     * listeners de eventos.
     *
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        loadMapImages();
        JFrame frame = new JFrame("PUBG Safe Zone Predictor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Painel de seleção de mapa
        JPanel topPanel = new JPanel();
        JLabel mapLabel = new JLabel("Selecione o mapa:");
        JComboBox<String> mapComboBox = new JComboBox<>(MAPS);
        JButton predictButton = new JButton("Prever Safe Zone");
        JButton clearButton = new JButton("Limpar");

        topPanel.add(mapLabel);
        topPanel.add(mapComboBox);
        topPanel.add(predictButton);
        topPanel.add(clearButton);

        // Painel de desenho
        JPanel drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Desenha a imagem de fundo do mapa
                BufferedImage mapImg = mapImages.get(MAPS[selectedMap]);
                if (mapImg != null) {
                    g2d.drawImage(mapImg, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2d.setColor(MAP_COLOR);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }

                // Desenha a grade
                g2d.setColor(Color.BLACK);
                for (int i = 0; i <= 10; i++) {
                    int x = i * getWidth() / 10;
                    int y = i * getHeight() / 10;
                    g2d.drawLine(x, 0, x, getHeight());
                    g2d.drawLine(0, y, getWidth(), y);
                }

                // Desenha as zonas
                g2d.setColor(new Color(255, 255, 0, 50));
                // Zona Central (25% da área)
                int centerSize = (int) (getWidth() * 0.5);
                int centerX = (getWidth() - centerSize) / 2;
                int centerY = (getHeight() - centerSize) / 2;
                g2d.fillOval(centerX, centerY, centerSize, centerSize);

                // Zona Periférica (próximos 50% da área)
                g2d.setColor(new Color(255, 165, 0, 50));
                int peripheralSize = (int) (getWidth() * 0.85);
                int peripheralX = (getWidth() - peripheralSize) / 2;
                int peripheralY = (getHeight() - peripheralSize) / 2;
                g2d.fillOval(peripheralX, peripheralY, peripheralSize, peripheralSize);

                // Desenha a rota do avião, se existir
                if (startPoint != null && endPoint != null) {
                    g2d.setColor(PLANE_ROUTE_COLOR);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.draw(new Line2D.Double(startPoint, endPoint));

                    // Desenha o texto do tipo de zona
                    String zoneType = "";
                    switch (routeZoneType) {
                        case ZONE_CENTER: zoneType = "Rota atravessa Zona Central"; break;
                        case ZONE_PERIPHERAL: zoneType = "Rota atravessa Zona Periférica"; break;
                        case ZONE_EDGE: zoneType = "Rota atravessa Zona de Borda"; break;
                    }
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(zoneType, 10, 20);
                }

                // Desenha a zona prevista, se existir
                if (predictedZone != null) {
                    g2d.setColor(PREDICTED_ZONE_COLOR);
                    int zoneSize = 40;
                    g2d.fillOval((int)predictedZone.getX() - zoneSize/2,
                                (int)predictedZone.getY() - zoneSize/2,
                                zoneSize, zoneSize);

                    // Desenha o texto do tipo de zona
                    String zoneType = getZoneType(predictedZone);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("Safe Zone prevista: " + zoneType, 10, 40);
                }

                // Desenha o nome do mapa
                g2d.setColor(Color.WHITE);
                g2d.drawString("Mapa: " + MAPS[selectedMap], 10, getHeight() - 10);
            }
        };

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                endPoint = null;
                predictedZone = null;
                drawingPanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endPoint = e.getPoint();
                determineRouteZoneType();
                drawingPanel.repaint();
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                endPoint = e.getPoint();
                drawingPanel.repaint();
            }
        });

        // Adiciona listeners de ação
        mapComboBox.addActionListener(e -> {
            selectedMap = mapComboBox.getSelectedIndex();
            drawingPanel.repaint();
        });

        predictButton.addActionListener(e -> {
            if (startPoint != null && endPoint != null) {
                predictSafeZone();
                drawingPanel.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Por favor, desenhe a rota do avião primeiro.");
            }
        });

        clearButton.addActionListener(e -> {
            startPoint = null;
            endPoint = null;
            predictedZone = null;
            routeZoneType = -1;
            drawingPanel.repaint();
        });

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(drawingPanel, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    /**
     * Carrega as imagens dos mapas da pasta 'maps'.
     * Se a pasta ou as imagens não forem encontradas, exibe um aviso
     * e cria imagens de fallback coloridas.
     */
    private static void loadMapImages() {
        // Primeiro verifica se a pasta maps existe
        File mapsDir = new File("maps");
        if (!mapsDir.exists()) {
            JOptionPane.showMessageDialog(null,
                "Pasta 'maps' não encontrada!\n" +
                "Crie uma pasta chamada 'maps' na mesma pasta onde está seu projeto\n" +
                "e coloque dentro os arquivos:\n" +
                "erangel.png, miramar.png, taego.png, rondo.png");
            createDefaultMapImages();
            return;
        }

        try {
            // Carrega as imagens da pasta maps
            mapImages.put("Erangel", ImageIO.read(new File("maps/erangel.png")));
            mapImages.put("Miramar", ImageIO.read(new File("maps/miramar.png")));
            mapImages.put("Taego", ImageIO.read(new File("maps/taego.png")));
            mapImages.put("Rondo", ImageIO.read(new File("maps/rondo.png")));
            System.out.println("Mapas carregados com sucesso!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                "Erro ao carregar imagens!\n" +
                "Verifique se os nomes dos arquivos estão corretos:\n" +
                "erangel.png, miramar.png, taego.png, rondo.png");
            createDefaultMapImages();
        }
    }

    /**
     * Cria imagens de fallback coloridas para os mapas caso os arquivos de imagem
     * não sejam encontrados. Isso garante que a aplicação possa continuar
     * funcionando mesmo sem as imagens dos mapas.
     */
    private static void createDefaultMapImages() {
        // Cria imagens coloridas simples caso não encontre os arquivos
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
     * Determina o tipo de zona que a rota do avião atravessa (central, periférica ou borda).
     * A classificação é baseada na distância da linha da rota até o centro do mapa.
     */
    private static void determineRouteZoneType() {
        if (startPoint == null || endPoint == null) return;

        // Calcula o centro do mapa
        double centerX = 400; // Assumindo largura do painel de 800
        double centerY = 300; // Assumindo altura do painel de 600
        double mapRadius = 400;

        // Calcula a distância da rota ao centro
        Line2D route = new Line2D.Double(startPoint, endPoint);
        double distToCenter = route.ptLineDist(centerX, centerY);

        if (distToCenter < mapRadius * 0.25) {
            routeZoneType = ZONE_CENTER;
        } else if (distToCenter < mapRadius * 0.75) {
            routeZoneType = ZONE_PERIPHERAL;
        } else {
            routeZoneType = ZONE_EDGE;
        }
    }

    /**
     * Prevê a localização da Zona Segura com base no tipo de rota do avião.
     * Utiliza a matriz de probabilidades para determinar uma zona e distância
     * aleatórias, e então gera um ponto dentro dessa área.
     */
    private static void predictSafeZone() {
        if (routeZoneType < 0 || routeZoneType > 2) return;

        // Obtém as probabilidades com base no tipo de rota
        double[][] probs = PROBABILITIES[routeZoneType];

        // Achata as probabilidades e cria um array cumulativo
        double[] flatProbs = new double[9];
        String[] zoneTypes = new String[9];
        int index = 0;

        for (int zone = 0; zone < 3; zone++) {
            for (int dist = 0; dist < 3; dist++) {
                flatProbs[index] = probs[zone][dist];
                zoneTypes[index] = zone + "," + dist;
                if (index > 0) {
                    flatProbs[index] += flatProbs[index - 1];
                }
                index++;
            }
        }

        // Normaliza as probabilidades (caso a soma não seja 100)
        double total = flatProbs[8];
        for (int i = 0; i < 9; i++) {
            flatProbs[i] = flatProbs[i] / total * 100;
        }

        // Seleção aleatória com base nas probabilidades
        Random rand = new Random();
        double randomValue = rand.nextDouble() * 100;
        int selected = 0;

        for (; selected < 9; selected++) {
            if (randomValue <= flatProbs[selected]) {
                break;
            }
        }

        // Obtém o tipo de zona e distância selecionados
        String[] parts = zoneTypes[selected].split(",");
        int zone = Integer.parseInt(parts[0]);
        int distance = Integer.parseInt(parts[1]);

        // Gera um ponto na zona e distância selecionadas
        predictedZone = generateZonePoint(zone, distance);
    }

    /**
     * Gera um ponto aleatório dentro de uma zona específica (central, periférica, borda)
     * e a uma distância específica da rota do avião (sobre, próximo, distante).
     *
     * @param zone A zona do mapa onde o ponto deve ser gerado (ZONE_CENTER, ZONE_PERIPHERAL, ZONE_EDGE).
     * @param distance A distância da rota do avião (0 para sobre, 1 para próximo, 2 para distante).
     * @return Um Point2D representando a localização do ponto gerado.
     */
    private static Point2D generateZonePoint(int zone, int distance) {
        Random rand = new Random();
        double centerX = 400; // Assumindo largura do painel de 800
        double centerY = 300; // Assumindo altura do painel de 600
        double mapRadius = 400;

        // Raios da zona
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

        // Distância da rota
        double minDistFromRoute, maxDistFromRoute;
        if (distance == 0) { // Sobre a rota
            minDistFromRoute = 0;
            maxDistFromRoute = 0.2 * mapRadius; // ~500m
        } else if (distance == 1) { // Próximo à rota
            minDistFromRoute = 0.2 * mapRadius;
            maxDistFromRoute = 0.6 * mapRadius; // ~1500m
        } else { // Distante da rota
            minDistFromRoute = 0.6 * mapRadius;
            maxDistFromRoute = mapRadius;
        }

        // Gera um ponto na zona com a distância apropriada da rota
        Point2D point;
        int attempts = 0;
        do {
            // Ângulo e raio aleatórios dentro da zona
            double angle = rand.nextDouble() * 2 * Math.PI;
            double radius = innerRadius + rand.nextDouble() * (outerRadius - innerRadius);

            // Converte para coordenadas cartesianas
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            point = new Point2D.Double(x, y);

            // Calcula a distância até a rota
            Line2D route = new Line2D.Double(startPoint, endPoint);
            double distToRoute = route.ptLineDist(point);

            // Verifica se a distância corresponde ao requisito
            if (distToRoute >= minDistFromRoute && distToRoute <= maxDistFromRoute) {
                return point;
            }

            attempts++;
        } while (attempts < 1000);

        // Se não conseguirmos encontrar uma correspondência perfeita, retorna qualquer ponto na zona
        double angle = rand.nextDouble() * 2 * Math.PI;
        double radius = innerRadius + rand.nextDouble() * (outerRadius - innerRadius);
        return new Point2D.Double(
            centerX + radius * Math.cos(angle),
            centerY + radius * Math.sin(angle)
        );
    }

    /**
     * Retorna o nome do tipo de zona (Central, Periférica, Borda) com base
     * na localização de um ponto.
     *
     * @param point O ponto a ser classificado.
     * @return Uma string com o nome da zona.
     */
    private static String getZoneType(Point2D point) {
        double centerX = 400; // Assumindo largura do painel de 800
        double centerY = 300; // Assumindo altura do painel de 600
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
}
