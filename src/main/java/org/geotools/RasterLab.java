package org.geotools;


import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.map.*;
import org.geotools.styling.SLD;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JParameterListWizard;
import org.geotools.swing.wizard.JWizard;
import org.geotools.util.KVP;
import org.geotools.util.factory.Hints;
import org.opengis.filter.FilterFactory;
import org.opengis.style.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @ClassName
 * @Description TODO
 * @Author LeviFan
 * @Date 2022/9/4 14:57
 * @Version 1.0
 **/
public class RasterLab {
    private StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    private FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    private JMapFrame frame;
    private GridCoverage2DReader reader;

    public static void main(String[] args) throws IOException {
        RasterLab  me = new RasterLab();
        me.getLayersAndDisplay();
    }
    private void getLayersAndDisplay() throws IOException {
        List<Parameter<?>> list = new ArrayList<>();
        list.add(new Parameter<>
                ("image", File.class,"Image","GeoTiff or world",new KVP(Parameter.EXT,"tiff",Parameter.EXT,"jpg")));
        list.add(new Parameter<>("shape",File.class,"Shapefile","Shapefile contents to display",new KVP(Parameter.EXT,"shp")));

        JParameterListWizard wizard = new JParameterListWizard("Image Lab","fill in ",list);
        int finish = wizard.showModalDialog();

        if(finish!= JWizard.FINISH){
            System.exit(0);
        }
        File imageFile =(File)wizard.getConnectionParameters().get("image");
        File shapeFile = (File)wizard.getConnectionParameters().get("shape");
        displayLayer(imageFile,shapeFile);
    }

    private void displayLayer(File imageFile, File shapeFile) throws IOException {
        AbstractGridFormat format = GridFormatFinder.findFormat(imageFile);
        Hints hints = new Hints();

        if(format instanceof GeoTiffFormat){
            hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER,Boolean.TRUE);
        }
        reader = format.getReader(imageFile,hints);
        Style rasterStyle = createGreyScaleStyle(1);

        FileDataStore dataStore = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource shapefileSource = dataStore.getFeatureSource();
        Style shpStyle = SLD.createPolygonStyle(Color.YELLOW,null,0.0f);

        final MapContent map = new MapContent();
        map.setTitle("ImageLab");

        Layer rasterLayer = new GridReaderLayer(reader,rasterStyle);
        map.addLayer(rasterLayer);

        Layer shpLayer = new FeatureLayer(shapefileSource,shpStyle);
        map.addLayer(shpLayer);

        frame = new JMapFrame(map);
        frame.setSize(800,600);
        frame.enableStatusBar(true);
        frame.enableToolBar(true);

        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        JMenu menu = new JMenu("Raster");
        menuBar.add(menu);

        menu.add(new SafeAction("Grey") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                Style style = createGreyScaleStyle();
                if(style!=null){
                    ((StyleLayer)map.layers().get(0)).setStyle(style);
                    frame.repaint();
                }
            }
        });
        menu.add(new SafeAction("RGB") {
            @Override
            public void action(ActionEvent e) throws Throwable {
                Style style = createRBGStyle();
                if(style!=null){
                    ((StyleLayer)map.layers().get(0)).setStyle(style);
                    frame.repaint();
                }
            }
        });
        frame.setVisible(true);
    }

    private Style createGreyScaleStyle(){
        GridCoverage2D cover = null;

        try {
            cover= reader.read(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int numBand = cover.getNumSampleDimensions();
        Integer[] bandNumbers = new Integer[numBand];
        for(int i =0;i<numBand;i++){
            bandNumbers[i] = i+1;
        }

        Object selection = JOptionPane.showInputDialog(frame,
                "Band to use for greys",
                "select an image band",
                JOptionPane.QUESTION_MESSAGE,
                null,
                bandNumbers,
                1);

        if(selection!=null){
            int band = ((Number)selection).intValue();
            return  createGreyScaleStyle(band);
        }
        return null;
    }
    private Style createGreyScaleStyle(int band){
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0), ContrastMethod.NORMALIZE);
        SelectedChannelType sct = sf.createSelectedChannelType(String.valueOf(band),ce);
        ChannelSelection sel = sf.channelSelection(sct);

        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        sym.setChannelSelection(sel);


        return SLD.wrapSymbolizers(sym);
    }

    private Style createRBGStyle(){
        GridCoverage2D cover = null;

        try {
            cover = reader.read(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int numBands = cover.getNumSampleDimensions();
        if(numBands<3){
            return null;
        }
        String[] sampleDimensionNames = new String[numBands];
        for(int i = 0;i<numBands;i++){
            GridSampleDimension dim = cover.getSampleDimension(i);
            sampleDimensionNames[i] = dim.getDescription().toString();
        }

        final int RED =0,GREEN = 1,BLUE=2;
        int[] channelNum = {-1,-1,-1};

        for(int i =0;i<numBands;i++){
            String name = sampleDimensionNames[i].toLowerCase();
            if(name.matches("red.*")){
                channelNum[RED]= i+1;
            }else if(name.matches("green.*")){
                channelNum[GREEN] = i+1;
            }else if(name.matches("blue.*")){
                channelNum[BLUE] =i+1;
            }
        }

        if(channelNum[RED]<0|| channelNum[GREEN]<0||channelNum[BLUE]<0){
            channelNum[RED] =1;
            channelNum[GREEN] =2;
            channelNum[BLUE] =3;
        }
        SelectedChannelType[] sct = new SelectedChannelType[cover.getNumSampleDimensions()];
        ContrastEnhancement ce = sf.contrastEnhancement(ff.literal(1.0),ContrastMethod.NORMALIZE);
        for(int i=0;i<3;i++){
            sct[i] = sf.createSelectedChannelType(String.valueOf(channelNum[i]),ce);
        }
        RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
        ChannelSelection sel = sf.channelSelection(sct[RED],sct[GREEN],sct[BLUE]);

        sym.setChannelSelection(sel);
        return SLD.wrapSymbolizers(sym);
    }
}
