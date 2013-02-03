
package mysh.codelib2.ui;

import java.awt.Color;
import java.awt.Desktop;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import mysh.codelib2.model.CodeLib2Element;
import mysh.codelib2.model.CodeLib2Element.Attachment;
import mysh.codelib2.model.DataHeader;
import mysh.codelib2.model.ExportEngine;
import mysh.codelib2.model.ExportEngine.ExportInfo;
import mysh.codelib2.model.SearchEngine;
import mysh.codelib2.model.SearchEngine.ResultCatcher;
import mysh.codelib2.ui.SaveStateManager.State;
import mysh.codelib2.ui.SaveStateManager.StateObserver;
import mysh.util.CompressUtil;
import mysh.util.FileUtil;
import mysh.util.HotKeyUtil;
import mysh.util.UIUtil;

import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.SearchContext;

/**
 * UI 控制器. 控制UI行为及状态.
 * 
 * @author Allen
 * 
 */
public class UIControllor implements StateObserver, ResultCatcher {

	private static final Logger log = Logger.getLogger(CompressUtil.class);

	/**
	 * 应用名.
	 */
	private static final String AppTitle = "CodeLib2";

	/**
	 * 临时目录.
	 */
	private static final String TempDir = System.getProperty("java.io.tmpdir") + AppTitle + File.separatorChar;

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
	 * 文件扩展名.
	 */
	static final String Extention = ".zcl2";

	/**
	 * 代码框文本搜索.
	 */
	private SearchContext findContext = new SearchContext();

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

		// 代码框文本搜索.
		this.findContext.setMatchCase(false);
		this.findContext.setRegularExpression(true);
		this.findContext.setSearchForward(true);
		this.findContext.setWholeWord(false);

		// this.testData();
	}

	/**
	 * for test only.
	 */
	@SuppressWarnings("unused")
	private void testData() {

		Random r = new Random();

		int length = 100_000;

		String dic = "abcdefghijklmnopqrstuvwxyz{}[].;:'/?()*&^%$#@!1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		char[] dicKeywords = dic.toCharArray();
		byte[] dicContent = (dic + ",\r\n").getBytes();
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

		// 注册 Ctrl+F 热键.
		HotKeyUtil.registHotKey(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, new AbstractAction("saveAction") {

			private static final long serialVersionUID = -6294554898524200651L;

			@Override
			public void actionPerformed(ActionEvent e) {

				ui.findText.requestFocus();
				ui.findText.selectAll();
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

		if (checkForSave()) {
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

			// this.fileChooser.setDialogTitle("打开");
			if (this.ui.zcl2OpenChooser.showOpenDialog(this.ui) == JFileChooser.APPROVE_OPTION) {
				File openFile = this.ui.zcl2OpenChooser.getSelectedFile();
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
			this.ui.filterText.requestFocus();
			this.filter("");

			this.ui.zcl2OpenChooser.setCurrentDirectory(openFile.getParentFile());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.ui, "打开文件失败.\n" + e.getMessage(), UIControllor.AppTitle,
					JOptionPane.ERROR_MESSAGE);
		} finally {
			System.gc();
			this.setStatusBarReady();
		}
	}

	/**
	 * 保存.
	 */
	void save() {

		File saveFile = this.file;

		if (saveFile == null) {
			saveFile = UIUtil.getSaveFileWithOverwriteChecking(this.ui.zcl2OpenChooser, this.ui,
					new UIUtil.FileExtentionGetter() {

						@Override
						public String getFileExtention() {

							return UIControllor.Extention;
						}
					});
			if (saveFile == null)
				return;
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
	@SuppressWarnings("unchecked")
	void addItem() {

		final CodeLib2Element newEle = new CodeLib2Element();
		this.eles.add(newEle);

		((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).addElement(newEle);
		this.ui.resultList.setSelectedValue(newEle, true);
		this.ui.keyWordText.requestFocus();

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

				for (CodeLib2Element item : selectedItems)
					((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).removeElement(item);

				this.saveState.changeState(State.MODIFIED);
			}
		}
	}

	/**
	 * 导出.
	 */
	@SuppressWarnings("unchecked")
	void export() {

		List<CodeLib2Element> selectedItems;
		if ((selectedItems = this.ui.resultList.getSelectedValuesList()).size() > 0) {
			File exportFile = UIUtil.getSaveFileWithOverwriteChecking(this.ui.itemExportChooser, this.ui,
					new UIUtil.FileExtentionGetter() {

						@Override
						public String getFileExtention() {

							String ext = ui.itemExportChooser.getFileFilter().getDescription();
							if (!ext.startsWith("."))
								ext = "";
							return ext;
						}
					});

			try {
				if (exportFile != null) {
					ExportEngine.ExportInfo info = new ExportInfo();

					info.filepath = exportFile.getPath();
					if (this.file == null) {
						info.title = AppTitle + " - "
								+ FileUtil.getFileNameWithoutExtention(info.filepath);
					} else {
						info.title = AppTitle + " - "
								+ FileUtil.getFileNameWithoutExtention(this.file.getPath());
					}

					ExportEngine.export(info, selectedItems);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this.ui, "导出失败.\n" + e, AppTitle, JOptionPane.ERROR_MESSAGE);
			} finally {
				System.gc();
			}
		}
	}

	/**
	 * 执行搜索过滤.
	 * 
	 * @param text
	 */
	@SuppressWarnings("unchecked")
	private void filter(String text) {

		this.ui.resultList.clearSelection();
		((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).clear();
		this.clearAttachmentTable();

		try {
			this.currentKeyword = text;
			this.searchEnging.addSearchTask(text);

			if (this.currentKeyword.split("[\\s,]+").length > 0)
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
	synchronized void selectItem(final Object selectedValue) {

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

				this.findText();
			} catch (UnsupportedEncodingException e) {
				log.error("编码失败.", e);
				JOptionPane.showMessageDialog(this.ui, e.getMessage(), AppTitle, JOptionPane.ERROR_MESSAGE);
			}

			this.clearAttachmentTable();
			if (item.getAttachments() != null) {
				for (Attachment attachment : item.getAttachments()) {
					((DefaultTableModel) this.ui.attachmentTable.getModel()).addRow(new Object[] {
							attachment, attachment.getBinaryContent().length });
				}
			}
			this.ui.attachmentTable.updateUI();

			this.currentItem = item;
		} else {
			this.ui.keyWordText.setText("");
			this.ui.keyWordText.setEditable(false);
			this.ui.codeText.setText("");
			this.ui.codeText.setEditable(false);
			((DefaultTableModel) this.ui.attachmentTable.getModel()).getDataVector().removeAllElements();
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
		case "dtd":
			result = SyntaxConstants.SYNTAX_STYLE_DTD;
			break;
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
		case "json":
			result = SyntaxConstants.SYNTAX_STYLE_JSON;
			break;
		case "jsp":
			result = SyntaxConstants.SYNTAX_STYLE_JSP;
			break;
		case "latex":
			result = SyntaxConstants.SYNTAX_STYLE_LATEX;
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
		case "mx":
		case "mxml":
			result = SyntaxConstants.SYNTAX_STYLE_MXML;
			break;
		case "nsi":
		case "nsis":
			result = SyntaxConstants.SYNTAX_STYLE_NSIS;
			break;
		case "perl":
			result = SyntaxConstants.SYNTAX_STYLE_PERL;
			break;
		case "php":
			result = SyntaxConstants.SYNTAX_STYLE_PHP;
			break;
		case "prolog":
			result = SyntaxConstants.SYNTAX_STYLE_CLOJURE;
			break;
		case "properties":
			result = SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
			break;
		case "py":
		case "python":
			result = SyntaxConstants.SYNTAX_STYLE_PYTHON;
			break;
		case "ruby":
			result = SyntaxConstants.SYNTAX_STYLE_RUBY;
			break;
		case "sas":
			result = SyntaxConstants.SYNTAX_STYLE_SAS;
			break;
		case "scala":
			result = SyntaxConstants.SYNTAX_STYLE_SCALA;
			break;
		case "shell":
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
			this.ui.codeText.setSyntaxEditingStyle(this.getSyntaxStyle(this.currentItem.getFirstKeyword().toLowerCase()));

			this.ui.resultList.repaint();
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
	public void onSearchComplete(final String keyword) {

		// update status bar
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				String[] keywords = keyword.trim().split("[\\s,]+");
				if (keywords.length > 0 && keywords[keywords.length - 1].trim().length() > 0) {
					ui.findText.setText(keywords[keywords.length - 1].trim());
				} else {
					ui.findText.setText(null);
				}

				ui.resultList.repaint();

				if (currentKeyword.split("[\\s,]+").length > 0) {
					setStatusBar("搜索 [" + currentKeyword + "] 完成, 共 "
							+ ui.resultList.getModel().getSize() + " 个结果.");
					if (ui.resultList.getModel().getSize() > 0) {
						ui.resultList.setSelectedIndex(0);
					}
				} else {
					setStatusBarReady();
				}
			}
		});
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
	void setStatusBar(String statusText) {

		this.ui.statusBar.setText(statusText);
	}

	/**
	 * 重置状态栏.
	 */
	void setStatusBarReady() {

		this.ui.statusBar.setText("就绪");
	}

	/**
	 * 代码编辑窗口的 url 点击事件.
	 * 
	 * @param url
	 */
	void urlClicked(URL url) {

		java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
		if (desktop.isSupported(Action.BROWSE)) {
			try {

				desktop.browse(url.toURI());
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 导入附件.
	 */
	 void addAttachment() {

		CodeLib2Element attachToItem = this.currentItem;
		if (attachToItem != null
				&& JFileChooser.APPROVE_OPTION == this.ui.attachmentImportChooser.showOpenDialog(this.ui)) {
			File[] files = this.ui.attachmentImportChooser.getSelectedFiles();

			final List<Attachment> attachments = new ArrayList<>();

			for (File file : files) {
				try {
					if (file.length() > 1_000_000
							&& JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this.ui,
									file.getName() + "\n文件超过1MB, 仍然导入?", AppTitle,
									JOptionPane.YES_NO_OPTION)) {
						continue;
					}

					attachments.add(new Attachment().setName(file.getName()).setBinaryContent(
							FileUtil.readFileToByteArray(file.getAbsolutePath(), Integer.MAX_VALUE)));
				} catch (Exception e) {
					log.error("导入附件失败. " + file.getAbsolutePath(), e);
					JOptionPane.showMessageDialog(this.ui, "导入附件失败.\n" + file.getName(), AppTitle,
							JOptionPane.ERROR_MESSAGE);
				}
			}

			if (attachments.size() > 0) {
				if (attachToItem.getAttachments() == null)
					attachToItem.setAttachments(new ArrayList<Attachment>());

				attachToItem.getAttachments().addAll(attachments);
				Collections.sort(attachToItem.getAttachments());

				this.saveState.changeState(State.MODIFIED);
				this.ui.resultList.repaint();
			}

			if (attachToItem == this.currentItem)
				this.addAttachmentToTable(attachments);
		}

	}

	/**
	 * 清空附件列表.
	 */
	private void clearAttachmentTable() {

		((DefaultTableModel) this.ui.attachmentTable.getModel()).getDataVector().clear();
		this.ui.attachmentTable.updateUI();
	}

	/**
	 * 将 List<Attachment> 添加到附件列表.
	 * 
	 * @param attachments
	 */
	private void addAttachmentToTable(List<Attachment> attachments) {

		DefaultTableModel dataModel = (DefaultTableModel) this.ui.attachmentTable.getModel();
		for (Attachment attachment : attachments) {
			dataModel.addRow(new Object[] { attachment, attachment.getBinaryContent().length });
		}
		this.ui.attachmentTable.updateUI();
	}

	/**
	 * 移除选定的附件.
	 */
	void removeAttachment() {

		int[] selectedRows = this.ui.attachmentTable.getSelectedRows();
		if (selectedRows.length > 0) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui, "确认移除附件?", AppTitle,
					JOptionPane.YES_NO_OPTION)) {
				for (int i = selectedRows.length - 1; i > -1; i--) {
					this.currentItem.getAttachments().remove(
							this.ui.attachmentTable.getValueAt(selectedRows[i], 0));
					((DefaultTableModel) this.ui.attachmentTable.getModel()).removeRow(selectedRows[i]);
				}
				this.saveState.changeState(State.MODIFIED);
				this.ui.resultList.repaint();
				this.ui.attachmentTable.updateUI();
			}
		}
	}

	/**
	 * 导出附件.
	 */
	void exportAttachment() {

		int[] selectedRows = this.ui.attachmentTable.getSelectedRows();
		Attachment attachment;
		if (selectedRows.length > 0
				&& JFileChooser.APPROVE_OPTION == this.ui.attachmentExportChooser.showSaveDialog(this.ui)) {
			File dir = this.ui.attachmentExportChooser.getSelectedFile();
			String filepath;
			for (int row : selectedRows) {
				if (dir != null && dir.isDirectory()) {
					String dirPath = dir.getAbsolutePath();
					attachment = (Attachment) this.ui.attachmentTable.getValueAt(row, 0);
					filepath = dirPath + '/' + attachment.getName();

					if (new File(filepath).exists()
							&& JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this.ui,
									"是否覆盖文件?\n" + filepath, AppTitle,
									JOptionPane.YES_NO_OPTION)) {
						continue;
					}

					if (!FileUtil.writeFile(filepath, attachment.getBinaryContent())) {
						JOptionPane.showMessageDialog(this.ui, "导出失败.\n" + attachment.getName(),
								AppTitle, JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}

	/**
	 * 导入 zcl2 文件.
	 */
	@SuppressWarnings("unchecked")
	public void importZcl2() {

		if (JFileChooser.APPROVE_OPTION == this.ui.zcl2ImportChooser.showOpenDialog(this.ui)) {
			File[] files = this.ui.zcl2ImportChooser.getSelectedFiles();
			// 成功导入计数.
			int successImportCount = 0;
			Collection<CodeLib2Element> readItems;

			this.setStatusBar("正在导入 ...");
			for (File file : files) {
				try {
					readItems = DataHeader.readFromFile(file.getAbsolutePath());
					this.eles.addAll(readItems);

					for (CodeLib2Element ele : readItems)
						((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).addElement(ele);

					successImportCount++;
				} catch (Exception e) {
					log.error("导入 zcl2 文件失败: " + file.getAbsolutePath(), e);
				}
			}
			Collections.sort(this.eles);
			this.setStatusBarReady();
			System.gc();

			if (successImportCount > 0)
				this.saveState.changeState(State.MODIFIED);

			if (successImportCount == files.length) {
				JOptionPane.showMessageDialog(this.ui, "导入成功: " + successImportCount + " 个文件", AppTitle,
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this.ui, "导入成功: " + successImportCount + " 个文件\n导入失败: "
						+ (files.length - successImportCount) + " 个文件\n失败详情查看日志", AppTitle,
						JOptionPane.INFORMATION_MESSAGE);
			}

		}
	}

	/**
	 * 查找(代码框).
	 */
	private void findText() {

		// if find nothing, set the find text to red
		this.ui.findText.setForeground(Color.RED);

		String text = this.ui.findText.getText().trim();
		try {
			Pattern.compile(text);
		} catch (PatternSyntaxException e) {
			text = "";
		}

		boolean findResult = false;
		if (text.length() > 0) {
			this.findContext.setSearchFor(text);

			int oldCaretPosition = this.ui.codeText.getCaretPosition();
			findResult = org.fife.ui.rtextarea.SearchEngine.find(this.ui.codeText, this.findContext);
			if (!findResult) {
				if (this.findContext.getSearchForward()) {
					this.ui.codeText.setCaretPosition(0);
				} else {
					this.ui.codeText.setCaretPosition(this.ui.codeText.getText().length());
				}
				findResult = org.fife.ui.rtextarea.SearchEngine.find(this.ui.codeText, this.findContext);
			}

			if (findResult) {
				this.ui.findText.setForeground(Color.BLACK);
			} else {
				this.ui.codeText.setCaretPosition(oldCaretPosition);
			}

		} else {
			this.ui.codeText.getHighlighter().removeAllHighlights();
		}

		this.ui.findPreviousBtn.setEnabled(findResult);
		this.ui.findNextBtn.setEnabled(findResult);
	}

	/**
	 * 查找前一个.(代码框)
	 */
	void findPrevious() {

		this.findContext.setSearchForward(false);
		this.findText();
	}

	/**
	 * 查找下一个.(代码框)
	 */
	void findNext() {

		this.findContext.setSearchForward(true);
		this.findText();
	}

	/**
	 * 打开附件.
	 */
	void openAttachment() {

		int selectedRow = this.ui.attachmentTable.getSelectedRow();
		if (selectedRow > -1) {
			Attachment attachment = (Attachment) this.ui.attachmentTable.getValueAt(selectedRow, 0);
			final String oriFilePath = TempDir + attachment.getName();
			String filePath = FileUtil.getWritableFile(oriFilePath).getPath();
			FileUtil.writeFile(filePath, attachment.getBinaryContent());

			File tempFile = new File(filePath);
			try {
				tempFile.deleteOnExit();
				Desktop.getDesktop().open(tempFile);
			} catch (IOException e) {
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui,
						"打开文件失败, 要尝试用文本方式打开吗?\n" + oriFilePath, "打开文件", JOptionPane.YES_NO_OPTION)) {
					File textFile = FileUtil.getWritableFile(oriFilePath + ".txt");
					if (tempFile.renameTo(textFile)) {
						textFile.deleteOnExit();
						try {
							Desktop.getDesktop().open(textFile);
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(this.ui,
									textFile.getAbsolutePath() + e1.getMessage(),
									"打开文本文件失败.", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		}
	}
}
