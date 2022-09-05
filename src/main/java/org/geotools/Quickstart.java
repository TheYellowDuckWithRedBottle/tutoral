package org.geotools;

import com.csvreader.CsvReader;
import org.geotools.data.*;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @ClassName
 * @Description TODO
 * @Author LeviFan
 * @Date 2022/8/24 15:44
 * @Version 1.0
 **/
public class Quickstart {
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(Quickstart.class);
    public static void main(String[] args) throws Exception{
        LOGGER.info("快速开始");
        LOGGER.config("welcome develops");
        LOGGER.info("jajj");

        File file = JFileDataStoreChooser.showOpenFile("shp",null);

        Map<String,Object> params = new HashMap<>();
        URL url = file.toURI().toURL();
        params.put("url",url);
        params.put("create spatial index",false);
        params.put("memory mapped buffer",false);
        params.put("charset","ISO-8859-1");

        DataStore datastore = DataStoreFinder.getDataStore(params);
        SimpleFeatureSource featureSource = datastore.getFeatureSource(datastore.getTypeNames()[0]);
        if(file == null){
            return;
        }

        LOGGER.config("File selected"+ file);
        FileDataStore filestore = FileDataStoreFinder.getDataStore(file);
//        SimpleFeatureSource featureSource = filestore.getFeatureSource();
        // 做一个缓存
        SpatialIndexFeatureCollection spatialIndexFeatureCollection = new SpatialIndexFeatureCollection(featureSource.getFeatures());
        SimpleFeatureSource cacheSource = DataUtilities.source(spatialIndexFeatureCollection);


        MapContent map = new MapContent();
        map.setTitle("快速");

        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(cacheSource,style);

        map.addLayer(layer);
        JMapFrame.showMap(map);
    }

}
