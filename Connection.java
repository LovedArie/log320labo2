import java.io.*;
import java.net.*;


class Client {
    public static void main(String[] args) {
    Socket MyClient;
    BufferedInputStream input;
    BufferedOutputStream output;
    Board board = new Board();
    MiniMax miniMax = new MiniMax();
    //String myColor = null; // Track which color this AI is playing
    String myColor = "red"; // Track which color this AI is playing

    try {
        MyClient = new Socket("localhost", 8888);

        input    = new BufferedInputStream(MyClient.getInputStream());
        output   = new BufferedOutputStream(MyClient.getOutputStream());

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        
        System.out.println("Connected to server. Waiting for commands...");
        
        while(true){
            char cmd = 0;
               
            cmd = (char)input.read();
            System.out.println("Received command: " + cmd);
            
            // Small delay to ensure all data is available
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            // cmd == '1': Start new game as RED player
            // Receives initial board state and finds best move using AI
            if(cmd == '1'){
    myColor = "red"; // Set our color
    System.out.println("Playing as RED");
    // Read the board configuration data
    byte[] aBuffer = new byte[256];
    int size = input.available();
    if (size > 0) {
        input.read(aBuffer, 0, size);
        String boardData = new String(aBuffer, 0, size).trim();
        System.out.println("Board data received: " + boardData);
        board.parseBoardFromServer(boardData);
    }
    board.setRedPlayer(true);
    System.out.println("Finding best move for RED...");
    String move = miniMax.findBestMove(board, "red");
    System.out.println("Best move found: " + move);
    
    // NEW: Fallback to random move if no best move found
    if (move == null) {
        System.out.println("No best move found, selecting random legal move...");
        
        String[] possibleMoves = MoveGenerator.move("red", board);
        if (possibleMoves != null && possibleMoves.length > 0) {
            java.util.Random random = new java.util.Random();
            move = possibleMoves[random.nextInt(possibleMoves.length)];
            System.out.println("Random move selected: " + move);
        } else {
            System.err.println("ERROR: No moves available at all!");
        }
    }
    
    if (move != null) {
        board.makeMoveFromServer(move);
        output.write(move.getBytes(), 0, move.length());
        output.flush();
        System.out.println("Move sent: " + move);
    }
}

// cmd == '2': Start new game as BLACK player
// Receives initial board state and waits for Red to move first
if(cmd == '2'){
    myColor = "black"; // Set our color
    System.out.println("Playing as BLACK - waiting for Red to move first");
    // Read the board configuration data
    byte[] aBuffer = new byte[256];
    int size = input.available();
    if (size > 0) {
        input.read(aBuffer, 0, size);
        String boardData = new String(aBuffer, 0, size).trim();
        System.out.println("Board data received: " + boardData);
        board.parseBoardFromServer(boardData);
    }
    board.setRedPlayer(false);
    System.out.println("Black player initialized. Waiting for Red's first move...");
    // Black does NOT move immediately - waits for command 3 or 4 with Red's move
}

        // cmd == '3': Server requests next move (ongoing game)
        // Receives opponent's last move and responds with AI move
        if(cmd == '3'){
        byte[] aBuffer = new byte[64]; // Increased buffer size
                
        int size = input.available();
        if (size > 0) {
            input.read(aBuffer, 0, Math.min(size, aBuffer.length));
            String opponentMove = new String(aBuffer, 0, size).trim();
            System.out.println("Opponent's move received: '" + opponentMove + "' (length=" + opponentMove.length() + ")");
            
            // Apply opponent's move to our board
            if (!opponentMove.isEmpty()) {
                boolean moveSuccess = board.makeMoveFromServer(opponentMove);
                if (!moveSuccess) {
                    System.out.println("WARNING: Failed to apply opponent move: " + opponentMove);
                }
            } else {
                System.out.println("WARNING: Received empty opponent move!");
            }
        }
        
        // Find our best move using our tracked color
        if (myColor == null) {
            System.err.println("ERROR: myColor is null! This shouldn't happen.");
            continue; // Skip this command and wait for proper initialization
        }
        System.out.println("Finding best move for " + myColor.toUpperCase() + "...");
        
        String move = miniMax.findBestMove(board, myColor);
        System.out.println("Best move found: " + move);
        
        // NEW: Fallback to random move if no best move found
        if (move == null) {
            System.out.println("No best move found, selecting random legal move...");
            
            String[] possibleMoves = MoveGenerator.move(myColor, board);
            if (possibleMoves != null && possibleMoves.length > 0) {
                java.util.Random random = new java.util.Random();
                move = possibleMoves[random.nextInt(possibleMoves.length)];
                System.out.println("Random move selected: " + move);
            } else {
                System.err.println("ERROR: No moves available at all!");
                continue; // Skip this turn
            }
        }
        
        if (move != null) {
            board.makeMoveFromServer(move);
            output.write(move.getBytes(), 0, move.length());
            output.flush();
            System.out.println("Move sent: " + move);
        }
                
         }
         
            // cmd == '4': Server requests next move (ongoing game)
            // Receives opponent's move and responds with AI move
            if(cmd == '4'){
                byte[] aBuffer = new byte[64]; // Increased buffer size
                int size = input.available();
                if (size > 0) {
                    input.read(aBuffer, 0, Math.min(size, aBuffer.length));
                    String opponentMove = new String(aBuffer, 0, size).trim();
                    System.out.println("Opponent's move received: '" + opponentMove + "' (length=" + opponentMove.length() + ")");
                          // Apply opponent's move to our board
                if (!opponentMove.isEmpty()) {
                    boolean moveSuccess = board.makeMoveFromServer(opponentMove);
                    if (!moveSuccess) {
                        System.out.println("WARNING: Failed to apply opponent move: " + opponentMove);
                    }
                } else {
                    System.out.println("WARNING: Received empty opponent move!");
                }
                }
                
                // Find our best move using our tracked color
                if (myColor == null) {
                    System.err.println("ERROR: myColor is null! This shouldn't happen.");
                    continue; // Skip this command and wait for proper initialization
                }
                System.out.println("Finding best move for " + myColor.toUpperCase() + "...");
                
                String move = miniMax.findBestMove(board, myColor);
                System.out.println("Best move found: " + move);
                
                // NEW: Fallback to random move if no best move found
                if (move == null) {
                    System.out.println("No best move found, selecting random legal move...");
                    
                    String[] possibleMoves = MoveGenerator.move(myColor, board);
                    if (possibleMoves != null && possibleMoves.length > 0) {
                        java.util.Random random = new java.util.Random();
                        move = possibleMoves[random.nextInt(possibleMoves.length)];
                        System.out.println("Random move selected: " + move);
                    } else {
                        System.err.println("ERROR: No moves available at all!");
                        continue; // Skip this turn
                    }
                }
                
                if (move != null) {
                    board.makeMoveFromServer(move);
                    output.write(move.getBytes(), 0, move.length());
                    output.flush();
                    System.out.println("Move sent: " + move);
                }
                
            }
            
        // cmd == '5': Server requests next move with opponent's last move info
        // Receives opponent's move data and waits for user input
        if(cmd == '5'){
                byte[] aBuffer = new byte[16];
                int size = input.available();
                input.read(aBuffer,0,size);
        String move = console.readLine();
        output.write(move.getBytes(),0,move.length());
        output.flush();
                
        }
        }
    }
    catch (IOException e) {
        System.err.println("Connection error: " + e.getMessage());
        e.printStackTrace();
    }
    catch (Exception e) {
        System.err.println("Unexpected error: " + e.getMessage());
        e.printStackTrace();
    }
    
    }
}
