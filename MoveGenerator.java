import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    public static String[] move(String color, Board board) {
        List<String> moves = new ArrayList<>();
        
        // DEBUG: Add comprehensive logging when no moves are found
        int colorPieceCount = 0;
        int totalPieces = 0;
        
        // Check every tile in the board (8x8)
        for (int i = 0; i < 8; i++) {

            int row = board.isRedPlayer() ? i : (7 - i); // Red: 0,1,2...7, Black: 7,6,5...0

            for (int col = 0; col < 8; col++) {
                int pieceValue = board.getPiece(row, col);
                
                if (pieceValue != Board.EMPTY) {
                    totalPieces++;
                }
                
                // Generate moves for ALL pieces that belong to the current player
                // Both pushers and pushed pieces can generate moves
                if (isPieceOfColor(pieceValue, color)) {
                    colorPieceCount++;
                    List<String> pieceMoves = PossibleMoves(pieceValue, col, row, board);
                    
                    // DEBUG: Log piece-specific move generation when debugging needed
                    if (pieceMoves.isEmpty()) {
                        String pos = board.positionToString(row, col);
                        String pieceDesc = getPieceDescription(pieceValue);
                        //System.out.println("DEBUG: " + pieceDesc + " at " + pos + " has no moves");
                    }
                    
                    moves.addAll(pieceMoves);
                }
            }
        }
        
        // DEBUG: Log detailed info if no moves found for a color that has pieces
        if (moves.isEmpty() && colorPieceCount > 0) {
            System.out.println("CRITICAL BUG: " + color + " has " + colorPieceCount + " pieces but NO MOVES!");
            System.out.println("Total pieces on board: " + totalPieces);
            System.out.println("Board state when no moves found:");
            board.printBoard();
            
            // Re-scan and log each piece and why it has no moves
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    int pieceValue = board.getPiece(row, col);
                    if (isPieceOfColor(pieceValue, color)) {
                        String pos = board.positionToString(row, col);
                        String pieceDesc = getPieceDescription(pieceValue);
                        System.out.println("Analyzing " + pieceDesc + " at " + pos + ":");
                        
                        List<String> pieceMoves = PossibleMoves(pieceValue, col, row, board);
                        if (pieceMoves.isEmpty()) {
                            System.out.println("  No moves available - checking why...");
                            debugWhyNoMoves(pieceValue, col, row, board);
                        } else {
                            System.out.println("  Has " + pieceMoves.size() + " moves: " + pieceMoves);
                        }
                    }
                }
            }
        }

        // In MoveGenerator.move(), add debugging for the specific case:
        /*System.out.println("=== MOVE GENERATOR DEBUG ===");
        System.out.println("Looking for moves for color: " + color);
        System.out.println("Board state at G1: " + board.getPiece(0, 6)); // Should be Red pusher
        System.out.println("Board state at H2: " + board.getPiece(1, 7)); // Should be Black pusher
        System.out.println("Is G1H2 valid? " + board.isValidMove(board.parseMove("G1H2")));*/
        
        return moves.toArray(new String[0]);
    }
    
    // Helper method to get a description of the piece
    private static String getPieceDescription(int piece) {
        switch (piece) {
            case Board.EMPTY: return "Empty";
            case Board.BLACK_PUSHED: return "Black Pushed";
            case Board.BLACK_PUSHER: return "Black Pusher";
            case Board.RED_PUSHED: return "Red Pushed";
            case Board.RED_PUSHER: return "Red Pusher";
            default: return "Unknown";
        }
    }
    private static List<String> PossibleMoves(int pieceValue, int col, int row, Board board)
    {
        List<String> moves = new ArrayList<>();

        if (getPieceDescription(pieceValue).equals("Black Pusher") || getPieceDescription(pieceValue).equals("Red Pusher"))
        {
            // PUSHER MOVES
            // Determine movement direction (Black moves down, Red moves up)
            int direction = getPieceDescription(pieceValue).startsWith("Black") ? 1 : -1;

            // Check front cell (row + direction, col) to see if blocked
            int frontRow = row + direction;
            if (frontRow >= 0 && frontRow < 8)
            {
                int frontPiece = board.getPiece(frontRow, col);
                if (frontPiece == Board.EMPTY)
                {
                    // Front is empty, can move
                    String fromPos = board.positionToString(row, col);
                    String toPos = board.positionToString(frontRow, col);
                    moves.add(fromPos + toPos);
                }
                // NOTE: Pusher CANNOT move to a square occupied by ANY piece
            }

            // Check diagonal left
            int diagLeftRow = row + direction;
            int diagLeftCol = col - 1;
            if (diagLeftRow >= 0 && diagLeftRow < 8 && diagLeftCol >= 0 && diagLeftCol < 8)
            {
                int diagLeftPiece = board.getPiece(diagLeftRow, diagLeftCol);
                if (diagLeftPiece == Board.EMPTY)
                {
                    // If empty, can move
                    String fromPos = board.positionToString(row, col);
                    String toPos = board.positionToString(diagLeftRow, diagLeftCol);
                    moves.add(fromPos + toPos);
                }
                else if (isOppositeColor(pieceValue, diagLeftPiece))
                {
                    // Can capture diagonally if opposite color is there
                    String fromPos = board.positionToString(row, col);
                    String toPos = board.positionToString(diagLeftRow, diagLeftCol);
                    moves.add(fromPos + toPos);
                }
                // NOTE: Cannot move diagonally if same color piece is there
            }

            // Check diagonal right
            int diagRightRow = row + direction;
            int diagRightCol = col + 1;
            if (diagRightRow >= 0 && diagRightRow < 8 && diagRightCol >= 0 && diagRightCol < 8)
            {
                int diagRightPiece = board.getPiece(diagRightRow, diagRightCol);
                if (diagRightPiece == Board.EMPTY)
                {
                    // If empty, can move
                    String fromPos = board.positionToString(row, col);
                    String toPos = board.positionToString(diagRightRow, diagRightCol);
                    moves.add(fromPos + toPos);
                }
                else if (isOppositeColor(pieceValue, diagRightPiece))
                {
                    // Can capture diagonally if opposite color is there
                    String fromPos = board.positionToString(row, col);
                    String toPos = board.positionToString(diagRightRow, diagRightCol);
                    moves.add(fromPos + toPos);
                }
                // NOTE: Cannot move diagonally if same color piece is there
            }
        }
        else if (getPieceDescription(pieceValue).equals("Black Pushed") || getPieceDescription(pieceValue).equals("Red Pushed"))
        {
            // PUSHED PIECE MOVES
            // Pushed pieces can only move when pushed by a pusher behind them
            int direction = getPieceDescription(pieceValue).startsWith("Black") ? 1 : -1;
            
            // Check for pushers behind this pushed piece that can push it
            
            // Check directly behind - pusher pushes piece straight forward
            int behindRow = row - direction;
            int behindCol = col;
            if (behindRow >= 0 && behindRow < 8)
            {
                int behindPiece = board.getPiece(behindRow, behindCol);
                if (isPusherOfSameColor(pieceValue, behindPiece))
                {
                    // There's a pusher behind - check if can move forward
                    int frontRow = row + direction;
                    if (frontRow >= 0 && frontRow < 8)
                    {
                        int frontPiece = board.getPiece(frontRow, col);
                        if (frontPiece == Board.EMPTY)
                        {
                            // Can move forward
                            String fromPos = board.positionToString(row, col);
                            String toPos = board.positionToString(frontRow, col);
                            moves.add(fromPos + toPos);
                        }
                        else if (isOppositeColor(pieceValue, frontPiece))
                        {
                            // Pushed pieces CAN capture straight forward when pushed by a pusher
                            String fromPos = board.positionToString(row, col);
                            String toPos = board.positionToString(frontRow, col);
                            moves.add(fromPos + toPos);
                        }
                    }
                }
            }
            
            // Check diagonal behind left - pusher pushes piece to diagonal right
            int behindLeftRow = row - direction;
            int behindLeftCol = col - 1;
            if (behindLeftRow >= 0 && behindLeftRow < 8 && behindLeftCol >= 0 && behindLeftCol < 8)
            {
                int behindLeftPiece = board.getPiece(behindLeftRow, behindLeftCol);
                if (isPusherOfSameColor(pieceValue, behindLeftPiece))
                {
                    // Pusher can push this piece diagonally
                    int diagRightRow = row + direction;
                    int diagRightCol = col + 1;
                    if (diagRightRow >= 0 && diagRightRow < 8 && diagRightCol >= 0 && diagRightCol < 8)
                    {
                        int diagRightPiece = board.getPiece(diagRightRow, diagRightCol);
                        if (diagRightPiece == Board.EMPTY)
                        {
                            String fromPos = board.positionToString(row, col);
                            String toPos = board.positionToString(diagRightRow, diagRightCol);
                            moves.add(fromPos + toPos);
                        }
                        else if (isOppositeColor(pieceValue, diagRightPiece))
                        {
                            // Can capture diagonally
                            String fromPos = board.positionToString(row, col);
                            String toPos = board.positionToString(diagRightRow, diagRightCol);
                            moves.add(fromPos + toPos);
                        }
                    }
                }
            }
            
            // Check diagonal behind right - pusher pushes piece to diagonal left
            int behindRightRow = row - direction;
            int behindRightCol = col + 1;
            if (behindRightRow >= 0 && behindRightRow < 8 && behindRightCol >= 0 && behindRightCol < 8)
            {
                int behindRightPiece = board.getPiece(behindRightRow, behindRightCol);
                if (isPusherOfSameColor(pieceValue, behindRightPiece))
                {
                    // Pusher can push this piece diagonally
                    int diagLeftRow = row + direction;
                    int diagLeftCol = col - 1;
                    if (diagLeftRow >= 0 && diagLeftRow < 8 && diagLeftCol >= 0 && diagLeftCol < 8)
                    {
                        int diagLeftPiece = board.getPiece(diagLeftRow, diagLeftCol);
                        if (diagLeftPiece == Board.EMPTY)
                        {
                            String fromPos = board.positionToString(row, col);
                            String toPos = board.positionToString(diagLeftRow, diagLeftCol);
                            moves.add(fromPos + toPos);
                        }
                        else if (isOppositeColor(pieceValue, diagLeftPiece))
                        {
                            // Can capture diagonally
                            String fromPos = board.positionToString(row, col);
                            String toPos = board.positionToString(diagLeftRow, diagLeftCol);
                            moves.add(fromPos + toPos);
                        }
                    }
                }
            }
        }

        return moves;
    }

    private static boolean isBlackPiece(int piece) {
        return piece == Board.BLACK_PUSHED || piece == Board.BLACK_PUSHER;
    }

    private static boolean isRedPiece(int piece) {
        return piece == Board.RED_PUSHED || piece == Board.RED_PUSHER;
    }

    private static boolean isOppositeColor(int pieceValue, int otherPieceValue) {
        if (otherPieceValue == Board.EMPTY) {
            return false;
        }
        return (isBlackPiece(pieceValue) && isRedPiece(otherPieceValue)) ||
               (isRedPiece(pieceValue) && isBlackPiece(otherPieceValue));
    }

    // Helper method to check if a piece belongs to the specified color
    private static boolean isPieceOfColor(int piece, String color) {
        if (piece == Board.EMPTY) {
            return false;
        }
        
        if (color.equalsIgnoreCase("red") || color.equalsIgnoreCase("r")) {
            return isRedPiece(piece);
        } else if (color.equalsIgnoreCase("black") || color.equalsIgnoreCase("b")) {
            return isBlackPiece(piece);
        }
        
        return false;
    }
    
    // Helper method to check if a piece is a pusher of the same color as the pushed piece
    private static boolean isPusherOfSameColor(int pushedPiece, int pusherPiece) {
        if (pushedPiece == Board.BLACK_PUSHED && pusherPiece == Board.BLACK_PUSHER) {
            return true;
        }
        if (pushedPiece == Board.RED_PUSHED && pusherPiece == Board.RED_PUSHER) {
            return true;
        }
        return false;
    }

    // Debug method to analyze why a piece has no available moves
    private static void debugWhyNoMoves(int pieceValue, int col, int row, Board board) {
        String pieceDesc = getPieceDescription(pieceValue);
        
        if (pieceDesc.equals("Black Pusher") || pieceDesc.equals("Red Pusher")) {
            int direction = pieceDesc.startsWith("Black") ? 1 : -1;
            
            // Check front
            int frontRow = row + direction;
            if (frontRow >= 0 && frontRow < 8) {
                int frontPiece = board.getPiece(frontRow, col);
                System.out.println("    Front (" + board.positionToString(frontRow, col) + "): " + getPieceDescription(frontPiece));
                if (frontPiece != Board.EMPTY) {
                    System.out.println("    Cannot move forward - blocked by " + getPieceDescription(frontPiece));
                }
            } else {
                System.out.println("    Cannot move forward - edge of board");
            }
            
            // Check diagonals
            int diagLeftRow = row + direction;
            int diagLeftCol = col - 1;
            if (diagLeftRow >= 0 && diagLeftRow < 8 && diagLeftCol >= 0 && diagLeftCol < 8) {
                int diagLeftPiece = board.getPiece(diagLeftRow, diagLeftCol);
                System.out.println("    Diagonal left (" + board.positionToString(diagLeftRow, diagLeftCol) + "): " + getPieceDescription(diagLeftPiece));
                if (diagLeftPiece != Board.EMPTY && !isOppositeColor(pieceValue, diagLeftPiece)) {
                    System.out.println("    Cannot move diagonal left - occupied by same color");
                }
            } else {
                System.out.println("    Cannot move diagonal left - edge of board");
            }
            
            int diagRightRow = row + direction;
            int diagRightCol = col + 1;
            if (diagRightRow >= 0 && diagRightRow < 8 && diagRightCol >= 0 && diagRightCol < 8) {
                int diagRightPiece = board.getPiece(diagRightRow, diagRightCol);
                System.out.println("    Diagonal right (" + board.positionToString(diagRightRow, diagRightCol) + "): " + getPieceDescription(diagRightPiece));
                if (diagRightPiece != Board.EMPTY && !isOppositeColor(pieceValue, diagRightPiece)) {
                    System.out.println("    Cannot move diagonal right - occupied by same color");
                }
            } else {
                System.out.println("    Cannot move diagonal right - edge of board");
            }
        } else if (pieceDesc.equals("Black Pushed") || pieceDesc.equals("Red Pushed")) {
            int direction = pieceDesc.startsWith("Black") ? 1 : -1;
            
            System.out.println("    Pushed piece - checking for pushers behind...");
            
            // Check directly behind
            int behindRow = row - direction;
            int behindCol = col;
            if (behindRow >= 0 && behindRow < 8) {
                int behindPiece = board.getPiece(behindRow, behindCol);
                System.out.println("    Behind (" + board.positionToString(behindRow, behindCol) + "): " + getPieceDescription(behindPiece));
                if (!isPusherOfSameColor(pieceValue, behindPiece)) {
                    System.out.println("    No pusher directly behind");
                }
            }
            
            // Check diagonal behind positions
            int behindLeftRow = row - direction;
            int behindLeftCol = col - 1;
            if (behindLeftRow >= 0 && behindLeftRow < 8 && behindLeftCol >= 0 && behindLeftCol < 8) {
                int behindLeftPiece = board.getPiece(behindLeftRow, behindLeftCol);
                System.out.println("    Behind left (" + board.positionToString(behindLeftRow, behindLeftCol) + "): " + getPieceDescription(behindLeftPiece));
            }
            
            int behindRightRow = row - direction;
            int behindRightCol = col + 1;
            if (behindRightRow >= 0 && behindRightRow < 8 && behindRightCol >= 0 && behindRightCol < 8) {
                int behindRightPiece = board.getPiece(behindRightRow, behindRightCol);
                System.out.println("    Behind right (" + board.positionToString(behindRightRow, behindRightCol) + "): " + getPieceDescription(behindRightPiece));
            }
        }
    }
}
