package org.geotools;

import org.geotools.data.DataStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @ClassName
 * @Description TODO
 * @Author LeviFan
 * @Date 2022/8/30 19:21
 * @Version 1.0
 **/
public class CreatePointUtility {
    static Point createPointByWkt(){
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

        WKTReader reader = new WKTReader(geometryFactory);

        try {
            Point point = (Point)reader.read("POINT(1 1)");
            return point;
        } catch (ParseException e) {
            System.out.println("读取点数据错误");
            e.printStackTrace();
        }
        return null;
    }
    static LineString createLineStringByWkt() throws ParseException {
        GeometryFactory geometryFactory =JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);
        LineString lineString = (LineString) reader.read("LINESTRING(0 2,2 0,8 6)");
        return lineString;
    }
    static Polygon createPolygonStringByWkt() throws ParseException {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        WKTReader reader = new WKTReader(geometryFactory);
        Polygon polygon = (Polygon) reader.read("POLYGON()");
        return polygon;
    }
    static Point createPointByCoord(){
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

        Coordinate coord = new Coordinate(1,1);
        Point point = geometryFactory.createPoint(coord);
        return point;
    }
    static LineString createLineStringByCoords() {
        GeometryFactory geometryFactory  = JTSFactoryFinder.getGeometryFactory(null);

        Coordinate[] coors = new Coordinate[]{new Coordinate(0,2),new Coordinate(2,0)};
        LineString line =geometryFactory.createLineString(coors);
        return line;
    }
    static Polygon createPolygonByCoords() {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
        Coordinate[] coors = new Coordinate[]{new Coordinate(0,0),new Coordinate(2,0),new Coordinate(2,2),new Coordinate(0,0) };
        Polygon polygon = geometryFactory.createPolygon(coors);
        return polygon;
    }

    static void checkType(Map<String, Serializable> params) throws IOException {
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore shapefileDataStore = (ShapefileDataStore) dataStoreFactory.createDataStore(params);
        String typeNames = shapefileDataStore.getTypeNames()[0];
    }
}
