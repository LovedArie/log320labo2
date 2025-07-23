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
        int advancement = pieceIsRed ? row : (7 - row);

        // Base advancement bonus
        int value = advancement * ADVANCEMENT_BONUS;

        // Pushers get bonus for advancement
        if (pieceIsPusher) {
            value *= PUSHER_BONUS_MULTIPLIER;
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
}