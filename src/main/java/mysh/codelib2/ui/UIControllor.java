
package mysh.codelib2.ui;

import java.awt.Desktop.Action;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

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
	 * 当前正在搜索的关键字.
	 */
	private volatile String currentKeyword;

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

	/**
	 * for test only.
	 */
	@SuppressWarnings("unused")
	private void testData() {

		Random r = new Random();

		int length = 100_000;

		String dic = "abcdefghijklmnopqrstuvwxyz{}[],.;:'/?()*&^%$#@!1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		char[] dicKeywords = dic.toCharArray();
		byte[] dicContent = (dic + "\r\n").getBytes();
		CodeLib2Element ele;
		for (int i = 0; i < length; i++) {
			ele = new CodeLib2Element();
			ele.setKeywords(this.testGenKeyword(r, dicKeywords, dicKeywords.length, r.nextInt(20) + 1));
			ele.setContent(this.testGenContent(r, dicContent, dicContent.length, r.nextInt(10_000) + 1));
			this.eles.add(ele);
		}

		Collections.sort(this.eles);
	}

	/**
	 * for test only.
	 */
	private String testGenKeyword(Random r, char[] dic, int dicLength, int length) {

		char[] c = new char[r.nextInt(length) + 1];
		for (int i = 0; i < c.length; i++)
			c[i] = dic[r.nextInt(dicLength)];

		return new String(c);
	}

	/**
	 * for test only.
	 */
	private byte[] testGenContent(Random r, byte[] dic, int dicLength, int length) {

		byte[] b = new byte[r.nextInt(length)];
		for (int i = 0; i < b.length; i++)
			b[i] = dic[r.nextInt(dicLength)];

		return b;
	}

	/**
	 * 注册热键.
	 */
	private void registHotKey() {

		// 注册 esc 热键.
		HotKeyUtil.registHotKey(KeyEvent.VK_ESCAPE, 0, new AbstractAction("escPressedAction") {

			private static final long serialVersionUID = -8642328380866972006L;

			@Override
			public void actionPerformed(ActionEvent e) {

				ui.filterText.setText("");
				ui.filterText.requestFocus();
			}
		});

		// 注册 Ctrl+S 热键.
		HotKeyUtil.registHotKey(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, new AbstractAction("saveAction") {

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
			int op = JOptionPane.showConfirmDialog(this.ui, "是否保存修改?", UIControllor.AppTitle,
					JOptionPane.YES_NO_CANCEL_OPTION);
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
				this.openFile(openFile);
			}
		}
	}

	/**
	 * 打开文件.
	 * 
	 * @param openFile
	 *               要打开的文件.
	 */
	void openFile(File openFile) {

		try {
			this.setStatusBar("正在打开文件 ...");
			Collection<CodeLib2Element> datas = DataHeader.readFromFile(openFile.getAbsolutePath());

			this.eles.clear();
			this.eles.addAll(datas);
			Collections.sort(this.eles);
			this.currentItem = null;
			this.file = openFile;

			this.saveState.changeState(State.SAVED);
			this.ui.filterText.setText("");
			this.filter("");

			this.fileChooser.setCurrentDirectory(openFile.getParentFile());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.ui, "打开文件失败.\n" + e.getMessage(), UIControllor.AppTitle,
					JOptionPane.ERROR_MESSAGE);
		} finally {
			System.gc();
			this.setStatusBarReady();
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

				if (f.isDirectory() || f.getName().toLowerCase().endsWith(UIControllor.Extention)) {
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
					saveFile = new File(saveFile.getAbsolutePath() + UIControllor.Extention);
				}
			} else {
				return;
			}
		}

		try {
			this.setStatusBar("正在保存 ...");
			new DataHeader().saveToFile(saveFile.getAbsolutePath(), this.eles);

			this.file = saveFile;
			this.saveState.changeState(State.SAVED);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.ui, "保存文件失败.\\n" + e.getMessage(), UIControllor.AppTitle,
					JOptionPane.ERROR_MESSAGE);
		} finally {
			System.gc();
			this.setStatusBarReady();
		}
	}

	/**
	 * 新增条目.
	 */
	void addItem() {

		final CodeLib2Element newEle = new CodeLib2Element();
		this.eles.add(newEle);

		SwingUtilities.invokeLater(new Runnable() {

			@SuppressWarnings("unchecked")
			@Override
			public void run() {

				((DefaultListModel<CodeLib2Element>) ui.resultList.getModel()).addElement(newEle);
				ui.resultList.setSelectedValue(newEle, true);
				ui.keyWordText.requestFocus();
			}
		});

		this.saveState.changeState(State.MODIFIED);
	}

	/**
	 * 移除条目.
	 */
	@SuppressWarnings("unchecked")
	synchronized void removeItem() {

		final List<CodeLib2Element> selectedItems = (List<CodeLib2Element>) this.ui.resultList.getSelectedValuesList();
		if (selectedItems.size() > 0) {

			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui, "删除确认?", AppTitle,
					JOptionPane.YES_NO_OPTION)) {

				this.eles.removeAll(selectedItems);

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						for (CodeLib2Element item : selectedItems)
							((DefaultListModel<CodeLib2Element>) ui.resultList.getModel()).removeElement(item);
					}
				});

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

		try {
			this.currentKeyword = text;
			this.searchEnging.addSearchTask(text);
			this.setStatusBar("正在搜索 [ " + text + " ] ...");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.ui, "创建搜索任务失败.\n关键字: [" + text + "]\n错误:\n" + e, AppTitle,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * 选中结果数据条目.
	 * 
	 * @param selectedValue
	 */
	synchronized void selectItem(Object selectedValue) {

		this.currentItem = null;

		if (selectedValue instanceof CodeLib2Element && this.ui.resultList.getSelectedIndices().length == 1) {

			CodeLib2Element item = (CodeLib2Element) selectedValue;
			this.ui.keyWordText.setText(item.getKeywords());
			this.ui.keyWordText.setEditable(true);
			try {
				this.ui.codeText.setSyntaxEditingStyle(this.getSyntaxStyle(item.getFirstKeyword().toLowerCase()));
				this.ui.codeText.setText(new String(item.getContent(), CodeLib2Element.DefaultCharsetEncode));
				this.ui.codeText.setCaretPosition(0);
				this.ui.codeText.setEditable(true);
			} catch (UnsupportedEncodingException e) {
				log.error("编码失败.", e);
				JOptionPane.showMessageDialog(this.ui, e.getMessage(), AppTitle, JOptionPane.ERROR_MESSAGE);
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
	 * 根据语法关键字取 RSyntaxTextArea 的语法样式.
	 * 
	 * @param syntaxKeyword
	 *               语法关键字.
	 * @return
	 */
	private String getSyntaxStyle(String syntaxKeyword) {

		String result = SyntaxConstants.SYNTAX_STYLE_NONE;

		switch (syntaxKeyword) {
		case "actionscript":
		case "as":
			result = SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT;
			break;
		case "asm":
			result = SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86;
			break;
		case "bat":
			result = SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH;
			break;
		case "bbcode":
			result = SyntaxConstants.SYNTAX_STYLE_BBCODE;
			break;
		case "c":
			result = SyntaxConstants.SYNTAX_STYLE_C;
			break;
		case "clj":
		case "clojure":
			result = SyntaxConstants.SYNTAX_STYLE_CLOJURE;
			break;
		case "cpp":
		case "c++":
			result = SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS;
			break;
		case "cs":
		case "c#":
			result = SyntaxConstants.SYNTAX_STYLE_CSHARP;
			break;
		case "css":
			result = SyntaxConstants.SYNTAX_STYLE_CSS;
			break;
		case "delphi":
		case "pas":
		case "pascal":
			result = SyntaxConstants.SYNTAX_STYLE_DELPHI;
			break;
		case "f":
		case "for":
		case "fortran":
			result = SyntaxConstants.SYNTAX_STYLE_FORTRAN;
			break;
		case "groovy":
		case "gsp":
			result = SyntaxConstants.SYNTAX_STYLE_GROOVY;
			break;
		case "htm":
		case "html":
			result = SyntaxConstants.SYNTAX_STYLE_HTML;
			break;
		case "java":
			result = SyntaxConstants.SYNTAX_STYLE_JAVA;
			break;
		case "javascript":
		case "js":
			result = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT;
			break;
		case "jsp":
			result = SyntaxConstants.SYNTAX_STYLE_JSP;
			break;
		case "lisp":
			result = SyntaxConstants.SYNTAX_STYLE_LISP;
			break;
		case "lua":
			result = SyntaxConstants.SYNTAX_STYLE_LUA;
			break;
		case "makefile":
			result = SyntaxConstants.SYNTAX_STYLE_MAKEFILE;
			break;
		case "mxml":
			result = SyntaxConstants.SYNTAX_STYLE_MXML;
			break;
		case "perl":
			result = SyntaxConstants.SYNTAX_STYLE_PERL;
			break;
		case "php":
			result = SyntaxConstants.SYNTAX_STYLE_PHP;
			break;
		case "properties":
			result = SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
			break;
		case "py":
		case "python":
			result = SyntaxConstants.SYNTAX_STYLE_PYTHON;
			break;
		case "rb":
		case "ruby":
			result = SyntaxConstants.SYNTAX_STYLE_RUBY;
			break;
		case "sas":
			result = SyntaxConstants.SYNTAX_STYLE_SAS;
			break;
		case "scala":
			result = SyntaxConstants.SYNTAX_STYLE_SCALA;
			break;
		case "sh":
			result = SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL;
			break;
		case "sql":
			result = SyntaxConstants.SYNTAX_STYLE_SQL;
			break;
		case "tcl":
			result = SyntaxConstants.SYNTAX_STYLE_TCL;
			break;
		case "xml":
		case "xsd":
		case "xsl":
			result = SyntaxConstants.SYNTAX_STYLE_XML;
			break;
		}

		return result;
	}

	/**
	 * 保存当前编辑条目的关键字.
	 */
	private void saveCurrentItemKeywords() {

		if (this.currentItem != null) {
			this.saveState.changeState(State.MODIFIED);

			this.currentItem.setKeywords(this.ui.keyWordText.getText());
			this.ui.codeText.setSyntaxEditingStyle(this.getSyntaxStyle(this.currentItem.getFirstKeyword()));

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
	public synchronized void onGetSearchResult(final String keyword, final CodeLib2Element ele) {

		if (keyword == this.currentKeyword)
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {

					if (keyword == currentKeyword)
						((DefaultListModel<CodeLib2Element>) ui.resultList.getModel()).addElement(ele);
					// this.ui.resultList.setSelectedValue(ele, true);
				}
			});
	}

	@Override
	public void onSearchComplete(String keyword) {

		this.refreshResultList();
		this.setStatusBarReady();
	}

	/**
	 * 关闭前检查或询问以确定否要关闭.
	 * 
	 * @return
	 */
	boolean doClose() {

		return this.checkForSave();
	}

	/**
	 * 将内容编辑窗的内容复制到系统剪贴板.
	 */
	public void copyContentToClipboard() {

		Clipboard sysclip = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable text = new StringSelection(this.ui.codeText.getText());
		sysclip.setContents(text, null);
	}

	/**
	 * 设置状态栏内容.
	 * 
	 * @param statusText
	 */
	private void setStatusBar(String statusText) {

		this.ui.statusBar.setText(statusText);
	}

	/**
	 * 重置状态栏.
	 */
	private void setStatusBarReady() {

		this.ui.statusBar.setText("就绪.");
	}

	void urlClicked(URL url) {

		java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
		if (desktop.isSupported(Action.BROWSE)) {
			try {

				this.setStatusBar(url.toString());
				desktop.browse(url.toURI());
			} catch (Exception e) {
			}
		}
	}
}
