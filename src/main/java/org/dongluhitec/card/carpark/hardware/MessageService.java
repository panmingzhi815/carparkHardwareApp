package org.dongluhitec.card.carpark.hardware;

import java.util.Date;

import com.google.common.util.concurrent.ListenableFuture;
import org.dongluhitec.card.carpark.connect.body.OpenDoorEnum;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.Device;

public interface MessageService {

	/**
	 * 开门
	 * @param device
	 * @param openDoorEnum
	 * @return
	 */
	ListenableFuture<Boolean> carparkOpenDoor(Device device, OpenDoorEnum openDoorEnum);

	/**
	 * 读当前记录
	 * @param device
	 * @return
	 */
	ListenableFuture<CarparkNowRecord> carparkReadNowRecord(Device device);

	/**
	 * 控制显示屏,音量,字体
	 * @param device
	 * @param screenID
	 * @param voice
	 * @param font
	 * @param door
	 * @param text
	 * @return
	 */
	ListenableFuture<Boolean> carparkScreenVoiceDoor(Device device, int screenID, int voice, int font, int door, String text);

	/**
	 * 下发设备时间
	 * @param device
	 * @param date
	 */
	void setDateTime(Device device, Date date);
	
	/**
	 * 下发广告语
	 * @param device
	 * @param adStr
	 * @return
	 */
	ListenableFuture<Boolean> setAD(Device device, String adStr);

	/**
	 * 读版本号
	 * @param device
	 * @return
	 */
	ListenableFuture<String> readVersion(Device device);
}
