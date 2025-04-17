package com.example;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Comparator;

import com.opencsv.exceptions.CsvValidationException;
import org.janusgraph.core.*;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import com.opencsv.CSVReader;


import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.Edge;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class InMemoryTest {
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();

        JanusGraph graph = JanusGraphFactory.build()
                .set("storage.backend", "foundationdb")
                .set("storage.backend", "org.janusgraph.diskstorage.foundationdb.FoundationDBStoreManager")
                .open();

        createAirRouteSchema(graph);

        loadAirports(graph, "data/air-routes-latest-nodes.csv");
        System.out.println("Total airports: " + graph.traversal().V().count().next());
        loadEdges(graph, "data/air-routes-latest-edges.csv");


        System.out.println("Total routes: " + graph.traversal().E().count().next());

        System.out.println("Loaded in " + (System.currentTimeMillis() - start) + " ms");
        graph.close();
    }
    private static void createAirRouteSchema(JanusGraph graph) {
        ManagementSystem mgmt = (ManagementSystem) graph.openManagement();
        // optimize lookup by id
        PropertyKey idKey = mgmt.makePropertyKey("id").dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
        mgmt.buildIndex("byId", Vertex.class).addKey(idKey).buildCompositeIndex();

        mgmt.makePropertyKey("code").dataType(String.class).make();
        mgmt.makePropertyKey("icao").dataType(String.class).make();
        mgmt.makePropertyKey("desc").dataType(String.class).make();
        mgmt.makePropertyKey("region").dataType(String.class).make();
        mgmt.makePropertyKey("runways").dataType(Integer.class).make();
        mgmt.makePropertyKey("longest").dataType(Integer.class).make();
        mgmt.makePropertyKey("elev").dataType(Integer.class).make();
        mgmt.makePropertyKey("city").dataType(String.class).make();
        mgmt.makePropertyKey("lat").dataType(Float.class).make();
        mgmt.makePropertyKey("lon").dataType(Float.class).make();
        mgmt.makePropertyKey("country").dataType(String.class).make();
        mgmt.makePropertyKey("type").dataType(String.class).make();

        mgmt.makeVertexLabel("airport").make();
        mgmt.makeEdgeLabel("route").make();

        mgmt.commit();
    }

    private static void loadAirports(JanusGraph graph, String filepath) {
        try {
            CSVReader reader = new CSVReader(new FileReader(filepath));
            String[] tokens;
            boolean firstline = true;
            while ((tokens = reader.readNext()) != null) {
                if (firstline) {
                    firstline = false;
                    continue;
                }
                if (!tokens[1].equals("airport")) continue;
                JanusGraphTransaction tx = graph.newTransaction();
                try {
                    Vertex airport = tx.addVertex("airport");
                    airport.property("id", Integer.parseInt(tokens[0].trim()));
                    airport.property("type", tokens[2]);
                    airport.property("code", tokens[3].trim());
                    airport.property("icao", tokens[4]);
                    airport.property("desc", tokens[5]);
                    airport.property("region", tokens[6]);
                    airport.property("country", tokens[10]);
                    airport.property("city", tokens[11]);

                    airport.property("runways", tokens[7].trim().isEmpty() ? 0 : Integer.parseInt(tokens[7]));
                    airport.property("longest", tokens[8].trim().isEmpty() ? 0 : Integer.parseInt(tokens[8]));
                    airport.property("elev", tokens[9].trim().isEmpty() ? 0 : Integer.parseInt(tokens[9]));
                    airport.property("lat", tokens[12].isEmpty() ? 0 : Float.parseFloat(tokens[12]));
                    airport.property("lon", tokens[13].isEmpty() ? 0 : Float.parseFloat(tokens[13]));

                    tx.commit();
                } catch (Exception e) {
                    // tx.rollback();
                    // System.out.println("Failed to insert airport: " + tokens[3]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
        } catch (IOException e) {
            System.out.println("Error reading file.");
        } catch (CsvValidationException e) {
            System.out.println("Error parsing CSV.");
        }
    }

    private static void loadEdges(JanusGraph graph, String filepath) {
        ManagementSystem mgmt = (ManagementSystem) graph.openManagement();
        JanusGraphTransaction tx = graph.newTransaction();
        try {
            CSVReader reader = new CSVReader(new FileReader(filepath));
            String[] tokens;
            boolean firstline = true;
            while ((tokens = reader.readNext()) != null) {
                if (firstline) {
                    firstline = false;
                    continue;
                }

                String from = tokens[1].trim();
                String to = tokens[2].trim();
                String distanceStr = tokens[4].trim();
                String label = tokens[3].trim();
                if (!label.equalsIgnoreCase("route")) continue;
                try {
                    Vertex one = tx.traversal().V().has("id", Integer.parseInt(from)).tryNext().orElse(null);
                    Vertex two = tx.traversal().V().has("id", Integer.parseInt(to)).tryNext().orElse(null);
                    if (one != null && two != null) {
                        Edge edge = one.addEdge("route", two);
                        if (!distanceStr.isEmpty()) {
                            edge.property("distance", Integer.parseInt(distanceStr));
                        }
                        // tx.commit();
                    }
                    // if (one == null || two == null) {
                        // tx.rollback();
                    // }

                } catch (Exception e) {
                    // tx.rollback();
                    // System.out.println("Failed to insert route from " + from + " to " + to);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
        } catch (IOException e) {
            System.out.println("Error reading file.");
        } catch (CsvValidationException e) {
            System.out.println("Error parsing CSV.");
        } finally {
            tx.commit();
        }
    }
}

