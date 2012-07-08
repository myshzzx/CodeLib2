
package mysh.codelib2.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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

			CodeLib2Element ele = null;
			boolean result;

			while (!this.isInterrupted() && searchTargetIndex.get() < targetLib.size()) {
				result = false;

				try {
					ele = targetLib.get(searchTargetIndex.getAndIncrement());

					for (String key : this.keys) {
						result |= ele.getKeywords().toLowerCase().contains(key);
						if (result)
							break;
					}

					if (!result) {
						for (String key : this.keys) {
							result |= ByteUtil.findStringIndexIgnoreCase(
									ele.getContent(),
									CodeLib2Element.DefaultCharsetEncode,
									0, key) > -1;
							if (result)
								break;
						}
					}

					if (!result && ele.getAttachments() != null) {
						String attachementEncode;
						AttachmentSearch: for (Attachment attachment : ele.getAttachments()) {
							attachementEncode = attachment.getContentType().getTextEncode();
							if (attachementEncode != null) {
								for (String key : this.keys) {
									result |= ByteUtil.findStringIndexIgnoreCase(
											ele.getContent(),
											attachementEncode, 0,
											key) > -1;
									if (result)
										break AttachmentSearch;
								}
							}
						}
					}

					if (!this.isInterrupted() && result)
						resultCatcher.onGetSearchResult(this.keyword, ele);
					else
						break;
				} catch (IndexOutOfBoundsException outOfBoundsEx) {
					break;
				} catch (Exception e) {
					log.error("搜索失败, [keyword: " + this.keyword + ", element: "
							+ ele + "]", e);
				}
			}
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
	private final Queue<SearchTask> runningTasks = new LinkedList<>();

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
	 * 搜索关键字.
	 * 
	 * @param keyword
	 */
	public void search(String keyword) {

		this.stopCurrentSearch();

		String[] keys = keyword.trim().toLowerCase().split("\\s,");
		if (keyword == null || keys == null || keyword.length() == 0 || keys.length == 0
				|| keys[0].length() == 0)
			throw new IllegalArgumentException("无效关键字: " + keyword);

		SearchTask task;
		for (int i = 0; i < ThreadCount; i++) {
			task = new SearchTask(keyword, keys);
			this.runningTasks.add(task);
			task.start();
		}

	}

	/**
	 * 停止当前搜索.
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
