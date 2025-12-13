import java.util.*;

class Station {
    private String id;
    private String name;
    private String line;
    
    public Station(String id, String name, String line) {
        this.id = id;
        this.name = name;
        this.line = line;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLine() {
        return line;
    }
}

class Edge {
    private String toStationId;
    private int time;
    
    public Edge(String toStationId, int time) {
        this.toStationId = toStationId;
        this.time = time;
    }
    
    public String getToStationId() {
        return toStationId;
    }
    
    public int getTime() {
        return time;
    }
}

class SubwayGraph {
    private Map<String, Station> stations;
    private Map<String, List<Edge>> adjacencyList;
    private Map<String, Integer> transferTimes;
    private Map<String, List<String>> stationsByName;
    
    public SubwayGraph() {
        this.stations = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.transferTimes = new HashMap<>();
        this.stationsByName = new HashMap<>();
    }
    
    public void addStation(Station station) {
        stations.put(station.getId(), station);
        adjacencyList.putIfAbsent(station.getId(), new ArrayList<>());
        
        String name = station.getName();
        stationsByName.putIfAbsent(name, new ArrayList<>());
        stationsByName.get(name).add(station.getId());
    }
    
    public void addEdge(String fromId, String toId, int time) {
        adjacencyList.get(fromId).add(new Edge(toId, time));
    }
    
    public void setTransferTime(String stationName, int time) {
        transferTimes.put(stationName, time);
    }
    
    public void connectTransferStations() {
        // 환승역 연결을 자동으로 하지 않음
        // Dijkstra에서 같은 이름 역으로 이동할 때 환승 시간을 적용
    }
    
    public Station getStation(String id) {
        return stations.get(id);
    }
    
    public List<Edge> getEdges(String stationId) {
        return adjacencyList.get(stationId);
    }
    
    public List<String> getStationIdsByName(String name) {
        return stationsByName.getOrDefault(name, new ArrayList<>());
    }
    
    public int getTransferTime(String stationName) {
        return transferTimes.getOrDefault(stationName, 5);
    }
    
    public Set<String> getAllStationIds() {
        return stations.keySet();
    }
}