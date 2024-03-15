package unsw.blackout;

public class Slope {
    private int startAngle;
    private int endAngle;
    private int gradient;

    public Slope(int startAngle, int endAngle, int gradient) {
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.gradient = gradient;
    }

    public int getStartAngle() {
        return startAngle;
    }

    public void setStartAngle(int startAngle) {
        this.startAngle = startAngle;
    }

    public int getEndAngle() {
        return endAngle;
    }

    public void setEndAngle(int endAngle) {
        this.endAngle = endAngle;
    }

    public int getGradient() {
        return gradient;
    }

    public void setGradient(int gradient) {
        this.gradient = gradient;
    }
}
