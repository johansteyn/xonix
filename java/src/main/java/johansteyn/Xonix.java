package johansteyn;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Xonix extends Frame {
	protected static final int DEFAULT_WIDTH = 240;
	protected static final int DEFAULT_HEIGHT = 320;
	protected static Dimension size;
	private Status status;

	public static void main(String[] args) throws Exception {
		parse(args);
		Frame frame = new Xonix(size);
		frame.setVisible(true);
	}

	protected static void parse(String[] args) {
		int width = DEFAULT_WIDTH;
		int height = DEFAULT_HEIGHT;
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-w")) {
					try {
						width = Integer.parseInt(args[++i]);
					} catch (NumberFormatException nfe) {
						usage();
						System.out.println("Invalid width: " + args[i]);
						System.out.println("");
						System.exit(1);
					}
					continue;
				}
				if (args[i].equals("-h")) {
					try {
						height = Integer.parseInt(args[++i]);
					} catch (NumberFormatException nfe) {
						usage();
						System.out.println("Invalid height: " + args[i]);
						System.out.println("");
						System.exit(1);
					}
					continue;
				}
				usage();
				System.exit(1);
			}
		}
		size = new Dimension(width, height);
	}

	protected static void usage() {
		System.out.println("");
		System.out.println("USAGE:");
		System.out.println("  $ java Xonix [-w width] [-h height]");
		System.out.println("");
		System.out.println("WHERE:");
		System.out.println("    width = Width of demo in pixels");
		System.out.println("    height = Height of demo in pixels");
		System.out.println("");
	}

	public Xonix(Dimension size) throws Exception {
		setBackground(Color.white);
		setLayout(new BorderLayout());
		Game game = new Game();
		status = new Status();
		add(game, BorderLayout.CENTER);
		add(status, BorderLayout.SOUTH);
		setStatus("Xonix");
		pack();
		setSize(size);
		Toolkit toolkit = getToolkit();
		Dimension screen = toolkit.getScreenSize();
		setLocation((screen.width - size.width) / 2, (screen.height - size.height) / 3);
		setTitle("Xonix");
		addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					System.exit(0);
				}
			}
		);
	}

	public void setStatus(String text) {
		if (status == null) {
			return;
		}
		if (text.equals(getStatus())) {
			return;
		}
		status.setText(text);
		status.repaint();
	}

	public String getStatus() {
		if (status == null) {
			return null;
		}
		return status.getText();
	}

	class Status extends Component {
		private String text = "";
		private Font font = new Font("sanserif", Font.BOLD, 12);
		private Dimension preferredSize;

		public Status() {
			setBackground(Color.green);
			setForeground(Color.black);
		}

		public void setText(String text) {
			this.text = text;
			if (status.isShowing()) {
				Graphics g = getGraphics();
				FontMetrics fm = g.getFontMetrics(font);
				int fw = fm.stringWidth(text);
				int fh = fm.getHeight();
				preferredSize = new Dimension(fw + 4, fh + 4);
			}
		}

		public String getText() {
			return text;
		}

		public Dimension getPreferredSize() {
			if (preferredSize == null) {
				Graphics g = getGraphics();
				FontMetrics fm = g.getFontMetrics(font);
				int fw = fm.stringWidth(text);
				int fh = fm.getHeight();
				preferredSize = new Dimension(fw + 4, fh + 4);
			}
			return preferredSize;
		}

		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
		}

		public void paint(Graphics g) {
			Color background = getBackground();
			Color foreground = getForeground();
			Dimension size = getSize();
			g.setColor(background);
			g.fillRect(0, 0, size.width, size.height);
			g.setColor(foreground);
			g.setFont(font);
			FontMetrics fm = g.getFontMetrics(font);
			int w = fm.stringWidth(text);
			g.drawString(text, (size.width - w) / 2, 2 * size.height / 3);
		}
	 }
}

