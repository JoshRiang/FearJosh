package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.render.TiledMapManager;
import java.util.*;

// A* Pathfinding
public class PathfindingSystem {
    
    private static final int GRID_SIZE = 16;
    private static final int MAX_PATH_LENGTH = 100;
    
    public static List<float[]> findPath(float startX, float startY, 
                                         float goalX, float goalY,
                                         TiledMapManager tiledMapManager,
                                         float worldWidth, float worldHeight) {
        
        // Grid coords
        int startGridX = (int)(startX / GRID_SIZE);
        int startGridY = (int)(startY / GRID_SIZE);
        int goalGridX = (int)(goalX / GRID_SIZE);
        int goalGridY = (int)(goalY / GRID_SIZE);
        
        int gridWidth = (int)(worldWidth / GRID_SIZE) + 1;
        int gridHeight = (int)(worldHeight / GRID_SIZE) + 1;
        
        // A* init
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Set<String> closedSet = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();
        
        Node startNode = new Node(startGridX, startGridY);
        startNode.gScore = 0;
        startNode.fScore = heuristic(startGridX, startGridY, goalGridX, goalGridY);
        
        openSet.add(startNode);
        allNodes.put(startNode.getKey(), startNode);
        
        int iterations = 0;
        
        while (!openSet.isEmpty() && iterations < MAX_PATH_LENGTH) {
            iterations++;
            Node current = openSet.poll();
            
            // Goal reached
            if (current.x == goalGridX && current.y == goalGridY) {
                return reconstructPath(current);
            }
            
            closedSet.add(current.getKey());
            
            // 8-directional
            double[][] directions = {
                {0, 1, 1.0},
                {1, 0, 1.0},
                {0, -1, 1.0},
                {-1, 0, 1.0},
                {1, 1, 1.41},
                {1, -1, 1.41},
                {-1, -1, 1.41},
                {-1, 1, 1.41}
            };
            
            for (double[] dir : directions) {
                int neighborX = current.x + (int)dir[0];
                int neighborY = current.y + (int)dir[1];
                double moveCost = dir[2];
                
                // Bounds check
                if (neighborX < 0 || neighborX >= gridWidth || 
                    neighborY < 0 || neighborY >= gridHeight) {
                    continue;
                }
                
                String neighborKey = neighborX + "," + neighborY;
                
                // Skip evaluated
                if (closedSet.contains(neighborKey)) {
                    continue;
                }
                
                // Collision check
                if (isBlocked(neighborX, neighborY, tiledMapManager)) {
                    continue;
                }
                
                // Corner check
                if (moveCost > 1.0) {
                    int dx = (int)dir[0];
                    int dy = (int)dir[1];
                    if (isBlocked(current.x + dx, current.y, tiledMapManager) || 
                        isBlocked(current.x, current.y + dy, tiledMapManager)) {
                        continue;
                    }
                }
                
                double tentativeGScore = current.gScore + moveCost;
                
                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    neighbor = new Node(neighborX, neighborY);
                    allNodes.put(neighborKey, neighbor);
                }
                
                // Better path
                if (tentativeGScore < neighbor.gScore) {
                    neighbor.parent = current;
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = tentativeGScore + heuristic(neighborX, neighborY, goalGridX, goalGridY);
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }
        
        // No path
        return new ArrayList<>();
    }
    
    private static boolean isBlocked(int gridX, int gridY, TiledMapManager tiledMapManager) {
        if (tiledMapManager == null || !tiledMapManager.hasCurrentMap()) {
            return false;
        }
        
        float worldX = gridX * GRID_SIZE + GRID_SIZE / 2f;
        float worldY = gridY * GRID_SIZE + GRID_SIZE / 2f;
        
        return !tiledMapManager.isWalkable(worldX, worldY);
    }
    
    @SuppressWarnings("unused")
    private static boolean overlaps(float x1, float y1, float w1, float h1,
                                    float x2, float y2, float w2, float h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 &&
               y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    // Heuristic
    private static double heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
    
    private static List<float[]> reconstructPath(Node goalNode) {
        List<float[]> path = new ArrayList<>();
        Node current = goalNode;
        
        while (current != null) {
            float worldX = current.x * GRID_SIZE + GRID_SIZE / 2f;
            float worldY = current.y * GRID_SIZE + GRID_SIZE / 2f;
            path.add(0, new float[]{worldX, worldY});
            current = current.parent;
        }
        
        return path;
    }
    
    public static List<float[]> simplifyPath(List<float[]> path) {
        if (path.size() <= 2) return path;
        
        List<float[]> simplified = new ArrayList<>();
        simplified.add(path.get(0));
        
        for (int i = 1; i < path.size() - 1; i++) {
            float[] prev = path.get(i - 1);
            float[] curr = path.get(i);
            float[] next = path.get(i + 1);
            
            float dx1 = curr[0] - prev[0];
            float dy1 = curr[1] - prev[1];
            float dx2 = next[0] - curr[0];
            float dy2 = next[1] - curr[1];
            
            // Not colinear
            if (Math.abs(dx1 * dy2 - dy1 * dx2) > 0.01f) {
                simplified.add(curr);
            }
        }
        
        simplified.add(path.get(path.size() - 1));
        return simplified;
    }
    
    // Node class
    private static class Node {
        int x, y;
        Node parent;
        double gScore = Double.POSITIVE_INFINITY;
        double fScore = Double.POSITIVE_INFINITY;
        
        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        String getKey() {
            return x + "," + y;
        }
    }
}
