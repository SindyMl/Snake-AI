public class TestLogic {
    public static void main(String[] args) {
        System.out.println("Testing snake movement logic...");
        
        // Test basic movement calculation
        int headX = 10, headY = 10;
        int appleX = 12, appleY = 10;
        
        System.out.println("Head at: (" + headX + "," + headY + ")");  
        System.out.println("Apple at: (" + appleX + "," + appleY + ")");
        
        // Test each direction
        for (int dir = 0; dir < 4; dir++) {
            int newX = headX + (dir == 2 ? -1 : dir == 3 ? 1 : 0);
            int newY = headY + (dir == 0 ? -1 : dir == 1 ? 1 : 0);
            int distance = Math.abs(newX - appleX) + Math.abs(newY - appleY);
            
            System.out.println("Dir " + dir + " -> (" + newX + "," + newY + ") distance=" + distance);
        }
        
        System.out.println("Expected: dir 3 (right) should have shortest distance");
    }
}
