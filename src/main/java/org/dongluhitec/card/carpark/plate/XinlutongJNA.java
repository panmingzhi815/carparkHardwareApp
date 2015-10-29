package org.dongluhitec.card.carpark.plate;


public interface XinlutongJNA {

	void openEx(String ip, XinlutongCallback.XinlutongResult xinluweiResult);

	void closeAllEx();

	void tigger(String ip);

}
