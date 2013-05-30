
package mysh.codelib2.model;

import mysh.annotation.ThreadSafe;
import mysh.util.ByteUtil;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 搜索引擎.<br/>
 * 一次只执行一个关键字搜索.
 *
 * @author Allen
 */
@ThreadSafe
public final class SearchEngine {

	private static final Logger log = Logger.getLogger(SearchEngine.class);

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
		 * 取得搜索结果.
		 *
		 * @param keyword 搜索关键字.
		 * @param ele     结果元素.
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
			this.setPriority(NORM_PRIORITY - 1);

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
			boolean keyResult; // 单个 key 匹配结果.
			while (!this.isInterrupted() && searchTargetIndex.get() < targetLib.size()) {

				try {
					ele = targetLib.get(searchTargetIndex.getAndIncrement());

					for (keyIndex = 0, keyResult = true; keyResult && keyIndex < keyLength; keyIndex++) {
						// 匹配关键字
						keyResult = this.lowerCaseKeys[keyIndex].length() == 0 ? true
								: ele.getKeywords().toLowerCase().contains(
								this.lowerCaseKeys[keyIndex]);

						// 匹配内容
						if (!keyResult) {
							keyResult = ByteUtil.findStringIndexIgnoreCase(ele.getContent(), 0,
									this.upperKeysByteArray[keyIndex],
									this.lowerKeysByteArray[keyIndex]) > -1;

							// // 匹配附件
							// if ((ele.getAttachments() != null) &&
							// !keyResult) {
							// String attachementEncode;
							// for (Attachment attachment :
							// ele.getAttachments()) {
							// attachementEncode =
							// attachment.getContentType().getTextEncode();
							// if (attachementEncode != null) {
							// keyResult = ByteUtil.findStringIndexIgnoreCase(
							// ele.getContent(),
							// attachementEncode, 0,
							// this.lowerCaseKeys[keyIndex]) > -1;
							//
							// if (keyResult)
							// break;
							// }
							// }
							// }
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
			} catch (Exception e) {
			}
		}

		;
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

//		启动定时 GC
		new Timer("gcTimer", true).schedule(new TimerTask() {
			@Override
			public void run() {
				System.gc();
			}
		}, 30 * 1000, 10 * 1000);
	}

	/**
	 * 添加搜索任务. 不阻塞.
	 *
	 * @param keyword
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
	 * @param keyword
	 * @throws Exception 创建搜索任务失败.
	 */
	private void search(String keyword) throws Exception {

		this.stopCurrentSearch();

		String[] upperCaseKeys = keyword.trim().toUpperCase().split("[\\s,]+");
		String[] lowerCaseKeys = keyword.trim().toLowerCase().split("[\\s,]+");
		if (lowerCaseKeys == null || keyword.length() == 0 || lowerCaseKeys.length == 0
				|| lowerCaseKeys[0].length() == 0) {
			// throw new IllegalArgumentException("无效关键字: " + keyword);
			this.resultCatcher.onSearchComplete(keyword);
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
