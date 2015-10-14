package org.dongluhitec.card.carpark.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 卡片进出记录
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
@Entity
public class CardUsage extends AbstractDomain{

    private static final long serialVersionUID = 1L;

    @Column
    private String identifier;
    @Column
    private String deviceName;
    @Column
    private Date databaseTime;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Date getDatabaseTime() {
        return databaseTime;
    }

    public void setDatabaseTime(Date databaseTime) {
        this.databaseTime = databaseTime;
    }
}
