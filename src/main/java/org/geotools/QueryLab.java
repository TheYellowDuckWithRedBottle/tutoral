package org.geotools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Map;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JDataStoreWizard;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.geotools.swing.wizard.JWizard;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;


/**
 * @ClassName
 * @Description TODO
 * @Author LeviFan
 * @Date 2022/9/2 14:19
 * @Version 1.0
 **/
@SuppressWarnings("serial")
public class QueryLab extends JFrame {
    private DataStore dataStore;
    private JComboBox<String> featureTypeCBox;
    private JTable table;
    private JTextField text;

    public QueryLab() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        text = new JTextField(80);
        text.setText("include");
        getContentPane().add(text,BorderLayout.NORTH);

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(new DefaultTableModel(5,5));
        table.setPreferredScrollableViewportSize(new Dimension(500,200));

         JScrollPane scrollPane = new JScrollPane(table);
         getContentPane().add(scrollPane,BorderLayout.CENTER);

         JMenuBar menubar = new JMenuBar();
         setJMenuBar(menubar);

         JMenu fileMenu = new JMenu("File");
         menubar.add(fileMenu);

         featureTypeCBox = new JComboBox<>();
         menubar.add(featureTypeCBox);

         JMenu dataMenu = new JMenu("Data");
         menubar.add(dataMenu);
         pack();

         fileMenu.add(new SafeAction("Open shapefile...") {
             @Override
             public void action(ActionEvent e) throws Throwable {
                 ShapefileDataStoreFactory shapefileDataStoreFactory = new ShapefileDataStoreFactory();
                 connect(shapefileDataStoreFactory);
             }
         });
         fileMenu.add(new SafeAction("Connect to PostGIS database...") {
             @Override
             public void action(ActionEvent e) throws Throwable {
                 PostgisNGDataStoreFactory postgisNGDataStoreFactory = new PostgisNGDataStoreFactory();
                 connect(postgisNGDataStoreFactory);
             }
         });
         fileMenu.add(new SafeAction("Connect to DataStore...") {
             @Override
             public void action(ActionEvent e) throws Throwable {
                 connect(null);
             }
         });
         fileMenu.addSeparator();
         fileMenu.add(new SafeAction("Exit") {
             @Override
             public void action(ActionEvent e) throws Throwable {
                 System.exit(0);
             }
         });
        dataMenu.add(new SafeAction("get features") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                filterFeatures();
            }
        });
        dataMenu.add(new SafeAction("Count") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                countFeatures();
            }
        });
        dataMenu.add(new SafeAction("geometry") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                queryFeatures();
            }
        });

    }
    private void filterFeatures() throws IOException {
        String typeName = (String)featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = null;
        try {
            filter = CQL.toFilter(text.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SimpleFeatureCollection features = source.getFeatures(filter);
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);

    }
    private void connect(DataStoreFactorySpi format) throws Exception{
        JDataStoreWizard wizard = new JDataStoreWizard(format,null);
        int result = wizard.showModalDialog();
        if(result == JWizard.FINISH){
            Map<String,Object> connectParameters = wizard.getConnectionParameters();

            dataStore = DataStoreFinder.getDataStore(connectParameters);
            if(dataStore == null){
                JOptionPane.showMessageDialog(null,"could not connet");
            }
            updateUI();
        }
    }
    private void countFeatures() throws IOException {
        String typeName = (String)featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        Filter filter = null;
        try {
            filter = CQL.toFilter(text.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SimpleFeatureCollection features = source.getFeatures(filter);
        int count = features.size();
        JOptionPane.showMessageDialog(text,"选中了"+count+"个");
    }
    private void queryFeatures() throws IOException {
        String  typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = dataStore.getFeatureSource(typeName);

        FeatureType schema = source.getSchema();
        String name = schema.getGeometryDescriptor().getLocalName();

        Filter filter = null;
        try {
            // CQL.toFilter("POPULATION > 30000")
            // CQL.toFilter("CONTAINS(THE_GEOM,POINT(1 2))")
            //CQL.toFilter("BBOX(ATTR1,152.12,151.14,-33.5,-33.51)")
            filter = CQL.toFilter(text.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Query query = new Query(typeName,filter,new String[]{name});

        SimpleFeatureCollection features =source.getFeatures(query);
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
//    private void createFilter(){
//        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
//        Filter filter = ff.propertyGreaterThan(ff.property("POPULATOIN"),ff.literal(12));
//    }
    private void updateUI(){
        try {
            ComboBoxModel<String> cbm = new DefaultComboBoxModel<>(dataStore.getTypeNames());
            featureTypeCBox.setModel(cbm);
            table.setModel(new DefaultTableModel(5,5));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        JFrame frame = new QueryLab();
        frame.setVisible(true);
    }
}
