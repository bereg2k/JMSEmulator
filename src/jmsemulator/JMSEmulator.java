/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmsemulator;

import com.ibm.mq.MQQueue;
import com.ibm.mq.jms.MQQueueConnection;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.mq.jms.MQQueueReceiver;
import com.ibm.mq.jms.MQQueueSender;
import com.ibm.mq.jms.MQQueueSession;
import com.ibm.msg.client.wmq.WMQConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

/**
 *
 * @author Apl-Satellite
 */
public class JMSEmulator {

    /**
     * @param args the command line arguments
     * @throws javax.jms.JMSException
     */
    public static void main(String[] args) throws JMSException {

        //инициализируем основные параметры для подключения MQ:
        String host = "localhost";
        int port = 1414;
        String mqQManager = "api_example";
        String mqQChannel = "SYSTEM.DEF.SVRCONN";
        String mqQIn = "MQ.Incoming";
        String mqQOut = "MQ.Outgoing";

        //Объявляем объекты для создания подключения и сессии внутри него,
        //объекты очередей и получателя-отправителя
        MQQueueConnection mqConn;
        MQQueueConnectionFactory mqCF;
        MQQueueSession mqQSession;
        MQQueue mqIn;
        MQQueue mqOut;
        MQQueueSender mqSender;
        MQQueueReceiver mqReceiver;
        MessageProducer replyProd;

        //задаем основные настройки подключения
        mqCF = new MQQueueConnectionFactory();
        mqCF.setHostName(host);
        mqCF.setPort(port);
        mqCF.setQueueManager(mqQManager);
        mqCF.setChannel(mqQChannel);
        mqCF.setTransportType(WMQConstants.TIME_TO_LIVE_UNLIMITED);

        //Устанавливаем соединение с сервером MQ:
        mqConn = (MQQueueConnection) mqCF.createQueueConnection();
        mqQSession = (MQQueueSession) mqConn.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);

        mqIn = (MQQueue) mqQSession.createQueue("SomeInQueue");
        mqOut = (MQQueue) mqQSession.createQueue("SomeOutQueue");

        mqSender = (MQQueueSender) mqQSession.createSender((Queue) mqOut);
        mqReceiver = (MQQueueReceiver) mqQSession.createReceiver((Queue) mqIn);

        replyProd = mqQSession.createProducer(null);
        replyProd.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        mqConn.start();

        MessageListener Listener = new MessageListener() {
            @Override
            public void onMessage(Message msg) {
                if (msg instanceof TextMessage) {
                    try {
                        TextMessage tMsg = (TextMessage) msg;
                        String msgText = ((TextMessage) msg).getText();
                        mqQSession.commit(); //Подтверждаем что мы прочитали это сообщение
                        sendAnswer(msg); //Отправляем ответное сообзение
                    } catch (JMSException ex) {
                        Logger.getLogger(JMSEmulator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

            public void sendAnswer(Message msg) throws JMSException {
                String xmlAns = "XML for response..."; //xmlAns–ответная xml
                TextMessage answer = mqQSession.createTextMessage(xmlAns); 
                answer.setJMSCorrelationID(msg.getJMSMessageID());
                mqSender.send(answer);
                mqQSession.commit();//Подтверждаем отправку сообщения
            }
        };

        mqReceiver.setMessageListener(Listener);
    }
}
