package cn.zyf.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class JMSQueueConsumer {

    public static void main(String[] args) {
        ConnectionFactory connectionFactory=
                new ActiveMQConnectionFactory
                        ("tcp://192.168.188.135:61616");
        Connection connection=null;
        try {

            connection=connectionFactory.createConnection();
            connection.start();

            Session session=connection.createSession
                    (Boolean.FALSE,Session.DUPS_OK_ACKNOWLEDGE); //延迟确认
            //创建目的地
            Destination destination=session.createQueue("myQueue");
            //创建消费者
            MessageConsumer consumer=session.createConsumer(destination);
            for(int i=0;i<10;i++) {
                TextMessage textMessage = (TextMessage) consumer.receive();
                System.out.println(textMessage.getText());
                if(i==8) {
                    textMessage.acknowledge();
                }
            }
//          session.commit();//表示消息被自动确认
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
