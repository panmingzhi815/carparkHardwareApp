package org.dongluhitec.card.carpark.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by panmingzhi815 on 2015/10/9 0009.
 */
public class Config implements Serializable {

    private String receiveIp;
    private Integer receivePort;
    private String gangtingName;
    private String ad;
    private Integer validateTimeLength;

    private List<LinkDevice> linkDeviceList = new ArrayList<>();

    public Config(String receiveIp, Integer receivePort, String gangtingName, String ad, Integer validateTimeLength) {
        this.receiveIp = receiveIp;
        this.receivePort = receivePort;
        this.gangtingName = gangtingName;
        this.ad = ad;
        this.validateTimeLength = validateTimeLength;
    }

    public String getReceiveIp() {
        return receiveIp;
    }

    public void setReceiveIp(String receiveIp) {
        this.receiveIp = receiveIp;
    }

    public Integer getReceivePort() {
        return receivePort;
    }

    public void setReceivePort(Integer receivePort) {
        this.receivePort = receivePort;
    }

    public String getGangtingName() {
        return gangtingName;
    }

    public void setGangtingName(String gangtingName) {
        this.gangtingName = gangtingName;
    }

    public String getAd() {
        return ad;
    }

    public void setAd(String ad) {
        this.ad = ad;
    }

    public Integer getValidateTimeLength() {
        return validateTimeLength;
    }

    public void setValidateTimeLength(Integer validateTimeLength) {
        this.validateTimeLength = validateTimeLength;
    }

    public List<LinkDevice> getLinkDeviceList() {
        return linkDeviceList;
    }

    public void setLinkDeviceList(List<LinkDevice> linkDeviceList) {
        this.linkDeviceList.clear();
        this.linkDeviceList.addAll(linkDeviceList);
    }
}
