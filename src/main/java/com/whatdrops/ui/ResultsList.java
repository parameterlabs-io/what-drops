package com.whatdrops.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;

/** A vertically scrolling stack of rows, headers and messages. */
final class ResultsList extends JPanel
{
	private static final int MESSAGE_WIDTH = PluginPanel.PANEL_WIDTH - 60;

	private final JPanel rows = new JPanel(new DynamicGridLayout(0, 1, 0, 2));
	private final JScrollPane scrollPane;

	ResultsList()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		rows.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel north = new WidthTrackingPanel();
		north.setBackground(ColorScheme.DARK_GRAY_COLOR);
		north.add(rows, BorderLayout.NORTH);

		scrollPane = new JScrollPane(north);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(scrollPane, BorderLayout.CENTER);
	}

	void clear()
	{
		rows.removeAll();
	}

	void add(JComponent c)
	{
		rows.add(c);
	}

	/** Call after adding rows. Scrolls back to the top. */
	void done()
	{
		rows.revalidate();
		rows.repaint();
		SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
	}

	void showMessage(String message)
	{
		clear();
		add(message(message));
		done();
	}

	static JComponent message(String text)
	{
		// An HTML label only wraps when it's given a width
		JLabel label = new JLabel("<html><div style='width:" + MESSAGE_WIDTH + "px;text-align:center'>"
			+ Styles.escape(text) + "</div></html>");
		label.setFont(Styles.bodyFont());
		label.setForeground(Styles.MUTED_TEXT);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));
		return label;
	}

	/** A small heading between groups of rows, e.g. "Rare (3)". */
	static JComponent section(String title, Color color)
	{
		JLabel label = Styles.label(title, Styles.bodyFont(), color);
		label.setBorder(BorderFactory.createEmptyBorder(8, 2, 2, 2));
		return label;
	}

	/** A full-width text button, used for "Show all" and "Retry". */
	static JComponent linkButton(String text, Runnable onClick)
	{
		JLabel label = Styles.label(text, Styles.bodyFont(), ColorScheme.BRAND_ORANGE);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (((Component) e.getSource()).contains(e.getPoint()))
				{
					onClick.run();
				}
			}
		});
		return label;
	}

	/**
	 * Keeps rows as wide as the visible area. Without this, a row with long text is laid out wider
	 * than the panel and its right-hand column (the drop rate) gets cut off when the scrollbar shows.
	 */
	private static final class WidthTrackingPanel extends JPanel implements Scrollable
	{
		private WidthTrackingPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(16, visibleRect.height - 16);
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
