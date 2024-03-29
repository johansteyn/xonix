package johansteyn;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.util.*;


public class Game extends Container implements KeyListener, FocusListener {
	private static final int OUTSIDE = 0;
	private static final int INSIDE = 1;
	private static final int ACTIVE = 2;
	private static final int FIXED = 3;
	private static final int SPARE = 4;
	private static final int BLANK = 5;
	static final Color COLOR_OUTSIDE = new Color(0, 160, 0);
	private static final Color COLOR_INSIDE = Color.black;
	private static final Color COLOR_ACTIVE = new Color(200, 200, 0);
	private static final Color COLOR_TEXT = new Color(160, 0, 0);
	private static final Color COLOR_SPARE = Color.gray;
	private static final Color COLOR_BLANK = Color.white;
	private static final int DEFAULT_DELAY = 120;
	private static final int DELAY_INCREMENT = 10;
	private static final int DEFAULT_LIVES = 3;
	private static final Color[] winnerColors = {
		Color.red,
		Color.black
	};
	private int width;
	private int height;
	private int rows = 30;
	private int cols = 30;
	private int[][] cells = new int[rows][cols];
	private int cellWidth;
	private int cellHeight;
	private int target;
	private int level = 1;
	private int total;
	private int delay = DEFAULT_DELAY;
	private int lives = DEFAULT_LIVES;
	private boolean initialized;
	private boolean stopped; // player stopped by user
	private boolean paused;  // game paused by user
	private boolean aborted; // game aborted by user
	private boolean halted;  // game halted by system
	private boolean crashed; 
	private boolean gameover;
	private boolean showHighScores;
	private boolean showHelpStrings;
	private Animator animator = new Animator();
	private Player player;
	private ArrayList enemies;
	private ArrayList highScores = new ArrayList();
	private ArrayList helpStrings = new ArrayList();
	private Image pausedImage;
	private Image abortedImage;
	private Image gameoverImage;
	private boolean newGame;
	private boolean winner;
	private int percentageArea;
	private int percentageTime;
	private long startTime;
	private long pausedTime;
	private long haltedTime;
	private boolean warmed;

	public Game() {
		// start early to "warm up"
		animator.start();
		pausedImage = loadImage(this, "images/paused.gif");
		abortedImage = loadImage(this, "images/aborted.gif");
		gameoverImage = loadImage(this, "images/gameover.gif");
		addKeyListener(this);
		addFocusListener(this);
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				requestFocus();
			}
		});
		helpStrings.add("n/1-9   New game");
		helpStrings.add("arrows      Move");
		helpStrings.add("i/j/k/l     Move");
		helpStrings.add("space       Stop");
		helpStrings.add("p          Pause");
		helpStrings.add("a/ESC      Abort");
		helpStrings.add("s/-       Slower");
		helpStrings.add("d/=      Default");
		helpStrings.add("f/+       Faster");
		helpStrings.add("z     Highscores");
		helpStrings.add("h           Help");
		loadHighScores();
	}

	public void paint(Graphics g) {
		requestFocus();
		if (winner) {
			return;
		}
		Dimension d = getSize();
		width = d.width;
		height = d.height;
		cellWidth = width / cols;
		cellHeight = height / (rows + 1);
		if (!initialized) {
			play(false);
			halted = true;
			showHelpStrings = true;
			initialized = true;
		}
		if (showHelpStrings) {
			drawStrings(g, "Xonix", helpStrings);
			return;
		}
		if (showHighScores) {
			loadHighScores();
			drawStrings(g, "High Scores", highScores, 10);
			return;
		}
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if (cells[row][col] == OUTSIDE) {
					g.setColor(COLOR_OUTSIDE);
				}
				if (cells[row][col] == INSIDE) {
					g.setColor(COLOR_INSIDE);
				}
				if (cells[row][col] == ACTIVE) {
					g.setColor(COLOR_ACTIVE);
				}
				if (cells[row][col] == SPARE) {
					g.setColor(COLOR_SPARE);
				}
				if (cells[row][col] == BLANK) {
					g.setColor(COLOR_BLANK);
				}
				g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
				if (cells[row][col] == SPARE) {
					g.setColor(COLOR_BLANK);
					g.drawRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
				}
			}
		}
		player.draw(g);
		for (int i = 0; i < enemies.size(); i++) {
			Enemy enemy = (Enemy) enemies.get(i);
			enemy.draw(g);
		}
		if (paused || aborted || gameover) {
			Image image = pausedImage;
			if (aborted) {
				image = abortedImage;
			}
			if (gameover) {
				image = gameoverImage;
			}
			((Graphics2D) g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g.drawImage(image, width / 10, 4 * height / 9, 8 * width / 10, height / 9, this);
		}
		drawPercentageTime();
	}

	private void drawStrings(Graphics g, String heading, ArrayList strings) {
		drawStrings(g, heading, strings, strings.size());
	}

	private void drawStrings(Graphics g, String heading, ArrayList strings, int number) {
		g.setColor(COLOR_OUTSIDE);
		g.fillRect(0, 0, width, height);
		String longest = "";
		for (int i = 0; i < strings.size(); i++) {
			String s = (String) strings.get(i);
			if (s.length() > longest.length()) {
				longest = s;
			}
		}
		int fontSize = height / (number + 2) - 2;
		g.setColor(COLOR_TEXT);
		int headingFontSize = 2 * fontSize;
		int fw = 0;
		while (true) {
			Font font = new Font("Monospaced", Font.BOLD, headingFontSize);
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics(font);
			fw = fm.stringWidth(heading);
			if (fw < width) {
				break;
			}
			headingFontSize -= 4;
		}
		g.drawString(heading, (width - fw) / 2, headingFontSize);
		g.setColor(Color.black);
		Font font = new Font("Monospaced", Font.PLAIN, fontSize);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics(font);
		fw = fm.stringWidth(longest);
		for (int i = 0; i < strings.size() && i < number; i++) {
			String s = (String) strings.get(i);
			g.drawString(s, (width - fw) / 2, (i + 3) * fontSize);
		}
	}

	private void play(boolean preserve) {
		if (preserve) {
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					if (cells[row][col] == ACTIVE) {
						cells[row][col] = INSIDE;
					}
				}
			}
		} else {
			target = 0;
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					if (row < rows / 10 || 
						row > 9 * rows / 10 ||
						row < 3 ||
						row >= rows - 3 ||
						col < cols / 10 ||
						col > 9 * cols / 10 ||
						col < 3 ||
						col >= cols - 3) {
						cells[row][col] = OUTSIDE;
					} else {
						cells[row][col] = INSIDE;
						target++;
					}
				}
			}
			int col = cols / 2 - DEFAULT_LIVES / 2;
			for (int c = 0; c < DEFAULT_LIVES; c++) {
				if (c < lives) {
					cells[0][col + c] = SPARE;
				} else {
					cells[0][col + c] = BLANK;
				}
			}
		}
		player = new Player();
		ArrayList newEnemies = new ArrayList();
		if (preserve) {
			for (int i = 0; i < enemies.size(); i++) {
				Enemy enemy = (Enemy) enemies.get(i);
				if (enemy.inside) {
					newEnemies.add(enemy);
				}
			}
		}
		for (int i = 0; i < level; i++) {
			if (!preserve) {
				newEnemies.add(new Enemy(true));
			}
		}
		newEnemies.add(new Enemy(false));
		enemies = newEnemies;
		percentageArea = percentageArea();
		setStatus("Level " + level + " Score " + (total + percentageArea));
		startTime = System.currentTimeMillis();
		percentageTime = 100;
		crashed = false;
		aborted = false;
		gameover = false;
	}

	private void crash() {
		halted = true;
		lives--;
		percentageArea = percentageArea();
		setStatus("Level " + level + " Score " + (total + percentageArea));
		if (lives > 0) {
			repaint();
			sleep(3000);
			// In case user aborted during delay...
			if (!aborted) {
				play(true);
				halted = false;
				repaint();
			}
			return;
		}
		total += percentageArea;
		if (total > 0) {
			String scoreString = "" + total;
			while (scoreString.length() < 3) {
				scoreString = " " + scoreString;
			}
			boolean newHighScore = false;
			boolean inserted = false;
			for (int i = 0; i < highScores.size(); i++) {
				String s = (String) highScores.get(i);
				s = s.trim();
				try {
					int score = Integer.parseInt(s);
					if (total >= score) {
						highScores.add(i, scoreString);
						inserted = true;
						if (i == 0) {
							newHighScore = true;
						}
						break;
					}
				} catch (NumberFormatException nfe) {
				}
			}
			if (!inserted) {
				highScores.add(scoreString);
				if (highScores.size() == 1) {
					newHighScore = true;
				}
			}
			while (highScores.size() > 10) {
				highScores.remove(highScores.size() - 1);
			}
			saveHighScores();
			if (newHighScore) {
				sleep(1000);
				winner();
				showHighScores = true;
				repaint();
			}
		}
		level = 1;
		gameover = true;
		repaint();
	}

	protected void setStatus(String text) {
		Container parent = getParent();
		while (parent != null) {
			if (parent instanceof Xonix) {
				Xonix xonix = (Xonix) parent;
				xonix.setStatus(text);
				return;
			}
			parent = parent.getParent();
		}
	}

	protected String getStatus() {
		Container parent = getParent();
		while (parent != null) {
			if (parent instanceof Xonix) {
				Xonix xonix = (Xonix) parent;
				return xonix.getStatus();
			}
			parent = parent.getParent();
		}
		return null;
	}

	private int percentageArea() {
		if (target == 0) {
			return 0;
		}
		int todo = 0;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if (cells[row][col] == INSIDE || cells[row][col] == ACTIVE) {
					todo++;
				}
			}
		}
		return (target - todo) * 100 / target;
	}

	private int percentageTime() {
		long currentTime = System.currentTimeMillis();
		int lapsedTime = (int) ((currentTime - startTime) / 1000);
		int totalTime = 60 * level;
		int x = 100 * (totalTime - lapsedTime) / totalTime;
		x = x < 0 ? 0 : x;
int numEnemies = 10 - x / 10;
if (enemies.size() - level < numEnemies) {
	enemies.add(new Enemy(false));
}
		return x;
	}

	private void fix(int row, int col) {
		if (row < 0 || row >= rows) {
			return;
		}
		if (col < 0 || col >= cols) {
			return;
		}
		if (cells[row][col] != INSIDE) {
			return;
		}
		cells[row][col] = FIXED;
		fix(row - 1, col - 1);
		fix(row - 1, col);
		fix(row - 1, col + 1);
		fix(row, col - 1);
		fix(row, col + 1);
		fix(row + 1, col - 1);
		fix(row + 1, col);
		fix(row + 1, col + 1);
	}

	private void winner() {
		winner = true;
		Graphics g = getGraphics();
		String string = "High Score!";
		int fontSize = width / string.length();
		fontSize = fontSize > height / 2 ? height / 2 : fontSize;
		int fw = 0;
		while (true) {
			Font font = new Font("SansSerif", Font.BOLD, fontSize);
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics(font);
			fw = fm.stringWidth(string);
			if (fw < width) {
				break;
			}
			fontSize -= 4;
		}
		int color = 0;
		for (int i = 0; i < 300; i++) {
			if (!hasFocus()) {
				break;
			}
			if (i % 10 == 0) {
				g.setColor(winnerColors[color++ % winnerColors.length]);
				g.fillRect(0, 0, width, height);
			}
			g.setColor(randomColor());
			g.drawString(string, width / 2 - fw / 2, height / 2 + fontSize / 2);
			sleep(10);
		}
		winner = false;
	}

	private Color randomColor() {
		float min = 0.3f;
		float red = (float) (Math.random() + min);
		float green = (float) (Math.random() + min);
		float blue = (float) (Math.random() + min);
		red = red > 1.0f ? 1.0f : red;
		green = green > 1.0f ? 1.0f : green;
		blue = blue > 1.0f ? 1.0f : blue;
		return new Color(red, green, blue);
	}

	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
		}
		startTime += millis;
	}

	public void keyPressed(KeyEvent e) {
		char keyChar = e.getKeyChar();
		int keyCode = e.getKeyCode();
		long currentTime = e.getWhen();
		if (keyChar == 'n') {
			level = 1;
			newGame = true;
		}
		try {
			int value = Integer.parseInt("" + keyChar);
			if (value >= 1 && value <= 9) {
				level = value;
			}
			newGame = true;
		} catch (NumberFormatException nfe) {
		}
		if (newGame) {
			warmed = true;
			// all the fields animator waits on...
			paused = false;
			aborted = false;
			halted = false;
			gameover = false;
			showHelpStrings = false;
			showHighScores = false;
			if (animator.isAlive()) {
				animator.interrupt();
			}
			return;
		}
		int dir = Player.NONE;
		if (keyChar == 'i' || keyCode == KeyEvent.VK_UP) {
			dir = Player.UP;
		}
		if (keyChar == 'k' || keyCode == KeyEvent.VK_DOWN) {
			dir = Player.DOWN;
		}
		if (keyChar == 'j' || keyCode == KeyEvent.VK_LEFT) {
			dir = Player.LEFT;
		}
		if (keyChar == 'l' || keyCode == KeyEvent.VK_RIGHT) {
			dir = Player.RIGHT;
		}
		if (dir == Player.UP || 
			dir == Player.DOWN ||
			dir == Player.LEFT ||
			dir == Player.RIGHT) {
			if (player != null) {
				player.dir = dir;
				stopped = false;
			}
			return;
		}
		if (keyChar == 'p') {
			paused = paused ? false : true;
			if (paused) {
				pausedTime = currentTime;
			} else {
				if (pausedTime > 0) {
					startTime += (currentTime - pausedTime);
				}
			}
			repaint();
			return;
		}
		if (keyCode == KeyEvent.VK_ENTER) {
			showHelpStrings = false;
			showHighScores = false;
			if (halted && haltedTime > 0) {
				startTime += (currentTime - haltedTime);
				halted = false;
			}
			repaint();
		}
		if (keyChar == ' ') {
			stopped = true;
			return;
		}
		if (keyChar == '+' || keyChar == 'f') {
			delay -= DELAY_INCREMENT;
			if (delay <= 0) {
				delay = 1;
			}
			return;
		}
		if (keyChar == '=' || keyChar == 'd') {
			delay = DEFAULT_DELAY;
			return;
		}
		if (keyChar == '-' || keyChar == 's') {
			delay += DELAY_INCREMENT;
			if (delay > 201) {
				delay = 201;
			}
			return;
		}
		if (keyChar == 'z') {
			showHighScores = true;
			showHelpStrings = false;
			if (!halted) {
				halted = true;
				haltedTime = currentTime;
			}
			repaint();
			return;
		}
		if (keyChar == 'h') {
			showHelpStrings = true;
			showHighScores = false;
				repaint();
			if (!halted) {
				halted = true;
				haltedTime = currentTime;
			}
			return;
		}
		if (keyChar == 'a' || keyCode == KeyEvent.VK_ESCAPE) {
			lives = 0;
			aborted = true;
			animator.interrupt();
			repaint();
			return;
		}
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
	}

	public void focusGained(FocusEvent e) {
		long currentTime = System.currentTimeMillis();
		if (!paused && halted && haltedTime > 0) {
			startTime += currentTime - haltedTime;
			haltedTime = 0;
			halted = false;
		}
	}

	public void focusLost(FocusEvent e) {
		long currentTime = System.currentTimeMillis();
		if (!paused && !halted) {
			halted = true;
			haltedTime = currentTime;
		}
	}

	private void loadHighScores() {
		ArrayList newHighScores = new ArrayList();
		try {
			FileInputStream fis = new FileInputStream("highscores");
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			while (true) {
				String s = br.readLine();
				if (s == null || s.length() <= 0) {
					break;
				}
				newHighScores.add(s);
			}
			br.close();
		} catch (IOException ioe) {
		}
		highScores = newHighScores;
	}

	private void saveHighScores() {
		try {
			FileOutputStream fos = new FileOutputStream("highscores");
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
			for (int i = 0; i < highScores.size(); i++) {
				String s = (String) highScores.get(i);
				pw.println(s);
			}
			pw.flush();
			pw.close();

		} catch (IOException ioe) {
		}
	}

	private void drawPercentageTime() {
		Graphics g = getGraphics();
		if (g == null) {
			return;
		}
		int x = 0;
		int y = rows * cellHeight;
		int w = cols * cellWidth;
		int h = height - y;
		g.setColor(Color.darkGray);
		g.fillRect(x, y, w, h);
		g.setColor(Color.gray);
		g.fillRect(x, y, percentageTime * w / 100, h);
		g.setColor(Color.black);
		g.drawRect(x, y, w - 1, h - 1);
	}

	static Image loadImage(Component component, String name) {
		Image image = null;
		URL url = null;
		MediaTracker mt = new MediaTracker(component);
		//First try from current directory
		ClassLoader cl = component.getClass().getClassLoader();
		if (cl != null) {
			url = cl.getResource(name);
		}
		//if not found, try CLASSPATH
		if (url == null) {
			url = ClassLoader.getSystemResource(name);
		}
		try {
			image = component.getToolkit().getImage(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			mt.addImage(image, 0);
			mt.waitForAll();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return image;
	}

	class Animator extends Thread {
		public void run() {
			setPriority(MAX_PRIORITY);
			while (true) {
				while (!warmed) {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException ie) {
					}
				}
				if (newGame) {
					total = 0;
					lives = DEFAULT_LIVES;
					play(false);
					repaint();
					newGame = false;
				}
				if (player != null) {
					player.move();
				}
				if (enemies != null) {
					for (int i = 0; i < enemies.size(); i++) {
						Enemy enemy = (Enemy) enemies.get(i);
						enemy.move();
					}
				}
				int x = percentageTime();
				if (x < percentageTime) {
					percentageTime = x;
					drawPercentageTime();
				}
				if (percentageTime <= 0) {
					crash();
				}
				do {
					try {
						Thread.sleep(delay);
					} catch (InterruptedException ie) {
					}
				} while (paused || aborted || halted || gameover || showHelpStrings || showHighScores);
			}
		}
	}
		
	class Player {
		static final int NONE = 0;
		static final int UP = 1;
		static final int DOWN = 2;
		static final int LEFT = 3;
		static final int RIGHT = 4;
		Color color = Color.yellow;
		int row;
		int col;
		int dir = UP;
		boolean inside;

		public Player() {
			stopped = true;
			for (int row = 0; row < rows; row++) {
				for (int col = cols - 1; col >= 0; col--) {
					if (cells[row][col] == SPARE) {
						cells[row][col] = BLANK;
						this.row = row;
						this.col = col;
						break;
					}
				}
			}
		}

		public void move() {
			if (crashed) {
				crash();
				crashed = false;
				return;
			}
			if (stopped || paused || aborted || halted || gameover || showHelpStrings || showHighScores) {
				return;
			}
			int oldRow = row;
			int oldCol = col;
			if (dir == UP) {
				row = oldRow > 0 ? oldRow - 1 : oldRow;
			}
			if (dir == DOWN) {
				row = oldRow < rows - 1 ? oldRow + 1 : oldRow;
			}
			if (dir == LEFT) {
				col = oldCol > 0 ? oldCol - 1 : oldCol;
			}
			if (dir == RIGHT) {
				col = oldCol < cols - 1 ? oldCol + 1 : oldCol;
			}
			if (row != oldRow || col != oldCol) {
				Graphics g = getGraphics();
				if (cells[oldRow][oldCol] == ACTIVE) {
					g.setColor(COLOR_ACTIVE);
				}
				if (cells[oldRow][oldCol] == OUTSIDE) {
					g.setColor(COLOR_OUTSIDE);
				}
				if (cells[oldRow][oldCol] == SPARE) {
					g.setColor(COLOR_SPARE);
				}
				if (cells[oldRow][oldCol] == BLANK) {
					g.setColor(COLOR_BLANK);
				}
				g.fillRect(oldCol * cellWidth, oldRow * cellHeight, cellWidth, cellHeight);
				if (cells[oldRow][oldCol] == SPARE) {
					g.setColor(COLOR_BLANK);
					g.drawRect(oldCol * cellWidth, oldRow * cellHeight, cellWidth - 1, cellHeight - 1);
				}
				draw(g);
			}
			if (cells[row][col] == ACTIVE) {
				// Crashed into himself
				crashed = true;
				crash();
				return;
			}
			if (cells[row][col] == INSIDE) {
				inside = true;
				cells[row][col] = ACTIVE;
			}
			if (inside && cells[row][col] == OUTSIDE) {
				percentageTime = percentageTime();
				stopped = true;
				inside = false;
				halted = true;
				for (int i = 0; i < enemies.size(); i++) {
					Enemy enemy = (Enemy) enemies.get(i);
					fix(enemy.row, enemy.col);
				}
				for (int row = 0; row < rows; row++) {
					for (int col = 0; col < cols; col++) {
						if (cells[row][col] == ACTIVE) {
							cells[row][col] = OUTSIDE;
						}
						if (cells[row][col] == INSIDE) {
							cells[row][col] = OUTSIDE;
						}
						if (cells[row][col] == FIXED) {
							cells[row][col] = INSIDE;
						}
					}
				}
				repaint();
				percentageArea = percentageArea();
				setStatus("Level " + level + " Score " + (total + percentageArea));
				// Target captured area is indirectly proportional to level.
				// ie. Level 1 = 90%, Level 2 = 85%, Level 3 = 80%, ... Level 9 = 50%
				int target = 100 - 5 * (level + 1);
				if (percentageArea >= target) {
					total += percentageArea + percentageTime;
					setStatus("Level " + level + " Score " + total);
					level++;
					sleep(3000);
					play(false);
					repaint();
				}
				halted = false;
			}
		}

		void draw(Graphics g) {
			g.setColor(color);
			g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
			g.setColor(Color.black);
			g.drawRect(col * cellWidth + 1, row * cellHeight + 1, cellWidth - 2, cellHeight - 2);
		}

	}

	class Enemy {
		static final int SE = 0;
		static final int SW = 1;
		static final int NW = 2;
		static final int NE = 3;
		int[][] array = {
			{SE, NE, SW, NW},
			{SW, SE, NW, NE},
			{NW, SW, NE, SE},
			{NE, NW, SE, SW}
		};
		Color color;
		boolean inside;
		int row;
		int col;
		int dir = (int) (Math.random() * 4);
		
		public Enemy(boolean inside) {
			this.inside = inside;
			color = new Color(160, 0, 0);
			while (true) {
				row = (int) (Math.random() * rows);
				col = (int) (Math.random() * cols);
				if (!inside && 
					((row > rows / 10 && row < 9 * rows / 10) ||
					(col > cols / 10 && col < 9 * cols / 10))) {
					continue;
				}
				if (inside && cells[row][col] == INSIDE) {
					break;
				}
				if (!inside && cells[row][col] == OUTSIDE) {
					break;
				}
			}
		}
		
		public void move() {
			if (paused || aborted || halted || crashed || gameover || showHelpStrings || showHighScores) {
				return;
			}
			int oldRow = row;
			int oldCol = col;
			int oldDir = dir;
			for (int i = 0; i < 4; i++) {
				dir = array[oldDir][i];
				if (dir == SE) {
					row = oldRow + 1;
					col = oldCol + 1;
				}
				if (dir == SW) {
					row = oldRow + 1;
					col = oldCol - 1;
				}
				if (dir == NW) {
					row = oldRow - 1;
					col = oldCol - 1;
				}
				if (dir == NE) {
					row = oldRow - 1;
					col = oldCol + 1;
				}
				if (row < 0 || row >= rows ||
					col < 0 || col >= cols) {
					continue;
				}
				if (inside && cells[row][col] != OUTSIDE) {
					break;
				}
				if (!inside && cells[row][col] == OUTSIDE) {
					break;
				}
			}
			Graphics g = getGraphics();
			boolean hit = false;
			if (inside && cells[player.row][player.col] == INSIDE ||
				!inside && cells[player.row][player.col] == OUTSIDE) {
				if (row == player.row && col == player.col) {
					hit = true;
				}
			}
			if (inside) {
				if (cells[row][col] == ACTIVE) {
					hit = true;
				}
			}
			draw(g);
			if (inside) {
				g.setColor(COLOR_INSIDE);
			} else {
				g.setColor(COLOR_OUTSIDE);
			}
			g.fillRect(oldCol * cellWidth, oldRow * cellHeight, cellWidth, cellHeight);
			if (hit) {
				crashed = true;
			}
		}

		void draw(Graphics g) {
			g.setColor(color);
			g.fillRect(col * cellWidth, row * cellHeight, cellWidth, cellHeight);
			g.setColor(Color.black);
			g.drawRect(col * cellWidth + 1, row * cellHeight + 1, cellWidth - 2, cellHeight - 2);
		}
	}
}

