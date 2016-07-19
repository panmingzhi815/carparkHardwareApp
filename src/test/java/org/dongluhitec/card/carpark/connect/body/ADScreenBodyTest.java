package org.dongluhitec.card.carpark.connect.body;

import org.dongluhitec.card.carpark.connect.*;
import org.dongluhitec.card.carpark.connect.util.ByteUtils;
import org.dongluhitec.card.carpark.connect.util.SerialDeviceAddress;
import org.junit.Test;

/**
 * Created by xiaopan on 2016/5/30.
 */
public class ADScreenBodyTest {

    @Test
    public void test(){
        ADScreenBody adScreenBody = new ADScreenBody();
        adScreenBody.setText("天津海吉星欢迎您");

        SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
        serialDeviceAddress.setAddress(1,1);
        MessageHeader messageHeader = new MessageHeader(serialDeviceAddress, DirectonType.请求, MessageConstance.Message_AD, ADScreenBody.LENGTH);
        Message<ADScreenBody> messageBodyMessage = new Message<>(messageHeader,adScreenBody);

        System.out.println(ByteUtils.byteArrayToHexString(messageBodyMessage.toBytes()));
    }

}