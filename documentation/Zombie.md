# SnakeAI - Comprehensive Code Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture and Design](#architecture-and-design)
3. [File Structure](#file-structure)
4. [Core Classes and Methods](#core-classes-and-methods)
5. [AI Strategies Implementation](#ai-strategies-implementation)
6. [Algorithm Details](#algorithm-details)
7. [Game Flow and Logic](#game-flow-and-logic)
8. [Performance Optimizations](#performance-optimizations)

## Project Overview

The SnakeAI project is an intelligent agent designed for playing a multiplayer version of the classic Snake game. The AI was developed for the COMS2015A course and implements multiple sophisticated algorithms to compete against other snakes on a 50x50 grid battlefield.

### Game Objectives
- **Primary Goal**: Eat apples to grow and become the longest snake
- **Secondary Goal**: Prevent other snakes from achieving the same
- **Survival**: Avoid collisions with other snakes, zombies, and boundaries

### Key Features
- **Pathfinding**: A* algorithm for optimal apple collection
- **Territory Control**: Voronoi diagram-based space analysis
- **Competitive Intelligence**: Multi-snake awareness and collision avoidance
- **Zombie Handling**: Special logic for dealing with AI-controlled zombie snakes

## Architecture and Design

The project follows a modular design pattern with clear separation of concerns:

```
Main.java (Agent Controller)
    ├── Pathfinding Module (Astar.java, Node.java)
    ├── Territory Analysis (BFS.java, NodeBFS.java)
    ├── Game State Management (Snake.java, Point.java)
    ├── Visualization (DrawBoard.java)
    └── Logging System (Logger.java)
```

## File Structure

### Core Files Created

| File | Purpose | Key Components |
|------|---------|----------------|
| `Main.java` | Primary agent controller and game loop | MyAgent class, decision-making logic |
| `Snake.java` | Snake entity representation | Snake properties and state management |
| `Point.java` | Coordinate system and distance calculations | Position tracking, Manhattan distance |
| `Astar.java` | A* pathfinding algorithm implementation | Optimal path calculation |
| `Node.java` | A* algorithm node structure | Heuristic calculations, path reconstruction |
| `BFS.java` | Breadth-First Search for Voronoi diagrams | Territory analysis, space control |
| `NodeBFS.java` | BFS-specific node implementation | Neighbor generation, territory mapping |
| `DrawBoard.java` | Game board visualization and rendering | Snake drawing, grid management |
| `Logger.java` | Debug logging system | File-based logging for analysis |

### External Dependencies
- `Snake2022-v1.jar` - Game framework library
- `za.ac.wits.snake.DevelopmentAgent` - Base agent interface

## Core Classes and Methods

### 1. Main.java - MyAgent Class

**Primary Class**: `MyAgent extends DevelopmentAgent`

#### Key Static Variables
```java
public static int mySnakeNum = 0;      // Current snake's ID
public static int logCount = 0;        // Logging counter
public static int deadSnakes = 0;      // Count of eliminated snakes
```

#### Core Method: `run()`
**Purpose**: Main game loop that processes game state and makes decisions

**Flow**:
1. **Initialization Phase**: Parse game parameters and setup data structures
2. **Board State Processing**: Read and interpret current game state
3. **Snake Management**: Process all snake positions and states
4. **Strategy Selection**: Choose between apple pursuit or territory control
5. **Move Execution**: Calculate and output the optimal move

**Detailed Implementation**:

```java
// Board initialization (50x50 grid)
char[][] board = new char[50][50];          // Game board representation
int[][] boardVoronoi = new int[50][50];     // Voronoi territory map
int[][] boardVoronoiCopy = new int[50][50]; // Backup for calculations
```

**Decision Logic**:
- **Apple Proximity Check**: Uses Voronoi diagram to determine if agent is closest to apple
- **Strategy A - Apple Pursuit**: If closest, use A* pathfinding to reach apple
- **Strategy B - Territory Control**: If not closest, maximize controlled territory using BFS

### 2. Snake.java - Snake Entity

**Purpose**: Represents a snake entity with complete state information

#### Properties
```java
public Point head, tail;        // Snake endpoints
public int length, kills;       // Snake statistics  
public int index;              // Unique identifier
boolean isAlive, isZombie;     // State flags
```

#### Constructor
```java
public Snake(Point head, Point tail, int length, int kills, 
             int index, boolean isAlive, boolean isZombie)
```

**Usage**: Creates snake objects for tracking all entities on the board, including:
- Player's snake (with complete head/tail information)
- Opponent snakes (head-only tracking)
- Zombie snakes (AI-controlled obstacles)

### 3. Point.java - Coordinate System

**Purpose**: Handles 2D coordinate operations and distance calculations

#### Core Methods

**Constructor Overloads**:
```java
public Point(int row, int col)                    // Direct coordinate input
public Point(String point, String split)         // Parse from string format
```

**Distance Calculation**:
```java
double distanceTo(Point other) {
    return Math.abs(this.row - other.row) + Math.abs(this.col - other.col);
}
```
- Uses Manhattan distance (optimal for grid-based movement)
- Critical for A* heuristic calculations

### 4. Astar.java - Pathfinding Implementation

**Purpose**: Implements A* algorithm for optimal pathfinding to apples

#### Key Method: `startAStar()`
```java
public static int startAStar(char[][] grid, Point start, Point end)
```

**Algorithm Flow**:
1. **Initialize**: Create priority queue with starting node
2. **Search Loop**: 
   - Extract lowest f-cost node
   - Check if destination reached
   - Generate valid neighbor nodes
   - Update costs and parent pointers
3. **Path Reconstruction**: Trace back from goal to determine first move

**Heuristic Function**: Uses Manhattan distance for admissible heuristic

#### Move Translation: `getMove()`
```java
public static int getMove(Point from, Point to)
```
**Returns**:
- 0: Up movement
- 1: Down movement  
- 2: Left movement
- 3: Right movement

#### Collision Avoidance
- Treats 'x' cells as obstacles (other snakes, boundaries)
- Returns -1 if no path exists (triggers fallback strategy)

### 5. Node.java - A* Node Structure

**Purpose**: Represents nodes in A* search with cost calculations

#### Key Components
```java
double fCost;           // Total cost (g + h)
Node parent;            // Path reconstruction
```

**Cost Calculation**:
- Uses Manhattan distance as heuristic (h-cost)
- Implements Comparable for priority queue ordering
- Overrides equals() and hashCode() for proper set operations

### 6. BFS.java - Territory Analysis

**Purpose**: Implements multi-directional BFS for Voronoi diagram generation

#### Core Method: `VoronoiDiagram()`
```java
public static void VoronoiDiagram(int[][] boardVoronoi, ArrayList<Snake> Snakes, 
                                  ArrayList<Integer> cellCount, int mySnakeIndex)
```

**Algorithm Details**:
1. **Multi-Source BFS**: Simultaneous expansion from all snake heads
2. **Territory Assignment**: Each cell assigned to nearest snake
3. **Zombie Handicap**: Zombie snakes expand every other turn (strategic balancing)
4. **Cell Counting**: Tracks territory size for decision making

**Strategic Importance**:
- Determines which snake is closest to apple
- Evaluates potential territory gain from moves
- Provides spatial awareness for competitive positioning

### 7. NodeBFS.java - BFS Node Implementation

**Purpose**: Specialized node for BFS operations

#### Key Method: `getNeighbours()`
```java
public NodeBFS[] getNeighbours() {
    int[][] adj = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};  // 4-directional movement
    // Returns array of neighboring positions
}
```

### 8. DrawBoard.java - Visualization System

**Purpose**: Handles board rendering and snake drawing

#### Snake Drawing: `drawSnake()`
```java
public static void drawSnake(String snake, char[][] board)
```
- Parses snake coordinate string
- Draws connected line segments between body points
- Marks occupied cells as obstacles ('x')

#### Voronoi Mapping: `drawSnakeVornoi()`
```java
public static void drawSnakeVornoi(String snake, int[][] board, int snakeNumber)
```
- Similar to drawSnake but for territory mapping
- Assigns snake indices to occupied cells

#### Debug Output
- `printGrid()` and `printIntGrid()` for visual debugging
- Outputs formatted board states to logger

### 9. Logger.java - Debug System

**Purpose**: File-based logging for debugging and analysis

```java
public static void log(String string)
```
- Creates timestamped log files
- Sequential numbering for easy tracking
- Essential for debugging complex multi-turn strategies

## AI Strategies Implementation

### Strategy 1: Apple Pursuit (Closest Snake)

**Trigger Condition**: `isCloser = boardVoronoi[applePoint.row][applePoint.col] == mySnake.index`

**Implementation**:
1. **Primary Path**: Use A* to find optimal route to apple
2. **Obstacle Handling**: If path blocked by zombie danger zones:
   - Remove temporary 'x' markings around zombie heads
   - Redraw actual zombie snake bodies
   - Retry A* pathfinding
3. **Collision Avoidance**: Built into A* through obstacle detection

**Code Flow**:
```java
if (isCloser) {
    move = Astar.startAStar(board, applePoint, myHead);
    
    if (move == -1) {  // Path blocked
        // Remove zombie head danger zones
        // Redraw actual zombie bodies  
        // Retry pathfinding
        move = Astar.startAStar(board, applePoint, myHead);
    }
}
```

### Strategy 2: Territory Maximization (Not Closest)

**Trigger Condition**: Snake is not closest to apple based on Voronoi analysis

**Implementation**:
1. **Move Evaluation**: Test all 4 possible moves
2. **Territory Simulation**: For each valid move:
   - Temporarily update snake head position
   - Run Voronoi diagram calculation
   - Count controlled territory
3. **Best Move Selection**: Choose move with maximum territory gain
4. **Fallback Safety**: If no territorial advantage, choose any safe move

**Code Flow**:
```java
else {  // Not closest to apple
    // Test each possible move (up, down, left, right)
    for (int i = 0; i < 4; i++) {
        Point newHead = possibleMoves.get(i);
        
        // Validate move
        if (isValid(newHead) && board[newHead.row][newHead.col] != 'x') {
            // Simulate territory with new position
            Snakes.get(mySnake.index - deadSnakes).head = newHead;
            BFS.VoronoiDiagram(boardVoronoi, Snakes, cellCount, mySnake.index);
            
            // Check if move gives territorial control
            if (cellCount.get(0) > highestCellCount) {
                bestMove = i;
                highestCellCount = cellCount.get(0);
            }
        }
    }
}
```

## Algorithm Details

### A* Pathfinding Algorithm

**Complexity**: O(b^d) where b is branching factor, d is depth
**Optimality**: Guaranteed optimal path with admissible heuristic
**Heuristic**: Manhattan distance (admissible for grid movement)

**Key Features**:
- **Priority Queue**: Uses Java's PriorityQueue for efficient node selection
- **Visited Set**: HashSet prevents revisiting nodes
- **Parent Tracking**: Enables path reconstruction
- **Obstacle Avoidance**: Dynamic obstacle detection

### Multi-Source BFS (Voronoi Diagram)

**Purpose**: Determines territorial control and spatial relationships
**Complexity**: O(V + E) where V is cells, E is edges

**Algorithm Steps**:
1. **Initialize**: Add all snake heads to separate queues
2. **Simultaneous Expansion**: Process all queues in parallel
3. **Territory Assignment**: First snake to reach cell claims it
4. **Zombie Balancing**: Zombies expand every other iteration
5. **Territory Counting**: Track controlled cells for strategy decisions

**Strategic Applications**:
- Apple proximity detection
- Territory maximization
- Spatial awareness
- Competitive positioning

## Game Flow and Logic

### Turn Structure

1. **Input Processing**:
   ```
   Game State → Parse Apple Position → Process Zombie Snakes → 
   Get My Snake ID → Process All Player Snakes
   ```

2. **Board State Construction**:
   - Initialize 50x50 grids (game board + Voronoi map)
   - Mark apple position ('G')
   - Draw all snake bodies ('x' for obstacles)
   - Mark danger zones around enemy heads
   - Set own head as special marker ('S')

3. **Strategy Selection**:
   - Run Voronoi analysis to determine apple proximity
   - Choose between apple pursuit or territory control
   - Execute chosen strategy with fallback options

4. **Move Output**:
   - Convert calculated move to integer (0-3)
   - Output to game engine

### Collision Detection System

**Multi-Layer Safety**:
1. **Boundary Checking**: Prevent out-of-bounds moves
2. **Snake Body Avoidance**: Treat all 'x' cells as obstacles  
3. **Head-to-Head Prevention**: Mark danger zones around enemy heads
4. **Zombie Awareness**: Special handling for AI-controlled snakes

**Danger Zone Creation**:
```java
// Mark cells around enemy heads as dangerous
for (int i = 0; i < 4; i++) {
    int row = enemyHead.row + dRow[i];
    int col = enemyHead.col + dCol[i];
    if (isValid(row, col)) {
        board[row][col] = 'x';  // Mark as obstacle
    }
}
```

## Performance Optimizations

### Memory Management
- **Array Copying**: Efficient board state preservation using `System.arraycopy()`
- **Object Reuse**: Minimal object creation in tight loops
- **Collection Sizing**: Appropriate initial capacities for collections

### Computational Efficiency
- **Early Termination**: A* stops immediately upon reaching goal
- **Pruning**: Invalid moves filtered before expensive calculations
- **Caching**: Voronoi diagrams preserved between calculations

### Strategic Optimizations
- **Zombie Balancing**: Artificial handicap prevents zombie dominance
- **Fallback Strategies**: Multiple decision layers ensure valid moves
- **Territory Prediction**: Lookahead for strategic positioning

### Code Structure Benefits
- **Modular Design**: Clear separation enables easy debugging
- **Immutable Operations**: Board states preserved during simulations
- **Type Safety**: Strong typing prevents runtime errors

## Conclusion

The SnakeAI project demonstrates sophisticated AI techniques applied to competitive gaming:

- **Multi-Algorithm Approach**: Combines A* pathfinding with BFS territory analysis
- **Adaptive Strategy**: Switches between aggressive apple pursuit and defensive territory control  
- **Robust Safety**: Multiple layers of collision avoidance
- **Competitive Intelligence**: Accounts for multiple opponents and environmental hazards

The implementation showcases practical AI development with real-time constraints, competitive dynamics, and the need for both optimal pathfinding and strategic territorial control.