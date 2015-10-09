package org.dongluhitec.card.carpark.plate;


public interface XinlutongJNA {

	public void openEx(String ip, XinlutongCallback.XinlutongResult xinluweiResult);

	public void closeEx();

	public void tigger(String ip);

}
