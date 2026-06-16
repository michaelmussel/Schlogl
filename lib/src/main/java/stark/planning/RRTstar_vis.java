package stark.planning;

import stark.DefaultRandomGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class RRTstar_vis extends JPanel {

    private final DefaultMap map;

    public RRTstar_vis(DefaultMap defaultMap){
        map = defaultMap;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Centering offset
        double offsetX = (getWidth() - map.getMap_size()) / 2;
        double offsetY = (getHeight() - map.getMap_size()) / 2;

        // Draw map boundary
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.GRAY);
        g2.draw(new Rectangle2D.Double(offsetX, offsetY, map.getMap_size(), map.getMap_size()));

        // Draw obstacles
        if (map.getObstacles() != null) {
            g2.setColor(Color.BLACK);
            for (Obstacle p : map.getObstacles()) {
                Rectangle2D rect = new Rectangle2D.Double(p.getX() + offsetX, p.getY() + offsetY, p.getW(), p.getW());
                g2.fill(rect);
            }
        }

        // Draw RRT tree edges
        g2.setColor(Color.LIGHT_GRAY);

        for (Pos[] edge : RRTstar.treeEdges) {
            g2.draw(new Line2D.Double(edge[0].getX() + offsetX, edge[0].getY() + offsetY,
                    edge[1].getX() + offsetX, edge[1].getY() + offsetY));
        }

        // Draw final path in blue
        g2.setColor(Color.BLUE);
        List<Pos> path = RRTstar.finalPath;
        for (int i = 0; i < path.size() - 1; i++) {
            Pos p1 = path.get(i);
            Pos p2 = path.get(i + 1);
            g2.draw(new Line2D.Double(p1.getX() + offsetX, p1.getY() + offsetY,
                    p2.getX() + offsetX, p2.getY() + offsetY));
        }

        if (RRTstar.startP != null && RRTstar.endP != null) {
            g2.setColor(Color.RED);
            Shape oval = new Ellipse2D.Double(RRTstar.startP.getX() + offsetX - 5, RRTstar.startP.getY() + offsetY - 5, 10, 10);
            g2.fill(oval);
            g2.setColor(Color.YELLOW);
            Shape oval2 = new Ellipse2D.Double(RRTstar.endP.getX() + offsetX - 5, RRTstar.endP.getY() + offsetY - 5, 10, 10);
            g2.fill(oval2);
        }
    }

    public static void main(String[] args) {

        double map_size = 300;
        double obs_max_size = 30;
        double obs_max_height = 7;
        int num_obstacles = 30;

        DefaultRandomGenerator rand = new DefaultRandomGenerator();
        DefaultMap map = new DefaultMap(rand, map_size, obs_max_size, obs_max_height, num_obstacles);

        JFrame frame = new JFrame("RRT Pathfinding");
        RRTstar_vis panel = new RRTstar_vis(map);
        frame.add(panel);
        frame.setSize((int)map_size + 10, (int)map_size + 10);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        double dimension = 3;
        double max_move = 3;
        double radius = 7;
        Pos start = RRTstar.generateFreePos(rand, map, dimension);
        Goal goal = new Goal(RRTstar.generateFreePos(rand, map, dimension));
        RRTstar.runEnvironment(rand, panel, start, goal, map, dimension, max_move, radius);
    }

}

