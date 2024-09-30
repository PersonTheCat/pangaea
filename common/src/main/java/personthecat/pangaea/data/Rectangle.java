package personthecat.pangaea.data;

public record Rectangle(int minX, int minY, int maxX, int maxY) {
    public boolean containsPoint(int x, int y) {
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY;
    }
}
