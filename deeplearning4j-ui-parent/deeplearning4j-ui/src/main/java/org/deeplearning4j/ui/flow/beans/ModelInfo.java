package org.deeplearning4j.ui.flow.beans;

import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This bean works as holder for unbounded list of layers. Each layer has it's own place in model's virtual coordinate space.
 * For now, coordinate space is limited to 2 dimensions
 *
 * @author raver119@gmail.com
 */
@Data
public class ModelInfo implements Serializable {
    private final static long serialVersionUID = 119L;
    private long time = System.currentTimeMillis();
    private transient int counter = 0;

    // PLEASE NOTE: Inverted coords here -> Y, X LayerInfo
    //private Table<Integer, Integer, LayerInfo> layers = HashBasedTable.create();
   // private Map<Pair<Integer, Integer>, LayerInfo> layers = new LinkedHashMap<>();
    private List<LayerInfo> layers = new ArrayList<>();

    /**
     * This method maps given layer into model coordinate space
     * @param layer
     */
    public synchronized void addLayer(@NonNull LayerInfo layer) {
        if (!layers.contains(layer)) {
            layer.setId(counter);
            this.layers.add(layer);
            counter++;
        }
    }

    /**
     * This method returns LayerInfo for specified layer name
     * @param name
     * @return
     */
    public LayerInfo getLayerInfoByName(String name) {
        for (LayerInfo layerInfo: layers) {
            if (layerInfo.getName().equalsIgnoreCase(name)) return layerInfo;
        }
        return null;
    }

    /**
     * This method returns LayerInfo for specified grid coordinates
     * @param x
     * @param y
     * @return
     */
    public LayerInfo getLayerInfoByCoords(int x, int y) {
        for (LayerInfo layerInfo: layers) {
            if (layerInfo.getX() == x && layerInfo.getY() == y) return layerInfo;
        }

        return null;
    }

    /**
     * This method returns the total number of nodes within described model
     *
     * @return number of elements
     */
    public int size() {
        return layers.size();
    }

    /**
     * This method returns all LayerTypes used in this model
     * @return
     */
    public Set<String> getLayerTypes() {
        Set<String> set = new LinkedHashSet<>();

        for (LayerInfo layerInfo: layers) {
            set.add(layerInfo.getLayerType());
        }
        return set;
    }

    public Set<LayerInfo> getLayersByType(String layerType) {
        Set<LayerInfo> set = new LinkedHashSet<>();

        for (LayerInfo layerInfo: layers) {
            if (layerInfo.getLayerType().equals(layerType)) set.add(layerInfo);
        }

        return set;
    }
}
