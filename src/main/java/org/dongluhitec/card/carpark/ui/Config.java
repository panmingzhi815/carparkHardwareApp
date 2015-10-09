package org.dongluhitec.card.carpark.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by panmingzhi815 on 2015/10/9 0009.
 */
public class Config implements Serializable {
    class LinkDevice implements Serializable{
        private String linkType;
        private String linkAddress;
        private String deviceAddress;
        private String plateIp;

        public String getLinkType() {
            return linkType;
        }

        public void setLinkType(String linkType) {
            this.linkType = linkType;
        }

        public String getLinkAddress() {
            return linkAddress;
        }

        public void setLinkAddress(String linkAddress) {
            this.linkAddress = linkAddress;
        }

        public String getDeviceAddress() {
            return deviceAddress;
        }

        public void setDeviceAddress(String deviceAddress) {
            this.deviceAddress = deviceAddress;
        }

        public String getPlateIp() {
            return plateIp;
        }

        public void setPlateIp(String plateIp) {
            this.plateIp = plateIp;
        }
    }

    private String receiveIp;
    private String receivePort;
    private String gangtingName;
    private String ad;
    private Long validateTimeLength;

    private List<LinkDevice> linkDeviceList = new ArrayList<>();

    public String getReceiveIp() {
        return receiveIp;
    }

    public void setReceiveIp(String receiveIp) {
        this.receiveIp = receiveIp;
    }

    public String getReceivePort() {
        return receivePort;
    }

    public void setReceivePort(String receivePort) {
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

    public Long getValidateTimeLength() {
        return validateTimeLength;
    }

    public void setValidateTimeLength(Long validateTimeLength) {
        this.validateTimeLength = validateTimeLength;
    }

    public List<LinkDevice> getLinkDeviceList() {
        return linkDeviceList;
    }

    public void setLinkDeviceList(List<LinkDevice> linkDeviceList) {
        this.linkDeviceList = linkDeviceList;
    }
}
