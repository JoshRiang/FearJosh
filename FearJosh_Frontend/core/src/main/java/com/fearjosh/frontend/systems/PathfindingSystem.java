package com.fearjosh.frontend.systems;

import com.fearjosh.frontend.world.Room;
import java.util.*;

/**
 * A* Pathfinding System for enemy navigation in Physical Mode
 * Uses grid-based pathfinding with furniture obstacles
 */
public class PathfindingSystem {
    
    private static final int GRID_SIZE = 16; // Size of each pathfinding cell
    private static final int MAX_PATH_LENGTH = 100; // Prevent infinite loops
    
    /**
     * Find path from start to goal using A* algorithm
     * @param startX Starting X coordinate
     * @param startY Starting Y coordinate
     * @param goalX Goal X coordinate
     * @param goalY Goal Y coordinate
     * @param room Current room with obstacles
     * @param worldWidth World width
     * @param worldHeight World height
     * @return List of waypoints (x,y pairs) or empty list if no path found
     */
    public static List<float[]> findPath(float startX, float startY, 
                                         float goalX, float goalY,
                                         Room room,
                                         float worldWidth, float worldHeight) {
        
        // Convert to grid coordinates
        int startGridX = (int)(startX / GRID_SIZE);
        int startGridY = (int)(startY / GRID_SIZE);
        int goalGridX = (int)(goalX / GRID_SIZE);
        int goalGridY = (int)(goalY / GRID_SIZE);
        
        int gridWidth = (int)(worldWidth / GRID_SIZE) + 1;
        int gridHeight = (int)(worldHeight / GRID_SIZE) + 1;
        
        // A* algorithm
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
            
            // Check all neighbors (8-directional for smooth diagonal movement)
            // Format: {dx, dy, cost}
            // Orthogonal (N/S/E/W) = cost 1.0
            // Diagonal (NE/SE/SW/NW) = cost 1.41 (sqrt(2))
            double[][] directions = {
                {0, 1, 1.0},   // North
                {1, 0, 1.0},   // East
                {0, -1, 1.0},  // South
                {-1, 0, 1.0},  // West
                {1, 1, 1.41},  // NE
                {1, -1, 1.41}, // SE
                {-1, -1, 1.41},// SW
                {-1, 1, 1.41}  // NW
            };
            
            for (double[] dir : directions) {
                int neighborX = current.x + (int)dir[0];
                int neighborY = current.y + (int)dir[1];
                double moveCost = dir[2];
                
                // Out of bounds
                if (neighborX < 0 || neighborX >= gridWidth || 
                    neighborY < 0 || neighborY >= gridHeight) {
                    continue;
                }
                
                String neighborKey = neighborX + "," + neighborY;
                
                // Already evaluated
                if (closedSet.contains(neighborKey)) {
                    continue;
                }
                
                // Check if walkable (not blocked by furniture)
                if (isBlocked(neighborX, neighborY, room)) {
                    continue;
                }
                
                // For diagonal moves, check if path is not blocked by corner obstacles
                if (moveCost > 1.0) { // Diagonal movement
                    int dx = (int)dir[0];
                    int dy = (int)dir[1];
                    // Check if adjacent orthogonal cells are blocked (prevent cutting corners)
                    if (isBlocked(current.x + dx, current.y, room) || 
                        isBlocked(current.x, current.y + dy, room)) {
                        continue; // Can't cut through corner
                    }
                }
                
                // Calculate tentative gScore with proper diagonal cost
                double tentativeGScore = current.gScore + moveCost;
                
                Node neighbor = allNodes.get(neighborKey);
                if (neighbor == null) {
                    neighbor = new Node(neighborX, neighborY);
                    allNodes.put(neighborKey, neighbor);
                }
                
                // Found better path to neighbor
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
        
        // No path found
        return new ArrayList<>();
    }
    
    /**
     * Check if grid cell is blocked by furniture
     * NOTE: Now returns false since TMX collision is handled elsewhere
     * Pathfinding should use TiledMapManager for proper obstaclecheck
     */
    private static boolean isBlocked(int gridX, int gridY, Room room) {
        // TMX collision is now handled via TiledMapManager in Enemy class
        // This method is a placeholder - proper pathfinding should integrate with TiledMapManager
        return false;
    }
    
    /**
     * Check rectangle overlap
     */
    private static boolean overlaps(float x1, float y1, float w1, float h1,
                                    float x2, float y2, float w2, float h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 &&
               y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    /**
     * Manhattan distance heuristic
     */
    private static double heuristic(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
    
    /**
     * Reconstruct path from goal node back to start
     */
    private static List<float[]> reconstructPath(Node goalNode) {
        List<float[]> path = new ArrayList<>();
        Node current = goalNode;
        
        while (current != null) {
            // Convert grid to world coordinates (center of cell)
            float worldX = current.x * GRID_SIZE + GRID_SIZE / 2f;
            float worldY = current.y * GRID_SIZE + GRID_SIZE / 2f;
            path.add(0, new float[]{worldX, worldY}); // Add to front
            current = current.parent;
        }
        
        return path;
    }
    
    /**
     * Simplify path by removing unnecessary waypoints (straight lines)
     */
    public static List<float[]> simplifyPath(List<float[]> path) {
        if (path.size() <= 2) return path;
        
        List<float[]> simplified = new ArrayList<>();
        simplified.add(path.get(0)); // Start
        
        for (int i = 1; i < path.size() - 1; i++) {
            float[] prev = path.get(i - 1);
            float[] curr = path.get(i);
            float[] next = path.get(i + 1);
            
            // Check if curr is on straight line between prev and next
            float dx1 = curr[0] - prev[0];
            float dy1 = curr[1] - prev[1];
            float dx2 = next[0] - curr[0];
            float dy2 = next[1] - curr[1];
            
            // Not colinear - keep this point
            if (Math.abs(dx1 * dy2 - dy1 * dx2) > 0.01f) {
                simplified.add(curr);
            }
        }
        
        simplified.add(path.get(path.size() - 1)); // Goal
        return simplified;
    }
    
    /**
     * Node class for A* algorithm
     */
    private static class Node {
        int x, y;
        Node parent;
        double gScore = Double.POSITIVE_INFINITY; // Cost from start
        double fScore = Double.POSITIVE_INFINITY; // Estimated total cost
        
        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        String getKey() {
            return x + "," + y;
        }
    }
}
