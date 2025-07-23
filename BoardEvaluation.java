/**
 * Simple Board Evaluation for Pushers Game
 *
 * Clean, minimal evaluation focusing on:
 * - Terminal positions (win/loss detection)
 * - Material balance
 * - Basic positional advancement
 * - Simple safety considerations
 */
public class BoardEvaluation {

    // Material values
    private static final int PUSHER_MATERIAL_VALUE = 100;
    private static final int PUSHED_MATERIAL_VALUE = 50;

    // Positional scoring
    private static final int ADVANCEMENT_BONUS = 10;  // Points per square advanced
    private static final int PUSHER_BONUS_MULTIPLIER = 2;  // Pushers get double advancement bonus

    // Terminal values
    private static final int WIN_VALUE = 1_000_000;
    private static final int LOSS_VALUE = -1_000_000;

    // NEW: Forced win detection
    private static final int FORCED_WIN_BONUS = 900_000;  // Huge bonus for forced wins

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

        // NEW: Check for forced wins
        int forcedWinValue = checkForcedWin(internalBoard, evaluatingForRed);
        if (forcedWinValue != 0) {
            return forcedWinValue;
        }

        // Perform basic evaluation
        return performBasicEvaluation(internalBoard, evaluatingForRed);
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
     * Basic evaluation combining material and positional factors
     */
    private static int performBasicEvaluation(int[][] board, boolean evaluatingForRed) {
        int ourScore = 0;
        int opponentScore = 0;
        int ourPushers = 0, ourPushed = 0;
        int opponentPushers = 0, opponentPushed = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);
                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);

                // Count material
                if (pieceIsOurs) {
                    if (pieceIsPusher) ourPushers++;
                    else ourPushed++;
                } else {
                    if (pieceIsPusher) opponentPushers++;
                    else opponentPushed++;
                }

                // Calculate positional value
                int positionalValue = calculatePositionalValue(row, pieceIsRed, pieceIsPusher);

                if (pieceIsOurs) {
                    ourScore += positionalValue;
                } else {
                    opponentScore += positionalValue;
                }
            }
        }

        // Material balance
        int materialBalance = PUSHER_MATERIAL_VALUE * (ourPushers - opponentPushers) +
                PUSHED_MATERIAL_VALUE * (ourPushed - opponentPushed);

        // Positional balance
        int positionalBalance = ourScore - opponentScore;

        return materialBalance + positionalBalance;
    }

    /**
     * Simple positional value based on advancement toward goal
     */
    private static int calculatePositionalValue(int row, boolean pieceIsRed, boolean pieceIsPusher) {
        // Calculate how many squares advanced toward opponent's goal
        int advancement = pieceIsRed ? (7 - row) : row;

        // Base advancement bonus
        int value = advancement * ADVANCEMENT_BONUS;

        // Pushers get bonus for advancement
        if (pieceIsPusher) {
            value *= PUSHER_BONUS_MULTIPLIER;
        }

        return value;
    }

    /**
     * Check for forced wins (unstoppable breakthrough paths)
     */
    private static int checkForcedWin(int[][] board, boolean evaluatingForRed) {
        int goalRow = evaluatingForRed ? 7 : 0;  // Opponent's goal row

        // Check each of our pushers for forced win paths
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);

                if (pieceIsOurs && pieceIsPusher) {
                    int movesToWin = findForcedWinPath(board, row, col, evaluatingForRed);
                    if (movesToWin > 0) {
                        return FORCED_WIN_BONUS - (movesToWin * 1000); // Prefer shorter wins
                    }
                }
            }
        }

        return 0; // No forced win found
    }

    /**
     * Find if this pusher has an unstoppable path to victory
     */
    private static int findForcedWinPath(int[][] board, int startRow, int startCol, boolean isRed) {
        int goalRow = isRed ? 7 : 0;
        int advanceDirection = isRed ? 1 : -1;

        // Try straight path first (most common forced win scenario)
        int movesToWin = checkStraightPath(board, startRow, startCol, goalRow, advanceDirection, isRed);
        if (movesToWin > 0) return movesToWin;

        // Could also try diagonal paths, but straight is most likely for forced wins
        return 0;
    }

    /**
     * Check if pusher can advance straight to goal without being stopped
     */
    private static int checkStraightPath(int[][] board, int startRow, int startCol, int goalRow, int advanceDirection, boolean isRed) {
        int currentRow = startRow;
        int moves = 0;

        while (currentRow != goalRow) {
            int nextRow = currentRow + advanceDirection;
            int nextCol = startCol; // Straight path

            if (!isValidPosition(nextRow, nextCol)) return 0;

            moves++;
            int targetSquare = board[nextRow][nextCol];

            // Check if we can move to this square
            if (targetSquare != EMPTY) {
                // Path is blocked by a piece
                return 0;
            }

            // Check if opponent can capture us on this square
            if (canOpponentCapture(board, nextRow, nextCol, isRed)) {
                return 0; // Opponent can capture us
            }

            // Check if opponent can block our next move (if not at goal yet)
            if (nextRow != goalRow && canOpponentBlock(board, nextRow, nextCol, advanceDirection, isRed)) {
                return 0; // Opponent can block
            }

            currentRow = nextRow;
        }

        return moves; // Found unstoppable path!
    }

    /**
     * Check if opponent can capture piece on given square
     */
    private static boolean canOpponentCapture(int[][] board, int row, int col, boolean ourColorIsRed) {
        int opponentAdvanceDirection = ourColorIsRed ? -1 : 1;

        // Check diagonal attack positions
        for (int deltaCol = -1; deltaCol <= 1; deltaCol += 2) { // Only diagonals
            int attackerRow = row - opponentAdvanceDirection;
            int attackerCol = col + deltaCol;

            if (!isValidPosition(attackerRow, attackerCol)) continue;

            int attacker = board[attackerRow][attackerCol];
            if (attacker == EMPTY) continue;

            boolean attackerIsRed = (attacker == RED_PUSHER || attacker == RED_PUSHED);
            boolean attackerIsPusher = (attacker == RED_PUSHER || attacker == BLACK_PUSHER);

            if (attackerIsRed != ourColorIsRed && attackerIsPusher) {
                return true; // Opponent pusher can capture
            }
        }

        return false;
    }

    /**
     * Utility methods
     */

    /**
     * Check if opponent can block our next advance
     */
    private static boolean canOpponentBlock(int[][] board, int ourRow, int ourCol, int advanceDirection, boolean ourColorIsRed) {
        int nextRow = ourRow + advanceDirection;
        int nextCol = ourCol;

        if (!isValidPosition(nextRow, nextCol)) return false;
        if (board[nextRow][nextCol] != EMPTY) return true; // Already blocked

        // Check if opponent has a piece that can reach the blocking square
        int opponentAdvanceDirection = ourColorIsRed ? -1 : 1;

        // Check squares opponent could advance from
        for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
            int sourceRow = nextRow - opponentAdvanceDirection;
            int sourceCol = nextCol + deltaCol;

            if (!isValidPosition(sourceRow, sourceCol)) continue;

            int piece = board[sourceRow][sourceCol];
            if (piece == EMPTY) continue;

            boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
            boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);

            if (pieceIsRed != ourColorIsRed && pieceIsPusher) {
                // Check if this opponent pusher can legally move to blocking position
                if (Math.abs(deltaCol) <= 1) { // Can move straight or diagonally
                    return true; // Opponent can block
                }
            }
        }

        return false;
    }
    private static boolean isValidPosition(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    private static boolean isFriendlyPiece(int piece, boolean ourColorIsRed) {
        if (piece == EMPTY) return false;
        boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
        return pieceIsRed == ourColorIsRed;
    }
}
