package com.whatdrops.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;

/**
 * A search box with a suggestion list under it. Suggestions come from an in-memory provider, so
 * they update on every keystroke. Up/Down pick a suggestion, Enter searches, Esc closes the list.
 *
 * <p>The list is shown with a plain {@link Popup} rather than a JPopupMenu: a JPopupMenu grabs the
 * arrow and Enter keys for menu navigation, which would stop the text field from receiving them.
 */
final class TypeaheadField extends JPanel
{
	private static final int MAX_SUGGESTIONS = 8;
	private static final int ROW_HEIGHT = 20;

	private final IconTextField field = new IconTextField();
	private final DefaultListModel<String> model = new DefaultListModel<>();
	private final JList<String> list = new JList<>(model);
	private final JPanel popupContent = new JPanel(new BorderLayout());
	private final PropertyChangeListener focusWatcher = e -> hideIfFocusLeft();
	/** Only registered while the list is open: closes it on a click anywhere outside the field and list. */
	private final AWTEventListener outsideClickWatcher = this::onGlobalMouseEvent;

	private Function<String, List<String>> provider = q -> Collections.emptyList();
	private Consumer<String> onSubmit = s ->
	{
	};

	private Popup popup;
	private boolean settingText;

	TypeaheadField()
	{
		setLayout(new BorderLayout());
		setOpaque(false);

		field.setIcon(IconTextField.Icon.SEARCH);
		field.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		field.setMinimumSize(new Dimension(0, 30));
		field.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		field.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		field.addActionListener(e -> submitCurrent());
		field.addClearListener(this::hidePopup);
		field.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				switch (e.getKeyCode())
				{
					case KeyEvent.VK_DOWN:
						moveSelection(1);
						e.consume();
						break;
					case KeyEvent.VK_UP:
						moveSelection(-1);
						e.consume();
						break;
					case KeyEvent.VK_ESCAPE:
						if (popup != null)
						{
							hidePopup();
							e.consume();
						}
						break;
					default:
						break;
				}
			}
		});
		field.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				textChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				textChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
			}
		});
		add(field, BorderLayout.CENTER);

		list.setFocusable(false);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setFixedCellHeight(ROW_HEIGHT);
		list.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		list.setForeground(ColorScheme.TEXT_COLOR);
		list.setSelectionBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		list.setSelectionForeground(java.awt.Color.WHITE);
		list.setFont(Styles.bodyFont());
		list.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> l, Object value, int index,
				boolean selected, boolean focused)
			{
				JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, selected, false);
				c.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
				return c;
			}
		});
		list.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				int i = list.locationToIndex(e.getPoint());
				if (i >= 0)
				{
					choose(model.get(i));
				}
			}
		});
		list.addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int i = list.locationToIndex(e.getPoint());
				if (i >= 0 && i != list.getSelectedIndex())
				{
					list.setSelectedIndex(i);
				}
			}
		});

		popupContent.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		popupContent.add(list, BorderLayout.CENTER);

		// Close the list if the panel scrolls, moves, or the tab holding this field is switched away
		addAncestorListener(new AncestorListener()
		{
			@Override
			public void ancestorAdded(AncestorEvent e)
			{
			}

			@Override
			public void ancestorRemoved(AncestorEvent e)
			{
				hidePopup();
			}

			@Override
			public void ancestorMoved(AncestorEvent e)
			{
				hidePopup();
			}
		});
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", focusWatcher);
	}

	@Override
	public void removeNotify()
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("focusOwner", focusWatcher);
		hidePopup();
		super.removeNotify();
	}

	void setSuggestionProvider(Function<String, List<String>> provider)
	{
		this.provider = provider;
	}

	void setOnSubmit(Consumer<String> onSubmit)
	{
		this.onSubmit = onSubmit;
	}

	String getText()
	{
		return field.getText();
	}

	/** Sets the text without opening the suggestion list. */
	void setText(String text)
	{
		settingText = true;
		try
		{
			field.setText(text);
		}
		finally
		{
			settingText = false;
		}
	}

	void setIcon(IconTextField.Icon icon)
	{
		field.setIcon(icon);
	}

	@Override
	public boolean requestFocusInWindow()
	{
		return field.requestFocusInWindow();
	}

	void hidePopup()
	{
		if (popup != null)
		{
			popup.hide();
			popup = null;
			Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickWatcher);
		}
	}

	private void onGlobalMouseEvent(AWTEvent event)
	{
		if (event.getID() != MouseEvent.MOUSE_PRESSED || !(event.getSource() instanceof Component))
		{
			return;
		}
		Component target = (Component) event.getSource();
		if (!SwingUtilities.isDescendingFrom(target, this) && !SwingUtilities.isDescendingFrom(target, popupContent))
		{
			hidePopup();
		}
	}

	private void textChanged()
	{
		if (!settingText)
		{
			// Document events fire mid-edit; wait until the edit is done before reading the text
			SwingUtilities.invokeLater(this::refreshSuggestions);
		}
	}

	private void refreshSuggestions()
	{
		String query = field.getText() == null ? "" : field.getText().trim();
		if (query.isEmpty())
		{
			hidePopup();
			return;
		}

		List<String> suggestions = provider.apply(query);
		boolean onlyExact = suggestions.size() == 1 && suggestions.get(0).equalsIgnoreCase(query);
		if (suggestions.isEmpty() || onlyExact)
		{
			hidePopup();
			return;
		}

		model.clear();
		for (int i = 0; i < suggestions.size() && i < MAX_SUGGESTIONS; i++)
		{
			model.addElement(suggestions.get(i));
		}
		list.clearSelection();
		showPopup();
	}

	private void showPopup()
	{
		hidePopup();
		if (!field.isShowing() || model.isEmpty())
		{
			return;
		}

		int height = model.size() * ROW_HEIGHT + 2;
		popupContent.setPreferredSize(new Dimension(field.getWidth(), height));

		Point p = field.getLocationOnScreen();
		popup = PopupFactory.getSharedInstance().getPopup(field, popupContent, p.x, p.y + field.getHeight());
		popup.show();
		Toolkit.getDefaultToolkit().addAWTEventListener(outsideClickWatcher, AWTEvent.MOUSE_EVENT_MASK);
	}

	private void moveSelection(int delta)
	{
		if (popup == null)
		{
			refreshSuggestions();
			return;
		}

		int n = model.size();
		if (n == 0)
		{
			return;
		}
		int i = list.getSelectedIndex();
		i = i < 0 ? (delta > 0 ? 0 : n - 1) : Math.floorMod(i + delta, n);
		list.setSelectedIndex(i);
	}

	private void submitCurrent()
	{
		String value = popup != null && list.getSelectedIndex() >= 0
			? list.getSelectedValue()
			: field.getText();
		choose(value);
	}

	private void choose(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return;
		}
		hidePopup();
		setText(value.trim());
		onSubmit.accept(value.trim());
	}

	private void hideIfFocusLeft()
	{
		if (popup == null)
		{
			return;
		}
		// Check after pending events so a click on the list is handled before the list closes
		SwingUtilities.invokeLater(() ->
		{
			Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
			if (owner == null || !SwingUtilities.isDescendingFrom(owner, this))
			{
				hidePopup();
			}
		});
	}
}
