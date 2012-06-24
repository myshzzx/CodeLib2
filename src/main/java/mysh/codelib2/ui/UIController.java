
package mysh.codelib2.ui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * UI 控制器. 控制UI行为及状态.
 * 
 * @author Allen
 * 
 */
public class UIController {

	private CodeLib2Main ui;

	public UIController(CodeLib2Main ui) {

		if (ui == null) {
			throw new NullPointerException();
		}
		this.ui = ui;

		this.registEscHotKey();
	}

	/**
	 * 注册 esc 热键.
	 */
	private void registEscHotKey() {

		final HashMap<KeyStroke, Action> actionMap = new HashMap<KeyStroke, Action>();
		KeyStroke escPressed = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

		actionMap.put(escPressed, new AbstractAction("escPressedAction") {

			private static final long serialVersionUID = -8642328380866972006L;

			@Override
			public void actionPerformed(ActionEvent e) {

				ui.filterText.setText("");
				ui.filterText.requestFocus();
			}
		});

		KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		kfm.addKeyEventDispatcher(new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {

				KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
				if (actionMap.containsKey(keyStroke)) {
					final Action a = actionMap.get(keyStroke);
					final ActionEvent ae = new ActionEvent(e.getSource(), e.getID(),
							null);
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {

							a.actionPerformed(ae);
						}
					});
					return true;
				}
				return false;
			}
		});
	}
}
