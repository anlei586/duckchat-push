package com.zaly.push.pns;

import com.akaxin.platform.common.utils.GsonUtils;

/**
 * 通过http发送push，需要这个push的结果信息
 * 
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2018-05-04 18:13:02
 */
public class PushResult {
	private boolean success;
	private Object resMsg;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Object getResMsg() {
		return resMsg;
	}

	public void setResMsg(Object resMsg) {
		this.resMsg = resMsg;
	}

	public String toString() {
		return GsonUtils.toJson(this);
	}

}
