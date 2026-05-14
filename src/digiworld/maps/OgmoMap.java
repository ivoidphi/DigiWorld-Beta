package digiworld.maps;

import java.util.List;

public class OgmoMap {
    int width;
    int height;
    int offsetX;
    int offsetY;
    List<Layer> layers;

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public List<Layer> getLayers() { return layers; }

    public static class Layer {
        String name;
        int offsetX;
        int offsetY;
        int gridCellWidth;
        int gridCellHeight;
        int gridCellsX;
        int gridCellsY;
        String tileset;
        List<Integer> data;

        public String getName() { return name; }
        public int getOffsetX() { return offsetX; }
        public int getOffsetY() { return offsetY; }
        public int getGridCellWidth() { return gridCellWidth; }
        public int getGridCellHeight() { return gridCellHeight; }
        public int getGridCellsX() { return gridCellsX; }
        public int getGridCellsY() { return gridCellsY; }
        public String getTileset() { return tileset; }
        public List<Integer> getData() { return data; }

        public int getTileId(int x, int y) {
            if (x < 0 || x >= gridCellsX || y < 0 || y >= gridCellsY) return -1;
            int index = y * gridCellsX + x;
            if (index >= 0 && index < data.size()) {
                return data.get(index);
            }
            return -1;
        }
    }
}