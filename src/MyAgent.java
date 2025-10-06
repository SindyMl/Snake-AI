import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {

    private int[] dx = { 0, 0, -1, 1 }; // up, down, left, right
    private int[] dy = { -1, 1, 0, 0 };
    private boolean[][] visited;
    private int[][] gameBoard;
    private int boardWidth, boardHeight;
    private Random random = new Random();

    public static void main(String args[]) {
        MyAgent agent = new MyAgent();
        MyAgent.start(agent, args);
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String initString = br.readLine();
            String[] temp = initString.split(" ");
            int nSnakes = Integer.parseInt(temp[0]);
            boardWidth = Integer.parseInt(temp[1]);
            boardHeight = Integer.parseInt(temp[2]);
            
            // Initialize reusable arrays once
            visited = new boolean[boardWidth][boardHeight];
            gameBoard = new int[boardWidth][boardHeight];

            int appleAge = 0;
            int lastAppleX = -1, lastAppleY = -1;

            while (true) {
                String line = br.readLine();
                if (line.contains("Game Over"))
                    break;

                String[] appleCoords = line.split(" ");
                int appleX = Integer.parseInt(appleCoords[0]);
                int appleY = Integer.parseInt(appleCoords[1]);

                // Apple age tracking
                if (appleX != lastAppleX || appleY != lastAppleY) {
                    appleAge = 0;
                    lastAppleX = appleX;
                    lastAppleY = appleY;
                } else {
                    appleAge++;
                }

                int mySnakeNum = Integer.parseInt(br.readLine());
                Snake[] snakes = new Snake[nSnakes];

                for (int i = 0; i < nSnakes; i++) {
                    snakes[i] = parseSnake(br.readLine());
                }

                int move = calculateBestMove(snakes[mySnakeNum], snakes, appleX, appleY, appleAge);
                System.out.println(move);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int calculateBestMove(Snake mySnake, Snake[] allSnakes, int appleX, int appleY, int appleAge) {
        if (mySnake == null || !mySnake.alive || mySnake.body.isEmpty())
            return random.nextInt(7); // Better fallback

        Point head = mySnake.body.get(0);
        double appleValue = Math.max(-10.0, 5.0 - (appleAge * 0.1));

        // Efficient board setup
        setupGameBoardEfficiently(allSnakes);

        // Enhanced emergency detection
        int immediateSpace = calculateImmediateSpace(head);
        if (immediateSpace <= 1 || isInImmediateDanger(mySnake, allSnakes)) {
            return findEnhancedEmergencyEscape(mySnake, allSnakes);
        }

        int bestMove = 0;
        int bestScore = Integer.MIN_VALUE;

        // Test all possible moves (including relative ones)
        List<Integer> possibleMoves = getAllPossibleMoves(mySnake);

        for (int move : possibleMoves) {
            Point newHead = calculateNewHead(mySnake, move);

            if (!isMoveSafe(mySnake, newHead, allSnakes)) {
                continue;
            }

            int score = evaluateEnhancedMove(newHead, mySnake, allSnakes, appleX, appleY, appleValue);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove;
    }

    private List<Integer> getAllPossibleMoves(Snake mySnake) {
        List<Integer> moves = new ArrayList<>();

        // Absolute moves (0-3)
        for (int i = 0; i < 4; i++) {
            moves.add(i);
        }

        // Relative moves (4-6) if we have movement history
        if (mySnake.body.size() > 1) {
            for (int i = 4; i <= 6; i++) {
                moves.add(i);
            }
        }

        return moves;
    }

    private Point calculateNewHead(Snake snake, int move) {
        Point head = snake.body.get(0);

        switch (move) {
            case 0:
                return new Point(head.x, head.y - 1); // Up
            case 1:
                return new Point(head.x, head.y + 1); // Down
            case 2:
                return new Point(head.x - 1, head.y); // Left
            case 3:
                return new Point(head.x + 1, head.y); // Right
            case 4: // Relative left
            case 5: // Straight
            case 6: // Relative right
                if (snake.body.size() > 1) {
                    Point neck = snake.body.get(1);
                    return calculateRelativeMove(head, neck, move);
                }
                break;
        }

        return new Point(head.x, head.y); // Fallback
    }

    private Point calculateRelativeMove(Point head, Point previous, int move) {
        int dx = head.x - previous.x;
        int dy = head.y - previous.y;

        switch (move) {
            case 4: // Left relative
                return new Point(head.x - dy, head.y + dx);
            case 5: // Straight
                return new Point(head.x + dx, head.y + dy);
            case 6: // Right relative
                return new Point(head.x + dy, head.y - dx);
            default:
                return new Point(head.x + dx, head.y + dy);
        }
    }

    private boolean isMoveSafe(Snake mySnake, Point newHead, Snake[] allSnakes) {
        // Wall collision
        if (newHead.x < 0 || newHead.x >= boardWidth || newHead.y < 0 || newHead.y >= boardHeight) {
            return false;
        }

        // Self collision (excluding tail)
        List<Point> myBody = mySnake.body;
        for (int i = 0; i < myBody.size() - 1; i++) {
            if (newHead.equals(myBody.get(i))) {
                return false;
            }
        }

        // Other snakes collision
        for (Snake snake : allSnakes) {
            if (snake != mySnake && snake.alive) {
                for (Point bodyPart : snake.body) {
                    if (newHead.equals(bodyPart)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private int evaluateEnhancedMove(Point newPos, Snake mySnake, Snake[] allSnakes, int appleX, int appleY,
            double appleValue) {
        int score = 0;

        // CRITICAL: Self-trap detection - HIGHEST PRIORITY
        int futureSpace = calculateFutureSpace(newPos, mySnake, allSnakes);
        if (futureSpace < mySnake.body.size() / 2) {
            score -= 2000; // Massive penalty for potential trap
        }
        if (futureSpace < 3) {
            score -= 5000; // Death penalty for immediate trap
        }

        // Enhanced self-collision avoidance
        if (willTrapSelf(newPos, mySnake, allSnakes)) {
            score -= 10000; // Absolute avoid self-trap
        }

        // 1. Enhanced Apple Strategy with Combat Intelligence
        int appleDistance = Math.abs(newPos.x - appleX) + Math.abs(newPos.y - appleY);

        if (appleValue <= -4) {
            // Deadly green apple - ABSOLUTE avoidance
            score -= (20 - appleDistance) * 50;
            if (appleDistance <= 2) score -= 1000; // Emergency avoidance
        } else if (appleValue > 2) {
            // Good red apple - SMART competition elimination (not suicidal)
            Snake targetEnemy = findBestTargetForElimination(newPos, appleX, appleY, allSnakes, mySnake);
            
            if (targetEnemy != null && futureSpace >= 8) { // Only hunt if we have escape space
                int sizeDiff = mySnake.body.size() - targetEnemy.body.size();
                int killDiff = mySnake.kills - targetEnemy.kills;
                
                if (sizeDiff > 0) {
                    // SMART KILL MODE - only if safe
                    score += 600 + (sizeDiff * 100) + (killDiff * 30);
                    
                    // Extra bonus for eliminating close competition
                    if (killDiff >= 0) score += 200; // Same or fewer kills - eliminate!
                    
                    int distanceToTarget = Math.abs(newPos.x - targetEnemy.body.get(0).x) + 
                                         Math.abs(newPos.y - targetEnemy.body.get(0).y);
                    if (distanceToTarget == 1 && futureSpace >= 10) score += 300; // Safe kill shot
                }
            } else {
                // Clear path to apple - but check safety first
                if (futureSpace >= 5) {
                    score += (int) (appleValue * 40) - appleDistance * 2;
                    if (appleDistance == 1 && futureSpace >= 8) score += 200;
                }
            }
        }

        // 2. Combat Intelligence Assessment (reduced aggression)
        score += evaluateCombatSituation(newPos, mySnake, allSnakes);

        // 3. ENHANCED Space Assessment - survival priority
        int reachableSpace = calculateReachableSpaceOptimized(newPos);
        score += reachableSpace * 8; // Much higher weight on space

        // 3. Enemy Assessment - ENHANCED COMBAT INTELLIGENCE
        for (Snake enemy : allSnakes) {
            if (enemy == mySnake || !enemy.alive || enemy.body.isEmpty())
                continue;

            Point enemyHead = enemy.body.get(0);
            int distToEnemy = Math.abs(newPos.x - enemyHead.x) + Math.abs(newPos.y - enemyHead.y);

            if (enemy.length >= mySnake.length && distToEnemy <= 2) {
                score -= 500; // Avoid larger snakes
            } else if (mySnake.length > enemy.length + 2 && distToEnemy <= 3) {
                score += 100; // Hunt smaller snakes
            }
        }

        // 4. Wall Safety
        int wallDistance = Math.min(Math.min(newPos.x, boardWidth - 1 - newPos.x),
                Math.min(newPos.y, boardHeight - 1 - newPos.y));
        score += wallDistance * 3;

        return score;
    }

    private Snake findEnemyBlockingApple(Point myPos, int appleX, int appleY, Snake[] allSnakes, Snake mySnake) {
        // Find enemy that's closer to apple or directly blocking path
        Snake closestEnemy = null;
        int myAppleDistance = Math.abs(myPos.x - appleX) + Math.abs(myPos.y - appleY);
        
        for (Snake enemy : allSnakes) {
            if (enemy != null && enemy.alive && enemy != mySnake && !enemy.body.isEmpty()) {
                Point enemyHead = enemy.body.get(0);
                int enemyAppleDistance = Math.abs(enemyHead.x - appleX) + Math.abs(enemyHead.y - appleY);
                
                // Enemy is closer to apple or directly blocking
                if (enemyAppleDistance < myAppleDistance || 
                    (enemyAppleDistance == 0) || // Enemy is on apple
                    isEnemyBlockingPath(myPos, appleX, appleY, enemy)) {
                    
                    if (closestEnemy == null || enemyAppleDistance < 
                        Math.abs(closestEnemy.body.get(0).x - appleX) + Math.abs(closestEnemy.body.get(0).y - appleY)) {
                        closestEnemy = enemy;
                    }
                }
            }
        }
        
        return closestEnemy;
    }

    private boolean isEnemyBlockingPath(Point myPos, int appleX, int appleY, Snake enemy) {
        // Simple path blocking detection - if enemy head is between us and apple
        Point enemyHead = enemy.body.get(0);
        
        // Check if enemy is on direct path to apple
        if ((myPos.x == appleX && enemyHead.x == appleX && 
             ((myPos.y < enemyHead.y && enemyHead.y < appleY) || (appleY < enemyHead.y && enemyHead.y < myPos.y))) ||
            (myPos.y == appleY && enemyHead.y == appleY && 
             ((myPos.x < enemyHead.x && enemyHead.x < appleX) || (appleX < enemyHead.x && enemyHead.x < myPos.x)))) {
            return true;
        }
        
        return false;
    }

    private int evaluateCombatSituation(Point newPos, Snake mySnake, Snake[] allSnakes) {
        int combatScore = 0;
        
        // Calculate our future space first for safety assessment
        int futureSpace = calculateFutureSpace(newPos, mySnake, allSnakes);
        
        for (Snake enemy : allSnakes) {
            if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) continue;
            
            Point enemyHead = enemy.body.get(0);
            int distance = Math.abs(newPos.x - enemyHead.x) + Math.abs(newPos.y - enemyHead.y);
            
            if (distance <= 4) { // Combat range
                int sizeDiff = mySnake.body.size() - enemy.body.size();
                int killDiff = mySnake.kills - enemy.kills;
                
                if (sizeDiff > 0) {
                    // CAREFUL HUNTING MODE - only hunt if we have good escape space
                    if (futureSpace >= 8) {
                        combatScore += (sizeDiff * 30) - (distance * 8);
                        
                        // Moderate bonus for eliminating competition with fewer kills
                        if (killDiff >= 0) {
                            combatScore += 200 + (killDiff * 50); // Reduced from previous version
                        }
                        
                        // Reduced close range bonuses to prevent reckless behavior
                        if (distance == 1 && futureSpace >= 12) combatScore += 300; // Safer kill shot
                        if (distance == 2 && futureSpace >= 10) combatScore += 150; // Safer setup
                        
                        // Enhanced head-to-head prediction with safety check
                        if (willWinHeadToHeadCollision(newPos, enemyHead, enemy, mySnake) && futureSpace >= 10) {
                            combatScore += 400; // Reduced but still significant
                        }
                    } else {
                        // Not enough space - avoid combat
                        combatScore -= 100;
                    }
                } else if (sizeDiff < 0) {
                    // Enemy is larger - ENHANCED EVASION
                    combatScore += distance * 40; // Higher reward for distance
                    
                    if (distance <= 2) {
                        combatScore -= 800; // Much higher danger penalty
                    }
                    
                    // ABSOLUTE head-to-head avoidance
                    if (willLoseHeadToHeadCollision(newPos, enemyHead, enemy, mySnake)) {
                        combatScore -= 3000; // Increased death penalty
                    }
                } else {
                    // Equal size - be very cautious
                    if (killDiff > 0 && futureSpace >= 10) {
                        // We have more kills - be moderately aggressive but safe
                        combatScore += 50 - (distance * 15);
                    } else {
                        // They have equal/more kills - be very cautious
                        if (distance <= 2) combatScore -= 300;
                        else combatScore += distance * 15;
                    }
                }
            }
        }
        
        return combatScore;
    }

    private boolean isHeadToHeadCollisionPossible(Point myNewPos, Point enemyHead, Snake enemy) {
        // Predict if we might collide head-to-head next turn
        for (int enemyMove = 0; enemyMove < 4; enemyMove++) {
            Point enemyNewPos = new Point(enemyHead.x, enemyHead.y);
            
            switch (enemyMove) {
                case 0: enemyNewPos.y--; break;
                case 1: enemyNewPos.x++; break;
                case 2: enemyNewPos.y++; break;
                case 3: enemyNewPos.x--; break;
            }
            
            if (enemyNewPos.equals(myNewPos)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean willWinHeadToHeadCollision(Point myNewPos, Point enemyHead, Snake enemy, Snake mySnake) {
        // Enhanced collision prediction with win guarantee
        if (!isHeadToHeadCollisionPossible(myNewPos, enemyHead, enemy)) {
            return false;
        }
        
        // We win if we're larger or equal size with more kills
        return mySnake.body.size() > enemy.body.size() || 
               (mySnake.body.size() == enemy.body.size() && mySnake.kills >= enemy.kills);
    }

    private boolean willLoseHeadToHeadCollision(Point myNewPos, Point enemyHead, Snake enemy, Snake mySnake) {
        // Enhanced collision prediction with loss detection
        if (!isHeadToHeadCollisionPossible(myNewPos, enemyHead, enemy)) {
            return false;
        }
        
        // We lose if enemy is larger or equal size with more kills
        return enemy.body.size() > mySnake.body.size() || 
               (enemy.body.size() == mySnake.body.size() && enemy.kills > mySnake.kills);
    }

    private Snake findBestTargetForElimination(Point myPos, int appleX, int appleY, Snake[] allSnakes, Snake mySnake) {
        Snake bestTarget = null;
        int bestScore = Integer.MIN_VALUE;
        
        // Check our space first - don't hunt if we don't have enough room
        int ourSpace = calculateFutureSpace(myPos, mySnake, allSnakes);
        if (ourSpace < 8) {
            return null; // Too risky to hunt with limited space
        }
        
        for (Snake enemy : allSnakes) {
            if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) continue;
            
            Point enemyHead = enemy.body.get(0);
            int distance = Math.abs(myPos.x - enemyHead.x) + Math.abs(myPos.y - enemyHead.y);
            
            // Only consider enemies we can safely kill (more conservative)
            if (mySnake.body.size() > enemy.body.size() + 1 && distance <= 4) { // Need bigger advantage and closer
                int targetScore = 0;
                
                // Size advantage bonus (reduced to be less aggressive)
                targetScore += (mySnake.body.size() - enemy.body.size()) * 60;
                
                // SMART competition elimination - fewer kills = higher priority
                int killDiff = mySnake.kills - enemy.kills;
                if (killDiff >= 0) {
                    targetScore += 300 + (killDiff * 60); // Reduced aggression
                }
                
                // Distance penalty (closer is better for kills, but not too close)
                if (distance == 1) {
                    targetScore += 100; // Immediate kill opportunity
                } else if (distance == 2) {
                    targetScore += 150; // Optimal hunting distance
                } else {
                    targetScore -= distance * 15; // Penalty for being far
                }
                
                // Apple competition bonus - if enemy is near apple, eliminate them
                int enemyAppleDistance = Math.abs(enemyHead.x - appleX) + Math.abs(enemyHead.y - appleY);
                if (enemyAppleDistance <= 3) {
                    targetScore += 150; // They're competing for our apple!
                }
                
                // Blocking path bonus
                if (isEnemyBlockingPath(myPos, appleX, appleY, enemy)) {
                    targetScore += 200;
                }
                
                // SAFETY CHECK - don't target if it puts us in danger
                if (willHuntingTrapUs(myPos, enemyHead, mySnake, allSnakes)) {
                    targetScore -= 1000; // Major penalty for risky hunts
                }
                
                if (targetScore > bestScore) {
                    bestScore = targetScore;
                    bestTarget = enemy;
                }
            }
        }
        
        return bestTarget;
    }

    private boolean willHuntingTrapUs(Point myPos, Point targetPos, Snake mySnake, Snake[] allSnakes) {
        // Simulate moving towards target and check if we get trapped
        int dx = Integer.signum(targetPos.x - myPos.x);
        int dy = Integer.signum(targetPos.y - myPos.y);
        
        Point huntPos = new Point(myPos.x + dx, myPos.y + dy);
        
        if (huntPos.x < 0 || huntPos.x >= boardWidth || huntPos.y < 0 || huntPos.y >= boardHeight) {
            return true; // Would move out of bounds
        }
        
        return calculateFutureSpace(huntPos, mySnake, allSnakes) < 6; // Not enough space to hunt safely
    }

    private int calculateImmediateSpace(Point pos) {
        int safeMoves = 0;
        for (int i = 0; i < 4; i++) {
            Point newPos = new Point(pos.x + dx[i], pos.y + dy[i]);
            if (newPos.x >= 0 && newPos.x < boardWidth && newPos.y >= 0 && newPos.y < boardHeight &&
                    gameBoard[newPos.x][newPos.y] == 0) {
                safeMoves++;
            }
        }
        return safeMoves;
    }

    private int findEmergencyEscape(Snake mySnake, Snake[] allSnakes) {
        Point head = mySnake.body.get(0);

        for (int move = 0; move < 4; move++) {
            Point newHead = calculateNewHead(mySnake, move);
            if (isMoveSafe(mySnake, newHead, allSnakes)) {
                return move;
            }
        }

        return 0; // Last resort
    }

    private int calculateReachableSpaceOptimized(Point start) {
        if (start.x < 0 || start.x >= boardWidth || start.y < 0 || start.y >= boardHeight) {
            return 0;
        }

        boolean[][] localVisited = new boolean[boardWidth][boardHeight];
        Queue<Point> queue = new LinkedList<>();
        queue.offer(start);
        localVisited[start.x][start.y] = true;
        int count = 0;

        while (!queue.isEmpty() && count < 30) { // Performance limit
            Point current = queue.poll();
            count++;

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                if (nx >= 0 && nx < boardWidth && ny >= 0 && ny < boardHeight &&
                        !localVisited[nx][ny] && gameBoard[nx][ny] == 0) {
                    localVisited[nx][ny] = true;
                    queue.offer(new Point(nx, ny));
                }
            }
        }

        return count;
    }

    private void setupGameBoardEfficiently(Snake[] allSnakes) {
        // Clear the reusable board efficiently
        for (int i = 0; i < boardWidth; i++) {
            for (int j = 0; j < boardHeight; j++) {
                gameBoard[i][j] = 0;
            }
        }

        for (Snake snake : allSnakes) {
            if (snake == null || !snake.alive)
                continue;
            for (Point body : snake.body) {
                if (body.x >= 0 && body.x < boardWidth && body.y >= 0 && body.y < boardHeight) {
                    gameBoard[body.x][body.y] = 1;
                }
            }
        }
    }

    private boolean isInImmediateDanger(Snake mySnake, Snake[] allSnakes) {
        Point head = mySnake.body.get(0);
        
        // Check for immediate head-to-head collisions with larger snakes
        for (Snake enemy : allSnakes) {
            if (enemy != null && enemy.alive && enemy != mySnake) {
                if (!enemy.body.isEmpty()) {
                    Point enemyHead = enemy.body.get(0);
                    int distance = Math.abs(head.x - enemyHead.x) + Math.abs(head.y - enemyHead.y);
                    
                    // If enemy is adjacent and larger, we're in danger
                    if (distance <= 2 && enemy.body.size() >= mySnake.body.size()) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private int findEnhancedEmergencyEscape(Snake mySnake, Snake[] allSnakes) {
        Point head = mySnake.body.get(0);
        int bestMove = 0;
        int maxSpace = -1;
        
        // Try all moves and find the one with most space
        for (int move = 0; move < 7; move++) {
            Point newPos = getNewPosition(head, move);
            
            if (isValidPosition(newPos) && !isCollision(newPos, allSnakes)) {
                int spaceAvailable = calculateReachableSpaceOptimized(newPos);
                
                if (spaceAvailable > maxSpace) {
                    maxSpace = spaceAvailable;
                    bestMove = move;
                }
            }
        }
        
        return bestMove;
    }

    private Snake parseSnake(String snakeLine) {
        String[] parts = snakeLine.split(" ");
        Snake snake = new Snake();

        if (parts[0].equals("dead")) {
            snake.alive = false;
            return snake;
        }

        snake.alive = parts[0].equals("alive");
        snake.length = Integer.parseInt(parts[1]);
        snake.kills = Integer.parseInt(parts[2]);
        snake.body = new ArrayList<>();

        for (int i = 3; i < parts.length; i++) {
            String[] coords = parts[i].split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            snake.body.add(new Point(x, y));
        }

        return snake;
    }

    private Point getNewPosition(Point head, int move) {
        int newX = head.x, newY = head.y;
        
        switch (move) {
            case 0: newY--; break; // North
            case 1: newX++; break; // East
            case 2: newY++; break; // South
            case 3: newX--; break; // West
            case 4: newX++; newY--; break; // Northeast (relative)
            case 5: newX++; newY++; break; // Southeast (relative)
            case 6: newX--; newY--; break; // Northwest (relative)
        }
        
        return new Point(newX, newY);
    }

    private boolean isValidPosition(Point pos) {
        return pos.x >= 0 && pos.x < boardWidth && pos.y >= 0 && pos.y < boardHeight;
    }

    private boolean isCollision(Point pos, Snake[] allSnakes) {
        for (Snake snake : allSnakes) {
            if (snake != null && snake.alive) {
                for (Point body : snake.body) {
                    if (body.equals(pos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int calculateFutureSpace(Point newPos, Snake mySnake, Snake[] allSnakes) {
        // Simulate the move and calculate available space
        boolean[][] tempBoard = new boolean[boardWidth][boardHeight];
        
        // Mark all snake bodies (including our future position)
        for (Snake snake : allSnakes) {
            if (snake != null && snake.alive) {
                List<Point> body = snake.body;
                if (snake == mySnake) {
                    // Our future body: new head + current body (minus tail)
                    tempBoard[newPos.x][newPos.y] = true;
                    for (int i = 0; i < body.size() - 1; i++) {
                        Point p = body.get(i);
                        if (p.x >= 0 && p.x < boardWidth && p.y >= 0 && p.y < boardHeight) {
                            tempBoard[p.x][p.y] = true;
                        }
                    }
                } else {
                    // Other snakes
                    for (Point p : body) {
                        if (p.x >= 0 && p.x < boardWidth && p.y >= 0 && p.y < boardHeight) {
                            tempBoard[p.x][p.y] = true;
                        }
                    }
                }
            }
        }
        
        // BFS to count reachable spaces from new position
        boolean[][] visited = new boolean[boardWidth][boardHeight];
        Queue<Point> queue = new LinkedList<>();
        queue.offer(newPos);
        visited[newPos.x][newPos.y] = true;
        int spaceCount = 0;
        
        while (!queue.isEmpty() && spaceCount < 50) { // Limit for performance
            Point current = queue.poll();
            spaceCount++;
            
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                
                if (nx >= 0 && nx < boardWidth && ny >= 0 && ny < boardHeight &&
                    !visited[nx][ny] && !tempBoard[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.offer(new Point(nx, ny));
                }
            }
        }
        
        return spaceCount;
    }

    private boolean willTrapSelf(Point newPos, Snake mySnake, Snake[] allSnakes) {
        // Check if this move will create a situation where we can't escape
        
        // 1. Check if we're creating a dead end
        int exitCount = 0;
        for (int i = 0; i < 4; i++) {
            int nx = newPos.x + dx[i];
            int ny = newPos.y + dy[i];
            
            if (nx >= 0 && nx < boardWidth && ny >= 0 && ny < boardHeight) {
                boolean blocked = false;
                
                // Check if this exit is blocked by walls or snakes
                for (Snake snake : allSnakes) {
                    if (snake != null && snake.alive) {
                        for (Point body : snake.body) {
                            if (body.x == nx && body.y == ny) {
                                // Special case: if it's our tail, it won't be there next turn
                                if (snake == mySnake && body.equals(mySnake.body.get(mySnake.body.size() - 1))) {
                                    continue; // Tail will move
                                }
                                blocked = true;
                                break;
                            }
                        }
                        if (blocked) break;
                    }
                }
                
                if (!blocked) {
                    exitCount++;
                }
            }
        }
        
        // If we have fewer than 2 exits, we might be trapping ourselves
        if (exitCount < 2) {
            return true;
        }
        
        // 2. Advanced trap detection: simulate a few moves ahead
        return isInDeadEndPath(newPos, mySnake, allSnakes, 3);
    }

    private boolean isInDeadEndPath(Point pos, Snake mySnake, Snake[] allSnakes, int depth) {
        if (depth <= 0) return false;
        
        int validMoves = 0;
        for (int move = 0; move < 4; move++) {
            Point nextPos = new Point(pos.x + dx[move], pos.y + dy[move]);
            
            if (nextPos.x >= 0 && nextPos.x < boardWidth && 
                nextPos.y >= 0 && nextPos.y < boardHeight) {
                
                boolean blocked = false;
                for (Snake snake : allSnakes) {
                    if (snake != null && snake.alive) {
                        for (int i = 0; i < snake.body.size() - (depth > 2 ? 1 : 0); i++) {
                            if (snake.body.get(i).equals(nextPos)) {
                                blocked = true;
                                break;
                            }
                        }
                        if (blocked) break;
                    }
                }
                
                if (!blocked) {
                    validMoves++;
                    if (validMoves >= 2) return false; // We have options
                }
            }
        }
        
        return validMoves < 2; // Dead end detected
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Point) {
                Point other = (Point) obj;
                return this.x == other.x && this.y == other.y;
            }
            return false;
        }
    }

    private static class Snake {
        boolean alive = false;
        int length = 0;
        int kills = 0;
        List<Point> body = new ArrayList<>();
    }
}