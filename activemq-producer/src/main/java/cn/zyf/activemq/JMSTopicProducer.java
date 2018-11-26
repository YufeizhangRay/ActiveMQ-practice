package cn.zyf.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;


public class JMSTopicProducer {

    public static void main(String[] args) {
        ConnectionFactory connectionFactory=
                new ActiveMQConnectionFactory
                        ("tcp://192.168.188.135:61616");
        Connection connection=null;
        try {

            connection=connectionFactory.createConnection();
            connection.start();

            Session session=connection.createSession(Boolean.TRUE,Session.AUTO_ACKNOWLEDGE);
            //创建目的地
            Destination destination=session.createTopic("myTopic");
            //创建发送者
            MessageProducer producer=session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            //创建需要发送的消息
            TextMessage message=session.createTextMessage("新的消息");

            //Text   Map  Bytes  Stream  Object

            producer.send(message);

            session.commit();
            session.rollback();

            session.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            if(connection!=null){
                try {
                    connection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
