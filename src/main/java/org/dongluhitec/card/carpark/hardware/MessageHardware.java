package org.dongluhitec.card.carpark.hardware;

import com.google.common.util.concurrent.ListenableFuture;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.Device;

import java.util.Date;

public interface MessageHardware {

	/**
	 * 读当前记录
	 * @param device 操作设备
	 * @return ListenableFuture<CarparkNowRecord> 可监听的读卡记录
	 */
	ListenableFuture<CarparkNowRecord> carparkReadNowRecord(Device device);

	/**
	 * 控制显示屏,音量,字体
	 * @param device 操作设备
	 * @param screenID 屏ID
	 * @param voice 音量
	 * @param font 字体
	 * @param door 是否开闸
	 * @param text 显示文本
	 * @return ListenableFuture<Boolean> 可监听结果
	 */
	ListenableFuture<Boolean> carparkScreenVoiceDoor(Device device, int screenID, int voice, int font, int door, String text);

	/**
	 * 下发设备时间
	 * @param device 操作设备
	 * @param date 时间
	 */
	void setDateTime(Device device, Date date);
	
	/**
	 * 下发广告语
	 * @param device 操作设备
	 * @param adStr 广告词
	 * @return ListenableFuture<Boolean> 可监听结果
	 */
	ListenableFuture<Boolean> setAD(Device device, String adStr);

	/**
	 * 读版本号
	 * @param device 操作设备
	 * @return ListenableFuture<String> 可监听结果
	 */
	ListenableFuture<String> readVersion(Device device);
}
