package org.dongluhitec.card.carpark.dao;

import org.dongluhitec.card.carpark.domain.AbstractDomain;
import org.dongluhitec.card.carpark.domain.CardUsage;
import org.dongluhitec.card.carpark.domain.ConnectionUsage;
import org.dongluhitec.card.carpark.exception.DongluServiceException;
import org.hibernate.*;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class HibernateDao
{
    public static Logger LOGGER = LoggerFactory.getLogger(HibernateDao.class);
    private static SessionFactory sessionFactory;
    private static final Object synObj = new Object();
    private static AtomicLong cardUsageId = new AtomicLong();

    static
    {
        LOGGER.info("开始初始化数据库连接池");
        try
        {
            Configuration cfg = new Configuration().addAnnotatedClass(CardUsage.class).addAnnotatedClass(ConnectionUsage.class).setProperty("hibernate.connection.url", "jdbc:derby:database;create=true").setProperty("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver").setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect").setProperty("hibernate.connection.c3p0.max_size", "5").setProperty("hibernate.connection.c3p0.min_size", "1").setProperty("hibernate.show_sql", "true").setProperty("hibernate.hbm2ddl.auto", "update");

            StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
            ServiceRegistry serviceRegistry = standardServiceRegistryBuilder.applySettings(cfg.getProperties()).build();
            sessionFactory = cfg.buildSessionFactory(serviceRegistry);
            LOGGER.info("初始化数据库连接池成功");
        }
        catch (HibernateException e)
        {
            throw new DongluServiceException("初始化数据库连接池错误", e);
        }
    }

    public Session getSession()
    {
        return sessionFactory.openSession();
    }

    private void save(Object o)
    {
        try (Session session = getSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(o);
            transaction.commit();

            LOGGER.debug("保存:{} 成功", o);
        } catch (Exception e) {
            throw new DongluServiceException("保存" + o.toString() + "失败", e);
        }
    }

    public void saveCardUsage(CardUsage o)
    {
        synchronized (synObj){
            Long max = max(CardUsage.class);
            o.setTable_id(max + 1);

            Long count = count(CardUsage.class);
            if(count > 200){
                deleteAll(CardUsage.class);
            }

            save(o);
        }
    }

    public void saveConnectionUsage(ConnectionUsage o)
    {
        synchronized (synObj){
            Long max = max(ConnectionUsage.class);
            o.setTable_id(max+1);

            if(max > 200){
                deleteAll(ConnectionUsage.class);
            }

            save(o);
        }
    }

    public void delete(Class<? extends AbstractDomain> cls, Long id)
    {
        try (Session session = getSession()) {
            Transaction transaction = session.beginTransaction();
            session.delete(session.get(cls, id));
            transaction.commit();
            LOGGER.debug("删除 {} id {} 成功", cls.getName(), id);
        } catch (Exception e) {
            throw new DongluServiceException("删除" + cls.getName() + " id " + id + "失败", e);
        }
    }

    public List list(Class<? extends AbstractDomain> cls, int start, int max)
    {

        try (Session session = getSession()) {
            Criteria criteria = session.createCriteria(cls);
            criteria.addOrder(Order.desc("table_id"));
            criteria.setFirstResult(start);
            criteria.setMaxResults(max);
            List list = criteria.list();
            LOGGER.debug("查询 {} 起始位置 {} 数量 {} 成功", cls.getName(), start, max);
            return list;
        } catch (HibernateException e) {
            throw new DongluServiceException("查询" + cls.getName() + " 列表失败", e);
        }
    }

    public Long count(Class<? extends AbstractDomain> cls)
    {
        try (Session session = getSession()) {
            Criteria criteria = session.createCriteria(cls);
            criteria.setProjection(Projections.count("table_id"));
            Object o = criteria.uniqueResult();
            long l = o == null ? 0 : (Long) o;
            LOGGER.debug("查询 {} 最大id {} 成功", cls.getName(),l);
            return l;
        } catch (HibernateException e) {
            throw new DongluServiceException("查询" + cls.getName() + " 最大id失败", e);
        }
    }

    public Long max(Class<? extends AbstractDomain> cls)
    {
        try (Session session = getSession()) {
            Criteria criteria = session.createCriteria(cls);
            criteria.setProjection(Projections.max("table_id"));
            Object o = criteria.uniqueResult();
            long l = o == null ? 0 : (Long) o;
            LOGGER.debug("查询 {} 最大id {} 成功", cls.getName(),l);
            return l;
        } catch (HibernateException e) {
            throw new DongluServiceException("查询" + cls.getName() + " 最大id失败", e);
        }
    }

    public void deleteAll(Class<? extends AbstractDomain> cls)
    {
        synchronized (synObj){
            try (Session session = getSession()) {
                Transaction transaction = session.beginTransaction();
                Query query = session.createQuery("delete from " + cls.getSimpleName());
                query.executeUpdate();
                transaction.commit();
                LOGGER.debug("删除 {} 全部记录成功 ", cls.getName());
            } catch (HibernateException e) {
                throw new DongluServiceException("删除" + cls.getName() + "失败", e);
            }
        }
    }
}
