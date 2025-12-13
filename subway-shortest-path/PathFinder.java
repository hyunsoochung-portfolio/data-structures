import java.util.*;

class PathFinder {
    
    static class Node implements Comparable<Node> {
        String stationId;
        int distance;
        
        Node(String stationId, int distance) {
            this.stationId = stationId;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }
    }
    
    static class PathResult {
        List<String> path;
        int totalTime;
        
        PathResult(List<String> path, int totalTime) {
            this.path = path;
            this.totalTime = totalTime;
        }
    }
    
    private SubwayGraph graph;
    
    public PathFinder(SubwayGraph graph) {
        this.graph = graph;
    }
    
    public PathResult findShortestPath(String startName, String endName) {
        List<String> startIds = graph.getStationIdsByName(startName);
        List<String> endIds = graph.getStationIdsByName(endName);
        
        int minTime = Integer.MAX_VALUE;
        List<String> bestPath = null;
        
        for (String startId : startIds) {
            for (String endId : endIds) {
                PathResult result = dijkstra(startId, endId);
                if (result.totalTime < minTime) {
                    minTime = result.totalTime;
                    bestPath = result.path;
                }
            }
        }
        
        return new PathResult(bestPath, minTime);
    }
    
    private PathResult dijkstra(String startId, String endId) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();
        
        for (String id : graph.getAllStationIds()) {
            distances.put(id, Integer.MAX_VALUE);
        }
        distances.put(startId, 0);
        pq.offer(new Node(startId, 0));
        
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            String currentId = current.stationId;
            
            if (current.distance > distances.get(currentId)) {
                continue;
            }
            
            if (currentId.equals(endId)) {
                break;
            }
            
            // 1. 일반 edge 처리
            for (Edge edge : graph.getEdges(currentId)) {
                String nextId = edge.getToStationId();
                int edgeTime = edge.getTime();
                
                int newDist = distances.get(currentId) + edgeTime;
                
                if (newDist < distances.get(nextId)) {
                    distances.put(nextId, newDist);
                    previous.put(nextId, currentId);
                    pq.offer(new Node(nextId, newDist));
                }
            }
            
            // 2. 환승 처리: 같은 이름의 다른 역으로 이동
            Station currentStation = graph.getStation(currentId);
            String currentName = currentStation.getName();
            String currentLine = currentStation.getLine();
            List<String> sameNameStations = graph.getStationIdsByName(currentName);
            
            if (sameNameStations.size() > 1) {
                for (String transferId : sameNameStations) {
                    if (!transferId.equals(currentId)) {
                        // 직접 edge로 연결되어 있는지 확인
                        boolean hasDirectEdge = false;
                        for (Edge edge : graph.getEdges(currentId)) {
                            if (edge.getToStationId().equals(transferId)) {
                                hasDirectEdge = true;
                                break;
                            }
                        }
                        
                        // 직접 연결되어 있지 않으면 환승으로 처리
                        if (!hasDirectEdge) {
                            Station transferStation = graph.getStation(transferId);
                            String transferLine = transferStation.getLine();
                            
                            // 환승 시간 적용 (같은 호선이어도 직접 연결 안 되면 환승)
                            int transferTime = graph.getTransferTime(currentName);
                            int newDist = distances.get(currentId) + transferTime;
                            
                            if (newDist < distances.get(transferId)) {
                                distances.put(transferId, newDist);
                                previous.put(transferId, currentId);
                                pq.offer(new Node(transferId, newDist));
                            }
                        }
                    }
                }
            }
        }
        
        List<String> path = reconstructPath(previous, startId, endId);
        return new PathResult(path, distances.get(endId));
    }
    
    private List<String> reconstructPath(Map<String, String> previous, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;
        
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        
        Collections.reverse(path);
        return path;
    }
    
    public void printPath(List<String> path) {
        StringBuilder sb = new StringBuilder();
        String lastOutput = null;  // 실제로 출력한 마지막 역 이름
        
        for (int i = 0; i < path.size(); i++) {
            String stationId = path.get(i);
            Station station = graph.getStation(stationId);
            String name = station.getName();
            
            // 환승 여부 판단 (출력 전에 먼저!)
            boolean isTransfer = false;
            if (i > 0 && i < path.size() - 1) {
                Station prevStation = graph.getStation(path.get(i - 1));
                Station nextStation = graph.getStation(path.get(i + 1));
                
                String prevLine = prevStation.getLine();
                String nextLine = nextStation.getLine();
                
                // 같은 이름의 다음 역들을 모두 건너뛰고 실제 다른 이름의 다음 역 찾기
                int nextIdx = i + 1;
                while (nextIdx < path.size()) {
                    Station candidateNext = graph.getStation(path.get(nextIdx));
                    if (!candidateNext.getName().equals(name)) {
                        nextStation = candidateNext;
                        nextLine = candidateNext.getLine();
                        break;
                    }
                    nextIdx++;
                }
                
                // 호선이 바뀌면 환승
                if (!prevLine.equals(nextLine)) {
                    isTransfer = true;
                }
            }
            
            // 같은 이름이면 건너뛰기 (환승역이 아닌 경우만)
            if (lastOutput != null && lastOutput.equals(name)) {
                // 하지만 환승역이면 이미 표시했으므로 계속 건너뜀
                continue;
            }
            
            // 공백 추가 (첫 역 제외)
            if (lastOutput != null) {
                sb.append(" ");
            }
            
            if (isTransfer) {
                sb.append("[").append(name).append("]");
            } else {
                sb.append(name);
            }
            
            lastOutput = name;
        }
        
        System.out.println(sb.toString());
    }
}