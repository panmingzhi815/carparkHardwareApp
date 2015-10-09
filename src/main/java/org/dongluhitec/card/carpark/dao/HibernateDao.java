package org.dongluhitec.card.carpark.dao;

import org.dongluhitec.card.carpark.domain.AbstractDomain;
import org.dongluhitec.card.carpark.domain.CardUsage;
import org.dongluhitec.card.carpark.domain.ConnectionUsage;
import org.dongluhitec.card.carpark.exception.DongluServiceException;
import org.hibernate.*;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 数据库连接信息
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class HibernateDao {

    public static Logger LOGGER = LoggerFactory.getLogger(HibernateDao.class);

    private static SessionFactory sessionFactory;

    public SessionFactory initSessionFactory() {
        LOGGER.info("开始初始化数据库连接");
        try {
            Configuration cfg = new Configuration()
            .addAnnotatedClass(CardUsage.class)
            .addAnnotatedClass(ConnectionUsage.class)
            .setProperty("hibernate.connection.url", "jdbc:derby:database;create=true")
            .setProperty("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver")
            .setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect")
            .setProperty("hibernate.connection.c3p0.max_size", "5")
            .setProperty("hibernate.connection.c3p0.min_size", "1")
            .setProperty("hibernate.show_sql", "true")
            .setProperty("hibernate.hbm2ddl.auto", "update");

            StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
            ServiceRegistry serviceRegistry = standardServiceRegistryBuilder.applySettings(cfg.getProperties()).build();
            SessionFactory sessionFactory = cfg.buildSessionFactory(serviceRegistry);
            LOGGER.info("初始化数据库连接成功");
            return sessionFactory;
        } catch (HibernateException e) {
            throw new DongluServiceException("初始化数据库失败",e);
        }
    }

    public Session getSession() {
        if (sessionFactory == null) {
            sessionFactory = initSessionFactory();
        }
        return sessionFactory.openSession();
    }

    public void save(Object o){
        LOGGER.info("保存:{}",o.toString());
        Session session = getSession();
        try {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(o);
            transaction.commit();
        } catch (Exception e) {
            throw new DongluServiceException("保存 " + o.toString() + "失败",e);
        } finally {
            session.close();
        }
    }

    public void delete(Class cls,Long id){
        LOGGER.info("删除 {} id {}",cls.getName(),id);
        Session session = getSession();
        try {
            Transaction transaction = session.beginTransaction();
            session.delete(session.get(cls,id));
            transaction.commit();
        } catch (Exception e) {
            throw new DongluServiceException("删除 " + cls.getName() + " id " + id + "失败",e);
        } finally {
            session.close();
        }
    }

    public List list(Class<? extends AbstractDomain> cls, int start, int max){
        LOGGER.info("查询 {} 起始位置 {} 数量 {} ",cls.getName(),start,max);
        Session session = getSession();
        try {
            Criteria criteria = session.createCriteria(cls);
            criteria.setFirstResult(start);
            criteria.setMaxResults(max);
            return criteria.list();
        } catch (HibernateException e) {
            throw new DongluServiceException("查询"+cls.getName()+"列表失败",e);
        } finally {
            session.close();
        }
    }
}
