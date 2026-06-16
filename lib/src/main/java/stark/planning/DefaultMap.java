package stark.planning;

import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;

public class DefaultMap {

    private final double map_size;
    private final double obs_max_size;
    private final double obs_max_height;
    private final int num_obstacles;

    public static List<Obstacle> obstacles;

    public DefaultMap(RandomGenerator rand, double size, double obs_size, double obs_height, int n_obs){
        this.map_size = size;
        this.obs_max_size = obs_size;
        this.obs_max_height = obs_height;
        this.num_obstacles = n_obs;

        obstacles = new ArrayList<>();
        while (obstacles.size() < num_obstacles) {
            Obstacle obs = new Obstacle(new Pos(rand.nextDouble()*(map_size-obs_max_size), rand.nextDouble()*(map_size-obs_max_size)),
                    rand.nextDouble()*obs_max_height, rand.nextDouble()*obs_max_size);
            if (isValid(obs.getPos(), obstacles)) {
                obstacles.add(obs);
            }
        }
    }

    public static boolean isValid(Pos p, List<Obstacle> obstacleList) {
        boolean in = true;
        for (int i = 0; i < obstacleList.size(); i++) {
            if (p.getX() >= obstacleList.get(i).getX()-obstacleList.get(i).getW() && p.getX() <= obstacleList.get(i).getX() + obstacleList.get(i).getW() &&
                    p.getY() >= obstacleList.get(i).getY()-obstacleList.get(i).getW() && p.getY() <= obstacleList.get(i).getY() + obstacleList.get(i).getW()){
                in = false;
            }
        }
        return in;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }

    public double getMap_size() {
        return map_size;
    }

    public double getObs_max_size() {
        return obs_max_size;
    }

    public double getObs_max_height() {
        return obs_max_height;
    }

    public void setObstacles(List<Obstacle> obstacles) {
        DefaultMap.obstacles = obstacles;
    }
}
