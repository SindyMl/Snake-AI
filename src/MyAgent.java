import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {
    private Random random = new Random();
    private int timestep = 0;
    private int prevAppleX = -1, prevAppleY = -1;
    private int width = 50, height = 50;

    public static void main(String[] args) {
        MyAgent agent = new MyAgent();
        MyAgent.start(agent, args);
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String initString = br.readLine();
            if (initString == null)
                return;
            String[] initParts = initString.split(" ");
            int nSnakes = Integer.parseInt(initParts[0]);
            width = Integer.parseInt(initParts[1]);
            height = Integer.parseInt(initParts[2]);
            int mode = Integer.parseInt(initParts[3]);

            System.out.println("log Initialized: " + nSnakes + " snakes, " + width + "x" + height);

            while (true) {
                String appleLine = br.readLine();
                if (appleLine == null || appleLine.trim().equals("Game Over")) {
                    System.out.println("log Game Over after " + timestep + " steps");
                    break;
                }
                String[] appleParts = appleLine.split(" ");
                int appleX = Integer.parseInt(appleParts[0]);
                int appleY = Integer.parseInt(appleParts[1]);

                // Reset on respawn
                if (appleX != prevAppleX || appleY != prevAppleY) {
                    timestep = 0;
                    System.out.println("log Apple respawned - fresh!");
                }
                prevAppleX = appleX;
                prevAppleY = appleY;

                double appleValue = Math.ceil(5 - 0.1 * timestep);
                boolean eatApple = appleValue > 0 && appleValue > -4; // Eat positive, avoid <= -4 kill
                System.out.println("log Apple (" + appleX + "," + appleY + "), value: " + appleValue
                        + (eatApple ? " (EAT!)" : " (AVOID - shrink/kill)"));

                int mySnakeNum = Integer.parseInt(br.readLine().trim());

                // Parse snakes, build board (Lab 1a matrix)
                int headX = -1, headY = -1;
                List<int[]> myBody = new ArrayList<>();
                List<int[]> allBodies = new ArrayList<>();
                List<Integer> enemyLengths = new ArrayList<>();
                int myLength = 0;
                int[][] board = new int[height][width]; // 0=free, 1=self/short, 2=longer enemy, 3=wall

                for (int i = 0; i < nSnakes; i++) {
                    String snakeLine = br.readLine().trim();
                    if (snakeLine.isEmpty() || !snakeLine.startsWith("alive"))
                        continue;

                    String[] parts = snakeLine.split(" ");
                    int length = Integer.parseInt(parts[1]);
                    int kills = Integer.parseInt(parts[2]);
                    String headStr = parts[3];
                    String[] headXY = headStr.split(",");
                    int hx = Integer.parseInt(headXY[0]);
                    int hy = Integer.parseInt(headXY[1]);

                    List<int[]> body = new ArrayList<>();
                    body.add(new int[] { hx, hy });
                    for (int j = 4; j < parts.length; j++) {
                        String coord = parts[j];
                        if (coord.isEmpty())
                            continue;
                        String[] xy = coord.split(",");
                        int bx = Integer.parseInt(xy[0]);
                        int by = Integer.parseInt(xy[1]);
                        if (by >= 0 && by < height && bx >= 0 && bx < width) {
                            body.add(new int[] { bx, by });
                            int cost = (i == mySnakeNum ? 1 : (length > myLength ? 2 : 1));
                            board[by][bx] = cost;
                        }
                    }
                    allBodies.addAll(body);

                    if (i == mySnakeNum) {
                        headX = hx;
                        headY = hy;
                        myBody = body;
                        myLength = length;
                        System.out.println(
                                "log My: Head (" + headX + "," + headY + "), Len " + length + ", Kills " + kills);
                    } else {
                        enemyLengths.add(length);
                    }
                }

                timestep++;

                if (headX == -1 || headY == -1) {
                    System.out.println(random.nextInt(4));
                    continue;
                }

                // Mark apple if safe
                if (eatApple && appleY >= 0 && appleY < height && appleX >= 0 && appleX < width) {
                    board[appleY][appleX] = -1; // Goal (low cost)
                }

                // Mark walls high cost
                for (int y = 0; y < height; y++) {
                    board[y][0] = 3;
                    board[y][width - 1] = 3;
                }
                for (int x = 0; x < width; x++) {
                    board[0][x] = 3;
                    board[height - 1][x] = 3;
                }

                int move = decideMove(headX, headY, appleX, appleY, eatApple, myBody, allBodies, board, myLength);

                System.out.println(move);
                System.out.println("log T" + timestep + ": Move " + move + " from (" + headX + "," + headY + ")");
            }
        } catch (Exception e) {
            System.err.println("log Error: " + e.getMessage());
            System.out.println(random.nextInt(4));
        }
    }

    private int decideMove(int headX, int headY, int appleX, int appleY, boolean eatApple, List<int[]> myBody,
            List<int[]> allBodies, int[][] board, int myLength) {
        int currDir = getCurrentDirection(myBody, headX, headY);

        // 1. 2-Step Lookahead: Avoid wall/longer snake in next 2 moves
        int lookaheadDir = lookaheadSafe(headX, headY, myBody, allBodies, board, myLength);
        if (lookaheadDir != -1) {
            System.out.println("log 2-Step Evade: Dir " + lookaheadDir);
            return lookaheadDir; // Cardinal
        }

        // 2. PRIORITIZE APPLE: A* shortest, direct first step
        if (eatApple) {
            List<int[]> path = aStar(board, new int[] { headY, headX }, new int[] { appleY, appleX }, height, width);
            if (path != null && path.size() > 1) {
                int nextX = path.get(1)[1];
                int nextY = path.get(1)[0];
                int dir = getCardinalDir(headX, headY, nextX, nextY);
                System.out.println("log Shortest to apple: " + path.size() + " steps");
                return dir;
            } else if (Math.hypot(headX - appleX, headY - appleY) < 10) { // Close
                int dir = straightToApple(headX, headY, appleX, appleY);
                System.out.println("log Close apple: straight " + dir);
                return dir;
            }
        }

        // 3. Respawn Near Apple: If just respawned (dist >30?), head straight to apple
        if (Math.hypot(headX - appleX, headY - appleY) > 30 && timestep < 5) {
            int dir = straightToApple(headX, headY, appleX, appleY);
            System.out.println("log Respawn chase: straight " + dir);
            return dir;
        }

        // 4. Explore open
        int[] open = findOpenSpace(board);
        List<int[]> path = aStar(board, new int[] { headY, headX }, new int[] { open[0], open[1] }, height, width);
        if (path != null && path.size() > 1) {
            int nextX = path.get(1)[1];
            int nextY = path.get(1)[0];
            return getCardinalDir(headX, headY, nextX, nextY);
        }

        // Fallback safe
        return randomSafe(headX, headY, myBody, allBodies, board);
    }

    // Straight to apple (direct, no diag)
    private int straightToApple(int hx, int hy, int ax, int ay) {
        int dx = ax - hx;
        int dy = ay - hy;
        if (Math.abs(dx) > Math.abs(dy)) {
            return dx > 0 ? 3 : 2; // Horizontal first
        } else {
            return dy > 0 ? 1 : 0; // Vertical
        }
    }

    // Cardinal dir
    private int getCardinalDir(int cx, int cy, int nx, int ny) {
        int dx = nx - cx;
        int dy = ny - cy;
        if (dx == 0 && dy < 0)
            return 0;
        if (dx == 0 && dy > 0)
            return 1;
        if (dy == 0 && dx < 0)
            return 2;
        if (dy == 0 && dx > 0)
            return 3;
        // Approx: Bigger delta
        if (Math.abs(dx) > Math.abs(dy))
            return dx > 0 ? 3 : 2;
        return dy > 0 ? 1 : 0;
    }

    // 2-Step Lookahead Safe
    private int lookaheadSafe(int hx, int hy, List<int[]> myBody, List<int[]> allBodies, int[][] board, int myLength) {
        int[][] dirs = { { 0, -1, 0 }, { 0, 1, 1 }, { -1, 0, 2 }, { 1, 0, 3 } }; // dy, dx, dir
        int best = -1;
        int maxSafe = 0;
        for (int[] d : dirs) {
            int nx1 = hx + d[1];
            int ny1 = hy + d[0];
            if (!isSafe(nx1, ny1, myBody, allBodies, board, myLength))
                continue;

            int nx2 = nx1 + d[1];
            int ny2 = ny1 + d[0];
            int safeSteps = isSafe(nx2, ny2, myBody, allBodies, board, myLength) ? 2 : 1;
            if (safeSteps > maxSafe) {
                maxSafe = safeSteps;
                best = d[2];
            }
        }
        return best;
    }

    private boolean isSafe(int x, int y, List<int[]> myBody, List<int[]> allBodies, int[][] board, int myLength) {
        if (x < 0 || x >= width || y < 0 || y >= height || board[y][x] >= 2)
            return false; // Wall/longer
        return !myBody.stream().anyMatch(p -> p[0] == x && p[1] == y) &&
                !allBodies.stream().anyMatch(p -> p[0] == x && p[1] == y);
    }

    private int randomSafe(int hx, int hy, List<int[]> myBody, List<int[]> allBodies, int[][] board) {
        List<Integer> safe = new ArrayList<>();
        int[][] dirs = {{0,-1,0}, {0,1,1}, {-1,0,2}, {1,0,3}};
        for (int[] d : dirs) {
            int nx = hx + d[1];
            int ny = hy + d[0];
            if (isSafe(nx, ny, myBody, allBodies, board, myBody.size())) {
                safe.add(d[2]);
            }
        }
        return safe.isEmpty() ? random.nextInt(4) : safe.get(random.nextInt(safe.size()));
    }

private int[] findOpenSpace(int[][] board) {
        int minOcc = Integer.MAX_VALUE;
        int openX = width / 2, openY = height / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (board[y][x] == 0) {
                    int occ = countNearbyOcc(board, x, y);
                    if (occ < minOcc) {
                        minOcc = occ;
                        openX = x;
                        openY =