
package mysh.codelib2.ui;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import mysh.codelib2.model.CodeLib2Element;
import mysh.codelib2.model.DataHeader;
import mysh.codelib2.model.SearchEngine;
import mysh.codelib2.model.SearchEngine.ResultCatcher;
import mysh.codelib2.ui.SaveStateManager.State;
import mysh.codelib2.ui.SaveStateManager.StateObserver;
import mysh.util.CompressUtil;
import mysh.util.HotKeyUtil;

import org.apache.log4j.Logger;

/**
 * UI 控制器. 控制UI行为及状态.
 * 
 * @author Allen
 * 
 */
public class UIControllor implements StateObserver, ResultCatcher {

	private static final Logger log = Logger.getLogger(CompressUtil.class);

	/**
	 * 刷新结果列表任务.
	 */
	private final Runnable refreshResultListTask = new Runnable() {

		@Override
		public void run() {

			Thread.yield();
			UIControllor.this.ui.resultList.repaint();
		}
	};

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
	 * 搜索引擎.
	 */
	private final SearchEngine searchEnging = new SearchEngine(this.eles, this);

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

	/**
	 * 文件选择器.
	 */
	private JFileChooser fileChooser = this.getFileChooser();

	private static final String Extention = ".zcl2";

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

		// 注册热键.
		this.registHotKey();

		// this.testData();
	}

	void testData() {

		this.eles.add(new CodeLib2Element().setKeywords("abcd"));
		this.eles.add(new CodeLib2Element().setKeywords("efg"));
		this.eles.add(new CodeLib2Element().setKeywords("hij"));
		this.eles.add(new CodeLib2Element().setKeywords("  java,   GUI, tree"));

		// DefaultListModel<CodeLib2Element> model = (DefaultListModel<CodeLib2Element>)
		// this.ui.resultList.getModel();
		// for (CodeLib2Element ele : this.eles) {
		// model.addElement(ele);
		// }
	}

	/**
	 * 注册热键.
	 */
	private void registHotKey() {

		// 注册 esc 热键.
		HotKeyUtil.registHotKey(KeyEvent.VK_ESCAPE, 0,
				new AbstractAction("escPressedAction") {

					private static final long serialVersionUID = -8642328380866972006L;

					@Override
					public void actionPerformed(ActionEvent e) {

						ui.filterText.setText("");
						ui.filterText.requestFocus();
					}
				});

		// 注册 Ctrl+S 热键.
		HotKeyUtil.registHotKey(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, new AbstractAction(
				"saveAction") {

			private static final long serialVersionUID = -6294554898524200651L;

			@Override
			public void actionPerformed(ActionEvent e) {

				save();
			}
		});
	}

	/**
	 * 新建.
	 */
	void newInst() {

		if (this.checkForSave()) {
			this.saveState.changeState(State.NEW);
			this.ui.filterText.setText("");
			this.filter("");
		}
	}

	/**
	 * 在改变状态检查当前状态是否需要保存.
	 * 
	 * @return 是否要继续改变状态. false 表示后面的操作不继续了.
	 */
	private boolean checkForSave() {

		if (this.saveState.getState() == State.MODIFIED) {
			int op = JOptionPane.showConfirmDialog(this.ui, "是否保存修改?",
					UIControllor.AppTitle, JOptionPane.YES_NO_CANCEL_OPTION);
			if (op == JOptionPane.YES_OPTION) {
				this.save();
			} else if (op == JOptionPane.CANCEL_OPTION) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 打开.
	 */
	void open() {

		if (this.checkForSave()) {

			this.fileChooser.setDialogTitle("打开");
			if (this.fileChooser.showOpenDialog(this.ui) == JFileChooser.APPROVE_OPTION) {
				File openFile = this.fileChooser.getSelectedFile();
				try {
					Collection<CodeLib2Element> datas = DataHeader.readFromFile(openFile.getAbsolutePath());

					this.eles.clear();
					this.eles.addAll(datas);
					this.currentItem = null;
					this.file = openFile;

					this.saveState.changeState(State.SAVED);
					this.ui.filterText.setText("");
					this.filter("");
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this.ui,
							"打开文件失败.\n" + e.getMessage(),
							UIControllor.AppTitle, JOptionPane.ERROR_MESSAGE);
				}

			}
		}
	}

	/**
	 * 取文件选择器.
	 * 
	 * @return
	 */
	private JFileChooser getFileChooser() {

		JFileChooser filec = new JFileChooser();
		filec.setFileSelectionMode(JFileChooser.FILES_ONLY);
		filec.setMultiSelectionEnabled(false);
		filec.setDialogTitle("选个文件吧");
		filec.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {

				if (f.isDirectory()
						|| f.getName().toLowerCase().endsWith(
								UIControllor.Extention)) {
					return true;
				}
				return false;
			}

			@Override
			public String getDescription() {

				return "*.zcl2 - zzx codelib2 文件";
			}
		});
		return filec;
	}

	/**
	 * 保存.
	 */
	void save() {

		File saveFile = this.file;

		this.fileChooser.setDialogTitle("保存");
		if (saveFile == null) {
			if (this.fileChooser.showOpenDialog(this.ui) == JFileChooser.APPROVE_OPTION) {
				saveFile = this.fileChooser.getSelectedFile();
				if (!saveFile.getName().endsWith(UIControllor.Extention)) {
					saveFile = new File(saveFile.getAbsolutePath()
							+ UIControllor.Extention);
				}
			} else {
				return;
			}
		}

		try {
			new DataHeader().saveToFile(saveFile.getAbsolutePath(), this.eles);

			this.file = saveFile;
			this.saveState.changeState(State.SAVED);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.ui, "保存文件失败.\\n" + e.getMessage(),
					UIControllor.AppTitle, JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * 新增条目.
	 */
	void addItem() {

		CodeLib2Element newEle = new CodeLib2Element();
		this.eles.add(newEle);
		this.onGetSearchResult(null, newEle);
		this.ui.keyWordText.requestFocus();

		this.saveState.changeState(State.MODIFIED);
	}

	/**
	 * 移除条目.
	 */
	@SuppressWarnings("unchecked")
	synchronized void removeItem() {

		int selectedIndex = this.ui.resultList.getSelectedIndex();
		if (selectedIndex > -1) {
			CodeLib2Element selectedValue = (CodeLib2Element) this.ui.resultList.getSelectedValue();

			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui, "删除确认:\n"
					+ selectedValue.getKeywords(), AppTitle,
					JOptionPane.YES_NO_OPTION)) {

				this.eles.remove(selectedValue);
				((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).removeElementAt(selectedIndex);

				this.saveState.changeState(State.MODIFIED);
			}
		}
	}

	/**
	 * 导出.
	 */
	void export() {

	}

	/**
	 * 执行搜索过滤.
	 * 
	 * @param text
	 */
	@SuppressWarnings("unchecked")
	void filter(String text) {

		this.ui.resultList.clearSelection();
		((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).removeAllElements();

		this.searchEnging.stopCurrentSearch();
		this.searchEnging.search(text);
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
			this.ui.keyWordText.setEditable(true);
			try {
				this.ui.codeText.setText(new String(item.getContent(),
						CodeLib2Element.DefaultCharsetEncode));
				this.ui.codeText.setCaretPosition(0);
				this.ui.codeText.setEditable(true);
			} catch (UnsupportedEncodingException e) {
				log.error("编码失败.", e);
				JOptionPane.showMessageDialog(this.ui, e.getMessage(), AppTitle,
						JOptionPane.ERROR_MESSAGE);
			}

			this.currentItem = item;
		} else {
			this.ui.keyWordText.setText("");
			this.ui.keyWordText.setEditable(false);
			this.ui.codeText.setText("");
			this.ui.codeText.setEditable(false);
		}
	}

	/**
	 * 保存当前编辑条目的关键字.
	 */
	private void saveCurrentItemKeywords() {

		if (this.currentItem != null) {
			this.saveState.changeState(State.MODIFIED);

			this.currentItem.setKeywords(this.ui.keyWordText.getText());

			this.refreshResultList();
		}
	}

	/**
	 * 刷新结果列表.
	 */
	private void refreshResultList() {

		SwingUtilities.invokeLater(this.refreshResultListTask);
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

	@Override
	public boolean onSaveStateChanged(State oldState, State newState) {

		switch (newState) {
		case NEW:
			this.currentItem = null;
			this.eles.clear();
			this.file = null;
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

	@SuppressWarnings("unchecked")
	@Override
	public synchronized void onGetSearchResult(String keyword, CodeLib2Element ele) {

		((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).addElement(ele);
		this.ui.resultList.setSelectedValue(ele, true);
		this.refreshResultList();
	}

	@Override
	public void onSearchComplete(String keyword) {

	}

	/**
	 * 关闭前检查或询问以确定否要关闭.
	 * 
	 * @return
	 */
	boolean doClose() {

		return this.checkForSave();
	}
}
