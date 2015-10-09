package org.dongluhitec.card.carpark.domain;

import org.dongluhitec.card.carpark.dao.HibernateDao;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ≤‚ ‘derby ˝æ›ø‚
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class AbstractHibernateDaoTest {

    @Test
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
        cardUsage.setDeviceTime(new Date());
        session.save(cardUsage);

        transaction.commit();

        Assert.assertNotNull(cardUsage.getTable_id());

        Criteria criteria = session.createCriteria(CardUsage.class);
        List<CardUsage> list = criteria.list();
        Assert.assertNotNull(list);
        Assert.assertEquals(1,list.size());
        Assert.assertEquals(cardUsage.getTable_id(),list.get(0).getTable_id());
    }

    @Test
    public void testSpeed(){
        long start = System.nanoTime();
        final HibernateDao abstractHibernateDao = new HibernateDao();
        for (int i = 0; i < 1; i++) {
            CardUsage cardUsage = new CardUsage();
            cardUsage.setIdentifier("46BE23F2");
            cardUsage.setDatabaseTime(new Date());
            cardUsage.setDeviceTime(new Date());
            abstractHibernateDao.save(cardUsage);
        }
        System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        start = System.nanoTime();
        List<? extends AbstractDomain> list = abstractHibernateDao.list(CardUsage.class, 0, 10000);
        list.forEach(each->abstractHibernateDao.delete(CardUsage.class,each.getTable_id()));
        System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

}