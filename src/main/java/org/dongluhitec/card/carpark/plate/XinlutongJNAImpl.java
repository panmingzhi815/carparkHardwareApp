package org.dongluhitec.card.carpark.plate;

import com.google.common.collect.Maps;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class XinlutongJNAImpl implements XinlutongJNA {

	private static final Logger LOGGER = LoggerFactory.getLogger(XinlutongJNAImpl.class);
	private static final String LIBRARY_NAME = "HvDevice";
	private static final XinlutongNativeInterface HvDevice;
	private static final Map<String,Pointer> closeHandlerMap = Maps.newHashMap();

	private final int ResultBufferSize = 60;
	/* 回调数据类型 */
	static int CALLBACK_TYPE_RECORD_PLATE			= (int)0xFFFF0001;
	static int CALLBACK_TYPE_RECORD_BIGIMAGE		= (int)0xFFFF0002;
	static int CALLBACK_TYPE_RECORD_SMALLIMAGE		= (int)0xFFFF0003;
	static int CALLBACK_TYPE_RECORD_INFOEND		= (int)0xFFFF0006;

	private Map<String,XinlutongCallback> callbackMap = new HashMap<>();
	private boolean inited = false;

	static {
		System.setProperty("jna.encoding", "GB2312" );
		String path = System.getProperty("user.dir") + File.separator + "native";
		NativeLibrary.addSearchPath(LIBRARY_NAME, path);
		HvDevice = (XinlutongNativeInterface) Native.loadLibrary(LIBRARY_NAME, XinlutongNativeInterface.class);
	}


	@Override
	public void openEx(final String ip,final XinlutongCallback.XinlutongResult xinlutongResult) {
		inited = true;
		LOGGER.info("开始准备实时接收{}的数据回调", ip);

		Pointer hw_pointer = HvDevice.HVAPI_OpenEx(ip, null);
		closeHandlerMap.put(ip, hw_pointer);

		if(callbackMap.get(ip) != null){
			return;
		}

		XinlutongCallback callback = new XinlutongCallback(xinlutongResult, ip, HvDevice);
		callbackMap.put(ip,callback);

		HvDevice.HVAPI_SetCallBackEx(hw_pointer, callback.plateNO, Pointer.NULL, 0, CALLBACK_TYPE_RECORD_PLATE, null);
		HvDevice.HVAPI_SetCallBackEx(hw_pointer, callback.bigImage, Pointer.NULL, 0, CALLBACK_TYPE_RECORD_BIGIMAGE, null);
		HvDevice.HVAPI_SetCallBackEx(hw_pointer, callback.smallImage, Pointer.NULL, 0, CALLBACK_TYPE_RECORD_SMALLIMAGE, null);
		HvDevice.HVAPI_SetCallBackEx(hw_pointer, callback.end, Pointer.NULL, 0, CALLBACK_TYPE_RECORD_INFOEND, null);
	}

	@Override
	public void closeAllEx() {
		inited = false;
		Set<Entry<String,Pointer>> entrySet = closeHandlerMap.entrySet();
		for (Entry<String,Pointer> entry : entrySet) {
			String ip = entry.getKey();
			Pointer value = entry.getValue();
			try{
				HvDevice.HVAPI_CloseEx(value);
				LOGGER.info("己停止接收设备{}的车牌信息回调",ip);
			}catch(Exception e){
				LOGGER.error("停止接收设备{}的车牌信息回调时发生异常：{}",ip,e.getMessage());
			}
		}
		callbackMap.clear();
		closeHandlerMap.clear();
	}

	@Override
	public void closeEx(String ip) {
		Pointer pointer = closeHandlerMap.get(ip);
		if(pointer != null){
			HvDevice.HVAPI_CloseEx(pointer);
		}
		callbackMap.remove(ip);
		closeHandlerMap.remove(ip);
	}


	@Override
	public void tigger(String ip) {
		if(!inited){
			return;
		}
		LOGGER.info("开始软件触发设备{}抓拍",ip);
		Pointer pointer = closeHandlerMap.get(ip);
		Memory memory = new Memory(ResultBufferSize);
		Pointer share = memory.share(0, ResultBufferSize);
		HvDevice.HVAPI_ExecCmdEx(pointer,"SoftTriggerCapture",share,ResultBufferSize,0);
	}

	public static void main(String[] args) {
		XinlutongJNA xinlutongJNA = new XinlutongJNAImpl();
		XinlutongCallback.XinlutongResult xinlutongResult = new XinlutongCallback.XinlutongResult() {
			
			@Override
			public void invok(String ip, int channel,String plateNO, byte[] bigImage, byte[] smallImage) {
				System.out.println(ip);
				System.out.println(plateNO);
				String filePath1 = "d:/image/" + System.currentTimeMillis()+"("+ plateNO+ ")_1.jpeg";
				String filePath2 = "d:/image/" + System.currentTimeMillis() +"("+ plateNO+ ")_2.bmp";
				try (FileOutputStream fos1 = new FileOutputStream(filePath1);
						FileOutputStream fos2 = new FileOutputStream(filePath2)) {
					fos1.write(bigImage);
					if(smallImage != null)
					fos2.write(smallImage);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
		};
		xinlutongJNA.openEx("192.168.1.159", xinlutongResult);
		xinlutongJNA.openEx("192.168.1.160", xinlutongResult);
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		xinlutongJNA.tigger("192.168.1.159");

		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
