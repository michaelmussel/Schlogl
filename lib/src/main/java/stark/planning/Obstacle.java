package stark.planning;

public class Obstacle {

    private Pos xy;
    private double h;
    private double w;

    public Obstacle(Pos pos, double height, double width){
        this.xy = pos;
        this.h = height;
        this.w = width;
    }

    public Obstacle(Pos pos){
        this.xy = pos;
        this.h = 0;
        this.w = 0;
    }

    public Pos getPos() {
        return xy;
    }

    public double getX() {
        return xy.getX();
    }

    public double getY() {
        return xy.getY();
    }

    public double getH() {
        return h;
    }

    public double getW() {
        return w;
    }

    public void setPos(Pos xy) {
        this.xy = xy;
    }

    public void setH(double h) {
        this.h = h;
    }

    public void setW(double w) {
        this.w = w;
    }
}
