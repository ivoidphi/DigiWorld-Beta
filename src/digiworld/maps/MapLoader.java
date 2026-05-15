package digiworld.maps;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MapLoader {
    private MapLoader() {}

    public static OgmoMap load(String path) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        } catch (IOException e) {
            System.err.println("[MapLoader] Failed to load: " + path);
            return null;
        }

        try {
            return parseJson(content.toString());
        } catch (Exception e) {
            System.err.println("[MapLoader] JSON parse error: " + e.getMessage());
            return null;
        }
    }

    private static OgmoMap parseJson(String json) {
        OgmoMap map = new OgmoMap();

        map.width = parseInt(json, "\"width\"");
        map.height = parseInt(json, "\"height\"");
        map.offsetX = parseInt(json, "\"offsetX\"");
        map.offsetY = parseInt(json, "\"offsetY\"");

        List<OgmoMap.Layer> layers = new ArrayList<>();
        int layerStart = 0;
        while (true) {
            int nameStart = json.indexOf("\"name\":", layerStart);
            if (nameStart == -1) break;

            int nameValStart = json.indexOf("\"", nameStart + 7);
            int nameValEnd = json.indexOf("\"", nameValStart + 1);
            String layerName = json.substring(nameValStart + 1, nameValEnd);

            OgmoMap.Layer layer = new OgmoMap.Layer();
            layer.name = layerName;

            int gridCellsXStart = json.indexOf("\"gridCellsX\"", nameStart);
            if (gridCellsXStart > 0 && gridCellsXStart < nameStart + 500) {
                layer.gridCellsX = parseIntValue(json, gridCellsXStart);
                layer.gridCellsY = parseIntValue(json, json.indexOf("\"gridCellsY\"", gridCellsXStart));
                layer.gridCellWidth = parseIntValue(json, json.indexOf("\"gridCellWidth\"", gridCellsXStart));
                layer.gridCellHeight = parseIntValue(json, json.indexOf("\"gridCellHeight\"", gridCellsXStart));
            }

            int tilesetStart = json.indexOf("\"tileset\"", nameStart);
            if (tilesetStart > 0 && tilesetStart < nameStart + 300) {
                int tsValStart = json.indexOf("\"", tilesetStart + 10);
                int tsValEnd = json.indexOf("\"", tsValStart + 1);
                if (tsValStart > 0 && tsValEnd > tsValStart) {
                    layer.tileset = json.substring(tsValStart + 1, tsValEnd);
                }
            }

            int dataStart = json.indexOf("\"data\": [", nameStart);
            if (dataStart == -1 || dataStart > nameStart + 500) {
                layerStart = nameStart + 10;
                continue;
            }

            layer.data = parseIntArray(json, dataStart);

            layers.add(layer);
            layerStart = nameStart + 10;
        }

        map.layers = layers;
        return map;
    }

    private static int parseInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return 0;
        return parseIntValue(json, idx);
    }

    private static int parseIntValue(String json, int start) {
        if (start < 0) return 0;
        int i = start;
        while (i < json.length() && json.charAt(i) != ':') i++;
        i++;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\n' || json.charAt(i) == '\r')) i++;
        int startVal = i;
        while (i < json.length() && (json.charAt(i) == '-' || (json.charAt(i) >= '0' && json.charAt(i) <= '9'))) i++;
        String num = json.substring(startVal, i).trim();
        try {
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<Integer> parseIntArray(String json, int dataStart) {
        List<Integer> result = new ArrayList<>();
        int arrayStart = json.indexOf("[", dataStart);
        if (arrayStart == -1) return result;

        int depth = 0;
        int i = arrayStart;
        StringBuilder number = new StringBuilder();

        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
                if (depth == 1) number.setLength(0);
            } else if (c == ']') {
                depth--;
                if (depth == 0) break;
            } else if (depth == 1 && (c == '-' || (c >= '0' && c <= '9'))) {
                number.append(c);
            } else if (depth == 1 && number.length() > 0 && (c == ',' || c == ' ')) {
                try {
                    result.add(Integer.parseInt(number.toString()));
                } catch (NumberFormatException ignored) {}
                number.setLength(0);
            }
            i++;
        }
        if (number.length() > 0) {
            try {
                result.add(Integer.parseInt(number.toString()));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}