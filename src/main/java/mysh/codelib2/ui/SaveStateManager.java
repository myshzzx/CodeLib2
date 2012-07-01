
package mysh.codelib2.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 描述及通知UI的保存状态.
 * 
 * @author Allen
 * 
 */
public class SaveStateManager {

	private List<StateObserver> observers = new CopyOnWriteArrayList<>();

	/**
	 * 当前状态.
	 */
	private State state;

	/**
	 * 状态描述符.
	 * 
	 * @author Allen
	 * 
	 */
	public static enum State {

		/**
		 * 新建.
		 */
		NEW, /**
		 * 修改.
		 */
		MODIFIED, /**
		 * 保存.
		 */
		SAVED;
	}

	/**
	 * 状态观察者. 接收状态更新.
	 * 
	 * @author Allen
	 * 
	 */
	public interface StateObserver {

		/**
		 * 观察者被通知状态更新, 观察者返回是否允许此次更新.
		 * 
		 * @param state
		 * @return
		 */
		boolean onSaveStateChanged(State oldState, State newState);
	}

	/**
	 * 构造器.
	 * 
	 * @param initState
	 */
	public SaveStateManager(State initState) {

		if (initState == null) {
			this.state = State.NEW;
		} else {
			this.state = initState;
		}

	}

	/**
	 * 取当前瞬时状态.
	 * 
	 * @return
	 */
	public State getState() {

		return this.state;
	}

	/**
	 * 状态变更. 返回操作结果.
	 * 
	 * @param newState
	 */
	public boolean changeState(State newState) {

		if (newState == null) {
			throw new NullPointerException();
		}

		boolean stateChangeFlag = true;

		for (StateObserver o : this.observers) {
			stateChangeFlag &= o.onSaveStateChanged(this.state, newState);
		}

		if (stateChangeFlag)
			this.state = newState;

		return stateChangeFlag;
	}

	/**
	 * 注册状态观察者.
	 * 
	 * @param o
	 */
	public void registStateObserver(StateObserver o) {

		this.observers.add(o);
	}
}
