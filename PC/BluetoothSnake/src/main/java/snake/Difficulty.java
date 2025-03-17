package snake;

public enum Difficulty {
    EASY(100, 0), MEDIUM(50, 0), HARD(50, 10);
    private final int delay;
    private final int numObstacles;

    Difficulty(int delay, int numObstacles) {
        this.delay = delay;
        this.numObstacles = numObstacles;
    }

    public int getDelay() {
        return delay;
    }

    public int getNumObstacles() {
        return numObstacles;
    }
}