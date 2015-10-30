package org.dongluhitec.card.carpark.domain;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * 数据对象基类
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
@MappedSuperclass
public abstract class AbstractDomain implements Serializable {
    @Id
    protected Long table_id;

    public Long getTable_id() {
        return table_id;
    }

    public void setTable_id(Long table_id) {
        this.table_id = table_id;
    }
}
