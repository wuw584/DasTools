package hdf.view.DAS;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import hdf.object.Datatype;
import hdf.object.FileFormat;
import hdf.object.Group;
import hdf.object.ScalarDS;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import hdf.view.ViewProperties.BITMASK_OP;

import hdf.view.DAS.Detrend;
import com.github.psambit9791.jdsp.filter.Butterworth;
import com.github.psambit9791.jdsp.windows.Tukey;


public final class DASTools {
    private static final Logger log = LoggerFactory.getLogger(DASTools.class);

    private static double[] median;

    private static double[] doubleData;

    private static double clim;

    private static Butterworth butter = new Butterworth(1000);

    private int order = 6;

    public static double[] getDoubleData(Object rawData, long w, long h){
        String cname = rawData.getClass().getName();
        char dname = cname.charAt(cname.lastIndexOf('[') + 1);
        int size = Array.getLength(rawData);
        double[] doubleData = new double[size];
        long idxSrc = 0;
        long idxDst = 0;
        switch (dname) {
            case 'S':
                short[] s = (short[]) rawData;
                for (long i = 0; i < h; i++) {
                    for (long j = 0; j < w; j++) {
                        idxSrc = idxDst = j * h + i;
                        doubleData[(int)idxDst] = (double) s[(int)idxSrc];
                    }
                }
                break;
                default:
                    doubleData = null;
                break;
        } 
        return doubleData;
    }

    public static double getClim(double[] doubleData){
        StandardDeviation standardDeviation = new StandardDeviation();
        return standardDeviation.evaluate(doubleData);
    }

    //Detrend data, remove common-mode noise
    public static double[] dasPreprocess(double[] doubleData){
        Detrend detrend = new Detrend(doubleData);
        //w = 2176 most of median of a row are too small to affect the image
        // double median = new Median().evaluate(doubleData);
        return detrend.detrendSignal();
    }

    public static double[] highPass(double[] rawData, double fs , int order , double cutoffFreq){
        double[] highPassed = new Butterworth(fs).highPassFilter(rawData,order,cutoffFreq);
        return highPassed;
    }

    public static double[] tapering(double[] rawData, long w,long h,  double alpha){
        double[] tukey = new Tukey((int)h, alpha).getWindow();
        int size = Array.getLength(rawData);
        long idxSrc = 0;
        long idxDst = 0;
        double[] tapering = new double[size];
        log.debug("{}",tukey.length);
        for (long i = 0; i < h; i++) {
            for (long j = 0; j < w; j++) {
                idxSrc = idxDst = j * h + i;
                tapering[(int)idxDst] = rawData[(int)idxSrc] * tukey[(int) j];
            }
        }
        return tapering;
    }

    //detrend data, remove common mode noise
    public static double[] detrend(Object[] rawData, long w , long h){
        double[] doubleData = DASTools.getDoubleData(rawData,w,h);
        double[] detrendedData = DASTools.dasPreprocess(doubleData);
        return detrendedData;
    }

    public static double[] filter(double[] detrendedData, long w , long h){
        double[] filter = DASTools.tapering(detrendedData,w,h,0.2);
        filter = DASTools.highPass(filter,0.001, 6,2);
        filter =  DASTools.dasPreprocess(filter);
        return filter;
    }


}


