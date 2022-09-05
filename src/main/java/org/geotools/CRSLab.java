package org.geotools;

import org.geotools.data.*;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JProgressWindow;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ExecutionException;

/**
 * @ClassName
 * @Description TODO
 * @Author LeviFan
 * @Date 2022/8/30 19:34
 * @Version 1.0
 **/
public class CRSLab {
    private File sourceFile;
    private SimpleFeatureSource featureSource;
    private MapContent map;

    public static void main(String[] args) throws Exception{
        CRSLab lab = new CRSLab();
        lab.displayShapefile();
    }
    private  void generateMathTransform(SimpleFeatureType schema,SimpleFeatureCollection featureCollection){
        CoordinateReferenceSystem dataCRS = schema.getCoordinateReferenceSystem();
        CoordinateReferenceSystem worldCRS = map.getCoordinateReferenceSystem();
        boolean lenient = true;
        try {
            MathTransform transform = CRS.findMathTransform(dataCRS,worldCRS,lenient);
            DataStoreFactorySpi factorySpi = new ShapefileDataStoreFactory();
            Map<String, Serializable> create = new HashMap<>();
            create.put("url", sourceFile.toURI().toURL());
            create.put("create spatial index",Boolean.TRUE);

            DataStore dataStore = factorySpi.createNewDataStore(create);
            SimpleFeatureType featureType = SimpleFeatureTypeBuilder.retype(schema,worldCRS);
            dataStore.createSchema(featureType);

            String createName = dataStore.getTypeNames()[0]; // 创建一个shapeFile

            Transaction transaction = new DefaultTransaction("Reject");
            try(FeatureWriter<SimpleFeatureType,SimpleFeature> write = dataStore.getFeatureWriterAppend(createName,transaction);
                SimpleFeatureIterator iterator = featureCollection.features()){
                while(iterator.hasNext()){
                    SimpleFeature feature = iterator.next();
                    SimpleFeature copy = write.next();
                    copy.setAttributes(feature.getAttributes());

                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    Geometry geometry1 = JTS.transform(geometry,transform);

                    copy.setDefaultGeometry(geometry1);
                    write.write();
                }
                transaction.commit();
                JOptionPane.showMessageDialog(null,"导出");
            } catch (TransformException e) {
                System.out.println("hahah");
                e.printStackTrace();
            }

        } catch (FactoryException e) {
            e.printStackTrace();
            System.out.println("没有生成坐标转换");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void displayShapefile() throws IOException {
        sourceFile = JFileDataStoreChooser.showOpenFile("shp",null);
        if(sourceFile ==null){
            return;
        }
        FileDataStore store= FileDataStoreFinder.getDataStore(sourceFile);
        featureSource =store.getFeatureSource();

        map = new MapContent();;
        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource,style);
        map.layers().add(layer);

        JMapFrame mapFrame = new JMapFrame(map);
        mapFrame.enableToolBar(true);
        mapFrame.enableStatusBar(true);

        JToolBar toolBar = mapFrame.getToolBar();
        toolBar.addSeparator();
        toolBar.add(new JButton(new ValidateGeometryAction2()));
        toolBar.add(new JButton(new ExportShapefileAction()));
        toolBar.add(new JButton(new ExportShapeProjAction()));
        mapFrame.setSize(800,600);
        mapFrame.setVisible(true);
    }
    class ValidateGeometryAction2 extends  SafeAction{

        public ValidateGeometryAction2() {
            super("Validate geometry");
            putValue(Action.SHORT_DESCRIPTION,"check geometry");
        }

        @Override
        public void action(ActionEvent e) throws Throwable {
            SwingWorker worker = new SwingWorker<String,Object>() {
                @Override
                protected String doInBackground() throws Exception {
                    final JProgressWindow progressWindow = new JProgressWindow(null);
                    progressWindow.setTitle("validate geometry");
                    int numInvalid = validateFeatureGeometry(progressWindow);
                    if(numInvalid == 0){
                        return "all right";
                    }else{
                        return "unvalid feature"+numInvalid;
                    }
                }
                protected  void done(){
                    try{
                        Object result =get();
                        JOptionPane.showMessageDialog(null,result,"geoemtry result",JOptionPane.INFORMATION_MESSAGE);
                    } catch (ExecutionException ex) {
                        ex.printStackTrace();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        }
    }
    class ValidateGeometryAction extends SafeAction{
        ValidateGeometryAction(){
            super("Validate geometry");
            putValue(Action.SHORT_DESCRIPTION,"check geometry");
        }

        @Override
        public void action(ActionEvent e) throws Throwable {
            int numInvalid = validateFeatureGeometry(null);
            String msg;
            if(numInvalid ==0){
                msg="nice";
            }else{
                msg="terrable"+numInvalid;
            }
            JOptionPane.showMessageDialog(null,msg,"result",JOptionPane.INFORMATION_MESSAGE);
        }
    }
    class ExportShapeProjAction extends SafeAction{
        ExportShapeProjAction(){
            super("Export");
            putValue(Action.SHORT_DESCRIPTION,"Export proj");
        }
        @Override
        public void action(ActionEvent e) throws Throwable {
            exportShapeProjAction();
        }
    }
    class ExportShapefileAction extends  SafeAction{
        ExportShapefileAction(){
            super("Export");
            putValue(Action.SHORT_DESCRIPTION,"Export using current crs");
        }

        @Override
        public void action(ActionEvent e) throws Throwable {
            exportToShapefile();
        }
    }
    private int validateFeatureGeometry(ProgressListener progress) throws IOException {
        final SimpleFeatureCollection featureCollection = featureSource.getFeatures();

        class ValidationVisitor implements FeatureVisitor{
            public int numInvalidGeometries = 0;
            public void visit(Feature f){
                SimpleFeature feature = (SimpleFeature) f;
                Geometry geom =(Geometry) feature.getDefaultGeometry();
                if(geom!=null && !geom.isValid()){
                    numInvalidGeometries++;
                    System.out.println("无效"+feature.getID());
                }
            }
        }
        ValidationVisitor visitor = new ValidationVisitor();
        featureCollection.accepts(visitor,progress);
        return visitor.numInvalidGeometries;
    }
    private void exportToShapefile() throws IOException {
        SimpleFeatureType schema = featureSource.getSchema();
        JFileDataStoreChooser chooser = new JFileDataStoreChooser("shp");
        chooser.setDialogTitle("save shapefile");
        chooser.setSaveFile(sourceFile);

        int returnVal = chooser.showSaveDialog(null);
        if(returnVal!=JFileDataStoreChooser.APPROVE_OPTION){//取消了
            return;
        }
        File fiel = chooser.getSelectedFile();
        if(fiel.equals(sourceFile)){
            return;
        }
        SimpleFeatureCollection featureCollection = featureSource.getFeatures();
        generateMathTransform(schema,featureCollection);
    }
    private void exportShapeProjAction(){
        CoordinateReferenceSystem worldCRS = map.getCoordinateReferenceSystem();
        String wkt = worldCRS.toWKT();
        System.out.println(wkt);
    }

    private void setCRS(){
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        try {
            CoordinateReferenceSystem crs = factory.createCoordinateReferenceSystem("EPSG:4326");
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        System.setProperty("org.geotools.referencing.forceXY","true");
    }
    private void autoTransform(String typeName) throws IOException {
        Query query = new Query(typeName);
        query.setCoordinateSystemReproject(map.getCoordinateReferenceSystem());

        SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);

        DataStoreFactorySpi factorySpi = new ShapefileDataStoreFactory();
        Map<String, Serializable> create= new HashMap<>();
        create.put("url","");
        create.put("create spatial index",Boolean.TRUE);
        DataStore newDataStore = factorySpi.createNewDataStore(create);

        newDataStore.createSchema(featureCollection.getSchema());
        Transaction transaction = new DefaultTransaction("Reject");
        SimpleFeatureStore featureStore = (SimpleFeatureStore) newDataStore.getFeatureSource(typeName);
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(featureCollection);
            transaction.commit();

        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        } finally {
            transaction.close();
        }
    }
}
