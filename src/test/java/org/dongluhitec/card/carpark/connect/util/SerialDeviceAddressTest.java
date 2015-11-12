package org.dongluhitec.card.carpark.connect.util;

import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by xiaopan on 2015-11-12.
 */
public class SerialDeviceAddressTest {

    @Test
    public void testSimple(){
        SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
        serialDeviceAddress.setAddress(255,1);
        Assert.assertEquals("255.1",serialDeviceAddress.getAddress());
    }

}