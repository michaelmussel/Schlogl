package stark.planning;

import org.apache.commons.math3.random.RandomGenerator;

import javax.swing.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class RRTstar {

    public static final List<Pos> treeNodes = new ArrayList<>();
    public static final List<Pos[]> treeEdges = new ArrayList<>();
    public static final List<Pos> finalPath = new ArrayList<>();

    public static Pos startP;
    public static Goal endP;

    public static void runEnvironment(RandomGenerator rand, JPanel panel, Pos start, Goal goal, DefaultMap map, double dimension, double max_move, double radius) {
        treeNodes.clear();
        treeEdges.clear();
        finalPath.clear();
        startP = start;
        endP = goal;
        rrtSearch(rand, startP, endP, map, dimension, max_move, radius);
        panel.repaint();
    }

    // Create a random free point not inside any obstacle
    public static Pos generateFreePos(RandomGenerator rand, DefaultMap map, double dimension) {
        Pos p;
        do {
            p = new Pos(rand.nextDouble()*map.getMap_size(), rand.nextDouble()* map.getMap_size());
        } while (!isFree(p, map.getObstacles(), dimension));
        return p;
    }

    // Check if a point is free (not inside obstacles)
    public static boolean isFree(Pos p, List<Obstacle> obstacleList, double dimension) {
        boolean free = true;
        for (int i = 0; i < obstacleList.size(); i++) {
            if (p.getX() >= obstacleList.get(i).getX()-obstacleList.get(i).getW() - dimension &&
                    p.getX() <= obstacleList.get(i).getX() + obstacleList.get(i).getW() + dimension &&
                    p.getY() >= obstacleList.get(i).getY()-obstacleList.get(i).getW() - dimension &&
                    p.getY() <= obstacleList.get(i).getY() + obstacleList.get(i).getW() + dimension){
                free = false;
            }
        }
        return free;
    }


    public static void rrtSearch(RandomGenerator rand, Pos start, Goal goal, DefaultMap map, double dimension, double max_move, double radius) {
        Map<Pos, Pos> parentMap = new HashMap<>();
        treeNodes.add(start);

        boolean goalConnected = false;

        while (!goalConnected) {
            Pos randPoint;
            Pos lastPoint = treeNodes.get(treeNodes.size()-1);
            do {
                randPoint = new Pos(rand.nextDouble()*map.getMap_size(), rand.nextDouble()*map.getMap_size());
            } while (!isFree(randPoint,map.getObstacles(),dimension)); //|| distance(randPoint,goal.getPos()) >= distance(lastPoint,goal.getPos()));

            Pos nearest = getNearest(randPoint);
            Pos newPoint = simulateMovement(nearest, randPoint, map.getObstacles(), dimension, max_move);

            if (!newPoint.equals(nearest) && isCollisionFree(nearest, newPoint, map.getObstacles(), dimension)) {
                List<Pos> possibleParents = near(newPoint,radius);
                Pos parent = chooseParent(possibleParents,nearest,newPoint,map,dimension);
                treeNodes.add(newPoint);
                treeEdges.add(new Pos[]{parent, newPoint});
                parentMap.put(newPoint, parent);
                for (Pos p : possibleParents) {
                    double weight = newPoint.getWeight() + distance(p,newPoint);
                    if (weight < p.getWeight() && isCollisionFree(p,newPoint,map.getObstacles(),dimension)) {
                        p.setWeight(weight);
                        treeEdges.add(new Pos[]{newPoint,p});
                        parentMap.put(p,newPoint);
                    }
                }
            }
            Pos closestToGoal = getNearest(goal.getPos());
            if (distance(closestToGoal, goal.getPos()) < max_move && isCollisionFree(closestToGoal, goal.getPos(), map.getObstacles(), dimension)) {
                parentMap.put(goal.getPos(), closestToGoal);
                treeEdges.add(new Pos[]{closestToGoal, goal.getPos()});
                goalConnected = true;
                Pos current = goal.getPos();
                while (current != null) {
                    finalPath.add(0, current);
                    current = parentMap.get(current);
                }
                System.out.println("Path found!");
            }
        }
    }


    public static Pos getNearest(Pos target) {
        Pos best = null;
        double minDist = Double.MAX_VALUE;
        for (Pos p : treeNodes) {
            double dist = distance(p, target);
            if (dist < minDist) {
                minDist = dist;
                best = p;
            }
        }
        return best;
    }


    public static Pos simulateMovement(Pos from, Pos to, List<Obstacle> obstacleList, double dimension, double max_move) {
        double angle = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
        double newX = from.getX() + max_move * Math.cos(angle);
        double newY = from.getY() + max_move * Math.sin(angle);
        Pos p = new Pos(newX, newY, from.getWeight() + max_move);
        return isFree(p,obstacleList,dimension) ? p : from;
    }


    public static boolean isCollisionFree(Pos a, Pos b, List<Obstacle> obstacleList, double dimension) {
        double steps = 100;
        boolean free = true;
        for (int i = 0; i <= steps; i++) {
            double t = i / steps;
            double x = a.getX() * (1 - t) + b.getX() * t;
            double y = a.getY() * (1 - t) + b.getY() * t;
            if (!isFree(new Pos(x, y), obstacleList, dimension)) {
                free = false;
            }
        }
        return free;
    }


    public static List<Pos> near(Pos target, double radius){
        List<Pos> candidates = new ArrayList<>();
        for (Pos p: treeNodes){
            if (distance(p,target) <= radius){
                candidates.add(p);
            }
        }
        return candidates;
    }

    public static Pos chooseParent(List<Pos> parents, Pos nearest, Pos newNode, DefaultMap map, double dimension){
        Pos candidate = nearest;
        double weight = nearest.getWeight() + distance(nearest,newNode);
        double new_weight;
        for (Pos p: parents){
            new_weight = p.getWeight() + distance(p,newNode);
            if (new_weight < weight && isCollisionFree(p,newNode,map.getObstacles(), dimension)) {
                candidate = p;
                weight = new_weight;
            }
        }
        newNode.setWeight(weight);
        return candidate;
    }


    // Calculate Euclidean distance
    public static double distance(Pos a, Pos b) {
        return Math.hypot(a.getX() - b.getX(), a.getY() - b.getY());
    }
}
