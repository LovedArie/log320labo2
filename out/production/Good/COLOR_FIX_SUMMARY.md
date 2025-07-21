## Fix Summary: AI Color/Side Logic in Connection.java

### PROBLEM IDENTIFIED:
When playing as BLACK, the AI was still generating and sending moves for the RED side instead of BLACK. This happened because the command handling logic incorrectly assumed that:
- Command '3' = always play as BLACK
- Command '4' = always play as RED

This assumption was wrong. The commands indicate when it's the AI's turn to move, but the AI needs to remember which color it was assigned at the start of the game.

### ROOT CAUSE:
The AI was not tracking which color it was assigned to play. Commands 3 and 4 are sent by the server to request moves during ongoing games, but they don't indicate the color - they just indicate it's the AI's turn.

### SOLUTION IMPLEMENTED:

1. **Added Color Tracking Variable**: 
   - Added `String myColor = null;` to track which color this AI instance is playing as

2. **Set Color During Game Initialization**:
   - Command '1' (start as RED): Sets `myColor = "red"`
   - Command '2' (start as BLACK): Sets `myColor = "black"`

3. **Use Tracked Color for All Subsequent Moves**:
   - Command '3' and '4' now both use `myColor` instead of hardcoded colors
   - This ensures the AI always generates moves for the correct side

### KEY CHANGES IN Connection.java:

```java
// Added color tracking
String myColor = null; // Track which color this AI is playing

// Command 1: Set color to red
if(cmd == '1'){
    myColor = "red"; // Set our color
    // ... rest of logic uses "red"
}

// Command 2: Set color to black  
if(cmd == '2'){
    myColor = "black"; // Set our color
    // ... rest of logic uses "black"
}

// Commands 3 & 4: Use tracked color
if(cmd == '3' || cmd == '4'){
    // ... process opponent move ...
    String move = miniMax.findBestMove(board, myColor); // Use tracked color!
    // ... send move ...
}
```

### VERIFICATION:
- Compiled successfully with no errors
- Created and ran TestColorTracking.java which confirms:
  - When playing as RED, AI generates RED moves (e.g., B2C3)
  - When playing as BLACK, AI generates BLACK moves (e.g., E7E6)
  - Different colors produce different moves, confirming correct logic

### RESULT:
The AI now correctly:
1. Tracks which color it was assigned during game start (commands 1 or 2)
2. Always generates moves for that assigned color during ongoing play (commands 3 and 4)
3. Never generates moves for the opponent's color
4. Works correctly whether playing as RED or BLACK

This fix resolves the critical bug where BLACK-playing AI instances were sending RED moves to the server.
