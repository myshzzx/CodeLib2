
package mysh.codelib2.ui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mysh.codelib2.model.CodeLib2Element;
import mysh.codelib2.ui.SaveStateManager.State;
import mysh.codelib2.ui.SaveStateManager.StateObserver;
import mysh.util.CompressUtil;

import org.apache.log4j.Logger;

/**
 * UI 控制器. 控制UI行为及状态.
 * 
 * @author Allen
 * 
 */
public class UIControllor implements StateObserver {

	private static final Logger log = Logger.getLogger(CompressUtil.class);

	/**
	 * 应用名.
	 */
	private static final String AppTitle = "CodeLib2";

	private final CodeLib2Main ui;

	/**
	 * 数据集.
	 */
	private final List<CodeLib2Element> eles = new ArrayList<>();

	/**
	 * 保存状态管理器.
	 */
	private SaveStateManager saveState = new SaveStateManager(State.NEW);

	/**
	 * 保存文件.
	 */
	private File file;

	/**
	 * 当前选中的条目.
	 */
	private volatile CodeLib2Element currentItem;

	public UIControllor(CodeLib2Main ui) {

		if (ui == null) {
			throw new NullPointerException();
		}
		this.ui = ui;

		// 注册保存状态监听.
		this.saveState.registStateObserver(this);

		// 过滤器条件更新
		this.ui.filterText.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {

				UIControllor.this.filter(UIControllor.this.ui.filterText.getText());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {

				UIControllor.this.filter(UIControllor.this.ui.filterText.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		});

		// 关键字编辑后立刻保存.
		this.ui.keyWordText.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {

				UIControllor.this.saveCurrentItemKeywords();
				Collections.sort(UIControllor.this.eles);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {

				UIControllor.this.saveCurrentItemKeywords();
				Collections.sort(UIControllor.this.eles);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

			}
		});

		// 代码编辑后立刻保存.
		this.ui.codeText.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {

			}

			@Override
			public void insertUpdate(DocumentEvent e) {

			}

			@Override
			public void changedUpdate(DocumentEvent e) {

				UIControllor.this.saveCurrentItemCodeContent();
			}
		});

		// 注册 ESC 热键.
		this.registEscHotKey();

		this.testData();
	}

	@SuppressWarnings("unchecked")
	void testData() {

		DefaultListModel<CodeLib2Element> model = (DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel();
		model.addElement(new CodeLib2Element().setKeywords("abcd"));
		model.addElement(new CodeLib2Element().setKeywords("efg"));
		model.addElement(new CodeLib2Element().setKeywords("hij"));
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

	/**
	 * 新建.
	 */
	void newInst() {

	}

	/**
	 * 打开.
	 */
	void open() {

	}

	/**
	 * 保存.
	 */
	void save() {

	}

	/**
	 * 新增条目.
	 */
	void addItem() {

	}

	/**
	 * 移除条目.
	 */
	void removeItem() {

	}

	/**
	 * 导出.
	 */
	void export() {

	}

	/**
	 * 执行过滤.
	 * 
	 * @param text
	 */
	void filter(String text) {

		if (text.length() == 0) {
			this.ui.resultList.clearSelection();
		}
	}

	/**
	 * 选中结果数据条目.
	 * 
	 * @param selectedValue
	 */
	void selectItem(Object selectedValue) {

		this.currentItem = null;

		if (selectedValue instanceof CodeLib2Element) {

			CodeLib2Element item = (CodeLib2Element) selectedValue;
			this.ui.keyWordText.setText(item.getKeywords());
			try {
				this.ui.codeText.setText(new String(item.getContent(),
						CodeLib2Element.DefaultCharsetEncode));
				this.ui.codeText.setCaretPosition(0);
			} catch (UnsupportedEncodingException e) {
				log.error("编码失败.", e);
				JOptionPane.showMessageDialog(this.ui, e.getMessage(), AppTitle,
						JOptionPane.ERROR_MESSAGE);
			}

			this.currentItem = item;
		} else {
			this.ui.keyWordText.setText("");
			this.ui.codeText.setText("");
		}
	}

	/**
	 * 保存当前编辑条目的关键字.
	 */
	private void saveCurrentItemKeywords() {

		if (this.currentItem != null) {
			this.saveState.changeState(State.MODIFIED);

			this.currentItem.setKeywords(this.ui.keyWordText.getText());

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {

					UIControllor.this.ui.resultList.repaint();
				}
			});
		}
	}

	/**
	 * 代码编辑区需要保存代码的情况, 将代码保存到当前编辑的 Element.
	 */
	private void saveCurrentItemCodeContent() {

		try {
			if (this.currentItem != null) {
				this.saveState.changeState(State.MODIFIED);

				byte[] newContent = this.ui.codeText.getText().getBytes(
						CodeLib2Element.DefaultCharsetEncode);
				this.currentItem.setContent(newContent);
			}
		} catch (UnsupportedEncodingException e) {
			log.error("转码失败.", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see mysh.codelib2.ui.SaveStateManager.StateObserver#onSaveStateChanged(mysh.codelib2.ui.
	 * SaveStateManager.State, mysh.codelib2.ui.SaveStateManager.State)
	 */
	@Override
	public boolean onSaveStateChanged(State oldState, State newState) {

		switch (newState) {
		case NEW:
			this.file = null;
			this.currentItem = null;
			this.ui.setAppTitle(UIControllor.AppTitle);
			break;
		case MODIFIED:
			String title = "*" + UIControllor.AppTitle;
			if (this.file != null) {
				title += " - " + file.getAbsolutePath();
			}
			this.ui.setAppTitle(title);
			break;
		case SAVED:
			title = UIControllor.AppTitle;
			if (this.file != null) {
				title += " - " + file.getAbsolutePath();
			}
			this.ui.setAppTitle(title);
			break;
		}
		return true;
	}
}
