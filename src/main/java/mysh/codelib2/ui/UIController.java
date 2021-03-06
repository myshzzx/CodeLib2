
package mysh.codelib2.ui;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import mysh.codelib2.model.CodeLib2Element;
import mysh.codelib2.model.CodeLib2Element.Attachment;
import mysh.codelib2.model.DataHeader;
import mysh.codelib2.model.ExportEngine;
import mysh.codelib2.model.SearchEngine;
import mysh.collect.Colls;
import mysh.collect.Pair;
import mysh.util.FilesUtil;
import mysh.util.HotKeysLocal;
import mysh.util.Strings;
import mysh.util.UIs;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Desktop.Action;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * UI 控制器. 控制UI行为及状态.
 *
 * @author Allen
 */
public class UIController implements SaveStateManager.StateObserver, SearchEngine.ResultCatcher {
	private static final Logger log = LoggerFactory.getLogger(UIController.class);
	
	/**
	 * 搜索结果.
	 */
	private static class SearchResult {
		
		private CodeLib2Element ele;
		private int matchDegree;
		
		private SearchResult(CodeLib2Element ele, int matchDegree) {
			this.ele = ele;
			this.matchDegree = matchDegree;
		}
		
	}
	
	/**
	 * 应用名.
	 */
	public static final String AppTitle = "CodeLib2 b20201030";
	
	static {
		MutableDataSet mdOpt = new MutableDataSet();
		//  set optional extensions
		mdOpt.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
		// convert soft-breaks to hard breaks
		mdOpt.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
		mdParser = Parser.builder(mdOpt).build();
		mdRenderer = HtmlRenderer.builder(mdOpt).build();
	}
	
	private static final Parser mdParser;
	private static final HtmlRenderer mdRenderer;
	
	private final CodeLib2Main ui;
	
	/**
	 * 浏览器引擎.
	 */
	private WebEngine browserEngine;
	
	/**
	 * 浏览器默认页面.
	 */
	private static String DefaultBrowserContent = "";
	
	/**
	 * 数据集.
	 */
	private final List<CodeLib2Element> eles = new ArrayList<>();
	
	/**
	 * 搜索引擎.
	 */
	private final SearchEngine searchEngine = new SearchEngine(this.eles, this);
	
	/**
	 * 保存状态管理器.
	 */
	private SaveStateManager saveState = new SaveStateManager(SaveStateManager.State.NEW);
	
	/**
	 * 保存文件.
	 */
	private File file;
	
	
	/**
	 * 当前正在搜索的关键字.
	 */
	private volatile String currentKeyword;
	
	/**
	 * 搜索结果队列.
	 */
	private final Queue<SearchResult> searchResults = new ConcurrentLinkedQueue<>();
	
	/**
	 * 当前选中的条目.
	 */
	private volatile CodeLib2Element currentItem;
	
	/**
	 * 文件扩展名.
	 */
	static final String Extension = ".zcl2";
	
	/**
	 * 代码框文本搜索.
	 */
	private SearchContext findContext = new SearchContext();
	
	public UIController(CodeLib2Main ui) {
		
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
				
				UIController.this.uiSearch(UIController.this.ui.filterText.getText());
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				
				UIController.this.uiSearch(UIController.this.ui.filterText.getText());
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
			
			}
		});
		
		// 关键字编辑后立刻保存.
		this.ui.keyWordText.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent e) {
				
				UIController.this.saveCurrentItemKeywords();
				Collections.sort(UIController.this.eles);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				
				UIController.this.saveCurrentItemKeywords();
				Collections.sort(UIController.this.eles);
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
				
				UIController.this.saveCurrentItemCodeContent();
			}
		});
		
		// 注册热键.
		this.registerHotKey();
		
		// 代码框文本搜索.
		this.findContext.setMatchCase(false);
		this.findContext.setRegularExpression(true);
		this.findContext.setSearchForward(true);
		this.findContext.setWholeWord(false);
		
		// this.testData();
		
		// 附件列表文件拖入事件
		this.ui.attachmentPanel.setDropTarget(new DropTarget() {
			@Override
			public synchronized void drop(DropTargetDropEvent evt) {
				try {
					Transferable transferable = evt.getTransferable();
					if (currentItem != null
							&&
							Arrays.asList(transferable.getTransferDataFlavors()).contains(DataFlavor.javaFileListFlavor)) {
						evt.acceptDrop(DnDConstants.ACTION_COPY);
						List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
						addAttachment(currentItem, transferData);
					} else {
						evt.rejectDrop();
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});
		
		// 浏览器初始化
		final JFXPanel fxPanel = new JFXPanel();
		Platform.runLater(() -> {
			ui.browserPanel.add(fxPanel);
			WebView browser = new WebView();
			Scene scene = new Scene(browser);
			fxPanel.setScene(scene);
			browserEngine = browser.getEngine();
			browserSetContent(DefaultBrowserContent);
			browserEngine.setOnAlert(evt -> JOptionPane.showMessageDialog(ui, evt.getData()));
			
			// menu
			browser.setContextMenuEnabled(false);
			ContextMenu browserMenu = new ContextMenu();
			browser.setOnMousePressed(e -> {
				if (e.getButton() == MouseButton.SECONDARY) {
					browserMenu.show(browser, e.getScreenX(), e.getScreenY());
				} else {
					browserMenu.hide();
				}
			});
			javafx.scene.control.MenuItem reload = new javafx.scene.control.MenuItem("Reload");
			reload.setOnAction(e -> browserEngine.reload());
			javafx.scene.control.MenuItem externalOpen = new javafx.scene.control.MenuItem("External Open");
			externalOpen.setOnAction(e -> browserOpenInExternal());
			javafx.scene.control.MenuItem externalOpenUrl = new javafx.scene.control.MenuItem("External Open Location");
			externalOpenUrl.setOnAction(e -> UIs.openOuterBrowser(browserEngine.getLocation()));
			browserMenu.getItems().addAll(reload, externalOpen, externalOpenUrl);
		});
		
		this.ui.contentTab.addChangeListener(new ChangeListener() {
			Component lastTab = ui.rTextScrollPane;
			
			@Override
			public void stateChanged(ChangeEvent e) {
				Component currentTab = ui.contentTab.getSelectedComponent();
				if (currentTab == ui.browserPanel && lastTab == ui.rTextScrollPane) {
					if (isCurrentMarkdown())
						renderMarkdown();
					else if (isCurrentPlantUml())
						renderPlantUml();
					else
						browserSetContent("");
				}
				lastTab = currentTab;
			}
		});
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
	
	private AbstractAction escAction = new AbstractAction("escAction") {
		private static final long serialVersionUID = -8642328380866972006L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!ui.filterText.isFocusOwner()) {
				ui.filterText.requestFocus();
				return;
			}
			
			String keywords = ui.filterText.getText().trim();
			int lastSeprIndex = keywords.length();
			for (int i = keywords.length() - 1; i > -1; i--) {
				if (keywords.charAt(i) != ' ' && keywords.charAt(i) != ',') {
					lastSeprIndex = i;
					break;
				}
			}
			lastSeprIndex = Math.max(keywords.lastIndexOf(' ', lastSeprIndex), keywords.lastIndexOf(',', lastSeprIndex));
			if (lastSeprIndex == -1) {
				ui.filterText.setText("");
			} else {
				ui.filterText.setText(keywords.substring(0, lastSeprIndex + 1));
			}
			ui.filterText.requestFocus();
		}
	};
	
	private AbstractAction findAction = new AbstractAction("findAction") {
		
		private static final long serialVersionUID = -6294554898524200651L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			ui.findText.requestFocus();
			ui.findText.selectAll();
		}
	};
	
	private AbstractAction saveAction = new AbstractAction("saveAction") {
		
		private static final long serialVersionUID = -6294554898524200651L;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			uiSave();
		}
	};
	
	/**
	 * 注册热键.
	 */
	private void registerHotKey() {
		// 注册 esc 热键.
		HotKeysLocal.registerHotKey(KeyEvent.VK_ESCAPE, 0, escAction);
		
		// 注册 Ctrl+F 热键.
		HotKeysLocal.registerHotKey(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, findAction);
		
		// 注册 Ctrl+S 热键.
		HotKeysLocal.registerHotKey(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, saveAction);
	}
	
	/**
	 * 反注册热键.
	 */
	private void unRegisterHotKey() {
		// 注册 esc 热键.
		HotKeysLocal.unRegisterHotKey(KeyEvent.VK_ESCAPE, 0, escAction);
		
		// 注册 Ctrl+F 热键.
		HotKeysLocal.unRegisterHotKey(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, findAction);
		
		// 注册 Ctrl+S 热键.
		HotKeysLocal.unRegisterHotKey(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, saveAction);
	}
	
	/**
	 * 新建.
	 */
	void uiNewInst() {
		
		if (checkForSave()) {
			this.saveState.changeState(SaveStateManager.State.NEW);
			this.ui.filterText.setText("");
			this.uiSearch("");
		}
	}
	
	/**
	 * 在改变状态检查当前状态是否需要保存.
	 *
	 * @return 是否要继续改变状态. false 表示后面的操作不继续了.
	 */
	private boolean checkForSave() {
		
		if (this.saveState.getState() == SaveStateManager.State.MODIFIED) {
			int op = JOptionPane.showConfirmDialog(this.ui, "是否保存修改?", UIController.AppTitle,
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (op == JOptionPane.YES_OPTION) {
				this.uiSave();
			} else if (op == JOptionPane.CANCEL_OPTION) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * 打开.
	 */
	void uiOpen() {
		
		if (this.checkForSave()) {
			
			// this.fileChooser.setDialogTitle("打开");
			if (this.ui.zclOpenChooser.showOpenDialog(this.ui) == JFileChooser.APPROVE_OPTION) {
				File openFile = this.ui.zclOpenChooser.getSelectedFile();
				this.uiOpenFile(openFile);
			}
		}
	}
	
	/**
	 * 打开文件.
	 *
	 * @param openFile 要打开的文件.
	 */
	void uiOpenFile(File openFile) {
		
		try {
			this.uiSetStatusBar("正在打开文件 ...");
			if (!openFile.exists()) {
				this.eles.clear();
				this.file = openFile;
				this.uiSave();
			}
			
			Pair<DataHeader, Collection<CodeLib2Element>> data = DataHeader.readFromFile(openFile);
			
			this.eles.clear();
			this.eles.addAll(data.getR());
			Collections.sort(this.eles);
			this.currentItem = null;
			this.file = openFile;
			
			this.saveState.changeState(SaveStateManager.State.SAVED);
			this.ui.filterText.setText("");
			this.ui.filterText.requestFocus();
			this.uiSearch("");
			
			this.ui.zclOpenChooser.setCurrentDirectory(openFile.getParentFile());
		} catch (Exception e) {
			log.error("open file error.", e);
			JOptionPane.showMessageDialog(this.ui, "打开文件失败.\n" + e.getMessage(), UIController.AppTitle,
					JOptionPane.ERROR_MESSAGE);
		} finally {
			this.uiSetStatusBarReady();
		}
	}
	
	/**
	 * 保存.
	 */
	void uiSave() {
		
		File saveFile = this.file;
		
		if (saveFile == null) {
			saveFile = UIs.getSaveFileWithOverwriteChecking(this.ui.zclOpenChooser, this.ui, () -> UIController.Extension);
			if (saveFile == null)
				return;
		}
		
		try {
			this.uiSetStatusBar("正在保存 ...");
			DataHeader.saveToFile(saveFile, this.eles);
			
			this.file = saveFile;
			this.saveState.changeState(SaveStateManager.State.SAVED);
		} catch (Exception e) {
			log.error("saveFile-fail: " + saveFile, e);
			JOptionPane.showMessageDialog(this.ui, "保存文件失败.\\n" + e.getMessage(), UIController.AppTitle,
					JOptionPane.ERROR_MESSAGE);
		} finally {
			this.uiSetStatusBarReady();
		}
	}
	
	/**
	 * 新增条目.
	 */
	@SuppressWarnings("unchecked")
	void itemAdd() {
		
		final CodeLib2Element newEle = new CodeLib2Element();
		this.eles.add(newEle);
		
		((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).addElement(newEle);
		this.ui.resultList.setSelectedValue(newEle, true);
		this.ui.keyWordText.requestFocus();
		
		this.saveState.changeState(SaveStateManager.State.MODIFIED);
	}
	
	/**
	 * 移除条目.
	 */
	@SuppressWarnings("unchecked")
	synchronized void itemRemove() {
		
		final List<CodeLib2Element> selectedItems = (List<CodeLib2Element>) this.ui.resultList.getSelectedValuesList();
		if (selectedItems.size() > 0) {
			
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui, "删除确认?", AppTitle,
					JOptionPane.YES_NO_OPTION)) {
				
				for (CodeLib2Element item : selectedItems) {
					item.delete();
					((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).removeElement(item);
				}
				
				this.saveState.changeState(SaveStateManager.State.MODIFIED);
			}
		}
	}
	
	/**
	 * 导出.
	 *
	 * @param exportType 导出类型. 1 为导出选中条目, 0 为导出全部.
	 */
	@SuppressWarnings("unchecked")
	void uiExport(int exportType) {
		List<CodeLib2Element> items = null;
		
		switch (exportType) {
			case 0:
				items = this.eles;
				break;
			case 1:
				items = this.ui.resultList.getSelectedValuesList();
				break;
		}
		
		if (items != null && items.size() > 0) {
			File exportFile = UIs.getSaveFileWithOverwriteChecking(
					this.ui.itemExportChooser, this.ui,
					() -> {
						String ext = ui.itemExportChooser.getFileFilter().getDescription();
						if (!ext.startsWith("."))
							ext = "";
						return ext;
					});
			
			try {
				if (exportFile != null) {
					ExportEngine.ExportInfo info = new ExportEngine.ExportInfo();
					
					info.file = exportFile;
					if (this.file == null) {
						info.title = AppTitle + " - "
								+ FilesUtil.getFileNameWithoutExtension(exportFile);
					} else {
						info.title = AppTitle + " - "
								+ FilesUtil.getFileNameWithoutExtension(this.file);
					}
					
					ExportEngine.export(info, items);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this.ui, "导出失败.\n" + e, AppTitle, JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	/**
	 * 执行搜索.
	 */
	@SuppressWarnings("unchecked")
	private void uiSearch(String text) {
		this.currentKeyword = text;
		this.orderType = 0;
		this.ui.orderType.setToolTipText("正排");
		
		this.clearShowItems(false);
		
		try {
			this.searchEngine.addSearchTask(text, System.currentTimeMillis() + 300);
			
			if (this.currentKeyword.split("[\\s,]+").length > 0)
				this.uiSetStatusBar("正在搜索 [ " + text + " ] ...");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this.ui, "创建搜索任务失败.\n关键字: [" + text + "]\n错误:\n" + e, AppTitle,
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void clearShowItems(boolean keepSearchResult) {
		this.ui.resultList.clearSelection();
		((DefaultListModel<CodeLib2Element>) this.ui.resultList.getModel()).clear();
		this.attachmentClearTable();
		if (!keepSearchResult)
			this.searchResults.clear();
	}
	
	/**
	 * 选中结果数据条目.
	 */
	synchronized void itemSelect(final Object selectedValue) {
		
		this.currentItem = null;
		this.ui.contentTab.setSelectedComponent(this.ui.rTextScrollPane);
		
		if (selectedValue instanceof CodeLib2Element && this.ui.resultList.getSelectedIndices().length == 1) {
			
			CodeLib2Element item = (CodeLib2Element) selectedValue;
			this.ui.keyWordText.setText(item.getKeywords());
			this.ui.keyWordText.setEditable(true);
			try {
				this.ui.codeText.setSyntaxEditingStyle(this.getRSyntaxStyle(item.getFirstKeyword().toLowerCase()));
				this.ui.codeText.setCaretPosition(0);
				this.ui.codeText.setText(new String(item.getContent(), CodeLib2Element.DefaultCharsetEncode));
				this.ui.codeText.setEditable(true);
				
				this.ui.infoText.setText(item.showInfo());
				
				this.browserSetContent(DefaultBrowserContent);
				
				this.findText();
			} catch (UnsupportedEncodingException e) {
				log.error("编码失败.", e);
				JOptionPane.showMessageDialog(this.ui, e.getMessage(), AppTitle, JOptionPane.ERROR_MESSAGE);
			}
			
			this.attachmentClearTable();
			if (item.getAttachments() != null) {
				for (Attachment attachment : item.getAttachments()) {
					((DefaultTableModel) this.ui.attachmentTable.getModel()).addRow(new Object[]{
							attachment, attachment.getBinaryContent().length});
				}
			}
			this.ui.attachmentTable.updateUI();
			
			this.currentItem = item;
			
			if (isCurrentMarkdown() || isCurrentPlantUml())
				this.ui.contentTab.setSelectedComponent(this.ui.browserPanel);
		} else {
			this.ui.keyWordText.setText("");
			this.ui.keyWordText.setEditable(false);
			this.ui.codeText.setCaretPosition(0);
			this.ui.codeText.setText("");
			this.ui.codeText.setEditable(false);
			
			this.browserSetContent(DefaultBrowserContent);
			
			((DefaultTableModel) this.ui.attachmentTable.getModel()).getDataVector().removeAllElements();
		}
	}
	
	private static final Map<String, String> rSyntaxMap;
	
	static {
		rSyntaxMap = Colls.ofHashMap(
				"actionscript", SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT,
				"as", SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT,
				
				"asm", SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86,
				
				"bat", SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH,
				
				"bbcode", SyntaxConstants.SYNTAX_STYLE_BBCODE,
				"bb", SyntaxConstants.SYNTAX_STYLE_BBCODE,
				
				"c", SyntaxConstants.SYNTAX_STYLE_C,
				
				"clj", SyntaxConstants.SYNTAX_STYLE_CLOJURE,
				"clojure", SyntaxConstants.SYNTAX_STYLE_CLOJURE,
				
				"cpp", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS,
				"c++", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS,
				
				"cs", SyntaxConstants.SYNTAX_STYLE_CSHARP,
				"c#", SyntaxConstants.SYNTAX_STYLE_CSHARP,
				
				"css", SyntaxConstants.SYNTAX_STYLE_CSS,
				
				"csv", SyntaxConstants.SYNTAX_STYLE_CSV,
				
				"d", SyntaxConstants.SYNTAX_STYLE_D,
				
				"dart", SyntaxConstants.SYNTAX_STYLE_DART,
				
				"delphi", SyntaxConstants.SYNTAX_STYLE_DELPHI,
				"pas", SyntaxConstants.SYNTAX_STYLE_DELPHI,
				"pascal", SyntaxConstants.SYNTAX_STYLE_DELPHI,
				
				"dtd", SyntaxConstants.SYNTAX_STYLE_DTD,
				
				"fortran", SyntaxConstants.SYNTAX_STYLE_FORTRAN,
				
				"go", SyntaxConstants.SYNTAX_STYLE_GO,
				
				"groovy", SyntaxConstants.SYNTAX_STYLE_GROOVY,
				"gsp", SyntaxConstants.SYNTAX_STYLE_GROOVY,
				
				"htm", SyntaxConstants.SYNTAX_STYLE_HTML,
				"html", SyntaxConstants.SYNTAX_STYLE_HTML,
				
				"java", SyntaxConstants.SYNTAX_STYLE_JAVA,
				
				"javascript", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT,
				"js", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT,
				
				"json", SyntaxConstants.SYNTAX_STYLE_JSON,
				
				"jsp", SyntaxConstants.SYNTAX_STYLE_JSP,
				
				"latex", SyntaxConstants.SYNTAX_STYLE_LATEX,
				"tex", SyntaxConstants.SYNTAX_STYLE_LATEX,
				
				"less", SyntaxConstants.SYNTAX_STYLE_LESS,
				
				"lisp", SyntaxConstants.SYNTAX_STYLE_LISP,
				
				"lua", SyntaxConstants.SYNTAX_STYLE_LUA,
				
				"markdown", SyntaxConstants.SYNTAX_STYLE_PYTHON,
				"md", SyntaxConstants.SYNTAX_STYLE_PYTHON,
				
				"makefile", SyntaxConstants.SYNTAX_STYLE_MAKEFILE,
				"make", SyntaxConstants.SYNTAX_STYLE_MAKEFILE,
				
				"mq4", SyntaxConstants.SYNTAX_STYLE_C,
				"mq5", SyntaxConstants.SYNTAX_STYLE_C,
				"mqh", SyntaxConstants.SYNTAX_STYLE_C,
				
				"mxml", SyntaxConstants.SYNTAX_STYLE_MXML,
				"mx", SyntaxConstants.SYNTAX_STYLE_MXML,
				
				"nsi", SyntaxConstants.SYNTAX_STYLE_NSIS,
				"nsis", SyntaxConstants.SYNTAX_STYLE_NSIS,
				
				"perl", SyntaxConstants.SYNTAX_STYLE_PERL,
				
				"php", SyntaxConstants.SYNTAX_STYLE_PHP,
				
				"prolog", SyntaxConstants.SYNTAX_STYLE_CLOJURE,
				
				"properties", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
				"prop", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE,
				
				"python", SyntaxConstants.SYNTAX_STYLE_PYTHON,
				"py", SyntaxConstants.SYNTAX_STYLE_PYTHON,
				
				"puml", SyntaxConstants.SYNTAX_STYLE_CSS,
				
				"ruby", SyntaxConstants.SYNTAX_STYLE_RUBY,
				
				"sas", SyntaxConstants.SYNTAX_STYLE_SAS,
				
				"scala", SyntaxConstants.SYNTAX_STYLE_SCALA,
				
				"scheme", SyntaxConstants.SYNTAX_STYLE_LISP,
				"scm", SyntaxConstants.SYNTAX_STYLE_LISP,
				"ss", SyntaxConstants.SYNTAX_STYLE_LISP,
				
				"sh", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL,
				"shell", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL,
				
				"sql", SyntaxConstants.SYNTAX_STYLE_SQL,
				
				"tcl", SyntaxConstants.SYNTAX_STYLE_TCL,
				
				"ts", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT,
				
				"vb", SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC,
				"vbs", SyntaxConstants.SYNTAX_STYLE_VISUAL_BASIC,
				
				"xml", SyntaxConstants.SYNTAX_STYLE_XML,
				"xsd", SyntaxConstants.SYNTAX_STYLE_XML,
				"xsl", SyntaxConstants.SYNTAX_STYLE_XML
		);
	}
	
	/**
	 * 根据语法关键字取 RSyntaxTextArea 的语法样式.
	 *
	 * @param syntaxKeyword 语法关键字.
	 */
	private String getRSyntaxStyle(String syntaxKeyword) {
		return rSyntaxMap.getOrDefault(syntaxKeyword, SyntaxConstants.SYNTAX_STYLE_NONE);
	}
	
	/**
	 * 保存当前编辑条目的关键字.
	 */
	private void saveCurrentItemKeywords() {
		
		if (this.currentItem != null) {
			this.saveState.changeState(SaveStateManager.State.MODIFIED);
			
			this.currentItem.setKeywords(this.ui.keyWordText.getText());
			this.ui.codeText.setSyntaxEditingStyle(this.getRSyntaxStyle(this.currentItem.getFirstKeyword().toLowerCase()));
			
			this.ui.resultList.repaint();
		}
	}
	
	/**
	 * 代码编辑区需要保存代码的情况, 将代码保存到当前编辑的 Element.
	 */
	private void saveCurrentItemCodeContent() {
		
		try {
			if (this.currentItem != null) {
				this.saveState.changeState(SaveStateManager.State.MODIFIED);
				
				byte[] newContent = this.ui.codeText.getText().getBytes(
						CodeLib2Element.DefaultCharsetEncode);
				this.currentItem.setContent(newContent);
			}
		} catch (UnsupportedEncodingException e) {
			log.error("转码失败.", e);
		}
	}
	
	@Override
	public boolean onSaveStateChanged(SaveStateManager.State oldState, SaveStateManager.State newState) {
		
		switch (newState) {
			case NEW:
				this.currentItem = null;
				this.eles.clear();
				this.file = null;
				this.ui.setAppTitle(UIController.AppTitle);
				break;
			case MODIFIED:
				String title = "* " + UIController.AppTitle;
				if (this.file != null) {
					title += " - " + file.getAbsolutePath();
				}
				this.ui.setAppTitle(title);
				break;
			case SAVED:
				title = UIController.AppTitle;
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
	public synchronized void onGetSearchResult(final String keyword, final CodeLib2Element ele, int matchDegree) {
		
		if (keyword == this.currentKeyword)
			this.searchResults.add(new SearchResult(ele, matchDegree));
	}
	
	@Override
	public void onSearchComplete(final String keyword, Comparator c) {
		
		final Object[] resultsArray = this.searchResults.toArray();
		if (c != null)
			Arrays.sort(resultsArray, c);
		else
			Arrays.sort(resultsArray, (o1, o2) -> {
				SearchResult sr1 = (SearchResult) o1;
				SearchResult sr2 = (SearchResult) o2;
				if (keyword.trim().equals("*"))
					return sr1.ele.getKeywords().compareTo(sr2.ele.getKeywords());
				else {
					int mdd = sr2.matchDegree - sr1.matchDegree;
					if (mdd == 0) {
						return sr2.ele.getUpdateTime().compareTo(sr1.ele.getUpdateTime());
					} else
						return mdd;
				}
			});
		
		// update status bar
		SwingUtilities.invokeLater(() -> {
			int displayLimit = 500;
			
			if (resultsArray.length > displayLimit
					&&
					"*".equals(currentKeyword.trim().split("[\\s,]+")[0])
					&&
					JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(ui,
							"结果条目 " + resultsArray.length + " 条, 建议只展示部分结果以缩短渲染时间\n" +
									"选 \"是\" 只展示前 " + displayLimit + " 条, 选 \"否\" 全部展示",
							AppTitle,
							JOptionPane.YES_NO_OPTION)) {
				displayLimit = Integer.MAX_VALUE;
			}
			
			int count = 1;
			for (Object sortedResult : resultsArray) {
				if (count++ > displayLimit)
					break;
				((DefaultListModel<CodeLib2Element>) ui.resultList.getModel()).addElement(((SearchResult) sortedResult).ele);
			}
			
			String[] keywords = keyword.trim().split("[\\s,]+");
			if (keywords.length > 0 && keywords[keywords.length - 1].trim().length() > 0) {
				ui.findText.setText(keywords[keywords.length - 1].trim());
			} else {
				ui.findText.setText(null);
			}
			
			ui.resultList.repaint();
			
			if (currentKeyword.trim().length() > 0 && currentKeyword.split("[\\s,]+").length > 0) {
				if (orderType > 0)
					uiSetStatusBar(this.ui.orderType.getToolTipText());
				else
					uiSetStatusBar("搜索 [" + currentKeyword + "] 完成, 展示条目/全部结果="
							+ ui.resultList.getModel().getSize() + "/" + resultsArray.length);
				if (ui.resultList.getModel().getSize() > 0) {
					ui.resultList.setSelectedIndex(0);
				}
			} else {
				uiSetStatusBarReady();
			}
		});
	}
	
	/**
	 * 关闭前检查或询问以确定否要关闭.
	 */
	boolean uiDoClose() {
		if (this.checkForSave()) {
			this.unRegisterHotKey();
			this.searchEngine.close();
			try {
				ClassLoader cl = CodeLib2Frame.class.getClassLoader();
				if (cl instanceof Closeable)
					((Closeable) cl).close();
			} catch (Exception e) {
			}
			return true;
		} else
			return false;
	}
	
	/**
	 * 将内容编辑窗的内容复制到系统剪贴板.
	 */
	void uiCopyContentToClipboard() {
		
		Clipboard sysclip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String contentText = "";
		if (this.ui.rTextScrollPane == this.ui.contentTab.getSelectedComponent())
			contentText = this.ui.codeText.getText();
		else if (this.ui.browserPanel == this.ui.contentTab.getSelectedComponent()) {
			contentText = this.browserEngine.getDocument().getDocumentElement().getLastChild().getTextContent();
		}
		Transferable text = new StringSelection(contentText);
		sysclip.setContents(text, null);
	}
	
	private int orderType;
	
	/**
	 * 按条目尺寸倒排/按修改时间倒排/正排
	 */
	void reorderItems() {
		this.clearShowItems(true);
		Comparator<SearchResult> c = null;
		switch ((++orderType) % 3) {
			case 0:
				this.ui.orderType.setToolTipText("正排");
				break;
			case 1:
				c = (r1, r2) -> r2.ele.getSize() - r1.ele.getSize();
				this.ui.orderType.setToolTipText("按条目尺寸倒排");
				break;
			case 2:
				c = (r1, r2) -> r2.ele.getUpdateTime().compareTo(r1.ele.getUpdateTime());
				this.ui.orderType.setToolTipText("按修改时间倒排");
				break;
		}
		this.onSearchComplete(this.currentKeyword, c);
		this.uiSetStatusBar(this.ui.orderType.getToolTipText());
	}
	
	/**
	 * 设置状态栏内容.
	 */
	void uiSetStatusBar(String statusText) {
		
		this.ui.statusBar.setText(statusText);
	}
	
	/**
	 * 重置状态栏.
	 */
	void uiSetStatusBarReady() {
		
		this.ui.statusBar.setText("就绪 (共 " + this.eles.stream().filter(e -> !e.isDeleted()).count() + " 个条目)");
	}
	
	/**
	 * 代码编辑窗口的 url 点击事件.
	 */
	void onUrlClicked(URL url) {
		
		Desktop desktop = Desktop.getDesktop();
		if (desktop.isSupported(Action.BROWSE)) {
			try {
				desktop.browse(url.toURI());
			} catch (Exception e) {
				log.error("URL 打开失败. " + url, e);
			}
		}
	}
	
	/**
	 * 导入附件.
	 */
	void attachmentAdd() {
		
		CodeLib2Element attachToItem = this.currentItem;
		if (attachToItem != null
				&& JFileChooser.APPROVE_OPTION == this.ui.attachmentImportChooser.showOpenDialog(this.ui)) {
			File[] files = this.ui.attachmentImportChooser.getSelectedFiles();
			this.addAttachment(attachToItem, Arrays.asList(files));
		}
	}
	
	/**
	 * 将文件作为附件加入条目元素.
	 *
	 * @param attachToItem
	 * @param files
	 */
	private void addAttachment(CodeLib2Element attachToItem, List<File> files) {
		if (attachToItem == null || files == null)
			return;
		
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
						Files.readAllBytes(Paths.get(file.getAbsolutePath()))));
			} catch (Exception e) {
				log.error("导入附件失败. " + file.getAbsolutePath(), e);
				JOptionPane.showMessageDialog(this.ui, "导入附件失败.\n" + file.getName(), AppTitle,
						JOptionPane.ERROR_MESSAGE);
			}
		}
		
		if (attachments.size() > 0) {
			if (attachToItem.getAttachments() == null)
				attachToItem.setAttachments(new ArrayList<>());
			
			attachToItem.getAttachments().addAll(attachments);
			attachToItem.setUpdateTime(Instant.now());
			Collections.sort(attachToItem.getAttachments());
			
			this.saveState.changeState(SaveStateManager.State.MODIFIED);
			this.ui.resultList.repaint();
		}
		
		if (attachToItem == this.currentItem)
			this.addAttachmentToTable(attachments);
	}
	
	/**
	 * 清空附件列表.
	 */
	private void attachmentClearTable() {
		this.ui.attachmentTable.setModel(new DefaultTableModel(
				new Object[][]{
				},
				new String[]{
						"附件名", "附件大小(字节)"
				}
		) {
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
		});
	}
	
	/**
	 * 将 List<Attachment> 添加到附件列表.
	 *
	 * @param attachments
	 */
	private void addAttachmentToTable(List<Attachment> attachments) {
		
		DefaultTableModel dataModel = (DefaultTableModel) this.ui.attachmentTable.getModel();
		for (Attachment attachment : attachments) {
			dataModel.addRow(new Object[]{attachment, attachment.getBinaryContent().length});
		}
		this.ui.attachmentTable.updateUI();
	}
	
	/**
	 * 移除选定的附件.
	 */
	void attachmentRemove() {
		
		int[] selectedRows = this.ui.attachmentTable.getSelectedRows();
		if (selectedRows.length > 0) {
			if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui, "确认移除附件?", AppTitle,
					JOptionPane.YES_NO_OPTION)) {
				for (int i = selectedRows.length - 1; i > -1; i--) {
					this.currentItem.getAttachments().remove(
							this.ui.attachmentTable.getValueAt(selectedRows[i], 0));
					this.currentItem.setUpdateTime(Instant.now());
					((DefaultTableModel) this.ui.attachmentTable.getModel()).removeRow(selectedRows[i]);
				}
				this.saveState.changeState(SaveStateManager.State.MODIFIED);
				this.ui.resultList.repaint();
				this.ui.attachmentTable.updateUI();
			}
		}
	}
	
	/**
	 * 导出附件.
	 */
	void attachmentExport() {
		
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
					
					File file = new File(filepath);
					if (file.exists()
							&& JOptionPane.NO_OPTION == JOptionPane.showConfirmDialog(this.ui,
							"是否覆盖文件?\n" + filepath, AppTitle,
							JOptionPane.YES_NO_OPTION)) {
						continue;
					}
					
					try {
						FilesUtil.writeFile(file, attachment.getBinaryContent());
					} catch (IOException ex) {
						log.error("export error.", ex);
						JOptionPane.showMessageDialog(this.ui, "导出失败.\n" + attachment.getName(),
								AppTitle, JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		}
	}
	
	/**
	 * 导入文件.
	 */
	@SuppressWarnings("unchecked")
	void uiImportFile() {
		
		if (JFileChooser.APPROVE_OPTION == this.ui.zclImportChooser.showOpenDialog(this.ui)) {
			File[] files = this.ui.zclImportChooser.getSelectedFiles();
			
			// 这里用set保证元素不重复, 以便下面去重操作
			List<CodeLib2Element> readItems = new ArrayList<>();
			
			this.uiSetStatusBar("正在导入 ...");
			try {
				for (File tFile : files) {
					String fileExt = FilesUtil.getFileExtension(tFile);
					if (UIController.Extension.equals('.' + fileExt)) {
						Pair<DataHeader, Collection<CodeLib2Element>> data = DataHeader.readFromFile(tFile);
						readItems.addAll(data.getR());
					} else {
						CodeLib2Element ele = new CodeLib2Element();
						ele.setKeywords(fileExt + ", " + FilesUtil.getFileNameWithoutExtension(tFile));
						ele.setContent(Files.readAllBytes(Paths.get(tFile.getAbsolutePath())));
						readItems.add(ele);
					}
				}
				
				// 去除重复元素
				readItems.addAll(this.eles);
				this.eles.clear();
				this.eles.addAll(itemsUniq(readItems));
				Collections.sort(this.eles);
				
				this.clearShowItems(false);
				this.saveState.changeState(SaveStateManager.State.MODIFIED);
				this.uiSetStatusBar("成功导入");
				JOptionPane.showMessageDialog(this.ui, "导入成功\n现有 "
								+ this.eles.stream().filter(e -> !e.isDeleted()).count() + " 项", AppTitle,
						JOptionPane.INFORMATION_MESSAGE);
			} catch (Throwable t) {
				this.uiSetStatusBarReady();
				JOptionPane.showMessageDialog(this.ui, "导入失败\n失败详情请查看日志", AppTitle,
						JOptionPane.ERROR_MESSAGE);
				log.error("导入失败: ", t);
			}
			
			System.gc();
		}
	}
	
	/**
	 * 去除重复
	 */
	private Collection<CodeLib2Element> itemsUniq(Collection<CodeLib2Element> readItems) {
		return readItems.stream().collect(
				Collectors.toMap(
						CodeLib2Element::getId, e -> e,
						(e1, e2) -> {
							if (e1.getUpdateTime() == null)
								return e2;
							else if (e2.getUpdateTime() == null)
								return e1;
							else
								return e1.getUpdateTime().compareTo(e2.getUpdateTime()) < 0 ? e2 : e1;
						}
				)).values();
	}
	
	/**
	 * 查找(代码框和浏览器).
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
			findResult = org.fife.ui.rtextarea.
					SearchEngine.find(this.ui.codeText, this.findContext).getCount() > 0;
			if (!findResult) {
				if (this.findContext.getSearchForward()) {
					this.ui.codeText.setCaretPosition(0);
				} else {
					this.ui.codeText.setCaretPosition(this.ui.codeText.getText().length());
				}
				findResult = org.fife.ui.rtextarea.
						SearchEngine.find(this.ui.codeText, this.findContext).getCount() > 0;
			}
			
			//			findResult |= this.browserSearch(text);
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
	 * 搜索浏览器(搜索字段高亮显示).
	 * 有匹配则返回 true.
	 */
	private boolean browserSearch(String text) {
		Platform.runLater(() -> browserEngine.executeScript("search('" + text + "')"));
		return false;
	}
	
	private String browserContent;
	
	/**
	 * 设置浏览器展示的内容(异步).
	 */
	private void browserSetContent(final String content) {
		this.browserContent = content;
		Platform.runLater(() -> browserEngine.loadContent(content));
	}
	
	/**
	 * 设置浏览器展示的页面(异步).
	 */
	private void browserSetUrl(final String url) {
		Platform.runLater(() -> browserEngine.load(url));
	}
	
	/**
	 * 浏览器执行脚本.
	 *
	 * @param js js 脚本.
	 * @return 执行结果.
	 */
	private Object browserExecuteScript(final String js) {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<Object> result = new AtomicReference<>();
		Platform.runLater(() -> {
			try {
				result.set(browserEngine.executeScript(js));
				System.out.println(browserEngine.getDocument().getDocumentElement().getTextContent());
			} catch (Throwable t) {
				log.error("js execute failed.", t);
			} finally {
				latch.countDown();
			}
		});
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			return null;
		}
		return result.get();
	}
	
	private void browserOpenInExternal() {
		try {
			if (Strings.isNotBlank(this.browserContent)) {
				File tempFile = File.createTempFile("codelib2-", ".html");
				tempFile.deleteOnExit();
				Files.write(tempFile.toPath(), this.browserContent.getBytes());
				Desktop.getDesktop().open(tempFile);
			}
		} catch (Exception e) {
			log.error("browserOpenInExternal-exp", e);
		}
	}
	
	/**
	 * 页面搜索高亮脚本.
	 */
	private String pageSearchJs;
	
	private String getPageSearchJs() throws IOException {
		if (pageSearchJs == null) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(
					UIController.class.getResourceAsStream("/js/pageSearchCompress.js")))) {
				StringBuilder sbjs = new StringBuilder();
				int len;
				char[] buf = new char[100_000];
				while ((len = in.read(buf)) > 0) {
					sbjs.append(buf, 0, len);
				}
				pageSearchJs = sbjs.toString();
			}
		}
		return pageSearchJs;
	}
	
	/**
	 * 展示附件.
	 */
	void attachmentShow() {
		
		int selectedRow = ui.attachmentTable.getSelectedRow();
		if (selectedRow > -1) {
			Attachment attachment = (Attachment) ui.attachmentTable.getValueAt(selectedRow, 0);
			
			// recheck attachment type
			Attachment.ContentType oldContentType = attachment.getContentType();
			attachment.judgeContentType();
			Attachment.ContentType newContentType = attachment.getContentType();
			if (oldContentType != newContentType)
				this.saveState.changeState(SaveStateManager.State.MODIFIED);
			
			if (newContentType != Attachment.ContentType.Binary && attachment.getBinaryContent() != null) {
				ui.contentTab.setSelectedComponent(ui.browserPanel);
				try {
					String fileExtension = FilesUtil.getFileExtension(new File(attachment.getName()));
					if (newContentType == Attachment.ContentType.Img) {
						String html = String.format("<html><body><img src=\"data:image/%s;base64,%s\" /></body></html>",
								fileExtension.toLowerCase(), Base64.getEncoder().encodeToString(attachment.getBinaryContent()));
						this.browserSetContent(html);
					} else {
						String content = new String(attachment.getBinaryContent(), newContentType.getTextEncode());
						if (!"html".equals(fileExtension) && !"htm".equals(fileExtension)) {
							String html = "<html><head><meta http-equiv='Content-Type' content='text/html; charset="
									+ newContentType.getTextEncode()
									+ "'/></head><body>"
									+ content.replace("&", "&amp;")
									         .replace("\"", "&quot;").replace("'", "&#39;")
									         .replace("<", "&lt;").replace(">", "&gt;")
									         .replace("\r\n", "<br/>")
									         .replace("\r", "<br/>")
									         .replace("\n", "<br/>")
									         .replace(" ", "&nbsp;").replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
									+ "<script>" + getPageSearchJs() + "</script>"
									+ "</body></html>";
							this.browserSetContent(html);
						} else {
							int endIdx = content.lastIndexOf("</html>");
							if (endIdx < 0)
								this.browserSetContent(content);
							else {
								String sb = content.substring(0, endIdx)
										+ "<script>" + getPageSearchJs() + "</script></html>";
								this.browserSetContent(sb);
							}
						}
					}
				} catch (Exception e) {
					this.browserSetContent(e.toString());
				}
			}
		}
	}
	
	/**
	 * 打开附件.
	 */
	void attachmentOpen() {
		
		int selectedRow = this.ui.attachmentTable.getSelectedRow();
		if (selectedRow > -1) {
			Attachment attachment = (Attachment) this.ui.attachmentTable.getValueAt(selectedRow, 0);
			File tempFile;
			try {
				tempFile = File.createTempFile("codelib2-", "." + FilesUtil.getFileExtension(new File(attachment.getName())));
				Files.write(tempFile.toPath(), attachment.getBinaryContent());
				tempFile.deleteOnExit();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this.ui, "写入临时文件失败.", "打开文件", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try {
				Desktop.getDesktop().open(tempFile);
			} catch (IOException e) {
				if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this.ui,
						"打开文件失败, 要尝试用文本方式打开吗?\n" + tempFile.getAbsolutePath(), "打开文件", JOptionPane.YES_NO_OPTION)) {
					if (tempFile.renameTo(new File(tempFile.getParentFile(), tempFile.getName() + ".txt"))) {
						tempFile.deleteOnExit();
						try {
							Desktop.getDesktop().open(tempFile);
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(this.ui,
									tempFile.getAbsolutePath() + e1.getMessage(),
									"打开文本文件失败.", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}
		}
	}
	
	private boolean isCurrentPlantUml() {
		if (this.currentItem == null)
			return false;
		String type = this.currentItem.getFirstKeyword().toLowerCase();
		return Objects.equals(type, "puml");
	}
	
	/**
	 * 渲染 plantUml
	 */
	private void renderPlantUml() {
		String text = this.ui.codeText.getText();
		String html = plantUml2html(text);
		this.browserSetContent(html);
	}
	
	private String plantUml2html(String text) {
		SourceStringReader reader = new SourceStringReader(text);
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			reader.outputImage(out, new FileFormatOption(FileFormat.SVG));
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			return "";
		}
	}
	
	/**
	 * 当前选中节点是否 markdown
	 */
	private boolean isCurrentMarkdown() {
		if (this.currentItem == null)
			return false;
		String type = this.currentItem.getFirstKeyword().toLowerCase();
		return Objects.equals(type, "md") || Objects.equals(type, "markdown");
	}
	
	/**
	 * 渲染 markdown
	 */
	private void renderMarkdown() {
		String text = this.ui.codeText.getText();
		String html = markdown2html(text);
		html = "<html lang=\"en\"><head><style>\n" +
				"\tbody {\n" +
				"\t\tfont-family: PingFang SC Medium, Microsoft YaHei;\n" +
				"\t\tmargin-left: 40px;\n" +
				"\t\tmargin-right: 50px;\n" +
				"\t}\n" +
				"\ttable, table tr th, table tr td {\n" +
				"\t\tborder: 1px solid grey;\n" +
				"\t\tborder-collapse: collapse;\n" +
				"\t}\n" +
				"</style>\n" +
				"</head><body>"
				+ html + "</body></html>";
		this.browserSetContent(html);
	}
	
	private String markdown2html(String text) {
		Document doc = mdParser.parse(text);
		return mdRenderer.render(doc);
	}
}
