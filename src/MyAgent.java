import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {
    
    // Direction constants
    private static final int UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3;
    private static final int[] DX = {0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0};
    
    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String initString = br.readLine();
            String[] temp = initString.split(" ");
            int boardWidth = Integer.parseInt(temp[0]);
            int boardHeight = Integer.parseInt(temp[1]);
            
            while (true) {
                String line = br.readLine();
                if (line.contains("Game Over")) {
                    break;
                }
                
                // Parse the board info
                String[] parts = line.split(" ");
                int appleX = Integer.parseInt(parts[0]);
                int appleY = Integer.parseInt(parts[1]);
                int myLength = Integer.parseInt(parts[2]);
                int myHeadX = Integer.parseInt(parts[3]);
                int myHeadY = Integer.parseInt(parts[4]);
                int enemyCount = Integer.parseInt(parts[5]);
                
                // Parse my body
                int[][] myBody = new int[myLength][2];
                for (int i = 0; i < myLength; i++) {
                    myBody[i][0] = Integer.parseInt(parts[6 + i * 2]);
                    myBody[i][1] = Integer.parseInt(parts[6 + i * 2 + 1]);
                }
                
                // Parse enemies
                List<int[][]> enemies = new ArrayList<>();
                int index = 6 + myLength * 2;
                
                for (int i = 0; i < enemyCount; i++) {
                    int enemyLength = Integer.parseInt(parts[index++]);
                    int[][] enemy = new int[enemyLength][2];
                    for (int j = 0; j < enemyLength; j++) {
                        enemy[j][0] = Integer.parseInt(parts[index++]);
                        enemy[j][1] = Integer.parseInt(parts[index++]);
                    }
                    enemies.add(enemy);
                }
                
                System.err.println("MOVE: Head(" + myHeadX + "," + myHeadY + ") Apple(" + appleX + "," + appleY + ") Length=" + myLength + " Enemies=" + enemyCount);
                
                // Make intelligent decision with BFS pathfinding
                int move = makeIntelligentMove(myHeadX, myHeadY, appleX, appleY, myBody, enemies, boardWidth, boardHeight);
                
                System.err.println("DECISION: " + move + " (0=up,1=down,2=left,3=right)");
                System.out.println(move);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int makeIntelligentMove(int headX, int headY, int appleX, int appleY, int[][] myBody, List<int[][]> enemies, int boardWidth, int boardHeight) {
        // Create board representation with danger zones
        int[][] board = createBoardWithDangerZones(boardWidth, boardHeight, myBody, enemies);
        
        // PRIORITY 1: Avoid immediate death from bigger snakes
        int[] biggerSnakeThreat = detectBiggerSnakeThreat(headX, headY, enemies, myBody.length);
        if (biggerSnakeThreat != null) {
            int escapeMove = findEscapeMove(headX, headY, biggerSnakeThreat, board, boardWidth, boardHeight);
            if (escapeMove != -1) {
                System.err.println("ESCAPE: Avoiding bigger snake at (" + biggerSnakeThreat[0] + "," + biggerSnakeThreat[1] + ")");
                return escapeMove;
            }
        }
        
        // PRIORITY 2: Aggressive shortest path to apple
        List<int[]> pathToApple = findShortestPath(headX, headY, appleX, appleY, board, boardWidth, boardHeight);
        
        // PRIORITY 3: Look for easy prey (much weaker snakes)
        int[] crowdTarget = findOptimalHuntingTarget(headX, headY, enemies, myBody.length);
        List<int[]> pathToCrowd = null;
        
        if (crowdTarget != null) {
            pathToCrowd = findShortestPath(headX, headY, crowdTarget[0], crowdTarget[1], board, boardWidth, boardHeight);
        }
        
        // Decision logic: Choose optimal strategy with survival priority
        int move = chooseAggressiveStrategy(headX, headY, pathToApple, pathToCrowd, board, boardWidth, boardHeight);
        
        return move;
    }
    
    private int[][] createBoardWithDangerZones(int width, int height, int[][] myBody, List<int[][]> enemies) {
        int[][] board = new int[width][height];
        
        // Mark my body (1 = my body, but tail will move so we can potentially occupy it)
        for (int i = 0; i < myBody.length - 1; i++) { // Skip tail as it moves
            if (isInBounds(myBody[i][0], myBody[i][1], width, height)) {
                board[myBody[i][0]][myBody[i][1]] = 1;
            }
        }
        
        // Mark enemies and their danger zones
        for (int[][] enemy : enemies) {
            if (enemy.length == 0) continue;
            
            // Mark enemy body (2 = enemy body)
            for (int[] segment : enemy) {
                if (isInBounds(segment[0], segment[1], width, height)) {
                    board[segment[0]][segment[1]] = 2;
                }
            }
            
            // Mark danger zones around bigger enemy heads (3 = danger zone)
            if (enemy.length >= myBody.length) {
                int enemyHeadX = enemy[0][0];
                int enemyHeadY = enemy[0][1];
                
                for (int dir = 0; dir < 4; dir++) {
                    int dangerX = enemyHeadX + DX[dir];
                    int dangerY = enemyHeadY + DY[dir];
                    
                    if (isInBounds(dangerX, dangerY, width, height) && board[dangerX][dangerY] == 0) {
                        board[dangerX][dangerY] = 3; // Danger zone
                    }
                }
            }
        }
        
        return board;
    }
    
    private int[] detectBiggerSnakeThreat(int headX, int headY, List<int[][]> enemies, int myLength) {
        int[] closestThreat = null;
        int minDistance = Integer.MAX_VALUE;
        
        for (int[][] enemy : enemies) {
            if (enemy.length > 0 && enemy.length >= myLength) { // Bigger or equal snakes are threats
                int enemyHeadX = enemy[0][0];
                int enemyHeadY = enemy[0][1];
                int distance = Math.abs(headX - enemyHeadX) + Math.abs(headY - enemyHeadY);
                
                // Only consider immediate threats (within 3 moves)
                if (distance <= 3 && distance < minDistance) {
                    minDistance = distance;
                    closestThreat = new int[]{enemyHeadX, enemyHeadY, enemy.length};
                }
            }
        }
        
        return closestThreat;
    }
    
    private int findEscapeMove(int headX, int headY, int[] threat, int[][] board, int width, int height) {
        int threatX = threat[0];
        int threatY = threat[1];
        
        int bestMove = -1;
        int maxDistance = -1;
        
        // Find move that maximizes distance from threat while staying safe
        for (int dir = 0; dir < 4; dir++) {
            int newX = headX + DX[dir];
            int newY = headY + DY[dir];
            
            if (isValidEscapeMove(newX, newY, board, width, height)) {
                int distanceFromThreat = Math.abs(newX - threatX) + Math.abs(newY - threatY);
                
                if (distanceFromThreat > maxDistance) {
                    maxDistance = distanceFromThreat;
                    bestMove = dir;
                }
            }
        }
        
        return bestMove;
    }
    
    private int[] findOptimalHuntingTarget(int headX, int headY, List<int[][]> enemies, int myLength) {
        int[] bestTarget = null;
        int bestScore = -1;
        
        for (int[][] enemy : enemies) {
            if (enemy.length > 0 && enemy.length < myLength - 2) { // Only hunt much smaller snakes
                int enemyHeadX = enemy[0][0];
                int enemyHeadY = enemy[0][1];
                int distance = Math.abs(headX - enemyHeadX) + Math.abs(headY - enemyHeadY);
                
                // Score based on size difference and proximity
                int sizeDifference = myLength - enemy.length;
                int score = sizeDifference * 100 - distance; // Bigger difference and closer = better
                
                if (distance <= 6 && score > bestScore) { // Reasonable hunting range
                    bestScore = score;
                    bestTarget = new int[]{enemyHeadX, enemyHeadY};
                }
            }
        }
        
        return bestTarget;
    }
    
    private int chooseAggressiveStrategy(int headX, int headY, List<int[]> pathToApple, List<int[]> pathToCrowd, int[][] board, int width, int height) {
        int appleDistance = pathToApple != null ? pathToApple.size() : Integer.MAX_VALUE;
        int crowdDistance = pathToCrowd != null ? pathToCrowd.size() : Integer.MAX_VALUE;
        
        // AGGRESSIVE APPLE FOCUS - prioritize apples more heavily
        if (appleDistance <= 4) {
            // Apple is close - go for it aggressively
            return executePathMove(headX, headY, pathToApple, "AGGRESSIVE_APPLE");
        } else if (crowdDistance <= 3 && crowdDistance < appleDistance) {
            // Very close easy prey
            return executePathMove(headX, headY, pathToCrowd, "QUICK_HUNT");
        } else if (appleDistance <= 12) {
            // Apple is reachable - prioritize growth
            return executePathMove(headX, headY, pathToApple, "APPLE_FOCUS");
        } else if (crowdDistance < Integer.MAX_VALUE) {
            // Hunt when apple is far
            return executePathMove(headX, headY, pathToCrowd, "HUNT_FALLBACK");
        }
        
        // Emergency fallback
        return findAnySafeMove(headX, headY, board, width, height);
    }
    
    private int executePathMove(int headX, int headY, List<int[]> path, String strategy) {
        if (path != null && !path.isEmpty()) {
            int[] nextStep = path.get(0);
            int moveDir = getDirectionTo(headX, headY, nextStep[0], nextStep[1]);
            System.err.println(strategy + ": moving " + moveDir + " to (" + nextStep[0] + "," + nextStep[1] + ")");
            return moveDir;
        }
        return -1; // No valid path
    }
    
    private List<int[]> findShortestPath(int startX, int startY, int targetX, int targetY, int[][] board, int width, int height) {
        if (startX == targetX && startY == targetY) {
            return new ArrayList<>(); // Already at target
        }
        
        // Aggressive BFS - prioritize speed over extreme safety
        Queue<int[]> queue = new LinkedList<>();
        boolean[][] visited = new boolean[width][height];
        int[][][] parent = new int[width][height][2];
        
        queue.offer(new int[]{startX, startY, 0}); // x, y, distance
        visited[startX][startY] = true;
        parent[startX][startY] = new int[]{-1, -1}; // No parent for start
        
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0], y = current[1], dist = current[2];
            
            if (x == targetX && y == targetY) {
                // Reconstruct path
                return reconstructPath(parent, startX, startY, targetX, targetY);
            }
            
            // Try all 4 directions with priority (toward target first)
            int[] dirPriority = getDirectionPriority(x, y, targetX, targetY);
            
            for (int i = 0; i < 4; i++) {
                int dir = dirPriority[i];
                int newX = x + DX[dir];
                int newY = y + DY[dir];
                
                if (isAggressiveValidMove(newX, newY, board, width, height) && !visited[newX][newY]) {
                    visited[newX][newY] = true;
                    parent[newX][newY] = new int[]{x, y};
                    queue.offer(new int[]{newX, newY, dist + 1});
                }
            }
        }
        
        return null; // No path found
    }
    
    private int[] getDirectionPriority(int x, int y, int targetX, int targetY) {
        // Prioritize directions that get us closer to target
        List<Integer> primary = new ArrayList<>();
        List<Integer> secondary = new ArrayList<>();
        
        if (targetX > x) primary.add(RIGHT);
        else if (targetX < x) primary.add(LEFT);
        
        if (targetY > y) primary.add(DOWN);
        else if (targetY < y) primary.add(UP);
        
        // Add remaining directions as secondary
        for (int dir = 0; dir < 4; dir++) {
            if (!primary.contains(dir)) {
                secondary.add(dir);
            }
        }
        
        // Combine lists
        primary.addAll(secondary);
        return primary.stream().mapToInt(i -> i).toArray();
    }
    
    private boolean isAggressiveValidMove(int x, int y, int[][] board, int width, int height) {
        if (!isInBounds(x, y, width, height)) return false;
        
        // Aggressive: allow movement through some danger zones if necessary for apple
        return board[x][y] == 0 || board[x][y] == 3; // Allow empty space or danger zones
    }
    
    private boolean isValidEscapeMove(int x, int y, int[][] board, int width, int height) {
        if (!isInBounds(x, y, width, height)) return false;
        
        // Conservative: avoid all danger zones when escaping
        return board[x][y] == 0; // Only empty spaces
    }
    
    private List<int[]> reconstructPath(int[][][] parent, int startX, int startY, int endX, int endY) {
        List<int[]> path = new ArrayList<>();
        int x = endX, y = endY;
        
        while (x != startX || y != startY) {
            path.add(0, new int[]{x, y});
            int[] p = parent[x][y];
            x = p[0];
            y = p[1];
        }
        
        return path;
    }
    
    private int getDirectionTo(int fromX, int fromY, int toX, int toY) {
        if (toX > fromX) return RIGHT;
        if (toX < fromX) return LEFT;
        if (toY > fromY) return DOWN;
        if (toY < fromY) return UP;
        return UP; // Fallback
    }
    
    private int findAnySafeMove(int headX, int headY, int[][] board, int width, int height) {
        // Try to find safest move (prefer moves away from danger)
        int bestMove = -1;
        int bestSafety = -1;
        
        for (int dir = 0; dir < 4; dir++) {
            int newX = headX + DX[dir];
            int newY = headY + DY[dir];
            
            if (isValidMove(newX, newY, board, width, height)) {
                int safety = calculateSafetyScore(newX, newY, board, width, height);
                if (safety > bestSafety) {
                    bestSafety = safety;
                    bestMove = dir;
                }
            }
        }
        
        if (bestMove != -1) {
            System.err.println("SAFE MOVE: dir=" + bestMove + " safety=" + bestSafety);
            return bestMove;
        }
        
        System.err.println("NO SAFE MOVES - EMERGENCY");
        return UP; // Last resort
    }
    
    private int calculateSafetyScore(int x, int y, int[][] board, int width, int height) {
        int score = 0;
        
        // Check surrounding area for safety
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int checkX = x + dx;
                int checkY = y + dy;
                
                if (isInBounds(checkX, checkY, width, height)) {
                    if (board[checkX][checkY] == 0) score += 1; // Empty space
                    else if (board[checkX][checkY] == 3) score -= 2; // Danger zone
                    else score -= 1; // Obstacle
                }
            }
        }
        
        return score;
    }
    
    private boolean isValidMove(int x, int y, int[][] board, int width, int height) {
        return isInBounds(x, y, width, height) && board[x][y] == 0;
    }
    
    private boolean isInBounds(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
