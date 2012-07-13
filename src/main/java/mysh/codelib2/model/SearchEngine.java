
package mysh.codelib2.model;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import mysh.annotation.NotThreadSafe;
import mysh.codelib2.model.CodeLib2Element.Attachment;
import mysh.util.ByteUtil;

import org.apache.log4j.Logger;

/**
 * 搜索引擎.<br/>
 * 非线程安全. 一次只执行一个关键字搜索.
 * 
 * @author Allen
 * 
 */
@NotThreadSafe
public final class SearchEngine {

	private static final Logger log = Logger.getLogger(SearchEngine.class);

	/**
	 * 搜索结果处理者.
	 * 
	 * @author Allen
	 * 
	 */
	public static interface ResultCatcher {

		/**
		 * 取得搜索结果.
		 * 
		 * @param keyword
		 *               搜索关键字.
		 * @param ele
		 *               结果元素.
		 */
		void onGetSearchResult(String keyword, CodeLib2Element ele);

		/**
		 * 搜索完成.
		 */
		void onSearchComplete(String keyword);
	}

	/**
	 * 搜索任务类.<br/>
	 * 由它执行搜索.
	 * 
	 * @author Allen
	 * 
	 */
	private final class SearchTask extends Thread {

		/**
		 * 关键字.
		 */
		private String keyword;

		/**
		 * 关键字 keyword 的小写分解.
		 */
		private String[] keys;

		/**
		 * 初始化方法.
		 * 
		 * @param keyword
		 *               关键字.
		 * @param lowerCaseKeys
		 *               关键字 keyword 的小写分解.
		 */
		public SearchTask(String keyword, String[] lowerCaseKeys) {

			this.keyword = keyword;
			this.keys = lowerCaseKeys;
		}

		@Override
		public void run() {

			final int keyLength = this.keys.length;
			if (keyLength < 1)
				return;

			CodeLib2Element ele = null;
			int keyIndex;
			boolean keyResult; // 单个 key 匹配结果.
			while (!this.isInterrupted() && searchTargetIndex.get() < targetLib.size()) {

				try {
					ele = targetLib.get(searchTargetIndex.getAndIncrement());

					for (keyIndex = 0, keyResult = true; keyResult && keyIndex < keyLength; keyIndex++) {
						// 匹配关键字
						keyResult = ele.getKeywords().toLowerCase().contains(this.keys[keyIndex]);

						// 匹配内容
						if (!keyResult) {
							keyResult = ByteUtil.findStringIndexIgnoreCase(ele.getContent(),
									CodeLib2Element.DefaultCharsetEncode, 0,
									this.keys[keyIndex]) > -1;

							// 匹配附件
							if ((ele.getAttachments() != null) && !keyResult) {
								String attachementEncode;
								for (Attachment attachment : ele.getAttachments()) {
									attachementEncode = attachment.getContentType().getTextEncode();
									if (attachementEncode != null) {
										keyResult = ByteUtil.findStringIndexIgnoreCase(
												ele.getContent(),
												attachementEncode, 0,
												this.keys[keyIndex]) > -1;

										if (keyResult)
											break;
									}
								}
							}
						}
					}

					if (this.isInterrupted())
						break;
					else if (keyResult)
						resultCatcher.onGetSearchResult(this.keyword, ele);
				} catch (IndexOutOfBoundsException outOfBoundsEx) {
					break;
				} catch (Exception e) {
					log.error("搜索失败, [keyword: " + this.keyword + ", element: " + ele + "]", e);
				}
			}

			SearchEngine.this.onSearchTaskComplete(this);
		}
	}

	/**
	 * 允许的工作线程数.
	 */
	private static final int ThreadCount = Runtime.getRuntime().availableProcessors();

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

		this.targetLib = targetLib;
		this.resultCatcher = resultCatcher;
	}

	/**
	 * 搜索关键字.<br/>
	 * * 表示展示全部.
	 * 
	 * @param keyword
	 */
	public void search(String keyword) {

		this.stopCurrentSearch();

		String[] keys = keyword.trim().toLowerCase().split("[\\s,]+");
		if (keys == null || keyword.length() == 0 || keys.length == 0 || keys[0].length() == 0) {
			// throw new IllegalArgumentException("无效关键字: " + keyword);
			this.resultCatcher.onSearchComplete(keyword);
			return;
		} else if (keys.length == 1 && "*".equals(keys[0])) {
			keys[0] = "";
		}

		SearchTask task;
		for (int i = 0; i < ThreadCount; i++) {
			task = new SearchTask(keyword, keys);
			this.runningTasks.add(task);
			task.start();
		}

	}

	/**
	 * 任务完成.
	 * 
	 * @param task
	 */
	private synchronized void onSearchTaskComplete(SearchTask task) {

		this.runningTasks.remove(task);

		if (this.runningTasks.size() == 0) {
			this.resultCatcher.onSearchComplete(task.keyword);
		}
	}

	/**
	 * 停止当前搜索.<br/>
	 * 阻塞直到操作完成.
	 */
	public void stopCurrentSearch() {

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
