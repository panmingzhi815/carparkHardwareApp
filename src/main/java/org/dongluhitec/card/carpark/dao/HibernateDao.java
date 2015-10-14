package org.dongluhitec.card.carpark.dao;

import java.util.List;
import org.dongluhitec.card.carpark.domain.AbstractDomain;
import org.dongluhitec.card.carpark.domain.CardUsage;
import org.dongluhitec.card.carpark.domain.ConnectionUsage;
import org.dongluhitec.card.carpark.exception.DongluServiceException;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HibernateDao
{
    public static Logger LOGGER = LoggerFactory.getLogger(HibernateDao.class);
    private static SessionFactory sessionFactory;

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

    public void save(Object o)
    {
        LOGGER.debug("保存:{}", o.toString());
        Session session = getSession();
        try
        {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(o);
            transaction.commit();
        }
        catch (Exception e)
        {
            throw new DongluServiceException("保存" + o.toString() + "失败", e);
        }
        finally
        {
            session.close();
        }
    }

    public void delete(Class cls, Long id)
    {
        LOGGER.debug("删除 {} id {}", cls.getName(), id);
        Session session = getSession();
        try
        {
            Transaction transaction = session.beginTransaction();
            session.delete(session.get(cls, id));
            transaction.commit();
        }
        catch (Exception e)
        {
            throw new DongluServiceException("删除" + cls.getName() + " id " + id + "失败", e);
        }
        finally
        {
            session.close();
        }
    }

    public List list(Class<? extends AbstractDomain> cls, int start, int max)
    {
        LOGGER.debug("查询 {} 起始位置 {} 数量 {} ", cls.getName(), Integer.valueOf(start), Integer.valueOf(max));
        Session session = getSession();
        try
        {
            Criteria criteria = session.createCriteria(cls);
            criteria.addOrder(Order.desc("table_id"));
            criteria.setFirstResult(start);
            criteria.setMaxResults(max);
            return criteria.list();
        }
        catch (HibernateException e)
        {
            throw new DongluServiceException("查询" + cls.getName() + "失败", e);
        }
        finally
        {
            session.close();
        }
    }

    public void deleteLeft(Class<? extends AbstractDomain> cls, int left)
    {
        LOGGER.debug("删除 {} 只剩 {} ", cls.getName(), Integer.valueOf(left));
        Session session = getSession();
        try
        {
            Criteria criteria = session.createCriteria(cls);
            criteria.setProjection(Projections.max("table_id"));
            Long o = (Long)criteria.uniqueResult();
            if (o == null) {
                return;
            }
            Transaction transaction = session.beginTransaction();
            Query query = session.createQuery("delete from " + cls.getSimpleName() + " where table_id < " + (o.longValue() - left));
            query.executeUpdate();
            transaction.commit();
        }
        catch (HibernateException e)
        {
            throw new DongluServiceException("删除" + cls.getName() + "失败", e);
        }
        finally
        {
            session.close();
        }
    }
}
