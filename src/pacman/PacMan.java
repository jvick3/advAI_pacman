package pacman;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.Animation;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * PacMan.fx created on 2009-1-1, 11:50:58 <br>
 * PacMan.java created October 2011
 *
 * @see <a href="http://www.javafxgame.com">http://www.javafxgame.com</a>
 * @author Henry Zhang
 * @author Patrick Webster
 */
public class PacMan extends MovingObject {


 private int timestep = 0;   
    
 /**
  * The number of dots eaten.
  */
  public int dotEatenCount;

 /**
  * Score of the game.
  */
  public SimpleIntegerProperty score;

 /**
  * Angles of rotating the images.
  */
  private static final int[] ROTATION_DEGREE = new int[] {0, 90, 180, 270};

 /**
  * Buffer to keep the keyboard input.
  */
  private int keyboardBuffer;

 /**
  * Current direction of Pac-Man.
  */
  private final SimpleIntegerProperty currentDirection;

  // Vickers: custom fields for logging human play
  private final String playerName;
  private int gameNumber = 1;
  private File logFile, hashLogFile, dotLocFile, magicDotLocFile;
  private BufferedWriter fileWriter, hashFileWriter, dotLocWriter, magicDotLocWriter;
  private GameStateFeatures lastGameState;
  
 /**
  * Constructor.
  *
  * @param maze
  * @param x
  * @param y
  * @param playerName
  */
  public PacMan(Maze maze, int x, int y, String playerName) {

    this.playerName = playerName;    
       
    this.maze = maze;
    this.x = x;
    this.y = y;

    Image defaultImage = new Image(getClass().getResourceAsStream("images/left1.png"));
    images = new Image[] {defaultImage,
      new Image(getClass().getResourceAsStream("images/left2.png")),
      defaultImage,
      new Image(getClass().getResourceAsStream("images/round.png"))
    };

    dotEatenCount = 0;
    score = new SimpleIntegerProperty(0);
    currentDirection = new SimpleIntegerProperty(MOVE_LEFT);


    imageX = new SimpleIntegerProperty(MazeData.calcGridX(x));
    imageY = new SimpleIntegerProperty(MazeData.calcGridX(y));

    xDirection = -1;
    yDirection = 0;


    ImageView pacmanImage = new ImageView(defaultImage);
    pacmanImage.xProperty().bind(imageX.add(-13));
    pacmanImage.yProperty().bind(imageY.add(-13));
    pacmanImage.imageProperty().bind(imageBinding);
//    pacmanImage.setCache(true);
    IntegerBinding rotationBinding = new IntegerBinding() {

      {
        super.bind(currentDirection);
      }

      @Override
      protected int computeValue() {
        return ROTATION_DEGREE[currentDirection.get()];
      }
    };
    pacmanImage.rotateProperty().bind(rotationBinding);

    keyboardBuffer = -1;

    getChildren().add(pacmanImage); // patweb
  }
  
  /* Data functions for logging */
  
  public void flushLogging()
  {
     // Flush and close file writer
     if (fileWriter != null)
     {  try 
        {  
            fileWriter.flush();
            fileWriter.close();
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(PacMan.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
          // Flush and close file writer
     if (hashFileWriter != null)
     {  try 
        {  
            hashFileWriter.flush();
            hashFileWriter.close();
        } 
        catch (IOException ex) 
        {
            Logger.getLogger(PacMan.class.getName()).log(Level.SEVERE, null, ex);
        }
     }
  }
  
  public void setGameNumber(int num)
  {
      // reset timesteps
      this.timestep = 0;
      
      
      // open new log file and set to current
      openNewLogFiles(num);
  }
  
  private void openNewLogFiles(int gameNum)
  {
      String fileName = "src/data/" + this.playerName + "_game_" + gameNum + ".txt";
      this.logFile = new File(fileName);
      this.hashLogFile = new File("src/data/hash_" + this.playerName + "_game_" + gameNum + ".txt");
      this.dotLocFile = new File("src/data/" + this.playerName + "_game_" + gameNum + "_dotLocs.txt");
      this.magicDotLocFile = new File("src/data/" + this.playerName + "_game_" + gameNum + "_magicDotLocs.txt");

      try 
      {
          if (! this.logFile.exists())
          {
               boolean success = logFile.createNewFile();
               if (! success)
                   System.err.println("ahh! unable to make log file : " + fileName);
          }
          if (! this.hashLogFile.exists())
          {
               boolean success = hashLogFile.createNewFile();
               if (! success)
                   System.err.println("ahh! unable to make log file : " + this.hashLogFile.getAbsolutePath());
          }
          if (! this.dotLocFile.exists())
          {
               boolean success = dotLocFile.createNewFile();
               if (! success)
                   System.err.println("ahh! unable to make log file : " + dotLocFile.getAbsolutePath());
          }
          if (! this.magicDotLocFile.exists())
          {
               boolean success = magicDotLocFile.createNewFile();
               if (! success)
                   System.err.println("ahh! unable to make log file : " + magicDotLocFile.getAbsolutePath());
          }
  
          this.fileWriter = new BufferedWriter(new FileWriter(this.logFile.getAbsoluteFile()));
          this.hashFileWriter = new BufferedWriter(new FileWriter(this.hashLogFile.getAbsoluteFile()));
          
          this.dotLocWriter = new BufferedWriter(new FileWriter(this.dotLocFile.getAbsoluteFile()));
          this.magicDotLocWriter = new BufferedWriter(new FileWriter(this.magicDotLocFile.getAbsoluteFile()));
          
          // write header
          this.fileWriter.write(GameStateFeatures.header());
          this.hashFileWriter.write("time         game_state\n");
          
      } 
      catch (IOException ex) 
      { System.err.println("Could not open BufferedWriter for log file!!");
      }
      
  }
  
  /***************************************************/

 /**
  * moving horizontally.
  */
  private void moveHorizontally() {

    moveCounter++;

    if (moveCounter < ANIMATION_STEP) {
      imageX.set(imageX.get() + (xDirection * MOVE_SPEED));
    }
    else {
      moveCounter = 0;
      x += xDirection;

      imageX.set(MazeData.calcGridX(x));

      // the X coordinate of the next point in the grid
      int nextX = xDirection + x;

      if ( (y == 14) && ( nextX <= 1 || nextX >= 28) ) {
        if ( (nextX < -1) && (xDirection < 0) ) {
          x = MazeData.GRID_SIZE_X;
          imageX.set(MazeData.calcGridX(x));
        }
        else {
          if ( (nextX > 30) && (xDirection > 0) ) {
            x = 0;
            imageX.set(MazeData.calcGridX(x));
          }
        }
      }
      else // check if the character hits a wall
      if (MazeData.getData(nextX, y) == MazeData.BLOCK) {
        state = STOPPED;
      }
    }
  }

 /**
  * moving vertically.
  */
  private void moveVertically() {

    moveCounter++;

    if (moveCounter < ANIMATION_STEP) {
      imageY.set(imageY.get() + (yDirection * MOVE_SPEED));
    }
    else {
      moveCounter = 0;
      y += yDirection;
      imageY.set(MazeData.calcGridX(y));

      // the Y coordinate of the next point in the grid
      int nextY = yDirection + y;

      // check if the character hits a wall
      if (MazeData.getData(x, nextY) == MazeData.BLOCK) {
        state = STOPPED;
      }
    }
  }

 /**
  * Turn Pac-Man to the right.
  */
  private void moveRight() {

    if (currentDirection.get() == MOVE_RIGHT) {
        return;
    }

    int nextX = x + 1;

    if (nextX >= MazeData.GRID_SIZE_X) {
      return;
    }

    if (MazeData.getData(nextX, y) == MazeData.BLOCK) {
      return;
    }

    xDirection = 1;
    yDirection = 0;

    keyboardBuffer = -1;
    currentDirection.set(MOVE_RIGHT);

    state = MOVING;
  }

 /**
  * Turn Pac-Man to the left.
  */
  private void moveLeft() {

    if (currentDirection.get() == MOVE_LEFT) {
        return;
    }

    int nextX = x - 1;

    if (nextX <= 1) {
      return;
    }

    if (MazeData.getData(nextX, y) == MazeData.BLOCK) {
      return;
    }

    xDirection = -1;
    yDirection = 0;

    keyboardBuffer = -1;
    currentDirection.set(MOVE_LEFT);

    state = MOVING;
  }

 /**
  * Turn Pac-Man up.
  */
  private void moveUp() {

    if (currentDirection.get() == MOVE_UP) {
      return;
    }

    int nextY = y - 1;

    if (nextY <= 1) {
      return;
    }

    if (MazeData.getData(x,nextY) == MazeData.BLOCK) {
      return;
    }

    xDirection = 0;
    yDirection = -1;

    keyboardBuffer = -1;
    currentDirection.set(MOVE_UP);

    state = MOVING;
  }

 /**
  * Turn Pac-Man down.
  */
  private void moveDown() {

    if (currentDirection.get() == MOVE_DOWN) {
        return;
    }

    int nextY = y + 1;

    if (nextY >= MazeData.GRID_SIZE_Y) {
      return;
    }

    if (MazeData.getData(x, nextY) == MazeData.BLOCK) {
      return;
    }

    xDirection = 0;
    yDirection = 1;

    keyboardBuffer = -1;
    currentDirection.set(MOVE_DOWN);

    state = MOVING;
  }

 /**
  * Handle keyboard input.
  */
  private void handleKeyboardInput() {
      
    if (keyboardBuffer < 0) {
      return;
    }

    if (keyboardBuffer == MOVE_LEFT) {
      moveLeft();
    } else if (keyboardBuffer == MOVE_RIGHT) {
      moveRight();
    } else if (keyboardBuffer == MOVE_UP) {
      moveUp();
    } else if (keyboardBuffer == MOVE_DOWN) {
      moveDown();
    }

  }


  public void setKeyboardBuffer(int k) {
    keyboardBuffer = k;
  }

 /**
  * Update score if a dot is eaten.
  */
  private void updateScore() {
    if ( y != 14 || ( x > 0 && x < MazeData.GRID_SIZE_X ) ) {
      Dot dot = (Dot) MazeData.getDot(x, y);

      if ( dot != null && dot.isVisible() ) {
        score.set(score.get() + 10);
        dot.setVisible(false);
        dotEatenCount++;
        
        Pair<Integer, Integer> dotLoc = new Pair(x, y);

        if (score.get() >= 10000) {
          maze.addLife();
        }

        if (dot.dotType == MazeData.MAGIC_DOT) {
          maze.makeGhostsHollow();
          if (this.maze.getMagicDotLocs().contains(dotLoc))
             this.maze.getMagicDotLocs().remove(dotLoc);
        }
        else
        {
            if (this.maze.getDotLocs().contains(dotLoc))
                this.maze.getDotLocs().remove(dotLoc);
        }

        // check if the player wins and should start a new level
        if (dotEatenCount >= MazeData.getDotTotal()) {
          maze.startNewLevel();
        }
      }
    }
  }

  public void hide() {
    setVisible(false);
    timeline.stop();
  }

 /**
  * Handle animation of one tick.
  */
  @Override
  public void moveOneStep() {
      
    this.logTimestep(this.fileWriter, this.hashFileWriter);
      
      
    if (maze.gamePaused.get()) {

      if (timeline.getStatus() != Animation.Status.PAUSED) {
        timeline.pause();
      }

      return;
    }

    // handle keyboard input only when Pac-Man is at a point on the grid
    if (currentImage.get() == 0) {
      handleKeyboardInput();
    }

    if (state == MOVING) {
        
      if (xDirection != 0) {
        moveHorizontally();
      }

      if (yDirection != 0) {
        moveVertically();
      }

      // switch to the image of the next frame
      if (currentImage.get() < ANIMATION_STEP - 1) {
        currentImage.set(currentImage.get() + 1);
      }
      else {
        currentImage.set(0);
        updateScore();
      }

    }

    maze.pacManMeetsGhosts();
  }

 /**
  * Place Pac-Man at the startup position for a new game.
  */
  public void resetStatus() {
    state = MOVING;
    currentDirection.set(MOVE_LEFT);
    xDirection = -1;
    yDirection = 0;

    keyboardBuffer = -1;
    currentImage.set(0);
    moveCounter = 0;

    x = 15;
    y = 24;

    imageX.set(MazeData.calcGridX(x));
    imageY.set(MazeData.calcGridY(y));

    setVisible(true); // patweb: Added because Pac-Man is invisible at start of new life.
    start();
  }
  
  
  
  private void logTimestep(BufferedWriter writer, BufferedWriter hashWriter)
  {
      
      Pair<Integer, Integer> pacmanLoc = new Pair(this.x, this.y);
      Pair<Integer, Integer> pacmanDir = new Pair(this.xDirection, this.yDirection);
      List<Pair<Integer, Integer>> ghost_locs = new ArrayList<>();
      
      
      
      boolean eatModeActive = false;
      for (Ghost gh : this.maze.ghosts)
      {
          ghost_locs.add( new Pair(gh.x, gh.y) );
          
          if (gh.isHollow)
              eatModeActive = true;
          
          //if (gh.state != Ghost.TRAPPED)
          //    num_free_ghosts++;
      }
      
      //int num_active_dots = MazeData.dotTotal - this.dotEatenCount;
      
      GameStateFeatures features = new GameStateFeatures(
                                        timestep, 
                                        eatModeActive,
                                        pacmanLoc,
                                        pacmanDir,
                                        ghost_locs   );
      
      //only log timestep if its different from previous
      if (this.lastGameState != null && (! this.lastGameState.equals(features)))
      {
        // only increment timestep on unique game features
        timestep++;  
          
        try 
        {
          writer.write(features.toString());
          writer.flush();
          
          hashWriter.write(String.format("%-5s        %-32s        %-5s\n", 
                                        timestep, 
                                        Long.toBinaryString(features.representAsLong()),
                                        action_to_int(keyboardBuffer)));
          
          hashWriter.flush();
          
          logDotLocations(this.dotLocWriter, this.magicDotLocWriter);
        } 
        catch (IOException ex) 
        {
          Logger.getLogger(PacMan.class.getName()).log(Level.SEVERE, null, ex);
        }
         
      } 
      
      // Update last seen features and timestep counter
      this.lastGameState = features;
      
  }
  
  private void logDotLocations(BufferedWriter dotLocWriter, BufferedWriter magicDotLocWriter)
  {
      //System.out.println("dot locs size = " + this.maze.getDotLocs().size());
      //System.out.println("magic dot locs size = " + this.maze.getMagicDotLocs().size());
      
      
      for (Pair<Integer, Integer> loc : this.maze.getDotLocs())
      {
          try 
          {
              dotLocWriter.write(String.format("(%d, %d) ,", loc.left, loc.right));
              dotLocWriter.flush();
          } 
          catch (IOException ex) 
          {
              Logger.getLogger(PacMan.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
          
      for (Pair<Integer, Integer> loc : this.maze.getMagicDotLocs())
      {   
          try 
          {
              magicDotLocWriter.write(String.format("(%d, %d) ,", loc.left, loc.right));
              magicDotLocWriter.flush();
          } 
          catch (IOException ex) 
          {
              Logger.getLogger(PacMan.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
      
     try {
         dotLocWriter.write("\n");
         magicDotLocWriter.write("\n");
     } catch (IOException ex) {
         Logger.getLogger(PacMan.class.getName()).log(Level.SEVERE, null, ex);
     }
      
  }
  
  private static double dist_formula(double x1, double x2, double y1, double y2)
  {
      return Math.sqrt( Math.pow(x2-x1, 2) +  Math.pow(y2-y1, 2) );
  }
  
  private static int action_to_int(int action)
  { 
      switch(action)
      {
          case MOVE_UP:
              //System.out.println("UP");
              return 1;
          case MOVE_RIGHT:
              //System.out.println("RIGHT");
              return 2;
          case MOVE_DOWN:
              //System.out.println("DOWN");
              return 3;
          case MOVE_LEFT:
              //System.out.println("LEFT");
              return 4;
      }
      //System.out.println("NO ACTION");
      return 0;  // no action.
  }

}
