/**
 * Enhanced Strategic Board Evaluation for Pushers Game
 *
 * Implements advanced evaluation techniques including:
 * - Dynamic phase-based evaluation (opening/middlegame/endgame)
 * - Breakthrough threat detection with heavy bonuses
 * - Pusher preservation strategies
 * - Pin/blockade detection to neutralize trapped pieces
 * - Adaptive safety vs aggression balance
 * - Enhanced opponent pressure calculation
 */
public class BoardEvaluation {

    // Material values
    private static final int PUSHER_MATERIAL_VALUE = 100;
    private static final int PUSHED_MATERIAL_VALUE = 50;

    // Positional scoring constants
    private static final int BASE_POSITIONAL_MULTIPLIER = 100;
    private static final int PUSHER_ADVANCEMENT_MULTIPLIER = 3;
    private static final int PUSHED_ADVANCEMENT_MULTIPLIER = 1; // Reduced from 2 to discourage wall-building

    // Mobility weight
    private static final int MOBILITY_WEIGHT_DIVISOR = 4;

    // Enhanced tactical safety penalties - INCREASED for danger prioritization
    private static final int UNDER_ATTACK_BASE_PENALTY = 2000;     // Massive penalty for threatened pieces
    private static final int POOR_PROTECTION_PENALTY = 800;        // Heavy penalty for unprotected pieces
    private static final int ISOLATION_PENALTY = 1000;             // Severe penalty for isolated pieces
    private static final int ADVANCED_EXPOSURE_MULTIPLIER = 500;   // Extreme penalty for losing advanced pieces
    private static final int DANGER_NULLIFICATION_THRESHOLD = 3000; // Threshold above which positional value is nullified

    // Coordination bonuses
    private static final int PUSHER_SUPPORT_BONUS = 150;
    private static final int DEFENSIVE_FORMATION_BONUS = 100;
    private static final int ATTACK_FORMATION_BONUS = 120;

    // Enhanced strategic bonuses
    private static final int BREAKTHROUGH_IMMINENT_BONUS = 3000;
    private static final int BREAKTHROUGH_CLOSE_BONUS = 1200;
    private static final int BREAKTHROUGH_NEAR_BONUS = 500;
    private static final int PUSHER_PRESERVATION_BONUS = 200;
    private static final int ENDGAME_AGGRESSION_MULTIPLIER = 2;
    private static final int CENTRAL_CONTROL_BONUS = 80;
    private static final int OPPONENT_PRESSURE_BONUS = 100;

    // NEW: Tactical trap penalties
    private static final int TACTICAL_TRAP_PENALTY = 1500;     // Heavy penalty for pieces that can be easily neutralized
    private static final int FUTURE_PIN_PENALTY = 1000;       // Penalty for positions opponent can pin next move
    private static final int EXPOSED_ADVANCE_MULTIPLIER = 2;   // Extra penalty for advanced trapped pieces
    private static final int PIN_BASE_PENALTY = 2000;   // Extra penalty for advanced trapped pieces
    private static final int BLOCKADE_PENALTY = 2500;   // Extra penalty for advanced trapped pieces

    // Terminal values
    private static final int WIN_VALUE = 1_000_000;
    private static final int LOSS_VALUE = -1_000_000;

    // Board constants
    private static final int EMPTY = 0;
    private static final int BLACK_PUSHED = 1;
    private static final int BLACK_PUSHER = 2;
    private static final int RED_PUSHED = 3;
    private static final int RED_PUSHER = 4;

    // Game phase thresholds
    private static final int ENDGAME_PIECE_THRESHOLD = 16;
    private static final int OPENING_PIECE_THRESHOLD = 28;

    /**
     * Enhanced evaluation with dynamic phase adaptation
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

        // Check for forced wins (mate-in-N detection)
        int forcedWinValue = checkForcedWin(internalBoard, evaluatingForRed);
        if (forcedWinValue != 0) {
            return forcedWinValue;
        }

        // Determine game phase
        GamePhase phase = determineGamePhase(internalBoard);

        // Perform enhanced evaluation
        return performEnhancedEvaluation(internalBoard, evaluatingForRed, phase);
    }

    /**
     * Game phase enumeration for dynamic evaluation
     */
    private enum GamePhase {
        OPENING, MIDDLEGAME, ENDGAME
    }

    /**
     * Determine current game phase based on piece count and advancement
     */
    private static GamePhase determineGamePhase(int[][] board) {
        int totalPieces = 0;
        int advancedPieces = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (board[row][col] != EMPTY) {
                    totalPieces++;
                    if (row >= 2 && row <= 5) {
                        advancedPieces++;
                    }
                }
            }
        }

        if (totalPieces <= ENDGAME_PIECE_THRESHOLD || advancedPieces >= totalPieces * 0.6) {
            return GamePhase.ENDGAME;
        } else if (totalPieces >= OPENING_PIECE_THRESHOLD) {
            return GamePhase.OPENING;
        }
        return GamePhase.MIDDLEGAME;
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
     * Check for forced wins (mate-in-N detection)
     */
    private static int checkForcedWin(int[][] board, boolean evaluatingForRed) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);

                if (pieceIsOurs && pieceIsPusher) {
                    int movesToWin = findUnstoppableBreakthrough(board, row, col, evaluatingForRed);
                    if (movesToWin > 0 && movesToWin <= 6) {
                        return WIN_VALUE - (movesToWin * 1000);
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Find unstoppable breakthrough path for a specific Pusher
     */
    private static int findUnstoppableBreakthrough(int[][] board, int startRow, int startCol, boolean isRed) {
        int goalRow = isRed ? 0 : 7;
        int advanceDirection = isRed ? -1 : 1;

        int straightPath = checkBreakthroughPath(board, startRow, startCol, 0, advanceDirection, isRed, goalRow);
        int leftDiagPath = checkBreakthroughPath(board, startRow, startCol, -1, advanceDirection, isRed, goalRow);
        int rightDiagPath = checkBreakthroughPath(board, startRow, startCol, 1, advanceDirection, isRed, goalRow);

        int shortestPath = Integer.MAX_VALUE;
        if (straightPath > 0) shortestPath = Math.min(shortestPath, straightPath);
        if (leftDiagPath > 0) shortestPath = Math.min(shortestPath, leftDiagPath);
        if (rightDiagPath > 0) shortestPath = Math.min(shortestPath, rightDiagPath);

        return shortestPath == Integer.MAX_VALUE ? 0 : shortestPath;
    }

    /**
     * Check if a specific breakthrough path is viable
     */
    private static int checkBreakthroughPath(int[][] board, int startRow, int startCol,
                                             int colDirection, int rowDirection, boolean isRed, int goalRow) {
        int currentRow = startRow;
        int currentCol = startCol;
        int moves = 0;

        while (currentRow != goalRow && moves < 8) {
            int nextRow = currentRow + rowDirection;
            int nextCol = currentCol + colDirection;

            if (!isValidPosition(nextRow, nextCol)) {
                return 0;
            }

            moves++;
            int targetSquare = board[nextRow][nextCol];

            if (targetSquare != EMPTY) {
                if (colDirection == 0) {
                    return 0;
                }

                boolean targetIsRed = (targetSquare == RED_PUSHER || targetSquare == RED_PUSHED);
                if (targetIsRed == isRed) {
                    return 0;
                }
            }

            if (canOpponentCapture(board, nextRow, nextCol, isRed)) {
                return 0;
            }

            if (moves < 7 && canOpponentBlock(board, nextRow, nextCol, rowDirection, colDirection, isRed)) {
                return 0;
            }

            currentRow = nextRow;
            currentCol = nextCol;
        }

        return currentRow == goalRow ? moves : 0;
    }

    /**
     * Check if opponent can capture piece on given square
     */
    private static boolean canOpponentCapture(int[][] board, int row, int col, boolean ourColorIsRed) {
        int opponentAdvanceDirection = ourColorIsRed ? 1 : -1;
        int attackFromRow = row - opponentAdvanceDirection;

        if (isValidPosition(attackFromRow, 0)) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol += 2) {
                int attackFromCol = col + deltaCol;

                if (isValidPosition(attackFromRow, attackFromCol)) {
                    int attacker = board[attackFromRow][attackFromCol];
                    boolean attackerIsPusher = (attacker == RED_PUSHER || attacker == BLACK_PUSHER);
                    boolean attackerIsOpponent = !isFriendlyPiece(attacker, ourColorIsRed);

                    if (attackerIsPusher && attackerIsOpponent && attacker != EMPTY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check if opponent can block our advance
     */
    private static boolean canOpponentBlock(int[][] board, int ourRow, int ourCol,
                                            int ourRowDir, int ourColDir, boolean ourColorIsRed) {
        int nextRow = ourRow + ourRowDir;
        int nextCol = ourCol + ourColDir;

        if (!isValidPosition(nextRow, nextCol)) return false;
        if (board[nextRow][nextCol] != EMPTY) return false;

        int opponentAdvanceDirection = ourColorIsRed ? 1 : -1;
        int[] checkRows = {nextRow - opponentAdvanceDirection};
        int[] checkCols = {nextCol - 1, nextCol, nextCol + 1};

        for (int checkRow : checkRows) {
            for (int checkCol : checkCols) {
                if (isValidPosition(checkRow, checkCol)) {
                    int piece = board[checkRow][checkCol];
                    if (piece != EMPTY && !isFriendlyPiece(piece, ourColorIsRed)) {
                        boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);
                        if (pieceIsPusher) {
                            int colDiff = nextCol - checkCol;
                            if (Math.abs(colDiff) <= 1) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Check for terminal positions
     */
    private static int checkTerminalPositions(int[][] board, boolean evaluatingForRed) {
        boolean redReachedGoal = false;
        boolean blackReachedGoal = false;
        int redPushers = 0;
        int blackPushers = 0;

        for (int col = 0; col < 8; col++) {
            int topPiece = board[0][col];
            int bottomPiece = board[7][col];

            if (topPiece == RED_PUSHER || topPiece == RED_PUSHED) redReachedGoal = true;
            if (bottomPiece == BLACK_PUSHER || bottomPiece == BLACK_PUSHED) blackReachedGoal = true;
        }

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == RED_PUSHER) redPushers++;
                else if (piece == BLACK_PUSHER) blackPushers++;
            }
        }

        boolean currentPlayerWon = evaluatingForRed ? (redReachedGoal || blackPushers == 0)
                : (blackReachedGoal || redPushers == 0);
        boolean currentPlayerLost = evaluatingForRed ? (blackReachedGoal || redPushers == 0)
                : (redReachedGoal || blackPushers == 0);

        if (currentPlayerWon) return WIN_VALUE;
        if (currentPlayerLost) return LOSS_VALUE;
        return 0;
    }

    /**
     * Enhanced evaluation with phase-aware scoring
     */
    private static int performEnhancedEvaluation(int[][] board, boolean evaluatingForRed, GamePhase phase) {
        int ourTotalScore = 0;
        int opponentTotalScore = 0;
        int ourPushers = 0, ourPushed = 0;
        int opponentPushers = 0, opponentPushed = 0;

        double advancementMultiplier = getAdvancementMultiplier(phase);
        double safetyMultiplier = getSafetyMultiplier(phase);

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);
                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);

                if (pieceIsOurs) {
                    if (pieceIsPusher) ourPushers++;
                    else ourPushed++;
                } else {
                    if (pieceIsPusher) opponentPushers++;
                    else opponentPushed++;
                }

                int pieceValue = evaluateEnhancedPieceValue(board, row, col, pieceIsRed,
                        pieceIsPusher, phase, advancementMultiplier, safetyMultiplier);

                if (pieceIsOurs) {
                    ourTotalScore += pieceValue;
                } else {
                    opponentTotalScore += pieceValue;
                }
            }
        }

        int materialBalance = PUSHER_MATERIAL_VALUE * (ourPushers - opponentPushers) +
                PUSHED_MATERIAL_VALUE * (ourPushed - opponentPushed);

        int strategicBonuses = calculateStrategicBonuses(board, evaluatingForRed, phase,
                ourPushers, opponentPushers);

        return materialBalance + (ourTotalScore - opponentTotalScore) + strategicBonuses;
    }

    /**
     * Get advancement multiplier based on game phase
     */
    private static double getAdvancementMultiplier(GamePhase phase) {
        switch (phase) {
            case OPENING: return 0.8;
            case MIDDLEGAME: return 1.0;
            case ENDGAME: return 1.5;
            default: return 1.0;
        }
    }

    /**
     * Get safety multiplier based on game phase
     */
    private static double getSafetyMultiplier(GamePhase phase) {
        switch (phase) {
            case OPENING: return 1.2;
            case MIDDLEGAME: return 1.0;
            case ENDGAME: return 0.7;
            default: return 1.0;
        }
    }

    /**
     * Enhanced piece evaluation with phase awareness and pin detection
     */
    private static int evaluateEnhancedPieceValue(int[][] board, int row, int col,
                                                  boolean pieceIsRed, boolean pieceIsPusher,
                                                  GamePhase phase, double advancementMultiplier,
                                                  double safetyMultiplier) {
        int totalValue = 0;

        // 1. Enhanced positional value with breakthrough detection
        int positionalValue = calculateEnhancedPositionalValue(row, pieceIsRed, pieceIsPusher, advancementMultiplier);
        totalValue += positionalValue;

        // 2. NEW: Tactical trap penalty - detects if opponent can easily neutralize this piece
        int tacticalTrapPenalty = calculateTacticalTrapPenalty(board, row, col, pieceIsRed, pieceIsPusher);
        totalValue -= tacticalTrapPenalty;

        // 3. Pin/blockade penalty - heavily reduces value of trapped pieces
        int pinPenalty = calculatePinPenalty(board, row, col, pieceIsRed, pieceIsPusher);
        totalValue -= pinPenalty;

        // 4. Phase-adjusted safety deductions
        //int safetyDeductions = (int)(calculateSafetyDeductions(board, row, col, pieceIsRed) * safetyMultiplier);
        //totalValue -= safetyDeductions;

        // 4. Enhanced coordination bonuses
        int coordinationBonuses = calculateEnhancedCoordinationBonuses(board, row, col, pieceIsRed, pieceIsPusher, phase);
        totalValue += coordinationBonuses;

        // 5. Mobility bonus (reduced influence)
        int mobilityBonus = calculateReducedMobilityBonus(board, row, col, pieceIsRed, pieceIsPusher);
        totalValue += mobilityBonus;

        // 6. Central control bonus
        totalValue += calculateCentralControlBonus(row, col, phase);

        return totalValue;
    }

    /**
     * NEW: Calculate penalty for pieces vulnerable to tactical traps
     * Detects positions where opponent can easily neutralize advanced pieces
     */
    private static int calculateTacticalTrapPenalty(int[][] board, int row, int col, boolean pieceIsRed, boolean pieceIsPusher) {
        if (!pieceIsPusher) return 0; // Only Pushers can create meaningful threats worth trapping

        int advancementLevel = pieceIsRed ? (7 - row) : row;
        if (advancementLevel < 2) return 0; // Only penalize advanced pieces

        int penalty = 0;
        int advanceDirection = pieceIsRed ? -1 : 1;

        // Check if opponent can easily block/pin this piece with simple moves
        if (canOpponentTrapNextMove(board, row, col, pieceIsRed)) {
            // Base tactical trap penalty
            penalty += TACTICAL_TRAP_PENALTY;

            // Extra penalty for very advanced trapped pieces (wasted tempo)
            if (advancementLevel >= 3) {
                penalty += advancementLevel * advancementLevel * EXPOSED_ADVANCE_MULTIPLIER;
            }
        }

        // Check for future pin vulnerability - if we advance, can opponent pin us?
        if (isVulnerableToFuturePin(board, row, col, pieceIsRed)) {
            penalty += FUTURE_PIN_PENALTY;
        }

        return penalty;
    }

    /**
     * Check if opponent can trap this piece with a simple next move
     */
    private static boolean canOpponentTrapNextMove(int[][] board, int row, int col, boolean pieceIsRed) {
        int opponentAdvanceDirection = pieceIsRed ? 1 : -1;

        // Look for opponent pieces that can easily block our advancement paths
        // Check positions where opponent could move to block us
        int[] blockingRows = {row + (pieceIsRed ? -1 : 1)}; // One square in front of us
        int[] blockingCols = {col - 1, col, col + 1}; // All three forward positions

        for (int blockRow : blockingRows) {
            for (int blockCol : blockingCols) {
                if (!isValidPosition(blockRow, blockCol)) continue;
                if (board[blockRow][blockCol] != EMPTY) continue; // Already occupied

                // Check if opponent has a piece that can easily reach this blocking position
                if (opponentCanReachSquare(board, blockRow, blockCol, !pieceIsRed)) {
                    return true; // Opponent can easily trap us
                }
            }
        }

        return false;
    }

    /**
     * Check if opponent can reach a specific square to block/pin
     */
    private static boolean opponentCanReachSquare(int[][] board, int targetRow, int targetCol, boolean opponentIsRed) {
        int opponentAdvanceDirection = opponentIsRed ? -1 : 1;

        // Check adjacent squares where opponent pieces could come from
        int[] sourceRows = {targetRow - opponentAdvanceDirection, targetRow, targetRow + opponentAdvanceDirection};
        int[] sourceCols = {targetCol - 1, targetCol, targetCol + 1};

        for (int sourceRow : sourceRows) {
            for (int sourceCol : sourceCols) {
                if (!isValidPosition(sourceRow, sourceCol)) continue;
                if (sourceRow == targetRow && sourceCol == targetCol) continue;

                int piece = board[sourceRow][sourceCol];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                if (pieceIsRed != opponentIsRed) continue; // Not opponent's piece

                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);

                // Check if this piece can legally reach the target square
                if (canPieceReachSquare(sourceRow, sourceCol, targetRow, targetCol, pieceIsPusher, opponentIsRed)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if piece can legally move from source to target
     */
    private static boolean canPieceReachSquare(int fromRow, int fromCol, int toRow, int toCol,
                                               boolean isPusher, boolean isRed) {
        if (!isPusher) return false; // Simplified - only check Pusher moves for efficiency

        int advanceDirection = isRed ? -1 : 1;
        int rowDiff = toRow - fromRow;
        int colDiff = Math.abs(toCol - fromCol);

        // Pusher can move one square forward (straight or diagonal)
        return rowDiff == advanceDirection && colDiff <= 1;
    }

    /**
     * Check if this piece is vulnerable to future pinning
     */
    private static boolean isVulnerableToFuturePin(int[][] board, int row, int col, boolean pieceIsRed) {
        int advanceDirection = pieceIsRed ? -1 : 1;

        // If we advance one square forward, check if opponent can pin us
        int futureRow = row + advanceDirection;
        int futureCol = col;

        if (!isValidPosition(futureRow, futureCol)) return false;
        if (board[futureRow][futureCol] != EMPTY) return false; // Can't advance

        // Check if opponent can block our future paths from that advanced position
        return canOpponentTrapNextMove(board, futureRow, futureCol, pieceIsRed);
    }

    /**
     * Calculate penalty for pinned/blocked pieces (existing function, kept for current pins)
     */
    private static int calculatePinPenalty(int[][] board, int row, int col, boolean pieceIsRed, boolean pieceIsPusher) {
        if (!pieceIsPusher) return 0; // Only Pushers can be meaningfully pinned

        int penalty = 0;
        int advancementLevel = pieceIsRed ? (7 - row) : row;
        int advanceDirection = pieceIsRed ? -1 : 1;

        // Check if straight advancement is blocked
        boolean straightBlocked = isAdvancementBlocked(board, row, col, 0, advanceDirection, pieceIsRed);

        // Check if diagonal movements are blocked/defended
        boolean leftDiagBlocked = isAdvancementBlocked(board, row, col, -1, advanceDirection, pieceIsRed);
        boolean rightDiagBlocked = isAdvancementBlocked(board, row, col, 1, advanceDirection, pieceIsRed);

        // Calculate pin severity
        int blockedPaths = 0;
        if (straightBlocked) blockedPaths++;
        if (leftDiagBlocked) blockedPaths++;
        if (rightDiagBlocked) blockedPaths++;

        if (blockedPaths >= 2) {
            // Severely pinned - most/all paths blocked
            penalty = PIN_BASE_PENALTY + (advancementLevel * advancementLevel * TACTICAL_TRAP_PENALTY);
        } else if (blockedPaths == 1 && straightBlocked) {
            // Key advancement path blocked
            penalty = BLOCKADE_PENALTY + (advancementLevel * advancementLevel);
        }

        return penalty;
    }

    /**
     * Check if advancement in a specific direction is blocked by enemy pieces
     */
    private static boolean isAdvancementBlocked(int[][] board, int row, int col,
                                                int colDirection, int rowDirection, boolean pieceIsRed) {
        int targetRow = row + rowDirection;
        int targetCol = col + colDirection;

        if (!isValidPosition(targetRow, targetCol)) return true; // Off-board = blocked

        int targetSquare = board[targetRow][targetCol];

        // Empty square - check if opponent can defend it
        if (targetSquare == EMPTY) {
            return isSquareDefended(board, targetRow, targetCol, !pieceIsRed);
        }

        // Occupied square
        boolean targetIsRed = (targetSquare == RED_PUSHER || targetSquare == RED_PUSHED);
        if (targetIsRed == pieceIsRed) {
            return true; // Blocked by friendly piece
        }

        // Enemy piece - can capture only diagonally
        if (colDirection == 0) {
            return true; // Can't capture straight ahead
        }

        // Can capture diagonally, but check if it's defended
        return isSquareDefended(board, targetRow, targetCol, !pieceIsRed);
    }

    /**
     * Check if a square is defended by the specified color
     */
    private static boolean isSquareDefended(int[][] board, int row, int col, boolean defenderIsRed) {
        int defenderAdvanceDirection = defenderIsRed ? -1 : 1;
        int attackFromRow = row - defenderAdvanceDirection;

        if (isValidPosition(attackFromRow, 0)) {
            // Check diagonal defense positions
            for (int deltaCol = -1; deltaCol <= 1; deltaCol += 2) {
                int defenderCol = col + deltaCol;

                if (isValidPosition(attackFromRow, defenderCol)) {
                    int defender = board[attackFromRow][defenderCol];
                    boolean defenderIsPusher = (defender == RED_PUSHER || defender == BLACK_PUSHER);
                    boolean defenderColorMatch = isFriendlyPiece(defender, defenderIsRed);

                    if (defenderIsPusher && defenderColorMatch && defender != EMPTY) {
                        return true; // Square is defended
                    }
                }
            }
        }

        return false;
    }

    /**
     * Enhanced positional value with breakthrough threat detection
     */
    private static int calculateEnhancedPositionalValue(int row, boolean pieceIsRed,
                                                        boolean pieceIsPusher, double multiplier) {
        int advancementLevel = pieceIsRed ? (7 - row) : row;

        int baseValue = (int)(BASE_POSITIONAL_MULTIPLIER * advancementLevel * advancementLevel * multiplier);
        int pieceTypeMultiplier = pieceIsPusher ? PUSHER_ADVANCEMENT_MULTIPLIER : PUSHED_ADVANCEMENT_MULTIPLIER;
        int positionalValue = baseValue * pieceTypeMultiplier;

        // Breakthrough threat bonuses
        if (advancementLevel >= 6) {
            positionalValue += BREAKTHROUGH_IMMINENT_BONUS;
        } else if (advancementLevel >= 5) {
            positionalValue += BREAKTHROUGH_CLOSE_BONUS;
        } else if (advancementLevel >= 4) {
            positionalValue += BREAKTHROUGH_NEAR_BONUS;
        }

        return positionalValue;
    }

    /**
     * Calculate strategic bonuses
     */
    private static int calculateStrategicBonuses(int[][] board, boolean evaluatingForRed,
                                                 GamePhase phase, int ourPushers, int opponentPushers) {
        int bonuses = 0;

        if (ourPushers > opponentPushers) {
            bonuses += (ourPushers - opponentPushers) * PUSHER_PRESERVATION_BONUS;
        }

        bonuses += calculateOpponentPressureBonus(board, evaluatingForRed);

        if (phase == GamePhase.ENDGAME) {
            bonuses += calculateEndgameSpecificBonuses(board, evaluatingForRed);
        }

        return bonuses;
    }

    /**
     * Calculate pressure on opponent pieces
     */
    private static int calculateOpponentPressureBonus(int[][] board, boolean evaluatingForRed) {
        int pressureBonus = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);
                boolean pieceIsPusher = (piece == RED_PUSHER || piece == BLACK_PUSHER);

                if (pieceIsOurs && pieceIsPusher) {
                    int threatenedEnemies = countThreatenedEnemies(board, row, col, evaluatingForRed);
                    pressureBonus += threatenedEnemies * OPPONENT_PRESSURE_BONUS;
                }
            }
        }

        return pressureBonus;
    }

    /**
     * Count enemy pieces threatened by this Pusher
     */
    private static int countThreatenedEnemies(int[][] board, int row, int col, boolean ourColorIsRed) {
        int threats = 0;
        int advanceDirection = ourColorIsRed ? -1 : 1;
        int targetRow = row + advanceDirection;

        if (isValidPosition(targetRow, 0)) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol += 2) {
                int targetCol = col + deltaCol;
                if (isValidPosition(targetRow, targetCol)) {
                    int targetPiece = board[targetRow][targetCol];
                    if (targetPiece != EMPTY && !isFriendlyPiece(targetPiece, ourColorIsRed)) {
                        threats++;
                    }
                }
            }
        }

        return threats;
    }

    /**
     * Calculate endgame-specific bonuses
     */
    private static int calculateEndgameSpecificBonuses(int[][] board, boolean evaluatingForRed) {
        int bonuses = 0;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int piece = board[row][col];
                if (piece == EMPTY) continue;

                boolean pieceIsRed = (piece == RED_PUSHER || piece == RED_PUSHED);
                boolean pieceIsOurs = (pieceIsRed == evaluatingForRed);

                if (pieceIsOurs) {
                    int advancementLevel = pieceIsRed ? (7 - row) : row;
                    if (advancementLevel >= 4) {
                        bonuses += advancementLevel * 50;
                    }
                }
            }
        }

        return bonuses;
    }

    /**
     * Calculate central control bonus
     */
    private static int calculateCentralControlBonus(int row, int col, GamePhase phase) {
        boolean isCentral = (row >= 2 && row <= 5) && (col >= 2 && col <= 5);
        boolean isInnerCenter = (row >= 3 && row <= 4) && (col >= 3 && col <= 4);

        if (phase == GamePhase.ENDGAME) return 0;

        if (isInnerCenter) return CENTRAL_CONTROL_BONUS;
        if (isCentral) return CENTRAL_CONTROL_BONUS / 2;

        return 0;
    }

    /**
     * Enhanced coordination bonuses with phase awareness
     */
    private static int calculateEnhancedCoordinationBonuses(int[][] board, int row, int col,
                                                            boolean pieceIsRed, boolean pieceIsPusher,
                                                            GamePhase phase) {
        int totalBonuses = 0;

        if (pieceIsPusher) {
            totalBonuses += calculatePusherSupportBonus(board, row, col, pieceIsRed);
        }

        totalBonuses += calculateDefensiveFormationBonus(board, row, col, pieceIsRed);
        totalBonuses += calculateAttackFormationBonus(board, row, col, pieceIsRed);

        if (phase == GamePhase.ENDGAME) {
            totalBonuses = (int)(totalBonuses * 0.8);
        }

        return totalBonuses;
    }

    // Remaining methods (mobility, safety, coordination) unchanged from previous version
    private static int calculateReducedMobilityBonus(int[][] board, int row, int col,
                                                     boolean pieceIsRed, boolean pieceIsPusher) {
        int fullMobilityScore = calculateFullMobilityScore(board, row, col, pieceIsRed, pieceIsPusher);
        return fullMobilityScore / MOBILITY_WEIGHT_DIVISOR;
    }

    private static int calculateFullMobilityScore(int[][] board, int row, int col,
                                                  boolean pieceIsRed, boolean pieceIsPusher) {
        int mobilityScore = 0;
        int advanceDirection = pieceIsRed ? -1 : 1;

        if (pieceIsPusher) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                int targetRow = row + advanceDirection;
                int targetCol = col + deltaCol;

                if (isValidPosition(targetRow, targetCol)) {
                    int target = board[targetRow][targetCol];

                    if (target == EMPTY) {
                        mobilityScore += (deltaCol == 0) ? 12 : 10;
                    } else if (!isFriendlyPiece(target, pieceIsRed) && Math.abs(deltaCol) == 1) {
                        boolean targetIsPusher = (target == RED_PUSHER || target == BLACK_PUSHER);
                        mobilityScore += targetIsPusher ? 18 : 12;
                    }
                }
            }
        } else {
            mobilityScore += calculatePushedMobility(board, row, col, pieceIsRed);
        }

        return mobilityScore;
    }

    private static int calculatePushedMobility(int[][] board, int row, int col, boolean pieceIsRed) {
        int mobilityScore = 0;
        int retreatDirection = pieceIsRed ? 1 : -1;
        int supportRow = row + retreatDirection;

        if (isValidPosition(supportRow, 0)) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                int supportCol = col + deltaCol;

                if (isValidPosition(supportRow, supportCol)) {
                    int supportPiece = board[supportRow][supportCol];
                    boolean supportIsPusher = (supportPiece == RED_PUSHER || supportPiece == BLACK_PUSHER);
                    boolean supportIsFriendly = isFriendlyPiece(supportPiece, pieceIsRed);

                    if (supportIsPusher && supportIsFriendly) {
                        int targetRow = row - retreatDirection;
                        int targetCol = col - deltaCol;

                        if (isValidPosition(targetRow, targetCol)) {
                            int target = board[targetRow][targetCol];
                            if (target == EMPTY) {
                                mobilityScore += 8;
                            } else if (!isFriendlyPiece(target, pieceIsRed)) {
                                mobilityScore += 12;
                            }
                        }
                    }
                }
            }
        }

        return mobilityScore;
    }

    /**
     * ENHANCED safety deductions with heavy penalties for threatened pieces
     */
    private static int calculateEnhancedSafetyDeductions(int[][] board, int row, int col, boolean pieceIsRed) {
        int totalDeductions = 0;
        int advancementLevel = pieceIsRed ? (7 - row) : row;

        // MASSIVE penalty for pieces under direct attack
        if (isUnderSimpleAttack(board, row, col, pieceIsRed)) {
            // Base penalty scales dramatically with advancement
            int attackPenalty = UNDER_ATTACK_BASE_PENALTY * (1 + advancementLevel);
            // Exponential penalty for losing advanced pieces
            attackPenalty += advancementLevel * advancementLevel * ADVANCED_EXPOSURE_MULTIPLIER;
            totalDeductions += attackPenalty;

            // If piece is very advanced and under attack, it's almost worthless
            if (advancementLevel >= 4) {
                totalDeductions += 2000; // Additional severe penalty
            }
        }

        // Heavy penalty for poorly protected pieces
        int protectionScore = analyzeLocalProtection(board, row, col, pieceIsRed);
        if (protectionScore < 2) {
            // Scale protection penalty with advancement - advanced unprotected pieces are very dangerous
            int protectionPenalty = (2 - protectionScore) * POOR_PROTECTION_PENALTY * (1 + advancementLevel);
            totalDeductions += protectionPenalty;
        }

        // Severe penalty for isolated pieces (especially advanced ones)
        if (!hasDefensiveSupport(board, row, col, pieceIsRed)) {
            int isolationPenalty = ISOLATION_PENALTY * (1 + advancementLevel);
            totalDeductions += isolationPenalty;
        }

        // Extra penalty for pieces in "no man's land" (advanced but unsupported)
        if (advancementLevel >= 3 && protectionScore == 0) {
            totalDeductions += 1500; // Severe penalty for advanced pieces with zero protection
        }

        return totalDeductions;
    }

    private static int calculatePusherSupportBonus(int[][] board, int row, int col, boolean pieceIsRed) {
        int bonus = 0;
        int advanceDirection = pieceIsRed ? -1 : 1;
        int frontRow = row + advanceDirection;

        if (isValidPosition(frontRow, 0)) {
            int supportedPieces = 0;

            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                int frontCol = col + deltaCol;

                if (isValidPosition(frontRow, frontCol)) {
                    int frontPiece = board[frontRow][frontCol];
                    boolean frontIsPushed = (frontPiece == RED_PUSHED || frontPiece == BLACK_PUSHED);
                    boolean frontIsFriendly = isFriendlyPiece(frontPiece, pieceIsRed);

                    if (frontIsPushed && frontIsFriendly) {
                        supportedPieces++;
                        bonus += PUSHER_SUPPORT_BONUS;

                        int frontAdvancement = pieceIsRed ? (7 - frontRow) : frontRow;
                        bonus += frontAdvancement * 20;
                    }
                }
            }

            if (supportedPieces > 1) {
                bonus += supportedPieces * 50;
            }
        }

        return bonus;
    }

    private static int calculateDefensiveFormationBonus(int[][] board, int row, int col, boolean pieceIsRed) {
        int bonus = 0;
        int advanceDirection = pieceIsRed ? -1 : 1;
        int frontRow = row + advanceDirection;

        if (isValidPosition(frontRow, 0)) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
                int frontCol = col + deltaCol;

                if (isValidPosition(frontRow, frontCol)) {
                    int frontPiece = board[frontRow][frontCol];
                    boolean frontIsPushed = (frontPiece == RED_PUSHED || frontPiece == BLACK_PUSHED);
                    boolean frontIsFriendly = isFriendlyPiece(frontPiece, pieceIsRed);

                    if (frontIsPushed && frontIsFriendly) {
                        bonus += DEFENSIVE_FORMATION_BONUS;
                    }
                }
            }
        }

        return bonus;
    }

    private static int calculateAttackFormationBonus(int[][] board, int row, int col, boolean pieceIsRed) {
        int bonus = 0;
        int advanceDirection = pieceIsRed ? -1 : 1;
        int attackRow = row + advanceDirection;

        if (isValidPosition(attackRow, 0)) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol += 2) {
                int attackCol = col + deltaCol;

                if (isValidPosition(attackRow, attackCol)) {
                    int targetPiece = board[attackRow][attackCol];

                    if (targetPiece != EMPTY && !isFriendlyPiece(targetPiece, pieceIsRed)) {
                        boolean targetIsPusher = (targetPiece == RED_PUSHER || targetPiece == BLACK_PUSHER);
                        bonus += ATTACK_FORMATION_BONUS + (targetIsPusher ? 50 : 0);
                    }
                }
            }
        }

        return bonus;
    }

    private static int analyzeLocalProtection(int[][] board, int row, int col, boolean pieceIsRed) {
        int protectionScore = 0;
        int[] deltaRow = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] deltaCol = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int checkRow = row + deltaRow[i];
            int checkCol = col + deltaCol[i];

            if (isValidPosition(checkRow, checkCol)) {
                int neighborPiece = board[checkRow][checkCol];

                if (isFriendlyPiece(neighborPiece, pieceIsRed)) {
                    boolean neighborIsPusher = (neighborPiece == RED_PUSHER || neighborPiece == BLACK_PUSHER);
                    protectionScore += neighborIsPusher ? 2 : 1;
                }
            }
        }

        return protectionScore;
    }

    private static boolean hasDefensiveSupport(int[][] board, int row, int col, boolean pieceIsRed) {
        int retreatDirection = pieceIsRed ? 1 : -1;

        for (int deltaCol = -1; deltaCol <= 1; deltaCol++) {
            int supportRow = row + retreatDirection;
            int supportCol = col + deltaCol;

            if (isValidPosition(supportRow, supportCol)) {
                int supportPiece = board[supportRow][supportCol];

                if (isFriendlyPiece(supportPiece, pieceIsRed)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isUnderSimpleAttack(int[][] board, int row, int col, boolean pieceIsRed) {
        int opponentAdvanceDirection = pieceIsRed ? 1 : -1;
        int attackFromRow = row - opponentAdvanceDirection;

        if (isValidPosition(attackFromRow, 0)) {
            for (int deltaCol = -1; deltaCol <= 1; deltaCol += 2) {
                int attackFromCol = col + deltaCol;

                if (isValidPosition(attackFromRow, attackFromCol)) {
                    int attacker = board[attackFromRow][attackFromCol];
                    boolean attackerIsPusher = (attacker == RED_PUSHER || attacker == BLACK_PUSHER);
                    boolean attackerIsFriendly = isFriendlyPiece(attacker, pieceIsRed);

                    if (attackerIsPusher && !attackerIsFriendly) {
                        return true;
                    }
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