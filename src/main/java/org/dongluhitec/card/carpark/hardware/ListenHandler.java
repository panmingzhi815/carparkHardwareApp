package org.dongluhitec.card.carpark.hardware;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dongluhitec.card.carpark.connect.body.OpenDoorEnum;
import org.dongluhitec.card.carpark.domain.ConnectionDirection;
import org.dongluhitec.card.carpark.model.Device;
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.ui.LinkDevice;
import org.dongluhitec.card.carpark.ui.controller.DongluCarparkAppController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 监听对接方的控制命令
 * Created by xiaopan on 2015/10/28 0028.
 */
public class ListenHandler extends IoHandlerAdapter {

    private static  final Logger LOGGER = LoggerFactory.getLogger(ListenHandler.class);

    private final ExecutorService newSingleThreadExecutor;
    private final MessageHardware messageService;

    public ListenHandler(MessageHardware messageService) {
        this.messageService = messageService;
        this.newSingleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void messageReceived(final IoSession session, Object message) throws Exception {
        String checkSubpackage = HardwareUtil.checkSubpackage(session, message);
        if(checkSubpackage == null){
            return;
        }

        WebMessage wm = new WebMessage(checkSubpackage);

        final Document dom = DocumentHelper.parseText(wm.getContent());
        final Element rootElement = dom.getRootElement();

        HardwareUtil.saveConnectionUsage(ConnectionDirection.接收,wm.getType().name(),wm.toString());

        if(wm.getType() == WebMessageType.成功){
            HardwareUtil.responseResult(dom);
            return;
        }

        if(wm.getType() == WebMessageType.广告){
            newSingleThreadExecutor.submit(() -> {
                try{
                    String deviceName = rootElement.element("device").element("deviceName").getTextTrim();
                    String ad = rootElement.element("ad").getTextTrim();

                    Config config = DongluCarparkAppController.config;
                    List<LinkDevice> collect = config.getLinkDeviceList().stream().filter(filter -> filter.getDeviceName().equals(deviceName)).collect(Collectors.toList());
                    if(!collect.isEmpty()){
                        LinkDevice linkDevice = collect.get(0);
                        Device device = toDevice(linkDevice);

                        ListenableFuture<Boolean> setAD = messageService.setAD(device, ad);
                        setAD.get();
                        HardwareUtil.responseDeviceControl(session);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            });

        }

        if(wm.getType() == WebMessageType.设备控制){
            newSingleThreadExecutor.submit(() -> {
                try {
                    Element controlElement = rootElement.element("control");
                    Element element = rootElement.element("device");

                    String deviceName = element.element("deviceName").getTextTrim();
                    String gate = controlElement.element("gate").getTextTrim();
                    String Insidevoice, Outsidevoice, InsideScreen, OutsideScreen, InsideScreenAndVoiceData, OutsideScreenAndVoiceData;
                    if (controlElement.element("insideVoice") == null) {
                        Insidevoice = controlElement.element("InsideVoice").getTextTrim();
                        Outsidevoice = controlElement.element("OutsideVoice").getTextTrim();
                        InsideScreen = controlElement.element("InsideScreen").getTextTrim();
                        OutsideScreen = controlElement.element("OutsideScreen").getTextTrim();
                        InsideScreenAndVoiceData = controlElement.element("InsideScreenAndVoiceData").getTextTrim();
                        OutsideScreenAndVoiceData = controlElement.element("OutsideScreenAndVoiceData").getTextTrim();
                    } else {
                        Insidevoice = controlElement.element("insideVoice").getTextTrim();
                        Outsidevoice = controlElement.element("outsideVoice").getTextTrim();
                        InsideScreen = controlElement.element("insideScreen").getTextTrim();
                        OutsideScreen = controlElement.element("outsideScreen").getTextTrim();
                        InsideScreenAndVoiceData = controlElement.element("insideScreenAndVoiceData").getTextTrim();
                        OutsideScreenAndVoiceData = controlElement.element("outsideScreenAndVoiceData").getTextTrim();
                    }
                    Config config = DongluCarparkAppController.config;
                    List<LinkDevice> collect = config.getLinkDeviceList().stream().filter(filter -> filter.getDeviceName().equals(deviceName)).collect(Collectors.toList());
                    if (collect.isEmpty()) {
                        LOGGER.warn("未配置设备:{} 信息，暂不能接收请求", deviceName);
                        return;
                    }
                    Device device = toDevice(collect.get(0));
                    if (device == null) {
                        return;
                    }
                    if (InsideScreen.equals("true")) {
                        int voice = Insidevoice.equals("false") ? 1 : 9;
                        ListenableFuture<Boolean> carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 1, voice, 0, OpenDoorEnum.parse(gate), InsideScreenAndVoiceData);
                        Boolean boolean1 = carparkScreenVoiceDoor.get();
                        if (boolean1 == null) {
                            carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 1, voice, 0, OpenDoorEnum.parse(gate), InsideScreenAndVoiceData);
                            carparkScreenVoiceDoor.get();
                        }
                    }
                    if (OutsideScreen.equals("true")) {
                        int voice = Outsidevoice.equals("false") ? 1 : 9;
                        ListenableFuture<Boolean> carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 2, voice, 0, OpenDoorEnum.parse(gate), OutsideScreenAndVoiceData);
                        Boolean boolean1 = carparkScreenVoiceDoor.get();
                        if (boolean1 == null) {
                            carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 2, voice, 0, OpenDoorEnum.parse(gate), OutsideScreenAndVoiceData);
                            carparkScreenVoiceDoor.get();
                        }
                    }
                    HardwareUtil.responseDeviceControl(session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        // TODO Auto-generated method stub
        super.messageSent(session, message);
    }

    public Device toDevice(LinkDevice linkDevice){
        Device device = new Device();
        device.setAddress(linkDevice.getLinkAddress());
        device.setType(linkDevice.getLinkType());
        device.setArea(linkDevice.getDeviceAddress());
        device.setInoutType(linkDevice.getDeviceType());
        device.setName(linkDevice.getDeviceName());
        device.setSupportChinese("支持");
        device.setSupportInsideVoice("支持");
        device.setSupportOutsideVoice("支持");
        return device;
    }
}