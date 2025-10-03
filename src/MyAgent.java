import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {

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
            int boardWidth = Integer.parseInt(temp[1]);
            int boardHeight = Integer.parseInt(temp[2]);

            int appleAge = 0;
            int lastAppleX = -1, lastAppleY = -1;

            while (true) {
                String line = br.readLine();
                if (line.contains("Game Over"))
                    break;

                String[] appleCoords = line.split(" ");
                int appleX = Integer.parseInt(appleCoords[0]);
                int appleY = Integer.parseInt(appleCoords[1]);

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

                int move = calculateMove(snakes[mySnakeNum], snakes, appleX, appleY, appleAge, boardWidth, boardHeight);
                System.out.println(move);
            }
        } catch (Exception e) {
            System.out.println(0);
        }
    }

    private Snake parseSnake(String snakeLine) {
        String[] parts = snakeLine.split(" ");
        if (parts[0].equals("dead")) {
            return new Snake(false, 0, new ArrayList<>());
        }

        boolean alive = parts[0].equals("alive");
        int length = Integer.parseInt(parts[1]);

        List<Point> kinks = new ArrayList<>();
        for (int i = 3; i < parts.length; i++) {
            String[] coords = parts[i].split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            kinks.add(new Point(x, y));
        }

        List<Point> fullBody = reconstructFullBody(kinks, length);
        return new Snake(alive, length, fullBody);
    }

    private List<Point> reconstructFullBody(List<Point> kinks, int length) {
        if (kinks.isEmpty())
            return new ArrayList<>();

        List<Point> fullBody = new ArrayList<>();

        for (int i = 0; i < kinks.size() - 1; i++) {
            Point start = kinks.get(i);
            Point end = kinks.get(i + 1);
            addLineSegment(fullBody, start, end, false);
        }

        if (!kinks.isEmpty()) {
            fullBody.add(new Point(kinks.get(kinks.size() - 1).x, kinks.get(kinks.size() - 1).y));
        }

        while (fullBody.size() > length) {
            fullBody.remove(fullBody.size() - 1);
        }

        return fullBody;
    }

    private void addLineSegment(List<Point> body, Point start, Point end, boolean includeEnd) {
        int dx = Integer.signum(end.x - start.x);
        int dy = Integer.signum(end.y - start.y);

        int x = start.x;
        int y = start.y;

        while (x != end.x || y != end.y) {
            body.add(new Point(x, y));
            x += dx;
            y += dy;
        }

        if (includeEnd) {
            body.add(new Point(end.x, end.y));
        }
    }

    private int calculateMove(Snake mySnake, Snake[] allSnakes, int appleX, int appleY, int appleAge, int boardWidth,
            int boardHeight) {
        if (mySnake == null || !mySnake.alive)
            return 0;

        Point head = mySnake.getHead();
        int[] dx = { 0, 0, -1, 1 };
        int[] dy = { -1, 1, 0, 0 };

        int bestMove = 0;
        int bestScore = Integer.MIN_VALUE;

        for (int dir = 0; dir < 4; dir++) {
            int newX = head.x + dx[dir];
            int newY = head.y + dy[dir];
            Point newPos = new Point(newX, newY);
            int score = evaluateMove(newPos, mySnake, allSnakes, appleX, appleY, appleAge, boardWidth, boardHeight);
            if (score > bestScore) {
                bestScore = score;
                bestMove = dir;
            }
        }
        return bestMove;
    }

    private int evaluateMove(Point newPos, Snake mySnake, Snake[] allSnakes, int appleX, int appleY, int appleAge,
            int boardWidth, int boardHeight) {
        if (newPos.x < 0 || newPos.x >= boardWidth || newPos.y < 0 || newPos.y >= boardHeight) {
            return -10000;
        }

        for (Snake snake : allSnakes) {
            if (snake == null || !snake.alive)
                continue;

            for (int i = 0; i < snake.body.size(); i++) {
                Point bodyPart = snake.body.get(i);

                if (snake == mySnake && i == snake.body.size() - 1) {
                    int appleDistance = Math.abs(appleX - mySnake.getHead().x) + Math.abs(appleY - mySnake.getHead().y);
                    if (appleDistance > 1) {
                        continue;
                    }
                }

                if (newPos.equals(bodyPart)) {
                    return -8000;
                }
            }
        }

        for (Snake snake : allSnakes) {
            if (snake == null || !snake.alive || snake == mySnake)
                continue;

            Point enemyHead = snake.getHead();
            int distance = manhattanDistance(newPos, enemyHead);

            if (snake.length >= mySnake.length) {
                if (distance <= 1) {
                    return -9000;
                }
                if (distance == 2) {
                    return -500;
                }
            } else {
                if (distance <= 2) {
                    return 200;
                }
                if (distance <= 4) {
                    return 50;
                }
            }
        }

        int score = 0;

        int appleDistance = manhattanDistance(newPos, new Point(appleX, appleY));
        double appleValue = Math.max(1.0, 5.0 - (appleAge * 0.1));

        if (appleValue >= 3.0) {
            score += Math.max(0, (int) (150 * appleValue) - appleDistance * 10);
        } else if (appleValue >= 1.0) {
            score += Math.max(0, (int) (100 * appleValue) - appleDistance * 6);
        } else {
            score += appleDistance * 2;
        }

        int wallDistance = Math.min(Math.min(newPos.x, boardWidth - 1 - newPos.x),
                Math.min(newPos.y, boardHeight - 1 - newPos.y));
        score += wallDistance * 8;

        score += calculateAvailableSpace(newPos, allSnakes, boardWidth, boardHeight) * 3;

        return score;
    }

    private int manhattanDistance(Point a, Point b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private int calculateAvailableSpace(Point position, Snake[] allSnakes, int boardWidth, int boardHeight) {
        boolean[][] occupied = new boolean[boardWidth][boardHeight];

        for (Snake snake : allSnakes) {
            if (snake == null || !snake.alive)
                continue;
            for (Point body : snake.body) {
                if (body.x >= 0 && body.x < boardWidth && body.y >= 0 && body.y < boardHeight) {
                    occupied[body.x][body.y] = true;
                }
            }
        }

        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[boardWidth][boardHeight];

        queue.offer(position);
        visited[position.x][position.y] = true;

        int space = 0;
        int[] dx = { -1, 1, 0, 0 };
        int[] dy = { 0, 0, -1, 1 };

        while (!queue.isEmpty() && space < 100) {
            Point current = queue.poll();
            space++;

            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];

                if (nx >= 0 && nx < boardWidth && ny >= 0 && ny < boardHeight && !visited[nx][ny]
                        && !occupied[nx][ny]) {
                    visited[nx][ny] = true;
                    queue.offer(new Point(nx, ny));
                }
            }
        }

        return space;
    }

    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Point point = (Point) obj;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private static class Snake {
        boolean alive;
        int length;
        List<Point> body;

        Snake(boolean alive, int length, List<Point> body) {
            this.alive = alive;
            this.length = length;
            this.body = body;
        }

        Point getHead() {
            return body.isEmpty() ? new Point(0, 0) : body.get(0);
        }
    }
}