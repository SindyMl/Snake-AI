public class TestMyAgent {
    public static void main(String[] args) {
        System.out.println("Testing MyAgent decision logic...");
        
        // Create a sample scenario
        int headX = 10, headY = 10;
        int appleX = 12, appleY = 10;
        int boardWidth = 20, boardHeight = 20;
        
        // Empty board
        int[][] board = new int[boardWidth][boardHeight];
        
        // Simple snake body (just head and one segment)
        int[][] myBody = {{10, 10}, {9, 10}};
        int myLength = 2;
        
        // No enemies
        int[][][] enemies = new int[0][][];
        
        MyAgent agent = new MyAgent();
        
        System.out.println("Sample scenario:");
        System.out.println("Head at (" + headX + "," + headY + ")");
        System.out.println("Apple at (" + appleX + "," + appleY + ")");
        System.out.println("Expected move: 3 (right toward apple)");
        System.out.println();
        
        // Test the enhanced decision
        try {
            java.lang.reflect.Method method = MyAgent.class.getDeclaredMethod(
                "enhancedDecision", int.class, int.class, int.class, int.class, 
                int[][][].class, int[][].class, int[][].class, int.class, int.class, int.class);
            method.setAccessible(true);
            
            int decision = (Integer) method.invoke(agent, headX, headY, appleX, appleY, 
                enemies, board, myBody, myLength, boardWidth, boardHeight);
            
            System.out.println("Actual decision: " + decision);
            
            if (decision == 3) {
                System.out.println("SUCCESS: Agent correctly chose to move right toward apple!");
            } else if (decision == -1) {
                System.out.println("PROBLEM: Agent returned -1, indicating no safe moves found");
            } else {
                System.out.println("ISSUE: Agent chose direction " + decision + " instead of 3 (right)");
            }
            
        } catch (Exception e) {
            System.out.println("Error testing decision logic: " + e.getMessage());
            e.printStackTrace();
        }
    }
}