package Snake_and_Ladder;

import java.util.LinkedList;
import java.util.Queue;

/**
 * ============================================================================================================
 *                         SNAKE AND LADDER LLD - UML CLASS DIAGRAM & ARCHITECTURE (SDE-1)
 * ============================================================================================================
 *
 * 1. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------------------+          +------------------------------------+
 *   |          SnakeAndLadder            |          |               Board                |
 *   +------------------------------------+          +------------------------------------+
 *   | - board   : Board                  | *------> | - boardSizeRow : int               |
 *   | - players : Queue<Player>          |          | - boardSizeCol : int               |
 *   | - dice    : Dice                   |          | - cells : Cell[][]                 |
 *   | - winner  : Player                 |          +------------------------------------+
 *   +------------------------------------+          | + getCell(position: int) : Cell    |
 *   | + initializeGame() : void          |          | + getWinningPosition() : int       |
 *   | + startGame() : void               |          | - addSnakesAndLadders() : void     |
 *   +------------------------------------+          +------------------------------------+
 *          |                     o                                    *
 *          | uses                | manages (Queue)                    | contains (NxM)
 *          v                     v                                    v
 *   +--------------+     +---------------+          +------------------------------------+
 *   |     Dice     |     |    Player     |          |               Cell                 |
 *   +--------------+     +---------------+          +------------------------------------+
 *   | - count: int |     | - id : int    |          | - jump : Jump (nullable)           |
 *   +--------------+     | - name : Str  |          +------------------------------------+
 *   | + rollDice() |     | - pos  : int  |                            o
 *   +--------------+     +---------------+                            | optional (0..1)
 *                                                                     v
 *                                                   +------------------------------------+
 *                                                   |               Jump                 |
 *                                                   +------------------------------------+
 *                                                   | - start : int                      |
 *                                                   | - end   : int                      |
 *                                                   +------------------------------------+
 *                                                   | Note:                              |
 *                                                   | • If start > end  => SNAKE 🐍      |
 *                                                   | • If start < end  => LADDER 🪜     |
 *                                                   +------------------------------------+
 *
 *
 * 2. GAME LOOP EXECUTION FLOW (Inside SnakeAndLadder.startGame()):
 * ----------------------------------------------------------------
 *   [START GAME] ---> Initialize 10x10 Board (Cells 0 to 99), 5 Snakes, 5 Ladders, 1 Dice, Queue[Alice, Bob]
 *         |
 *         v
 *   +---> WHILE (winner == null)
 *   |       |
 *   |       +---> 1. Poll current player from Queue: `Player currentPlayer = players.poll()`
 *   |       |
 *   |       +---> 2. Roll Dice: `int diceValue = dice.rollDice()`
 *   |       |        Calculate `int nextPos = currentPlayer.getPosition() + diceValue`
 *   |       |
 *   |       +---> 3. Check Winning Condition (`nextPos >= board.getWinningPosition()`):
 *   |       |          |-- YES --> `winner = currentPlayer` -> Print Winner -> BREAK!
 *   |       |
 *   |       +---> 4. Check Cell for Snake or Ladder (`Cell cell = board.getCell(nextPos)`):
 *   |       |          |-- `cell.getJump() != null`?
 *   |       |               |-- If `jump.end < jump.start` (SNAKE 🐍): Bite down to `jump.end`!
 *   |       |               |-- If `jump.end > jump.start` (LADDER 🪜): Climb up to `jump.end`!
 *   |       |
 *   |       +---> 5. Update Player Position (`currentPlayer.setPosition(finalPos)`)
 *   |       |
 *   |       +---> 6. Re-queue Player (`players.add(currentPlayer)`) for their next turn!
 *   +-------+
 * ============================================================================================================
 */

class Dice {
  private final int diceCount;

  public Dice(int count) {
    this.diceCount = count;
  }

  public int rollDice() {
    int total = 0;
    int diceThrown = 0;
    while (diceThrown < diceCount) {
      total += (int) (Math.random() * 6) + 1;
      diceThrown++;
    }
    return total;
  }
}

class Jump {
  private final int start;
  private final int end;

  public Jump(int start, int end) {
    this.start = start;
    this.end = end;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }
}

class Cell {
  private Jump jump;

  public Cell() {
    this.jump = null;
  }

  public void setJump(Jump jump) {
    this.jump = jump;
  }

  public Jump getJump() {
    return jump;
  }
}

class Board {
  private final int boardSizeRow;
  private final int boardSizeCol;
  private final int numberOfSnakes;
  private final int numberOfLadders;
  private final Cell[][] cells;

  public Board(int boardSizeRow, int boardSizeCol, int numberOfSnakes, int numberOfLadders) {
    this.boardSizeRow = boardSizeRow;
    this.boardSizeCol = boardSizeCol;
    this.numberOfSnakes = numberOfSnakes;
    this.numberOfLadders = numberOfLadders;
    this.cells = new Cell[boardSizeRow][boardSizeCol];

    for (int i = 0; i < boardSizeRow; i++) {
      for (int j = 0; j < boardSizeCol; j++) {
        cells[i][j] = new Cell();
      }
    }

    addSnakesAndLadders();
  }

  public int getWinningPosition() {
    return (boardSizeRow * boardSizeCol) - 1; // e.g., 99 for a 10x10 board (0 to 99)
  }

  public Cell getCell(int position) {
    int row = position / boardSizeCol;
    int col = position % boardSizeCol;
    return cells[row][col];
  }

  private void addSnakesAndLadders() {
    int totalCells = boardSizeRow * boardSizeCol;
    int snakeCount = 0;
    int ladderCount = 0;

    // 1. Add Snakes: start MUST be GREATER than end (pulls player DOWN)
    while (snakeCount < numberOfSnakes) {
      int start = (int) (Math.random() * (totalCells - 2)) + 1; // Avoid cell 0 and cell 99
      int end = (int) (Math.random() * (totalCells - 2)) + 1;
      if (start > end && getCell(start).getJump() == null) {
        getCell(start).setJump(new Jump(start, end));
        snakeCount++;
      }
    }

    // 2. Add Ladders: start MUST be LESS than end (boosts player UP)
    while (ladderCount < numberOfLadders) {
      int start = (int) (Math.random() * (totalCells - 2)) + 1;
      int end = (int) (Math.random() * (totalCells - 2)) + 1;
      if (start < end && getCell(start).getJump() == null) {
        getCell(start).setJump(new Jump(start, end));
        ladderCount++;
      }
    }
  }
}

class Player {
  private final int id;
  private final String name;
  private int position;

  public Player(int id, String name) {
    this.id = id;
    this.name = name;
    this.position = 0;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }
}

public class SnakeAndLadder {
  private Board board;
  private Queue<Player> players;
  private Player winner;
  private Dice dice;

  public SnakeAndLadder() {
    initializeGame();
  }

  public void initializeGame() {
    board = new Board(10, 10, 5, 5);
    dice = new Dice(1);
    Player player1 = new Player(1, "Alice");
    Player player2 = new Player(2, "Bob");

    players = new LinkedList<>();
    players.add(player1);
    players.add(player2);
    winner = null;
  }

  public void startGame() {
    System.out.println("==================================================");
    System.out.println("        SNAKE AND LADDER LLD - GAME START         ");
    System.out.println("==================================================\n");

    int winningPos = board.getWinningPosition();

    while (winner == null) {
      Player currentPlayer = players.poll();
      int diceValue = dice.rollDice();
      int nextPosition = currentPlayer.getPosition() + diceValue;

      System.out.print(
          "[" + currentPlayer.getName() + "] rolled " + diceValue
              + " (from " + currentPlayer.getPosition() + " -> " + Math.min(nextPosition, winningPos) + ")");

      // 1. Check Winning Condition
      if (nextPosition >= winningPos) {
        currentPlayer.setPosition(winningPos);
        winner = currentPlayer;
        System.out.println("\n\n🎉 WINNER IS " + winner.getName().toUpperCase() + " (Reached " + winningPos + ")!");
        break;
      }

      // 2. Check if the cell has a Snake or a Ladder
      Cell cell = board.getCell(nextPosition);
      if (cell.getJump() != null) {
        Jump jump = cell.getJump();
        if (jump.getEnd() < jump.getStart()) {
          System.out.print(" 🐍 SNAKE BITE! Drops from " + jump.getStart() + " to " + jump.getEnd());
        } else {
          System.out.print(" 🪜 LADDER CLIMB! Boosts from " + jump.getStart() + " to " + jump.getEnd());
        }
        nextPosition = jump.getEnd();
      }
      System.out.println();

      // 3. Always update player's position and add back to Queue for next turn
      currentPlayer.setPosition(nextPosition);
      players.add(currentPlayer);
    }
  }

  public static void main(String[] args) {
    SnakeAndLadder game = new SnakeAndLadder();
    game.startGame();
  }
}
