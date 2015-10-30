package org.dongluhitec.card.carpark.domain;

import org.dongluhitec.card.carpark.dao.HibernateDao;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 测试derby数据库
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class AbstractHibernateDaoTest {

    private static  final Logger LOGGER = LoggerFactory.getLogger(AbstractHibernateDaoTest.class);

    @Test
    @Ignore
    public void create(){
        long start = System.nanoTime();
        HibernateDao abstractHibernateDao = new HibernateDao();
        Session session = abstractHibernateDao.getSession();
        System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));

        Criteria listCriteria = session.createCriteria(CardUsage.class);
        List<CardUsage> list1 = listCriteria.list();
        for (CardUsage o : list1) {
            session.delete(o);
        }
        session.flush();

        CardUsage cardUsage = new CardUsage();
        Transaction transaction = session.beginTransaction();

        cardUsage.setIdentifier("46BE23F2");
        cardUsage.setDatabaseTime(new Date());
        cardUsage.setDeviceName("12345");
        session.save(cardUsage);

        transaction.commit();

        Assert.assertNotNull(cardUsage.getTable_id());

        Criteria criteria = session.createCriteria(CardUsage.class);
        List<CardUsage> list = criteria.list();
        Assert.assertNotNull(list);
        Assert.assertEquals(1,list.size());
        Assert.assertEquals(cardUsage.getTable_id(),list.get(0).getTable_id());
    }

    @Ignore
    @Test
    public void testSpeed(){
        final HibernateDao abstractHibernateDao = new HibernateDao();
        for (long i = 0; i < 2; i++) {
            CardUsage cardUsage = new CardUsage();
            cardUsage.setIdentifier("46BE23F2");
            cardUsage.setDatabaseTime(new Date());
            cardUsage.setDeviceName("12345");
            abstractHibernateDao.saveCardUsage(cardUsage);
        }
        List<? extends AbstractDomain> list = abstractHibernateDao.list(CardUsage.class, 0, 10000);
        list.forEach(each->abstractHibernateDao.delete(CardUsage.class,each.getTable_id()));
    }

}