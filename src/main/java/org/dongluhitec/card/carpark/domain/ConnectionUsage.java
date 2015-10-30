package org.dongluhitec.card.carpark.domain;

import javax.persistence.*;

/**
 * 对接信息记录
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
@Entity
public class ConnectionUsage extends AbstractDomain{

    private static final long serialVersionUID = 2L;

    @Column
    private ConnectionDirection direction;
    @Column
    private String shortContent;
    @Column
    @Lob
    @Basic(fetch = FetchType.EAGER)
    private byte[] LongContent;

    public ConnectionDirection getDirection() {
        return direction;
    }

    public void setDirection(ConnectionDirection direction) {
        this.direction = direction;
    }

    public String getShortContent() {
        return shortContent;
    }

    public void setShortContent(String shortContent) {
        this.shortContent = shortContent;
    }

    public byte[] getLongContent() {
        return LongContent;
    }

    public void setLongContent(byte[] longContent) {
        LongContent = longContent;
    }

    @Override
    public String toString() {
        return "ConnectionUsage{" +
                "table_id=" + table_id +
                ",direction=" + direction +
                ", shortContent='" + shortContent + '\'' +
                '}';
    }
}
