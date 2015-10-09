package org.dongluhitec.card.carpark.domain;

import javax.persistence.*;
import java.util.Date;

/**
 * ¿¨Æ¬½ø³ö¼ÇÂ¼
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
@Entity
public class CardUsage extends AbstractDomain{
    @Column
    private String identifier;
    @Column
    private Date deviceTime;
    @Column
    private Date databaseTime;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Date getDeviceTime() {
        return deviceTime;
    }

    public void setDeviceTime(Date deviceTime) {
        this.deviceTime = deviceTime;
    }

    public Date getDatabaseTime() {
        return databaseTime;
    }

    public void setDatabaseTime(Date databaseTime) {
        this.databaseTime = databaseTime;
    }
}
