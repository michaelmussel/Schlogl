package stark.planning;

import java.util.Objects;

public class Pos {

    private double x;
    private double y;
    private double weight;

    public Pos(double x, double y, double w){
        this.x = x;
        this.y = y;
        this.weight = w;
    }

    public Pos(double x, double y){
        this.x = x;
        this.y = y;
        this.weight = 0.0;
    }

    public double getX(){
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWeight(){
        return weight;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void  setWeight(double w){
        this.weight = w;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pos)) return false;
        Pos position = (Pos) o;
        return x == position.getX() && y == position.getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

}
