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

public class PUBGSafeZonePredictor {
    private static final String[] MAPS = {"Erangel", "Miramar", "Taego", "Rondo"};
    private static final Color MAP_COLOR = new Color(100, 150, 100);
    private static final Color PLANE_ROUTE_COLOR = new Color(0, 0, 255, 150);
    private static final Color PREDICTED_ZONE_COLOR = new Color(255, 0, 0, 100);
    private static final int ZONE_CENTER = 0;
    private static final int ZONE_PERIPHERAL = 1;
    private static final int ZONE_EDGE = 2;
    
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
    
    // Map to store loaded map images
    private static HashMap<String, BufferedImage> mapImages = new HashMap<>();

    public static void main(String[] args) {
        loadMapImages();
        JFrame frame = new JFrame("PUBG Safe Zone Predictor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Map selection panel
        JPanel topPanel = new JPanel();
        JLabel mapLabel = new JLabel("Selecione o mapa:");
        JComboBox<String> mapComboBox = new JComboBox<>(MAPS);
        JButton predictButton = new JButton("Prever Safe Zone");
        JButton clearButton = new JButton("Limpar");
        
        topPanel.add(mapLabel);
        topPanel.add(mapComboBox);
        topPanel.add(predictButton);
        topPanel.add(clearButton);
        
        // Drawing panel
        JPanel drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Draw map image background
                BufferedImage mapImg = mapImages.get(MAPS[selectedMap]);
                if (mapImg != null) {
                    g2d.drawImage(mapImg, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2d.setColor(MAP_COLOR);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                
                // Draw grid
                g2d.setColor(Color.BLACK);
                for (int i = 0; i <= 10; i++) {
                    int x = i * getWidth() / 10;
                    int y = i * getHeight() / 10;
                    g2d.drawLine(x, 0, x, getHeight());
                    g2d.drawLine(0, y, getWidth(), y);
                }
                
                // Draw zones
                g2d.setColor(new Color(255, 255, 0, 50));
                // Central zone (25% of area)
                int centerSize = (int) (getWidth() * 0.5);
                int centerX = (getWidth() - centerSize) / 2;
                int centerY = (getHeight() - centerSize) / 2;
                g2d.fillOval(centerX, centerY, centerSize, centerSize);
                
                // Peripheral zone (next 50% of area)
                g2d.setColor(new Color(255, 165, 0, 50));
                int peripheralSize = (int) (getWidth() * 0.85);
                int peripheralX = (getWidth() - peripheralSize) / 2;
                int peripheralY = (getHeight() - peripheralSize) / 2;
                g2d.fillOval(peripheralX, peripheralY, peripheralSize, peripheralSize);
                
                // Draw plane route if exists
                if (startPoint != null && endPoint != null) {
                    g2d.setColor(PLANE_ROUTE_COLOR);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.draw(new Line2D.Double(startPoint, endPoint));
                    
                    // Draw zone type text
                    String zoneType = "";
                    switch (routeZoneType) {
                        case ZONE_CENTER: zoneType = "Rota atravessa Zona Central"; break;
                        case ZONE_PERIPHERAL: zoneType = "Rota atravessa Zona Periférica"; break;
                        case ZONE_EDGE: zoneType = "Rota atravessa Zona de Borda"; break;
                    }
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(zoneType, 10, 20);
                }
                
                // Draw predicted zone if exists
                if (predictedZone != null) {
                    g2d.setColor(PREDICTED_ZONE_COLOR);
                    int zoneSize = 40;
                    g2d.fillOval((int)predictedZone.getX() - zoneSize/2, 
                                (int)predictedZone.getY() - zoneSize/2, 
                                zoneSize, zoneSize);
                    
                    // Draw zone type text
                    String zoneType = getZoneType(predictedZone);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("Safe Zone prevista: " + zoneType, 10, 40);
                }
                
                // Draw map name
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
        
        // Add action listeners
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
    
    private static void determineRouteZoneType() {
        if (startPoint == null || endPoint == null) return;
        
        // Calculate the center of the map
        double centerX = 400; // Assuming panel width is 800
        double centerY = 300; // Assuming panel height is 600
        double mapRadius = 400;
        
        // Calculate distance from route to center
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
    
    private static void predictSafeZone() {
        if (routeZoneType < 0 || routeZoneType > 2) return;
        
        // Get probabilities based on route zone type
        double[][] probs = PROBABILITIES[routeZoneType];
        
        // Flatten probabilities and create cumulative array
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
        
        // Normalize probabilities (just in case they don't sum to 100)
        double total = flatProbs[8];
        for (int i = 0; i < 9; i++) {
            flatProbs[i] = flatProbs[i] / total * 100;
        }
        
        // Random selection based on probabilities
        Random rand = new Random();
        double randomValue = rand.nextDouble() * 100;
        int selected = 0;
        
        for (; selected < 9; selected++) {
            if (randomValue <= flatProbs[selected]) {
                break;
            }
        }
        
        // Get the selected zone type and distance
        String[] parts = zoneTypes[selected].split(",");
        int zone = Integer.parseInt(parts[0]);
        int distance = Integer.parseInt(parts[1]);
        
        // Generate a point in the selected zone and distance
        predictedZone = generateZonePoint(zone, distance);
    }
    
    private static Point2D generateZonePoint(int zone, int distance) {
        Random rand = new Random();
        double centerX = 400; // Assuming panel width is 800
        double centerY = 300; // Assuming panel height is 600
        double mapRadius = 400;
        
        // Zone radii
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
        
        // Distance from route
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
        
        // Generate point in the zone with proper distance from route
        Point2D point;
        int attempts = 0;
        do {
            // Random angle and radius within zone
            double angle = rand.nextDouble() * 2 * Math.PI;
            double radius = innerRadius + rand.nextDouble() * (outerRadius - innerRadius);
            
            // Convert to cartesian coordinates
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            point = new Point2D.Double(x, y);
            
            // Calculate distance to route
            Line2D route = new Line2D.Double(startPoint, endPoint);
            double distToRoute = route.ptLineDist(point);
            
            // Check if distance matches the requirement
            if (distToRoute >= minDistFromRoute && distToRoute <= maxDistFromRoute) {
                return point;
            }
            
            attempts++;
        } while (attempts < 1000);
        
        // If we can't find a perfect match, return any point in the zone
        double angle = rand.nextDouble() * 2 * Math.PI;
        double radius = innerRadius + rand.nextDouble() * (outerRadius - innerRadius);
        return new Point2D.Double(
            centerX + radius * Math.cos(angle),
            centerY + radius * Math.sin(angle)
        );
    }
    
    private static String getZoneType(Point2D point) {
        double centerX = 400; // Assuming panel width is 800
        double centerY = 300; // Assuming panel height is 600
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