package org.dongluhitec.card.carpark.hardware;

import org.dongluhitec.card.carpark.model.Device;

import java.util.List;

public interface WebService {
	
	public void sendSecretKey();
	
	public void responseSecretKey();
	
	public void sendDeviceInfo(List<Device> deviceList);
	
	public void responseResult();
	
	public void sendCardID(String cardID);
	
	public void responseControl();

}
