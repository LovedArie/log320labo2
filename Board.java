public class Board {
    // Board representation: 0=empty, 1=black pushed, 2=black pusher, 3=red pushed, 4=red pusher
    private int[][] board;
    private boolean isRedPlayer;
    private int redPushers, redPushed, blackPushers, blackPushed;
    
    // Constants for piece types
    public static final int EMPTY = 0;
    public static final int BLACK_PUSHED = 1;
    public static final int BLACK_PUSHER = 2;
    public static final int RED_PUSHED = 3;
    public static final int RED_PUSHER = 4;
    
    public Board() {
        //board = new int[8][8];
        board = new int[][]{
                {2, 2, 0, 2, 0, 0, 2, 0},  // Row 0
                {1, 1, 0, 0, 1, 0, 2, 1},  // Row 1
                {0, 2, 1, 0, 1, 0, 0, 0},  // Row 2 - changed last element from 2 to 0
                {0, 4, 0, 4, 0, 0, 0, 0},  // Row 3 - changed index 3 from 0 to 4, index 6 from 1 to 0
                {3, 4, 0, 1, 0, 0, 0, 0},  // Row 4 - changed index 5 from 3 to 0
                {0, 0, 0, 3, 1, 0, 2, 0},  // Row 5 - changed index 4 from 0 to 1, index 6 from 0 to 2
                {0, 0, 0, 4, 0, 0, 4, 3},  // Row 6 - changed index 1 from 4 to 0, index 5 from 3 to 0, index 6 from 2 to 4
                {0, 0, 0, 0, 4, 0, 4, 4}   // Row 7 - changed index 5 from 4 to 0
        };
        setRedPlayer(true);
        //initializeBoard();
    }
    
    // Initialize the board with starting configuration
    private void initializeBoard() {
        // Clear board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = EMPTY;
            }
        }
        
        // Black pieces (top two rows)
        // Row 0 (rank 8): Black pushers
        for (int j = 0; j < 8; j++) {
            board[0][j] = BLACK_PUSHER;
        }
        // Row 1 (rank 7): Black pushed
        for (int j = 0; j < 8; j++) {
            board[1][j] = BLACK_PUSHED;
        }
        
        // Red pieces (bottom two rows)
        // Row 7 (rank 1): Red pushers
        for (int j = 0; j < 8; j++) {
            board[7][j] = RED_PUSHER;
        }
        // Row 6 (rank 2): Red pushed
        for (int j = 0; j < 8; j++) {
            board[6][j] = RED_PUSHED;
        }
        
        // Initialize piece counts
        redPushers = blackPushers = 8;
        redPushed = blackPushed = 8;
    }
    
    // Parse server board configuration message
    public void parseBoardFromServer(String boardData) {
        String[] pieces = boardData.trim().split("\\s+");
        int index = 0;
        
        // Reset piece counts
        redPushers = redPushed = blackPushers = blackPushed = 0;
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (index < pieces.length) {
                    board[row][col] = Integer.parseInt(pieces[index]);
                    
                    // Count pieces
                    switch (board[row][col]) {
                        case BLACK_PUSHED: blackPushed++; break;
                        case BLACK_PUSHER: blackPushers++; break;
                        case RED_PUSHED: redPushed++; break;
                        case RED_PUSHER: redPushers++; break;
                    }
                    index++;
                }
            }
        }
    }
    
    // Convert algebraic notation to board coordinates
    private int[] parsePosition(String pos) {
        if (pos.length() != 2) return null;
        
        char file = pos.charAt(0);
        char rank = pos.charAt(1);
        
        if (file < 'A' || file > 'H' || rank < '1' || rank > '8') {
            return null;
        }
        
        int col = file - 'A';  // A=0, B=1, ..., H=7
        int row = 8 - (rank - '0');  // 8=0, 7=1, ..., 1=7
        
        return new int[]{row, col};
    }
    
    // Convert board coordinates to algebraic notation
    public String positionToString(int row, int col) {
        char file = (char)('A' + col);
        char rank = (char)('8' - row);
        return "" + file + rank;
    }
    
    // Parse move from server format (e.g., "D6-D5" or "D6D5")
    public Move parseMove(String moveStr) {
        if (moveStr == null || moveStr.trim().isEmpty()) {
            return null;
        }
        
        moveStr = moveStr.trim();
        String fromPos, toPos;
        
        if (moveStr.contains("-")) {
            String[] parts = moveStr.split("-");
            if (parts.length != 2) return null;
            fromPos = parts[0].trim();
            toPos = parts[1].trim();
        } else if (moveStr.length() == 4) {
            fromPos = moveStr.substring(0, 2);
            toPos = moveStr.substring(2, 4);
        } else {
            return null;
        }
        
        int[] from = parsePosition(fromPos);
        int[] to = parsePosition(toPos);
        
        if (from == null || to == null) return null;
        
        return new Move(from[0], from[1], to[0], to[1]);
    }
    
    // Execute a move on the board
    public boolean makeMove(Move move) {

        if (!isValidMove(move)) return false;

        int piece = board[move.fromRow][move.fromCol];
        
        // Handle capture
        if (board[move.toRow][move.toCol] != EMPTY) {
            int capturedPiece = board[move.toRow][move.toCol];
            switch (capturedPiece) {
                case BLACK_PUSHED: blackPushed--; break;
                case BLACK_PUSHER: blackPushers--; break;
                case RED_PUSHED: redPushed--; break;
                case RED_PUSHER: redPushers--; break;
            }
        }
        
        // Move the piece
        board[move.toRow][move.toCol] = piece;
        board[move.fromRow][move.fromCol] = EMPTY;
        
        // Check if this is a pusher moving a pushed piece
        if (isPusher(piece)) {
            Move pushedMove = getPushedPieceMove(move, piece);
            if (pushedMove != null) {
                // Move the pushed piece
                int pushedPiece = board[pushedMove.fromRow][pushedMove.fromCol];
                if (board[pushedMove.toRow][pushedMove.toCol] != EMPTY) {
                    // Capture by pushed piece
                    int capturedPiece = board[pushedMove.toRow][pushedMove.toCol];
                    switch (capturedPiece) {
                        case BLACK_PUSHED: blackPushed--; break;
                        case BLACK_PUSHER: blackPushers--; break;
                        case RED_PUSHED: redPushed--; break;
                        case RED_PUSHER: redPushers--; break;
                    }
                }
                board[pushedMove.toRow][pushedMove.toCol] = pushedPiece;
                board[pushedMove.fromRow][pushedMove.fromCol] = EMPTY;
            }
        }

        return true;
    }
    
    // Check if a move is valid
    public boolean isValidMove(Move move) {
        if (!isValidPosition(move.fromRow, move.fromCol) || 
            !isValidPosition(move.toRow, move.toCol)) {
            return false;
        }
        
        int piece = board[move.fromRow][move.fromCol];
        if (piece == EMPTY) return false;
        
        // Check if it's the right player's piece
        if (isRedPlayer && !isRedPiece(piece)) return false;
        if (!isRedPlayer && !isBlackPiece(piece)) return false;
        
        // Check direction (can only move toward opponent's end)
        int direction = isRedPiece(piece) ? -1 : 1; // Red moves up (decreasing row), Black moves down
        int rowDiff = move.toRow - move.fromRow;
        int colDiff = Math.abs(move.toCol - move.fromCol);
        
        // Valid moves: forward, diagonal left, diagonal right
        if (rowDiff != direction || colDiff > 1) return false;
        
        // Check destination
        int targetPiece = board[move.toRow][move.toCol];
        if (targetPiece != EMPTY) {
            // Can only capture opponent pieces and only on diagonal moves
            if (colDiff == 0) return false; // Can't capture moving straight
            if (isRedPiece(piece) && isRedPiece(targetPiece)) return false;
            if (isBlackPiece(piece) && isBlackPiece(targetPiece)) return false;
        }
        
        // PUSHERS can always move by themselves (if above conditions are met)
        if (isPusher(piece)) {
            return true;
        }
        
        // PUSHED PIECES can only move if there's a pusher behind them in the right position
        if (isPushedPiece(piece)) {
            return isPushedPieceMoveValid(move, piece);
        }
        
        return false;
    }
    
    // Find pushed piece that would be moved by a pusher
    private Move getPushedPieceMove(Move pusherMove, int pusherPiece) {
        int rowDiff = pusherMove.toRow - pusherMove.fromRow;
        int colDiff = pusherMove.toCol - pusherMove.fromCol;
        
        // Find the pushed piece that should be behind the pusher
        int pushedRow = pusherMove.fromRow - rowDiff;
        int pushedCol = pusherMove.fromCol - colDiff;
        
        if (!isValidPosition(pushedRow, pushedCol)) return null;
        
        int pushedPiece = board[pushedRow][pushedCol];
        if (pushedPiece == EMPTY) return null;
        
        // Check if it's the right type of pushed piece
        boolean isPushedPieceValid = (isRedPiece(pusherPiece) && pushedPiece == RED_PUSHED) ||
                                   (isBlackPiece(pusherPiece) && pushedPiece == BLACK_PUSHED);
        
        if (!isPushedPieceValid) return null;
        
        // Calculate where the pushed piece should move
        int newPushedRow = pushedRow + rowDiff;
        int newPushedCol = pushedCol + colDiff;
        
        if (!isValidPosition(newPushedRow, newPushedCol)) return null;
        
        // Check if destination is valid for pushed piece
        int targetPiece = board[newPushedRow][newPushedCol];
        if (targetPiece != EMPTY) {
            // Can only capture opponent pieces and only on diagonal moves
            if (colDiff == 0) return null;
            if (isRedPiece(pushedPiece) && isRedPiece(targetPiece)) return null;
            if (isBlackPiece(pushedPiece) && isBlackPiece(targetPiece)) return null;
        }
        
        return new Move(pushedRow, pushedCol, newPushedRow, newPushedCol);
    }
    
    // Convenience function to make a move using server notation (e.g., "D6-D5" or "D6D5")
    public boolean makeMoveFromServer(String moveStr) {
        Move move = parseMove(moveStr);
        if (move == null) {
            return false;
        }
        return makeMove(move);
    }
    
    // Get current board configuration in server format (space-separated integers)
    public String getBoardConfiguration() {
        StringBuilder config = new StringBuilder();
        
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (config.length() > 0) {
                    config.append(" ");
                }
                config.append(board[row][col]);
            }
        }
        
        return config.toString();
    }

    // Helper methods
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
    
    private boolean isPusher(int piece) {
        return piece == RED_PUSHER || piece == BLACK_PUSHER;
    }
    
    private boolean isRedPiece(int piece) {
        return piece == RED_PUSHER || piece == RED_PUSHED;
    }
    
    private boolean isBlackPiece(int piece) {
        return piece == BLACK_PUSHER || piece == BLACK_PUSHED;
    }
    
    private boolean isPushedPiece(int piece) {
        return piece == RED_PUSHED || piece == BLACK_PUSHED;
    }
    
    /**
     * Check if a pushed piece move is valid (i.e., there's a pusher behind it in the right position)
     */
    private boolean isPushedPieceMoveValid(Move move, int pushedPiece) {
        int rowDiff = move.toRow - move.fromRow;
        int colDiff = move.toCol - move.fromCol;
        
        // Find where the pusher should be to enable this move
        int pusherRow = move.fromRow - rowDiff;
        int pusherCol = move.fromCol - colDiff;
        
        if (!isValidPosition(pusherRow, pusherCol)) return false;
        
        int pusherPiece = board[pusherRow][pusherCol];
        
        // Check if there's a pusher of the same color at the expected position
        if (pushedPiece == RED_PUSHED && pusherPiece == RED_PUSHER) {
            return true;
        }
        if (pushedPiece == BLACK_PUSHED && pusherPiece == BLACK_PUSHER) {
            return true;
        }
        
        return false;
    }

    // Check win conditions
    public boolean isGameOver() {
        return hasWinner() || redPushers == 0 || blackPushers == 0;
    }
    
    public boolean hasWinner() {
        // Check if any piece reached the opposite end
        // Red wins if any red piece reaches row 0 (rank 8)
        for (int col = 0; col < 8; col++) {
            if (isRedPiece(board[0][col])) return true;
        }
        
        // Black wins if any black piece reaches row 7 (rank 1)
        for (int col = 0; col < 8; col++) {
            if (isBlackPiece(board[7][col])) return true;
        }
        
        return false;
    }
    
    public String getWinner() {
        // Check positional win
        for (int col = 0; col < 8; col++) {
            if (isRedPiece(board[0][col])) return "Red";
            if (isBlackPiece(board[7][col])) return "Black";
        }
        
        // Check capture win
        if (redPushers == 0) return "Black";
        if (blackPushers == 0) return "Red";
        
        return null;
    }
    
    // Getters and setters
    public void setRedPlayer(boolean isRed) {
        this.isRedPlayer = isRed;
    }
    
    public boolean isRedPlayer() {
        return isRedPlayer;
    }
    
    public int getPiece(int row, int col) {
        if (isValidPosition(row, col)) {
            return board[row][col];
        }
        return -1;
    }
    
    public void setPiece(int row, int col, int piece) {
        if (isValidPosition(row, col)) {
            board[row][col] = piece;
        }
    }
    
    // Display board (for debugging)
    public void printBoard() {
        System.out.println("   A B C D E F G H");
        for (int row = 0; row < 8; row++) {
            System.out.print((8 - row) + "  ");
            for (int col = 0; col < 8; col++) {
                char symbol;
                switch (board[row][col]) {
                    case EMPTY: symbol = '.'; break;
                    case BLACK_PUSHED: symbol = 'p'; break;
                    case BLACK_PUSHER: symbol = 'P'; break;
                    case RED_PUSHED: symbol = 'r'; break;
                    case RED_PUSHER: symbol = 'R'; break;
                    default: symbol = '?'; break;
                }
                System.out.print(symbol + " ");
            }
            System.out.println();
        }
        System.out.println("Red: " + redPushers + " pushers, " + redPushed + " pushed");
        System.out.println("Black: " + blackPushers + " pushers, " + blackPushed + " pushed");
    }
    
    // Inner class for representing moves
    public static class Move {
        public int fromRow, fromCol, toRow, toCol;
        
        public Move(int fromRow, int fromCol, int toRow, int toCol) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
        }
        
        @Override
        public String toString() {
            char fromFile = (char)('A' + fromCol);
            char fromRank = (char)('8' - fromRow);
            char toFile = (char)('A' + toCol);
            char toRank = (char)('8' - toRow);
            return "" + fromFile + fromRank + "-" + toFile + toRank;
        }
        
        public String toServerFormat() {
            return toString().replace("-", "");
        }
    }
}
