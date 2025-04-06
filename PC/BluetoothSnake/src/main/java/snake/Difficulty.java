package snake;

public enum Difficulty {
    EASY(100, 0), MEDIUM(50, 0), HARD(50, 10);

    // The delay between snake movements in milliseconds
    private final int delay;

    // The number of obstacles for this difficulty level
    private final int numObstacles;

    // Enum constructor for difficulty levels.
    Difficulty(int delay, int numObstacles) {
        this.delay = delay;
        this.numObstacles = numObstacles;
    }

    // Gets the movement delay for this difficulty level.
    public int getDelay() {
        return delay;
    }

    // Gets the number of obstacles for this difficulty level.
    public int getNumObstacles() {
        return numObstacles;
    }
}