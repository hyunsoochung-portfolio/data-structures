import java.io.*;

public class Subway {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java Subway [data]");
            return;
        }
        
        try {
            SubwayGraph graph = loadData(args[0]);
            PathFinder pathFinder = new PathFinder(graph);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.equals("QUIT")) {
                    break;
                }
                
                String[] parts = line.split(" ");
                if (parts.length != 2) {
                    continue;
                }
                
                String startStation = parts[0];
                String endStation = parts[1];
                
                PathFinder.PathResult result = pathFinder.findShortestPath(startStation, endStation);
                
                pathFinder.printPath(result.path);
                System.out.println(result.totalTime);
            }
            
            reader.close();
            
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
    
    private static SubwayGraph loadData(String filename) throws IOException {
        SubwayGraph graph = new SubwayGraph();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        
        loadStations(reader, graph);
        loadEdges(reader, graph);
        loadTransferTimes(reader, graph);
        
        graph.connectTransferStations();
        
        reader.close();
        return graph;
    }
    
    private static void loadStations(BufferedReader reader, SubwayGraph graph) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) break;
            
            String[] parts = line.split(" ");
            String id = parts[0];
            String name = parts[1];
            String lineNum = parts[2];
            
            Station station = new Station(id, name, lineNum);
            graph.addStation(station);
        }
    }
    
    private static void loadEdges(BufferedReader reader, SubwayGraph graph) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) break;
            
            String[] parts = line.split(" ");
            String from = parts[0];
            String to = parts[1];
            int time = Integer.parseInt(parts[2]);
            
            graph.addEdge(from, to, time);
        }
    }
    
    private static void loadTransferTimes(BufferedReader reader, SubwayGraph graph) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split(" ");
            String stationName = parts[0];
            int time = Integer.parseInt(parts[1]);
            
            graph.setTransferTime(stationName, time);
        }
    }
}