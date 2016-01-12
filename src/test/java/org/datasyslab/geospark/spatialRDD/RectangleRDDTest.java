package org.datasyslab.geospark.spatialRDD;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.lang.IllegalClassException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.datasyslab.geospark.gemotryObjects.EnvelopeWithGrid;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Created by jinxuanwu on 1/3/16.
 */
public class RectangleRDDTest implements Serializable{
    public static JavaSparkContext sc;
    static Properties prop;
    static InputStream input;
    static String InputLocation;
    static Integer offset;
    static String splitter;
    static String gridType;
    static String indexType;
    static Integer numPartitions;
    @BeforeClass
    public static void onceExecutedBeforeAll() {
        SparkConf conf = new SparkConf().setAppName("RectangleTest").setMaster("local[2]");
        sc = new JavaSparkContext(conf);
        Logger.getLogger("org").setLevel(Level.WARN);
        Logger.getLogger("akka").setLevel(Level.WARN);
        prop = new Properties();
        input = PointRDDTest.class.getClassLoader().getResourceAsStream("rectangle.test.properties");
        InputLocation = "file://"+RectangleRDD.class.getClassLoader().getResource("primaryroads.csv").getPath();
        offset = 0;
        splitter = "";
        gridType = "";
        indexType = "";
        numPartitions = 0;

        try {
            // load a properties file
            prop.load(input);
            //InputLocation = prop.getProperty("inputLocation");
            offset = Integer.parseInt(prop.getProperty("offset"));
            splitter = prop.getProperty("splitter");
            gridType = prop.getProperty("gridType");
            indexType = prop.getProperty("indexType");
            numPartitions = Integer.parseInt(prop.getProperty("numPartitions"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }




    /*
        This test case will load a sample data file and
     */
    @Test
    public void testConstructor() throws Exception {
        //The grid type is X right now.
        RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter, gridType, numPartitions);
        //todo: Set this to debug level
        for (EnvelopeWithGrid d : rectangleRDD.grids) {
            System.out.println(d);
        }

        //todo: Move this into log4j.
        Map<Integer, Object> map = rectangleRDD.gridRectangleRDD.countByKey();
        for (Map.Entry<Integer, Object> entry : map.entrySet()) {
            Long number = (Long) entry.getValue();
            Double percentage = number.doubleValue() / rectangleRDD.totalNumberOfRecords;
            System.out.println(entry.getKey() + " : " + String.format("%.4f", percentage));
        }
    }

    /*
     *  This test case test whether the X-Y grid can be build correctly.
     */
    @Test
    public void testXYGrid() throws Exception {
        RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter, "X-Y", 10);

        //todo: Move this into log4j.
        Map<Integer, Object> map = rectangleRDD.gridRectangleRDD.countByKey();

        System.out.println(map.size());

        for (Map.Entry<Integer, Object> entry : map.entrySet()) {
            Long number = (Long) entry.getValue();
            Double percentage = number.doubleValue() / rectangleRDD.totalNumberOfRecords;
            System.out.println(entry.getKey() + " : " + String.format("%.4f", percentage));
        }
    }

    /*
     * If we try to build a index on a rawPointRDD which is not construct with grid. We shall see an error.
     */
    @Test(expected=IllegalClassException.class)
    public void testBuildIndexWithoutSetGrid() throws Exception {
        PointRDD pointRDD = new PointRDD(sc, InputLocation, offset, splitter, numPartitions);
        pointRDD.buildIndex("R-Tree");
    }

    /*
        Test build Index.
     */
    @Test
    public void testBuildIndex() throws Exception {
        RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter, gridType, numPartitions);
        rectangleRDD.buildIndex("R-Tree");
        List<Polygon> result = rectangleRDD.indexedRDD.take(1).get(0)._2().query(rectangleRDD.boundaryEnvelope);
        for(Polygon e: result) {
            System.out.println(e.getEnvelopeInternal());
        }
    }
    /*
     *  If we want to use a grid type that is not supported yet, an exception will be throwed out.
     */
    @Test(expected=IllegalArgumentException.class)
    public void testBuildWithNoExistsGrid() throws Exception {
        RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter, "ff", numPartitions);
    }

    /*
     * If the partition number is set too large, we will
     */
    @Test(expected=IllegalArgumentException.class)
    public void testTooLargePartitionNumber() throws Exception {
        RectangleRDD rectangleRDD = new RectangleRDD(sc, InputLocation, offset, splitter, "X-Y", 1000000);
    }

    @AfterClass
    public static void TearDown() {
        sc.stop();
    }
}