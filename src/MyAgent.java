import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

/**
 * ELITE SNAKE AI - TOURNAMENT GRADE
 * 
 * Core Strategy Philosophy:
 * 1. AGGRESSIVE apple collection with smart competition analysis
 * 2. RUTHLESS hunting of shorter snakes when safe
 * 3. DEFENSIVE evasion from longer snakes with escape routes
 * 4. BFS pathfinding as PRIMARY strategy
 * 5. Hamiltonian cycle as EMERGENCY fallback only
 * 6. Space preservation is NON-NEGOTIABLE
 */
public class MyAgent extends DevelopmentAgent {

    private int[] dx = { 0, 0, -1, 1 };
    private int[] dy = { -1, 1, 0, 0 };
    private boolean[][] visited;
    private int[][] gameBoard;
    private int boardWidth, boardHeight;
    private Random random = new Random();

    // Pathfinding structures
    private List<Point> hamiltonianCycle;
    private boolean useHamiltonian = false;
    private Queue<Point> bfsQueue = new LinkedList<>();
    private int[][] bfsDistance;
    private Point[][] bfsParent;

    // Game state tracking
    private int turnCount = 0;
    private int lastAppleEatenTurn = 0;

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

            visited = new boolean[boardWidth][boardHeight];
            gameBoard = new int[boardWidth][boardHeight];
            bfsDistance = new int[boardWidth][boardHeight];
            bfsParent = new Point[boardWidth][boardHeight];

            hamiltonianCycle = generateHamiltonianCycle();
            useHamiltonian = (hamiltonianCycle != null && hamiltonianCycle.size() > 0);

            int appleAge = 0;
            int lastAppleX = -1, lastAppleY = -1;

            while (true) {
                String line = br.readLine();
                if (line.contains("Game Over"))
                    break;

                turnCount++;

                String[] appleCoords = line.split(" ");
                int appleX = Integer.parseInt(appleCoords[0]);
                int appleY = Integer.parseInt(appleCoords[1]);

                // Apple respawn detection
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

                int move = calculateOptimalMove(snakes[mySnakeNum], snakes, appleX, appleY, appleAge);
                System.out.println(move);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * MASTER DECISION ENGINE
     * Prioritizes aggressive apple collection while maintaining survival
     */
    private int calculateOptimalMove(Snake mySnake, Snake[] allSnakes, int appleX, int appleY, int appleAge) {
        if (mySnake == null || !mySnake.alive || mySnake.body == null || mySnake.body.isEmpty()) {
            return 0;
        }

        Point head = mySnake.body.get(0);
        if (head == null) {
            return 0;
        }

        // Calculate apple value with proper rounding
        double rawAppleValue = 5.0 - (appleAge * 0.1);
        int appleValue = (int) Math.ceil(rawAppleValue);

        // Setup game board
        setupGameBoardEfficiently(allSnakes);

        // ==================================================================
        // PHASE 1: ABSOLUTE EMERGENCY ESCAPE (life or death)
        // ==================================================================
        if (isInAbsoluteDanger(mySnake, allSnakes)) {
            int emergencyMove = findAbsoluteEmergencyEscape(mySnake, allSnakes);
            if (emergencyMove != -1) {
                Point emergencyHead = calculateNewHead(mySnake, emergencyMove);
                if (emergencyHead != null && isMoveSafe(mySnake, emergencyHead, allSnakes)) {
                    int emergencySpace = calculateReachableSpaceOptimized(emergencyHead);
                    if (emergencySpace >= 2) { // Accept minimal space in emergency
                        return emergencyMove;
                    }
                }
            }
        }

        // ==================================================================
        // PHASE 2: AGGRESSIVE APPLE PURSUIT (primary objective)
        // ==================================================================
        if (appleValue >= 1) {
            // Calculate distance to apple
            int distToApple = Math.abs(head.x - appleX) + Math.abs(head.y - appleY);

            // CRITICAL: If we're very close to apple, GO FOR IT unless deadly
            if (distToApple <= 2 && appleValue >= 2) {
                int directMove = findDirectPathToApple(head, appleX, appleY, mySnake, allSnakes);
                if (directMove != -1) {
                    Point directHead = calculateNewHead(mySnake, directMove);
                    if (directHead != null && isMoveSafe(mySnake, directHead, allSnakes)) {
                        int directSpace = calculateReachableSpaceOptimized(directHead);

                        // Aggressive apple grab - only check minimal safety
                        if (directSpace >= mySnake.body.size() * 0.3) {
                            // Check if we'll win head-on collision
                            if (!willLoseHeadOnAtApple(directHead, appleX, appleY, mySnake, allSnakes)) {
                                return directMove;
                            }
                        }
                    }
                }
            }

            // Use BFS for optimal pathfinding
            List<Point> bfsPath = findBFSPath(head, new Point(appleX, appleY), allSnakes);
            if (bfsPath != null && bfsPath.size() > 1) {
                Point nextStep = bfsPath.get(1);
                int bfsMove = getDirectionToPoint(head, nextStep);

                if (bfsMove != -1 && isMoveSafe(mySnake, nextStep, allSnakes)) {
                    int pathSpace = calculateReachableSpaceOptimized(nextStep);

                    // Aggressive space threshold - don't be too cautious
                    if (pathSpace >= mySnake.body.size() * 0.4) {
                        // Additional safety: check if path is relatively clear
                        if (!isPathBlockedByLongerSnake(bfsPath, mySnake, allSnakes, 3)) {
                            return bfsMove;
                        }
                    }
                }
            }
        }

        // ==================================================================
        // PHASE 3: HUNTING MODE (kill shorter snakes for advantage)
        // ==================================================================
        Snake huntTarget = findBestHuntingOpportunity(head, mySnake, allSnakes, appleX, appleY);
        if (huntTarget != null && appleValue >= 2) {
            int huntMove = findHuntingMove(head, huntTarget, mySnake, allSnakes);
            if (huntMove != -1) {
                Point huntHead = calculateNewHead(mySnake, huntMove);
                if (huntHead != null && isMoveSafe(mySnake, huntHead, allSnakes)) {
                    int huntSpace = calculateReachableSpaceOptimized(huntHead);

                    // Ensure hunting doesn't trap us
                    if (huntSpace >= mySnake.body.size() * 0.5) {
                        return huntMove;
                    }
                }
            }
        }

        // ==================================================================
        // PHASE 4: COMPREHENSIVE MOVE EVALUATION
        // ==================================================================
        int bestMove = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int move = 0; move < 4; move++) {
            Point newHead = calculateNewHead(mySnake, move);

            if (newHead == null || !isMoveSafe(mySnake, newHead, allSnakes)) {
                continue;
            }

            int score = evaluateMove(newHead, mySnake, allSnakes, appleX, appleY, appleValue);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        // ==================================================================
        // PHASE 5: HAMILTONIAN FALLBACK (safe survival mode)
        // ==================================================================
        if (bestMove == -1 && useHamiltonian) {
            int hamiltonianMove = getHamiltonianMove(mySnake, allSnakes);
            if (hamiltonianMove != -1) {
                Point hamiltonianHead = calculateNewHead(mySnake, hamiltonianMove);
                if (hamiltonianHead != null && isMoveSafe(mySnake, hamiltonianHead, allSnakes)) {
                    int hamiltonianSpace = calculateReachableSpaceOptimized(hamiltonianHead);
                    if (hamiltonianSpace >= mySnake.body.size() * 0.5) {
                        return hamiltonianMove;
                    }
                }
            }
        }

        // ==================================================================
        // PHASE 6: RELATIVE MOVES (last attempt before desperation)
        // ==================================================================
        if (bestMove == -1 && mySnake.body.size() > 1) {
            for (int move = 4; move < 7; move++) {
                Point newHead = calculateNewHead(mySnake, move);

                if (newHead != null && isMoveSafe(mySnake, newHead, allSnakes)) {
                    int space = calculateReachableSpaceOptimized(newHead);
                    if (space >= 2) {
                        return move;
                    }
                }
            }
        }

        // ==================================================================
        // PHASE 7: DESPERATION MODE (any safe move)
        // ==================================================================
        if (bestMove == -1) {
            for (int move = 0; move < 4; move++) {
                Point newHead = calculateNewHead(mySnake, move);
                if (newHead != null && isBasicallySafe(newHead)) {
                    if (calculateReachableSpaceOptimized(newHead) >= 1) {
                        return move;
                    }
                }
            }
            return 0; // Ultimate fallback
        }

        return bestMove;
    }

    /**
     * CRITICAL: Direct pathfinding to apple when close
     */
    private int findDirectPathToApple(Point head, int appleX, int appleY, Snake mySnake, Snake[] allSnakes) {
        int bestMove = -1;
        int minDist = Integer.MAX_VALUE;

        for (int move = 0; move < 4; move++) {
            Point newHead = calculateNewHead(mySnake, move);

            if (newHead != null && isMoveSafe(mySnake, newHead, allSnakes)) {
                int dist = Math.abs(newHead.x - appleX) + Math.abs(newHead.y - appleY);

                if (dist < minDist) {
                    minDist = dist;
                    bestMove = move;
                }
            }
        }

        return bestMove;
    }

    /**
     * CRITICAL: Check if we'll lose head-on collision at apple
     */
    private boolean willLoseHeadOnAtApple(Point myNewPos, int appleX, int appleY,
            Snake mySnake, Snake[] allSnakes) {
        // If we're not at the apple, no collision
        if (myNewPos.x != appleX || myNewPos.y != appleY) {
            return false;
        }

        // Check if any enemy can also reach apple this turn
        for (Snake enemy : allSnakes) {
            if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) {
                continue;
            }

            Point enemyHead = enemy.body.get(0);

            // Check if enemy is 1 move away from apple
            for (int move = 0; move < 4; move++) {
                int nx = enemyHead.x + dx[move];
                int ny = enemyHead.y + dy[move];

                if (nx == appleX && ny == appleY) {
                    // Enemy can reach apple - check who wins
                    if (enemy.body.size() > mySnake.body.size()) {
                        return true; // We lose
                    }
                    if (enemy.body.size() == mySnake.body.size() && enemy.kills > mySnake.kills) {
                        return true; // We lose on tiebreaker
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if path is blocked by longer snakes
     */
    private boolean isPathBlockedByLongerSnake(List<Point> path, Snake mySnake,
            Snake[] allSnakes, int checkDepth) {
        if (path == null || path.size() <= 1) {
            return false;
        }

        int depth = Math.min(checkDepth, path.size());

        for (int i = 1; i < depth; i++) {
            Point pathPoint = path.get(i);

            for (Snake enemy : allSnakes) {
                if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) {
                    continue;
                }

                if (enemy.body.size() > mySnake.body.size()) {
                    Point enemyHead = enemy.body.get(0);
                    int enemyDist = Math.abs(enemyHead.x - pathPoint.x) + Math.abs(enemyHead.y - pathPoint.y);

                    // If enemy can reach this point before or same time as us
                    if (enemyDist <= i) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Find best hunting opportunity
     */
    private Snake findBestHuntingOpportunity(Point myPos, Snake mySnake,
            Snake[] allSnakes, int appleX, int appleY) {
        Snake bestTarget = null;
        int bestScore = Integer.MIN_VALUE;

        for (Snake enemy : allSnakes) {
            if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) {
                continue;
            }

            // Only hunt snakes that are shorter
            if (mySnake.body.size() > enemy.body.size()) {
                Point enemyHead = enemy.body.get(0);
                int distance = Math.abs(myPos.x - enemyHead.x) + Math.abs(myPos.y - enemyHead.y);

                // Only consider nearby targets
                if (distance <= 6) {
                    int score = 0;

                    // Size advantage bonus
                    score += (mySnake.body.size() - enemy.body.size()) * 150;

                    // Kill count advantage
                    int killDiff = mySnake.kills - enemy.kills;
                    if (killDiff >= 0) {
                        score += 300 + (killDiff * 100);
                    }

                    // Proximity bonus (but not too aggressive)
                    if (distance <= 2) {
                        score += 200;
                    } else if (distance <= 4) {
                        score += 100;
                    } else {
                        score -= distance * 10;
                    }

                    // Bonus if enemy is near apple (intercept opportunity)
                    int enemyAppleDist = Math.abs(enemyHead.x - appleX) + Math.abs(enemyHead.y - appleY);
                    if (enemyAppleDist <= 3) {
                        score += 250;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        bestTarget = enemy;
                    }
                }
            }
        }

        // Only return target if score is high enough
        return bestScore > 400 ? bestTarget : null;
    }

    /**
     * Find move to intercept/hunt target
     */
    private int findHuntingMove(Point myPos, Snake target, Snake mySnake, Snake[] allSnakes) {
        if (target == null || target.body.isEmpty()) {
            return -1;
        }

        Point targetHead = target.body.get(0);
        int bestMove = -1;
        int minDist = Integer.MAX_VALUE;

        for (int move = 0; move < 4; move++) {
            Point newPos = calculateNewHead(mySnake, move);

            if (newPos != null && isMoveSafe(mySnake, newPos, allSnakes)) {
                int dist = Math.abs(newPos.x - targetHead.x) + Math.abs(newPos.y - targetHead.y);

                if (dist < minDist) {
                    minDist = dist;
                    bestMove = move;
                }
            }
        }

        return bestMove;
    }

    /**
     * Check if snake is in absolute danger
     */
    private boolean isInAbsoluteDanger(Snake mySnake, Snake[] allSnakes) {
        Point head = mySnake.body.get(0);

        // Check immediate space
        int safeSquares = 0;
        for (int i = 0; i < 4; i++) {
            Point newPos = new Point(head.x + dx[i], head.y + dy[i]);
            if (newPos.x >= 0 && newPos.x < boardWidth && newPos.y >= 0 && newPos.y < boardHeight) {
                if (gameBoard[newPos.x][newPos.y] == 0) {
                    safeSquares++;
                }
            }
        }

        if (safeSquares <= 1) {
            return true;
        }

        // Check proximity to longer/equal snakes
        for (Snake enemy : allSnakes) {
            if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) {
                continue;
            }

            if (enemy.body.size() >= mySnake.body.size()) {
                Point enemyHead = enemy.body.get(0);
                int dist = Math.abs(head.x - enemyHead.x) + Math.abs(head.y - enemyHead.y);

                if (dist <= 1) {
                    return true; // Imminent collision with equal/longer snake
                }
            }
        }

        return false;
    }

    /**
     * Find absolute emergency escape
     */
    private int findAbsoluteEmergencyEscape(Snake mySnake, Snake[] allSnakes) {
        Point head = mySnake.body.get(0);
        int bestMove = -1;
        int maxSpace = -1;

        for (int move = 0; move < 4; move++) {
            Point newPos = calculateNewHead(mySnake, move);

            if (newPos != null && isMoveSafe(mySnake, newPos, allSnakes)) {
                int space = calculateReachableSpaceOptimized(newPos);

                // Also consider distance from longer snakes
                int minDistToLonger = Integer.MAX_VALUE;
                for (Snake enemy : allSnakes) {
                    if (enemy != null && enemy.alive && enemy != mySnake && !enemy.body.isEmpty()) {
                        if (enemy.body.size() >= mySnake.body.size()) {
                            Point enemyHead = enemy.body.get(0);
                            int dist = Math.abs(newPos.x - enemyHead.x) + Math.abs(newPos.y - enemyHead.y);
                            minDistToLonger = Math.min(minDistToLonger, dist);
                        }
                    }
                }

                int combinedScore = space * 10 + minDistToLonger * 5;

                if (combinedScore > maxSpace) {
                    maxSpace = combinedScore;
                    bestMove = move;
                }
            }
        }

        return bestMove;
    }

    /**
     * COMPREHENSIVE MOVE EVALUATION
     */
    private int evaluateMove(Point newPos, Snake mySnake, Snake[] allSnakes,
            int appleX, int appleY, int appleValue) {
        int score = 0;

        // Space evaluation (critical for survival)
        int reachableSpace = calculateReachableSpaceOptimized(newPos);
        score += reachableSpace * 12; // Increased weight

        // Penalty for tight spaces
        if (reachableSpace < mySnake.body.size() * 0.5) {
            score -= 2000;
        }
        if (reachableSpace < 3) {
            score -= 8000;
        }

        // Apple distance (aggressive pursuit)
        int appleDist = Math.abs(newPos.x - appleX) + Math.abs(newPos.y - appleY);

        if (appleValue >= 1) {
            // AGGRESSIVE: Strong pull toward apple
            score += (50 - appleDist) * 40; // Increased multiplier

            if (appleDist == 0) {
                score += 5000; // MASSIVE bonus for eating apple
            } else if (appleDist == 1) {
                score += 1500; // Huge bonus for being adjacent
            } else if (appleDist == 2) {
                score += 800; // Strong bonus for being close
            }
        } else if (appleValue < 0 && appleValue > -4) {
            // Avoid bad apples
            score -= (10 - appleDist) * 30;
        } else if (appleValue <= -4) {
            // FLEE from deadly apples
            score -= (20 - appleDist) * 150;
            if (appleDist <= 1) {
                score -= 5000;
            }
        }

        // Combat evaluation
        for (Snake enemy : allSnakes) {
            if (enemy == null || !enemy.alive || enemy == mySnake || enemy.body.isEmpty()) {
                continue;
            }

            Point enemyHead = enemy.body.get(0);
            int distToEnemy = Math.abs(newPos.x - enemyHead.x) + Math.abs(newPos.y - enemyHead.y);

            int sizeDiff = mySnake.body.size() - enemy.body.size();

            if (sizeDiff > 0) {
                // We're longer - hunting mode
                if (distToEnemy <= 3) {
                    score += 300 + (sizeDiff * 50);
                }
                if (distToEnemy == 1) {
                    score += 400; // Kill opportunity
                }
            } else if (sizeDiff < 0) {
                // Enemy is longer - evasion mode
                if (distToEnemy <= 2) {
                    score -= 1500; // Strong avoidance
                } else if (distToEnemy <= 4) {
                    score -= 500;
                } else {
                    score += distToEnemy * 10; // Bonus for distance
                }
            } else {
                // Equal size - use kills as tiebreaker
                int killDiff = mySnake.kills - enemy.kills;
                if (killDiff > 0 && distToEnemy <= 2) {
                    score += 200; // Slight aggression if we have more kills
                } else if (killDiff < 0 && distToEnemy <= 2) {
                    score -= 400; // Avoid if they have more kills
                }
            }
        }

        // Wall proximity (prefer center)
        int wallDist = Math.min(
                Math.min(newPos.x, boardWidth - 1 - newPos.x),
                Math.min(newPos.y, boardHeight - 1 - newPos.y));
        score += wallDist * 5;

        // Penalty for corners
        if ((newPos.x == 0 || newPos.x == boardWidth - 1) &&
                (newPos.y == 0 || newPos.y == boardHeight - 1)) {
            score -= 500;
        }

        return score;
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private Point calculateNewHead(Snake snake, int move) {
        if (snake == null || snake.body == null || snake.body.isEmpty()) {
            return null;
        }

        Point head = snake.body.get(0);
        if (head == null) {
            return null;
        }

        switch (move) {
            case 0:
                return new Point(head.x, head.y - 1); // North
            case 1:
                return new Point(head.x, head.y + 1); // South
            case 2:
                return new Point(head.x - 1, head.y); // West
            case 3:
                return new Point(head.x + 1, head.y); // East
            case 4: // Relative left
            case 5: // Relative straight
            case 6: // Relative right
                if (snake.body.size() > 1) {
                    Point neck = snake.body.get(1);
                    if (neck != null) {
                        return calculateRelativeMove(head, neck, move);
                    }
                }
                return new Point(head.x, head.y - 1);
            default:
                return new Point(head.x, head.y - 1);
        }
    }

    private Point calculateRelativeMove(Point head, Point previous, int move) {
        if (head == null || previous == null) {
            return new Point(head.x, head.y - 1);
        }

        int dx = head.x - previous.x;
        int dy = head.y - previous.y;

        if (Math.abs(dx) > 1)
            dx = Integer.signum(dx);
        if (Math.abs(dy) > 1)
            dy = Integer.signum(dy);

        switch (move) {
            case 4:
                return new Point(head.x - dy, head.y + dx); // Turn left
            case 5:
                return new Point(head.x + dx, head.y + dy); // Straight
            case 6:
                return new Point(head.x + dy, head.y - dx); // Turn right
            default:
                return new Point(head.x + dx, head.y + dy);
        }
    }

    private boolean isMoveSafe(Snake mySnake, Point newHead, Snake[] allSnakes) {
        if (newHead == null || mySnake == null || !mySnake.alive) {
            return false;
        }

        // Wall check
        if (newHead.x < 0 || newHead.x >= boardWidth || newHead.y < 0 || newHead.y >= boardHeight) {
            return false;
        }

        // Self-collision check (excluding tail)
        if (mySnake.body != null) {
            for (int i = 0; i < mySnake.body.size() - 1; i++) {
                Point bodyPart = mySnake.body.get(i);
                if (bodyPart != null && newHead.equals(bodyPart)) {
                    return false;
                }
            }
        }

        // Enemy collision check (excluding tails)
        if (allSnakes != null) {
            for (Snake snake : allSnakes) {
                if (snake != null && snake != mySnake && snake.alive && snake.body != null) {
                    for (int i = 0; i < snake.body.size() - 1; i++) {
                        Point bodyPart = snake.body.get(i);
                        if (bodyPart != null && newHead.equals(bodyPart)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private int calculateReachableSpaceOptimized(Point start) {
        if (start == null || start.x < 0 || start.x >= boardWidth ||
                start.y < 0 || start.y >= boardHeight) {
            return 0;
        }

        if (gameBoard == null) {
            return 5;
        }

        boolean[][] localVisited = new boolean[boardWidth][boardHeight];
        Queue<Point> queue = new LinkedList<>();
        queue.offer(start);
        localVisited[start.x][start.y] = true;
        int count = 0;

        while (!queue.isEmpty() && count < 30) { // Increased limit for better accuracy
            Point current = queue.poll();
            if (current == null)
                continue;

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

        return Math.max(count, 1);
    }

    private void setupGameBoardEfficiently(Snake[] allSnakes) {
        // Clear board
        for (int i = 0; i < boardWidth; i++) {
            for (int j = 0; j < boardHeight; j++) {
                gameBoard[i][j] = 0;
            }
        }

        // Mark all snake bodies
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

    private boolean isBasicallySafe(Point pos) {
        return pos.x >= 0 && pos.x < boardWidth && pos.y >= 0 && pos.y < boardHeight;
    }

    // ========================================================================
    // BFS PATHFINDING
    // ========================================================================

    private List<Point> findBFSPath(Point start, Point target, Snake[] allSnakes) {
        if (start == null || target == null)
            return null;

        // Initialize BFS structures
        for (int i = 0; i < boardWidth; i++) {
            for (int j = 0; j < boardHeight; j++) {
                bfsDistance[i][j] = -1;
                bfsParent[i][j] = null;
                visited[i][j] = false;
            }
        }

        // Mark obstacles (snake bodies excluding tails)
        for (Snake snake : allSnakes) {
            if (snake != null && snake.alive && snake.body != null) {
                // Don't mark tail as it will move
                for (int i = 0; i < snake.body.size() - 1; i++) {
                    Point body = snake.body.get(i);
                    if (body != null && isValidBounds(body)) {
                        visited[body.x][body.y] = true;
                    }
                }
            }
        }

        // BFS initialization
        bfsQueue.clear();
        bfsQueue.offer(start);
        bfsDistance[start.x][start.y] = 0;
        visited[start.x][start.y] = false; // Start should be accessible

        // BFS search
        while (!bfsQueue.isEmpty()) {
            Point current = bfsQueue.poll();

            if (current.equals(target)) {
                return reconstructBFSPath(start, target);
            }

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                Point next = new Point(nx, ny);

                if (isValidBounds(next) && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    bfsDistance[nx][ny] = bfsDistance[current.x][current.y] + 1;
                    bfsParent[nx][ny] = current;
                    bfsQueue.offer(next);
                }
            }
        }

        return null;
    }

    private List<Point> reconstructBFSPath(Point start, Point target) {
        List<Point> path = new ArrayList<>();
        Point current = target;

        while (current != null && !current.equals(start)) {
            path.add(0, current);
            current = bfsParent[current.x][current.y];
        }

        if (current != null) {
            path.add(0, start);
            return path;
        }

        return null;
    }

    private boolean isValidBounds(Point p) {
        return p.x >= 0 && p.x < boardWidth && p.y >= 0 && p.y < boardHeight;
    }

    private int getDirectionToPoint(Point from, Point to) {
        if (from == null || to == null)
            return -1;

        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (dx == 0 && dy == -1)
            return 0; // North
        if (dx == 0 && dy == 1)
            return 1; // South
        if (dx == -1 && dy == 0)
            return 2; // West
        if (dx == 1 && dy == 0)
            return 3; // East

        return -1;
    }

    // ========================================================================
    // HAMILTONIAN CYCLE (Emergency Fallback)
    // ========================================================================

    private List<Point> generateHamiltonianCycle() {
        List<Point> cycle = new ArrayList<>();

        if (boardWidth % 2 == 0 && boardHeight % 2 == 0) {
            // Simple snake pattern for even dimensions
            for (int y = 0; y < boardHeight; y++) {
                if (y % 2 == 0) {
                    for (int x = 0; x < boardWidth; x++) {
                        cycle.add(new Point(x, y));
                    }
                } else {
                    for (int x = boardWidth - 1; x >= 0; x--) {
                        cycle.add(new Point(x, y));
                    }
                }
            }
        } else {
            return generateModifiedHamiltonianCycle();
        }

        return cycle.size() == boardWidth * boardHeight ? cycle : null;
    }

    private List<Point> generateModifiedHamiltonianCycle() {
        List<Point> cycle = new ArrayList<>();

        int left = 0, right = boardWidth - 1;
        int top = 0, bottom = boardHeight - 1;

        while (left <= right && top <= bottom) {
            // Top row
            for (int x = left; x <= right; x++) {
                cycle.add(new Point(x, top));
            }
            top++;

            // Right column
            for (int y = top; y <= bottom; y++) {
                cycle.add(new Point(right, y));
            }
            right--;

            // Bottom row
            if (top <= bottom) {
                for (int x = right; x >= left; x--) {
                    cycle.add(new Point(x, bottom));
                }
                bottom--;
            }

            // Left column
            if (left <= right) {
                for (int y = bottom; y >= top; y--) {
                    cycle.add(new Point(left, y));
                }
                left++;
            }
        }

        return cycle;
    }

    private int getHamiltonianMove(Snake mySnake, Snake[] allSnakes) {
        if (hamiltonianCycle == null || hamiltonianCycle.isEmpty())
            return -1;

        Point head = mySnake.body.get(0);

        // Find current position in cycle
        int currentIndex = -1;
        for (int i = 0; i < hamiltonianCycle.size(); i++) {
            if (hamiltonianCycle.get(i).equals(head)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            currentIndex = findClosestHamiltonianPoint(head);
        }

        // Get next position in cycle
        int nextIndex = (currentIndex + 1) % hamiltonianCycle.size();
        Point nextPos = hamiltonianCycle.get(nextIndex);

        // Validate safety
        if (!isMoveSafe(mySnake, nextPos, allSnakes)) {
            return -1;
        }

        return getDirectionToPoint(head, nextPos);
    }

    private int findClosestHamiltonianPoint(Point head) {
        int closestIndex = 0;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < hamiltonianCycle.size(); i++) {
            Point cyclePoint = hamiltonianCycle.get(i);
            int distance = Math.abs(head.x - cyclePoint.x) + Math.abs(head.y - cyclePoint.y);

            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    // ========================================================================
    // SNAKE PARSING
    // ========================================================================

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

        // Parse kink points
        List<Point> kinks = new ArrayList<>();
        for (int i = 3; i < parts.length; i++) {
            String[] coords = parts[i].split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            kinks.add(new Point(x, y));
        }

        // Reconstruct full body from kinks
        snake.body = reconstructFullBody(kinks, snake.length);

        return snake;
    }

    /**
     * CRITICAL: Reconstruct complete snake body from coordinate chain
     * The game provides kink points - we must fill in all segments between them
     */
    private List<Point> reconstructFullBody(List<Point> kinks, int totalLength) {
        if (kinks.isEmpty())
            return new ArrayList<>();

        List<Point> fullBody = new ArrayList<>();
        fullBody.add(new Point(kinks.get(0).x, kinks.get(0).y)); // Add head

        // Reconstruct body by filling in points between kinks
        for (int i = 0; i < kinks.size() - 1; i++) {
            Point start = kinks.get(i);
            Point end = kinks.get(i + 1);

            // Determine direction
            int dx = Integer.signum(end.x - start.x);
            int dy = Integer.signum(end.y - start.y);

            // Fill in all points from start to end
            int x = start.x + dx;
            int y = start.y + dy;

            while ((x != end.x || y != end.y) && fullBody.size() < totalLength) {
                fullBody.add(new Point(x, y));
                x += dx;
                y += dy;
            }

            // Add the kink point itself (unless it's the head)
            if (fullBody.size() < totalLength && (i > 0 || !end.equals(kinks.get(0)))) {
                fullBody.add(new Point(end.x, end.y));
            }

            if (fullBody.size() >= totalLength) {
                break;
            }
        }

        // Ensure exact length
        while (fullBody.size() > totalLength) {
            fullBody.remove(fullBody.size() - 1);
        }

        return fullBody;
    }

    // ========================================================================
    // DATA STRUCTURES
    // ========================================================================

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

        @Override
        public int hashCode() {
            return x * 1000 + y;
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    private static class Snake {
        boolean alive = false;
        int length = 0;
        int kills = 0;
        List<Point> body = new ArrayList<>();

        @Override
        public String toString() {
            return String.format("Snake[alive=%s, length=%d, kills=%d, head=%s]",
                    alive, length, kills, body.isEmpty() ? "none" : body.get(0));
        }
    }
}
