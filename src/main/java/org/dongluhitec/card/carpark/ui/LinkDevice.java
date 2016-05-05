package org.dongluhitec.card.carpark.ui;

import org.dongluhitec.card.carpark.domain.AbstractDomain;
import org.dongluhitec.card.carpark.model.Device;

import java.io.Serializable;

/**
 * Created by panmingzhi815 on 2015/10/9 0009.
 */
public class LinkDevice extends AbstractDomain implements Serializable {

    public static final long serialVersionUID = 1L;

    private String linkType;
    private String linkAddress;
    private String deviceType;
    private String deviceName;
    private String deviceAddress;
    private String plateIp;
    private String deviceVersion;

    public LinkDevice() {
    }

    public LinkDevice(String linkType, String linkAddress, String deviceType,String deviceName,String deviceAddress, String plateIp) {
        this.linkType = linkType;
        this.linkAddress = linkAddress;
        this.deviceType = deviceType;
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.plateIp = plateIp;
    }

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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LinkDevice that = (LinkDevice) o;

        if (!linkType.equals(that.linkType)) return false;
        if (!linkAddress.equals(that.linkAddress)) return false;
        if (!deviceAddress.equals(that.deviceAddress)) return false;
        return !(plateIp != null ? !plateIp.equals(that.plateIp) : that.plateIp != null);

    }

    @Override
    public int hashCode() {
        int result = linkType.hashCode();
        result = 31 * result + linkAddress.hashCode();
        result = 31 * result + deviceAddress.hashCode();
        result = 31 * result + (plateIp != null ? plateIp.hashCode() : 0);
        return result;
    }

    public void setDeviceVersion(String deviceVersion) {
        this.deviceVersion = deviceVersion;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public Device toDevice(){
        Device device = new Device();
        device.setAddress(this.getLinkAddress());
        device.setType(this.getLinkType());
        device.setArea(this.getDeviceAddress());
        device.setInoutType(this.getDeviceType());
        device.setName(this.getDeviceName());
        device.setSupportChinese("支持");
        device.setSupportInsideVoice("支持");
        device.setSupportOutsideVoice("支持");
        return device;
    }
}
