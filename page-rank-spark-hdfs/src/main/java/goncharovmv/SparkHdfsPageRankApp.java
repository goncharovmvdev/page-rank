package goncharovmv;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.lib.PageRank;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.storage.StorageLevel;
import scala.Tuple2;
import scala.reflect.ClassTag;

import java.util.ArrayList;
import java.util.List;

public class SparkHdfsPageRankApp {
    public static void main(String[] args) {
        SparkConf conf = new SparkConf()
                .setAppName("PageRankHdfs")
                .setMaster("local[*]") // Локальный режим
                .set("spark.hadoop.fs.defaultFS", "hdfs://localhost:9000");

        JavaSparkContext jsc = new JavaSparkContext(conf);

        SparkSession spark = SparkSession.builder().config(conf).getOrCreate();

        genGraph(spark);

        Dataset<Row> edgesDF = spark.read().parquet("hdfs://edges.parquet");

        JavaRDD<Edge<Integer>> edgesRDD = edgesDF.javaRDD().map(row -> {
            long src = row.getLong(row.fieldIndex("src"));
            long dst = row.getLong(row.fieldIndex("dst"));
            return new Edge<>(src, dst, 1);  // Для невзвешенного графа вес 1
        });

        JavaRDD<Tuple2<Object, String>> verticesSrc = edgesDF.select("src").javaRDD()
                .map(row -> new Tuple2<>(row.getLong(0), ""));

        JavaRDD<Tuple2<Object, String>> verticesDst = edgesDF.select("dst").javaRDD()
                .map(row -> new Tuple2<>(row.getLong(0), ""));

        JavaRDD<Tuple2<Object, String>> verticesRDD = verticesSrc.union(verticesDst).distinct();

        ClassTag<String> vertexTag = scala.reflect.ClassTag$.MODULE$.apply(String.class);
        ClassTag<Integer> edgeTag = scala.reflect.ClassTag$.MODULE$.apply(Integer.class);

        RDD<Tuple2<Object, String>> vertices = JavaRDD.toRDD(verticesRDD);
        RDD<Edge<Integer>> edges = JavaRDD.toRDD(edgesRDD);

        Graph<String, Integer> graph = Graph.apply(
                vertices,
                edges,
                "",
                StorageLevel.MEMORY_AND_DISK(),
                StorageLevel.MEMORY_AND_DISK(),
                vertexTag,
                edgeTag
        );

        Graph<Object, Object> ranks = PageRank.runUntilConvergence(
                graph,
                0.0001,
                0.15,
                vertexTag,
                edgeTag
        );

        ranks.vertices().toJavaRDD().foreach(tuple -> {
            System.out.println("Vertex " + tuple._1 + " has rank " + tuple._2);
        });

        jsc.stop();
        spark.stop();
    }

    public static void genGraph(SparkSession spark) {
        StructType schema = new StructType(new StructField[]{
                new StructField("src", DataTypes.LongType, false, Metadata.empty()),
                new StructField("dst", DataTypes.LongType, false, Metadata.empty())
        });

        List<Row> rows = new ArrayList<>();

        long numEdges = 1_000L;
        for (long i = 0; i < numEdges; i++) {
            long src = i % numEdges;
            long dst = (i * 7) % numEdges;
            rows.add(RowFactory.create(src, dst));
            if (i % numEdges == 0) {
                System.out.println("Generated " + i + " edges");
            }
        }

        Dataset<Row> edgesDF = spark.createDataFrame(rows, schema);

        edgesDF.write().mode("overwrite")
                .parquet("hdfs://localhost:9000/input/edges.parquet");
    }
}