package com.whatdrops.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A two-line clickable row:
 * <pre>
 * [icon] Title                 Rate
 *        Detail              Amount
 * </pre>
 */
final class ResultRow extends JPanel
{
	private static final int ICON_WIDTH = 36;
	private static final int ICON_HEIGHT = 32;

	private final JLabel icon = new JLabel();
	private final JPanel textPanel = new JPanel(new GridLayout(2, 1));
	private final JPanel valuePanel = new JPanel(new GridLayout(2, 1));

	ResultRow(boolean withIcon, String title, String detail, String rate, Color rateColor, String amount,
		String tooltip, Runnable onClick)
	{
		setLayout(new BorderLayout(6, 0));
		setBackground(Styles.ROW_BACKGROUND);
		setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

		if (withIcon)
		{
			icon.setPreferredSize(new Dimension(ICON_WIDTH, ICON_HEIGHT));
			icon.setHorizontalAlignment(SwingConstants.CENTER);
			add(icon, BorderLayout.WEST);
		}

		textPanel.setOpaque(false);
		textPanel.add(Styles.label(title, Styles.bodyFont(), Color.WHITE));
		textPanel.add(Styles.label(detail == null ? "" : detail, Styles.bodyFont(), Styles.MUTED_TEXT));
		add(textPanel, BorderLayout.CENTER);

		valuePanel.setOpaque(false);
		JLabel rateLabel = Styles.label(rate, Styles.bodyFont(), rateColor);
		rateLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel amountLabel = Styles.label(amount == null ? "" : amount, Styles.bodyFont(), Styles.MUTED_TEXT);
		amountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		valuePanel.add(rateLabel);
		valuePanel.add(amountLabel);
		add(valuePanel, BorderLayout.EAST);

		setToolTipText(tooltip);

		if (onClick != null)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					setBackground(Styles.ROW_HOVER);
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					setBackground(Styles.ROW_BACKGROUND);
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (contains(e.getPoint()))
					{
						onClick.run();
					}
				}
			});
		}
	}

	JLabel getIconLabel()
	{
		return icon;
	}
}
