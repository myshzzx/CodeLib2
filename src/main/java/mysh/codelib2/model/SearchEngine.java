
package mysh.codelib2.model;

import mysh.collect.Colls;
import mysh.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static mysh.codelib2.model.CodeLib2Element.Attachment;

/**
 * 搜索引擎.<br/>
 * 一次只执行一个关键字搜索.
 * thread safe.
 *
 * @author Allen
 */
public final class SearchEngine implements Closeable {
	
	private static final Logger log = LoggerFactory.getLogger(SearchEngine.class);
	/**
	 * 搜索开始时间(毫秒).
	 */
	private volatile long searchStartTime;
	
	
	/**
	 * 搜索结果处理者.
	 *
	 * @author Allen
	 */
	public static interface ResultCatcher {
		
		/**
		 * 初始化(最小) 搜索结果匹配度.
		 */
		int InitMatchDegree = 0;
		
		/**
		 * 取得搜索结果.
		 *
		 * @param keyword     搜索关键字.
		 * @param ele         结果元素.
		 * @param matchDegree 匹配度(最小为 {@link ResultCatcher#InitMatchDegree}), 越大表示匹配程度越高.
		 */
		void onGetSearchResult(String keyword, CodeLib2Element ele, int matchDegree);
		
		/**
		 * 搜索完成.
		 */
		void onSearchComplete(String keyword, Comparator c);
	}
	
	/**
	 * 搜索任务类.<br/>
	 * 由它执行搜索.
	 *
	 * @author Allen
	 */
	private final class SearchTask extends Thread {
		
		/**
		 * 关键字.
		 */
		private String keyword;
		
		/**
		 * 关键字 keyword 的大写分解.
		 */
		private String[] upperCaseKeys;
		
		/**
		 * 关键字 keyword 的小写分解.
		 */
		private String[] lowerCaseKeys;
		
		/**
		 * 关键字 keyword 大写分解的默认编码数组(仅用于内容匹配).
		 */
		private byte[][] upperKeysByteArray;
		
		/**
		 * 关键字 keyword 小写分解的默认编码数组(仅用于内容匹配).
		 */
		private byte[][] lowerKeysByteArray;
		
		/**
		 * 创建搜索任务.<br/>
		 * 要求提供关键字分解是 关键字分解策略自由化 的考虑.<br/>
		 * 不检查关键字分解的结果是否符合参数含义, 后果由客户端代码承担.
		 *
		 * @param keyword       关键字.
		 * @param upperCaseKeys 关键字 keyword 的大写分解.
		 * @param lowerCaseKeys 关键字 keyword 的小写分解.
		 * @throws Exception 创建搜索任务失败.
		 */
		public SearchTask(String keyword, String[] upperCaseKeys, String[] lowerCaseKeys) throws Exception {
			
			this.setName("SearchTask Thread");
			this.setDaemon(true);
			this.setPriority(MIN_PRIORITY);
			
			if (upperCaseKeys.length != lowerCaseKeys.length) {
				throw new IllegalArgumentException();
			}
			
			this.keyword = keyword;
			this.upperCaseKeys = upperCaseKeys;
			this.lowerCaseKeys = lowerCaseKeys;
			
			this.upperKeysByteArray = new byte[upperCaseKeys.length][];
			this.lowerKeysByteArray = new byte[lowerCaseKeys.length][];
			for (int i = 0; i < upperCaseKeys.length; i++) {
				this.upperKeysByteArray[i] = upperCaseKeys[i].getBytes(CodeLib2Element.DefaultCharsetEncode);
				this.lowerKeysByteArray[i] = lowerCaseKeys[i].getBytes(CodeLib2Element.DefaultCharsetEncode);
			}
		}
		
		@Override
		public void run() {
			
			final int keyLength = this.lowerCaseKeys.length;
			if (keyLength < 1 || this.upperCaseKeys.length != this.lowerCaseKeys.length)
				return;
			
			CodeLib2Element ele = null;
			int keyIndex;
			boolean isSingleKeyMatches; // 单个 key 匹配结果.
			while (!this.isInterrupted() && searchTargetIndex.get() < targetLib.size()) {
				try {
					ele = targetLib.get(searchTargetIndex.getAndIncrement());
					if (ele.isDeleted())
						continue;
					
					// 单个 key 匹配
					for (keyIndex = 0, isSingleKeyMatches = true; isSingleKeyMatches && keyIndex < keyLength; keyIndex++) {
						// 匹配关键字
						isSingleKeyMatches = this.lowerCaseKeys[keyIndex].length() == 0
								                     || ele.getKeywords().toLowerCase().contains(this.lowerCaseKeys[keyIndex]);
						
						// 匹配内容
						if (!isSingleKeyMatches) {
							isSingleKeyMatches = Bytes.findStringIndexIgnoreCase(ele.getContent(), 0,
									this.upperKeysByteArray[keyIndex],
									this.lowerKeysByteArray[keyIndex]) > -1;
							
							// 匹配附件
							if (!isSingleKeyMatches && (ele.getAttachments() != null)) {
								String attachmentEncode;
								for (Attachment attachment : ele.getAttachments()) {
									// 附件名
									isSingleKeyMatches = attachment.getName().toLowerCase().contains(
											this.lowerCaseKeys[keyIndex]);
									
									// 附件内容
									attachmentEncode = attachment.getContentType().getTextEncode();
									if (!isSingleKeyMatches && attachmentEncode != null) {
										isSingleKeyMatches = Bytes.findStringIndexIgnoreCase(
												attachment.getBinaryContent(), attachmentEncode, 0, this.lowerCaseKeys[keyIndex])
												                     > -1;
									}
									
									if (isSingleKeyMatches)
										break;
								}
							}
						}
					}
					
					if (this.isInterrupted())
						break;
					else if (isSingleKeyMatches)
						resultCatcher.onGetSearchResult(this.keyword, ele,
								countMatchDegree(ele, this.upperKeysByteArray, this.lowerKeysByteArray));
				} catch (IndexOutOfBoundsException outOfBoundsEx) {
					break;
				} catch (Exception e) {
					log.error("搜索失败, [keyword: " + this.keyword + ", element: " + ele + "]", e);
				}
			}
			
			SearchEngine.this.onSearchTaskComplete(this);
		}
	}
	
	@Override
	public void close() {
		taskScheduler.interrupt();
	}
	
	/**
	 * 计算条目的关键字匹配度.
	 *
	 * @param ele                条目
	 * @param upperKeysByteArray 大写关键字字节数组
	 * @param lowerKeysByteArray 小写关键字字节数组
	 * @return 匹配度
	 */
	private static int countMatchDegree(CodeLib2Element ele, byte[][] upperKeysByteArray, byte[][] lowerKeysByteArray) {
		final int KeyWeight = 100;
		final int ContentWeightP = 5;
		final int AttachmentNameWeight = 50;
		
		int tMatchIndex;
		byte[] tSearchContent;
		int degree = 0;
		
		try {
			for (int i = 0; i < upperKeysByteArray.length && upperKeysByteArray[i].length > 0; i++) {
				// key
				tSearchContent = ele.getKeywords().getBytes(CodeLib2Element.DefaultCharsetEncode);
				tMatchIndex = Bytes.findStringIndexIgnoreCase(tSearchContent, 0, upperKeysByteArray[i], lowerKeysByteArray[i]);
				if (tMatchIndex > -1) {
					degree += KeyWeight * (tSearchContent.length - tMatchIndex) / tSearchContent.length;
				}
				
				// content
				tMatchIndex = -1;
				int tContentLimit = 10;
				while ((tMatchIndex = Bytes.findStringIndexIgnoreCase(ele.getContent(), tMatchIndex + 1,
						upperKeysByteArray[i], lowerKeysByteArray[i])) > -1) {
					degree += ContentWeightP;
					if (--tContentLimit < 1) break;
				}
				
				//attachment name
				int attachmentMatchCount = 0;
				if (Colls.isNotEmpty(ele.getAttachments())) {
					for (Attachment attachment : ele.getAttachments()) {
						if (Bytes.findStringIndexIgnoreCase(attachment.getName().getBytes(CodeLib2Element.DefaultCharsetEncode),
								0, upperKeysByteArray[i], lowerKeysByteArray[i]) > -1) {
							attachmentMatchCount++;
						}
					}
					degree += AttachmentNameWeight * attachmentMatchCount / ele.getAttachments().size();
				}
			}
		} catch (Exception e) {
			log.error("匹配度计算失败.", e);
		}
		
		//		log.debug(ele.getKeywords() + " : " + degree);
		return degree;
	}
	
	/**
	 * 允许的工作线程数.
	 */
	private static final int ThreadCount = Runtime.getRuntime().availableProcessors();
	
	/**
	 * 请求的任务队列.
	 */
	private final BlockingQueue<String> taskList = new LinkedBlockingQueue<>();
	
	/**
	 * 守护线程. 维护 taskList.
	 */
	private Thread taskScheduler = new Thread("SearchEngine TaskScheduler") {
		
		public void run() {
			
			String keyword, tempKey;
			
			try {
				while (true) {
					keyword = taskList.take();
					
					while (System.currentTimeMillis() < SearchEngine.this.searchStartTime) {
						Thread.sleep(100);
					}
					
					while ((tempKey = taskList.poll()) != null) {
						keyword = tempKey;
					}
					
					search(keyword);
				}
			} catch (InterruptedException e) {
			} catch (Exception e) {
				log.error("搜索守护线程异常退出", e);
			}
		}
	};
	
	/**
	 * 搜索目标.<br/>
	 * final - 用于 SearchTask.
	 */
	private final List<CodeLib2Element> targetLib;
	
	/**
	 * 搜索结果处理者..<br/>
	 * final - 用于 SearchTask.
	 */
	private final ResultCatcher resultCatcher;
	
	/**
	 * 当前正在执行的搜索任务.
	 */
	private final Queue<SearchTask> runningTasks = new ConcurrentLinkedQueue<>();
	
	/**
	 * 当前正在搜索的目标位置.
	 */
	private final AtomicInteger searchTargetIndex = new AtomicInteger(0);
	
	public SearchEngine(List<CodeLib2Element> targetLib, ResultCatcher resultCatcher) {
		
		if (targetLib == null || resultCatcher == null)
			throw new IllegalArgumentException();
		
		this.taskScheduler.setDaemon(true);
		this.taskScheduler.start();
		
		this.targetLib = targetLib;
		this.resultCatcher = resultCatcher;
	}
	
	/**
	 * 添加搜索任务. 不阻塞.
	 *
	 * @param keyword        搜索关键字.
	 * @param startTimeLimit 若在这个时间后没有新的搜索任务, 则执行此搜索.
	 */
	public void addSearchTask(String keyword, long startTimeLimit) {
		
		this.searchStartTime = startTimeLimit;
		this.taskList.add(keyword);
	}
	
	/**
	 * 搜索关键字.<br/>
	 * * 表示展示全部.
	 *
	 * @param keyword 搜索关键字.
	 * @throws Exception 创建搜索任务失败.
	 */
	private void search(String keyword) throws Exception {
		
		this.stopCurrentSearch();
		
		String[] upperCaseKeys = keyword.trim().toUpperCase().split("[\\s,]+");
		String[] lowerCaseKeys = keyword.trim().toLowerCase().split("[\\s,]+");
		if (keyword.length() == 0 || lowerCaseKeys.length == 0 || lowerCaseKeys[0].length() == 0) {
			// throw new IllegalArgumentException("无效关键字: " + keyword);
			this.resultCatcher.onSearchComplete(keyword, null);
			return;
		} else if (lowerCaseKeys.length == 1 && "*".equals(lowerCaseKeys[0])) {
			upperCaseKeys[0] = "";
			lowerCaseKeys[0] = "";
		}
		
		SearchTask task;
		for (int i = 0; i < ThreadCount; i++) {
			task = new SearchTask(keyword, upperCaseKeys, lowerCaseKeys);
			this.runningTasks.add(task);
			task.start();
		}
		
	}
	
	/**
	 * 任务完成.
	 */
	private synchronized void onSearchTaskComplete(SearchTask task) {
		
		this.runningTasks.remove(task);
		
		if (this.runningTasks.size() == 0) {
			this.resultCatcher.onSearchComplete(task.keyword, null);
		}
	}
	
	/**
	 * 停止当前搜索.<br/>
	 * 阻塞直到操作完成.
	 */
	private void stopCurrentSearch() {
		
		SearchTask task;
		while ((task = this.runningTasks.poll()) != null) {
			try {
				task.interrupt();
				task.join();
			} catch (Exception e) {
			}
		}
		
		this.searchTargetIndex.set(0);
	}
}
