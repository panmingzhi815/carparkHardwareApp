package org.dongluhitec.card.carpark.plate;


public interface XinlutongJNA {

	public void openEx(String ip, XinlutongCallback.XinlutongResult xinluweiResult);

	void closeAllEx();

	void closeEx(String ip);

	public void tigger(String ip);

}
