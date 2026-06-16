package stark.planning;

public class Goal {

    private Pos xy;
    private double priority;

    public Goal(Pos pos, double prio){
        this.xy = pos;
        this.priority = prio;
    }

    public Goal(Pos pos){
        this.xy = pos;
        this.priority = 0;
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

    public double getPriority() {
        return priority;
    }

    public void setPos(Pos xy) {
        this.xy = xy;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }

}
