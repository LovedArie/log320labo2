/**
 * Board Evaluation for Pushers Game with Piece Mobility
 *
 * Enhanced evaluation focusing on:
 * - Terminal positions (win/loss detection)
 * - Material balance (only mobile pushed pieces count)
 * - Basic positional advancement
 * - Piece mobility for pushed pieces
 */
public class BoardEvaluation {

    // Material values
    private static final int PUSHER_MATERIAL_VALUE = 200;
    private static final int PUSHED_MATERIAL_VALUE = 100;

    // Positional scoring
    private static final int ADVANCEMENT_BONUS = 10;  // Points per square advanced
    private static final int PUSHER_BONUS_MULTIPLIER = 2;  // Pushers get double advancement bonus
    private static final int HALF_BOARD_MULTIPLIER = 2;  // Extra multiplier for pushers past halfway

    // Terminal values
    private static final int WIN_VALUE = 1_000_000;
    private static final int LOSS_VALUE = -1_000_000;

    // Board constants
    private static final int EMPTY = 0;
    private static final int BLACK_PUSHED = 1;
    private static final int BLACK_PUSHER = 2;
    private static final int RED_PUSHED = 3;
    private static final int RED_PUSHER = 4;

    /**
     * Main evaluation method
     */
    public static int evaluate(char[][] board, String color) {
        if (board == null || board.length != 8 || board[0].length != 8) {
            throw new IllegalArgumentException("Board must be 8x8");
        }

        String normalizedColor = color.toLowerCase().trim();
        if (!normalizedColor.equals("red") && !normalizedColor.equals("black")) {
            throw new IllegalArgumentException("Color must be 'red' or 'black'");
        }

        boolean evaluatingForRed = normalizedColor.equals("red");
        int[][] internalBoard = convertToInternalFormat(board);

        // Check terminal positions first
        int terminalValue = checkTerminalPositions(internalBoard, evaluatingForRed);
        if (terminalValue != 0) {
            return terminalValue;
        }

        // Perform enhanced evaluation with mobility
        return performEnhancedEvaluation(internalBoard, evaluatingForRed);
    }

    /**
     * Convert character board to internal format
     */
    private static int[][] convertToInternalFormat(char[][] charBoard) {
        int[][] intBoard = new int[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                char piece = charBoard[row][col];
                switch (piece) {
                    case 'R': intBoard[row][col] = RED_PUSHER; break;
                    case 'r': intBoard[row][col] = RED_PUSHED; break;
                    case 'B': intBoard[row][col] = BLACK_PUSHER; break;
                    case 'b': intBoard[row][col] = BLACK_PUSHED; break;
                    default:  intBoard[row][col] = EMPTY; break;
                }
            }
        }
        return intBoard;
    }

    /**
     * Check for terminal positions (win/loss conditions)
     */
    private static int checkTerminalPositions(int[][] board, boolean evaluatingForRed) {
        boolean redReachedGoal = false;
        boolean blackReachedGoal = false;
        int redPushers = 0;
        int blackPushers = 0;

        // Check goal rows
        for (int col = 0; col < 8; col++) {
            int topPiece = board[0][col];    // Red's goal
            int bottomPiece = board[7][col]; // Black's goal

            if (topPiece == RED_PUSHER || topPiece == RED_PUSHED) redReachedGoal = true;
            if (bottomPiece == BLACK_PUSHER || bottomPiece == BLACK_PUSHED) blackReachedGoal = true;
        }

        // Count pushers
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == RED_PUSHER) redPushers++;
                else if (piece == BLACK_PUSHER) blackPushers++;
            }
        }

        // Determine win/loss
        boolean currentPlayerWon = evaluatingForRed ? (redReachedGoal || blackPushers == 0)
                : (blackReachedGoal || redPushers == 0);
        boolean currentPlayerLost = evaluatingForRed ? (blackReachedGoal || redPushers == 0)
                : (redReachedGoal || blackPushers == 0);

        if (currentPlayerWon) return WIN_VALUE;
        if (currentPlayerLost) return LOSS_VALUE;
        return 0;
    }

    /**
     * Enhanced evaluation combining material and positional factors with mobility
     */
    private static int performEnhancedEvaluation(int[][] board, boolean evaluatingForRed) {
        int ourScore = 0;
        int opponentScore = 0;
        int ourPushers = 0, ourMobilePushed = 0;
        int opponentPushers = 0, opponentMobilePushed = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);
                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);
                boolean pieceIsPushed = (piece == RED_PUSHED || piece == BLACK_PUSHED);


                // Skip pieces that haven't moved from their starting positions
                if(isInStartingPosition(row, col, piece)){
                    continue;
                }

                // Count material - only mobile pushed pieces count (and only if they've moved
                if (pieceIsOurs) {
                    if (pieceIsPusher) {
                        ourPushers++;
                    } else if (pieceIsPushed && isPushedPieceMobile(board, row, col, piece)) {
                        ourMobilePushed++;
                    }
                } else {
                    if (pieceIsPusher) {
                        opponentPushers++;
                    } else if (pieceIsPushed && isPushedPieceMobile(board, row, col, piece)) {
                        opponentMobilePushed++;
                    }
                }

                // Calculate positional value - only for mobile pieces that have moved
                if (pieceIsPusher || (pieceIsPushed && isPushedPieceMobile(board, row, col, piece))) {
                    int positionalValue = calculatePositionalValue(row, pieceIsRed, pieceIsPusher);

                    if (pieceIsOurs) {
                        ourScore += positionalValue;
                    } else {
                        opponentScore += positionalValue;
                    }
                }
            }
        }

        // Material balance - only count mobile pieces that have moved
        int materialBalance = PUSHER_MATERIAL_VALUE * (ourPushers - opponentPushers) +
                PUSHED_MATERIAL_VALUE * (ourMobilePushed - opponentMobilePushed);

        // Positional balance
        int positionalBalance = ourScore - opponentScore;

        return materialBalance + positionalBalance;
    }

    /**

     * Check if a piece is still in its starting position

     */

    private static boolean isInStartingPosition(int row, int col, int piece) {

        switch (piece) {

            case RED_PUSHER:

                // Red pushers start on row 7 (rank 1)

                return row == 7;



            case RED_PUSHED:

                // Red pushed pieces start on row 6 (rank 2)

                return row == 6;



            case BLACK_PUSHER:

                // Black pushers start on row 0 (rank 8)

                return row == 0;



            case BLACK_PUSHED:

                // Black pushed pieces start on row 1 (rank 7)

                return row == 1;



            default:

                return false;

        }

    }

    /**
     * Check if a pushed piece is mobile (has legal moves)
     */
    private static boolean isPushedPieceMobile(int[][] board, int row, int col, int pushedPiece) {
        boolean pieceIsRed = (pushedPiece == RED_PUSHED);
        int requiredPusher = pieceIsRed ? RED_PUSHER : BLACK_PUSHER;
        int direction = pieceIsRed ? 1 : -1; // Red pushed pieces need pusher behind them (higher row), Black vice versa

        // Check the three possible pusher positions that could move this pushed piece
        int[] pusherRows = {row + direction, row + direction, row + direction};
        int[] pusherCols = {col - 1, col, col + 1}; // Left diagonal, straight behind, right diagonal

        for (int i = 0; i < 3; i++) {
            int pusherRow = pusherRows[i];
            int pusherCol = pusherCols[i];

            // Check if pusher position is valid and contains the right pusher
            if (isValidPosition(pusherRow, pusherCol) && board[pusherRow][pusherCol] == requiredPusher) {
                // Check if the pushed piece can move in the direction the pusher would push it
                int moveDirection = pieceIsRed ? -1 : 1; // Red moves up (decreasing row), Black moves down
                int newRow = row + moveDirection;
                int newCol = col + (pusherCol - col); // Same direction as pusher relationship

                if (isValidPosition(newRow, newCol)) {
                    int targetPiece = board[newRow][newCol];

                    // Can move to empty square
                    if (targetPiece == EMPTY) {
                        return true;
                    }

                    // Can capture opponent piece on diagonal moves
                    if (pusherCol != col) { // Diagonal move
                        boolean targetIsRed = (targetPiece == RED_PUSHER || targetPiece == RED_PUSHED);
                        if (pieceIsRed != targetIsRed) { // Different colors
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Simple positional value based on advancement toward goal
     */
    private static int calculatePositionalValue(int row, boolean pieceIsRed, boolean pieceIsPusher) {
        // Calculate how many squares advanced toward opponent's goal
        int advancement = pieceIsRed ? (8 - row) : row;

        // Base advancement bonus
        int value = advancement * ADVANCEMENT_BONUS;

        // Pushers get bonus for advancement
        if (pieceIsPusher) {
            value *= PUSHER_BONUS_MULTIPLIER;

            // Additional bonus for pushers that corss the halfway line
            boolean crossedHalfway = pieceIsRed ? (row <=3) : (row >=4);
            if(crossedHalfway){
                value *= HALF_BOARD_MULTIPLIER;
            }
        }

        return value;
    }

    /**
     * Utility methods
     */
    private static boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private static boolean isFriendlyPiece(int piece, boolean ourColorIsRed) {
        if (piece == EMPTY) return false;
        boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
        return pieceIsRed == ourColorIsRed;
    }

    /**
     * Debug method to print mobility analysis
     */
    public static void printMobilityAnalysis(char[][] board) {
        int[][] internalBoard = convertToInternalFormat(board);

        System.out.println("=== MOBILITY ANALYSIS ===");

        // Analyze Red pushed pieces
        System.out.println("Red pushed pieces:");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (internalBoard[row][col] == RED_PUSHED) {
                    boolean mobile = isPushedPieceMobile(internalBoard, row, col, RED_PUSHED);
                    char file = (char)('A' + col);
                    char rank = (char)('8' - row);
                    System.out.println("  " + file + rank + ": " + (mobile ? "MOBILE" : "IMMOBILE"));
                }
            }
        }

        // Analyze Black pushed pieces
        System.out.println("Black pushed pieces:");
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (internalBoard[row][col] == BLACK_PUSHED) {
                    boolean mobile = isPushedPieceMobile(internalBoard, row, col, BLACK_PUSHED);
                    char file = (char)('A' + col);
                    char rank = (char)('8' - row);
                    System.out.println("  " + file + rank + ": " + (mobile ? "MOBILE" : "IMMOBILE"));
                }
            }
        }

        System.out.println("========================");
    }
}