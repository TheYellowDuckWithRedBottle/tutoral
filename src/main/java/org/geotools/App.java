package org.geotools;

import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args ) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException, SchemaException, IOException {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        // 读取csv文件
        File file = JFileDataStoreChooser.showOpenFile("csv",null);
        if(file == null){
            return;
        }

        // 构建一个FeatureType 为点
        final SimpleFeatureType TYPE =
                DataUtilities.createType(
                        "Location",
                        "the_geom:Point:srid=4326,"
                                + // <- the geometry attribute: Point type
                                "name:String,"
                                + // <- a String attribute
                                "number:Integer" // a number attribute
                );

        // 用来将点转为要素 其中还要加入属性变为要素 需要simpleFeatureType来实例化
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        // 定义一串要素
        List<SimpleFeature> features = new ArrayList<>();
        // 用来生成点工厂
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

        try(BufferedReader reader = new BufferedReader(new FileReader(file))){
            String line = reader.readLine();
            for(line = reader.readLine();line!=null;line=reader.readLine()){
                if(line.trim().length() > 0){
                    String[] tokens = line.split("\\,");

                    double latitude = Double.parseDouble(tokens[0]);
                    double longitude = Double.parseDouble(tokens[1]);
                    String name = tokens[2].trim();
                    int number = Integer.parseInt(tokens[3].trim());
                    // 用geometryFactory构建点
                    Point point = geometryFactory.createPoint(new Coordinate(longitude,latitude));
                    Polygon polygon = (Polygon) point.buffer(0.1);

                    featureBuilder.add(point);
                    featureBuilder.add(name);
                    featureBuilder.add(number);

                    SimpleFeature feature = featureBuilder.buildFeature(null);
                    features.add(feature);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        // 到此为止生成了features
        /**
         * 生成shpfile，入参为 type，features,file
         */
        File newFile = getNewShapeFile(file);
        URL URL = newFile.toURI().toURL();
        // 生成shapeFile
        writeShpFile(TYPE,features,URL);

    }

    /**
     * 将features写进shp文件
     * @param TYPE
     * @param features
     * @param URL
     * @throws IOException
     */
    static void writeShpFile(SimpleFeatureType TYPE,List<SimpleFeature> features,URL URL) throws IOException {

        Map<String,Serializable> params = new HashMap<>();
        params.put("url",URL);
        params.put("create spatial index",true);

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        ShapefileDataStore ShpfileStore = (ShapefileDataStore)dataStoreFactory.createDataStore(params);

        ShpfileStore.createSchema(TYPE);
        //用transaction创建文件 将features写进shp
        Transaction transaction = new DefaultTransaction("create");

        String typeName = ShpfileStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = ShpfileStore.getFeatureSource(typeName);
        SimpleFeatureType SHAP_TYPE = featureSource.getSchema();

        if(featureSource instanceof SimpleFeatureStore){
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            SimpleFeatureCollection collection = new ListFeatureCollection(TYPE,features);
            featureStore.setTransaction(transaction);
            try{
                featureStore.addFeatures(collection);
                transaction.commit();
            }catch (Exception problem){
                problem.printStackTrace();
                transaction.rollback();
            }finally {
                transaction.close();
            }
        }else{
            System.out.println("不支持读写");
            System.exit(1);
        }
    }
    // 保存shapefile生成一个，返回一个文件
    private static File getNewShapeFile(File csvFile){
        String path = csvFile.getAbsolutePath();
        String newPath = path.substring(0,path.length() -4)+".shp";

        JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
        chooser.setDialogTitle("SAVE SHP");
        chooser.setSelectedFile(new File(newPath));

        int returnVal =chooser.showSaveDialog(null);
        if(returnVal!=JFileDataStoreChooser.APPROVE_OPTION){
            System.exit(0);
        }

        File newFile = chooser.getSelectedFile();
        if(newFile.equals(csvFile)){
            System.out.println("不能替换文件");
            System.exit(0);
        }
        return newFile;
    }
    private static SimpleFeatureType createFeatureType(){
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setSRS(String.valueOf(DefaultGeographicCRS.WGS84));

        builder.add("the_geom",Point.class);
        builder.length(15).add("Name",String.class);
        builder.add("number",Integer.class);

        final SimpleFeatureType LOCATION = builder.buildFeatureType();
        return LOCATION;
    }
    private static SimpleFeatureType createFeatrueType1() throws SchemaException {
        SimpleFeatureType Type = DataUtilities.createType(
                "Location",
                "the_geom:Point:srid=4326,"
                        + // <- the geometry attribute: Point type
                        "name:String,"
                        + // <- a String attribute
                        "number:Integer" // a number attribute
        );
        return Type;
    }

}
